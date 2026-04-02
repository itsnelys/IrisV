package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IEntityTooltipProvider;
import net.opal.irisv.commun.utils.StorageUtils;
import net.opal.irisv.mixin.DestroyAccessor;
import net.opal.irisv.network.ClientDataCache;
import net.opal.irisv.option.ConfigOptions;

import java.util.ArrayList;
import java.util.List;

public class TooltipManager {

    private static BlockPos lastTargetPos = null;
    private static float visualProgress = 0f;
    private static long lastTimeMillis = System.currentTimeMillis();
    private static long finishTime = 0;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ConfigOptions.getInstance().enableBlockTooltipOverlay) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || mc.gameMode == null) return;

        // 1. DÉFINITION DES VECTEURS
        double reach = 5.0;
        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 viewVec = mc.player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(viewVec.scale(reach));

        // 2. RAYCAST DES ENTITÉS (Priorité maximale)
        AABB searchBox = mc.player.getBoundingBox().expandTowards(viewVec.scale(reach)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.player, eyePos, endPos, searchBox,
                entity -> !entity.isSpectator() && entity.isPickable(), reach * reach
        );

        if (entityHit != null && entityHit.getEntity() != null) {
            Entity target = entityHit.getEntity();

            // CAS A : C'est un item au sol (Logique groupée existante)
            if (target instanceof ItemEntity itemEntity) {
                handleItemEntity(event, mc, itemEntity);
                updateProgress(null, 0, null);
                return;
            }

            // CAS B : C'est une entité spéciale (ArmorStand, Frame, EndCrystal, etc.)
            if (handleSpecialEntity(event, mc, target)) {
                updateProgress(null, 0, null);
                return;
            }
        }

        // 3. RAYCAST DES BLOCS (Si aucune entité trouvée)
        HitResult hitResult = mc.level.clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.OUTLINE,
                mc.player.isUnderWater() || mc.player.isInLava() ? ClipContext.Fluid.NONE : ClipContext.Fluid.ANY,
                mc.player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            updateProgress(null, 0, null);
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos pos = blockHit.getBlockPos();
        BlockState originalState = mc.level.getBlockState(pos);

        // --- SÉCURITÉ DESTRUCTION ---
        var gameMode = (DestroyAccessor) mc.gameMode;
        float progress = gameMode.getDestroyProgress();

        if (originalState.isAir() || progress >= 1.0f) {
            ClientDataCache.remove(pos);
            updateProgress(null, 0, null);
            return;
        }

        BlockPos targetPos = StorageUtils.getActualTarget(mc.level, pos, originalState);
        CompoundTag data = ClientDataCache.get(targetPos);
        BlockState finalState = (targetPos.equals(pos)) ? originalState : mc.level.getBlockState(targetPos);
        BlockEntity finalBE = (targetPos.equals(pos)) ? mc.level.getBlockEntity(pos) : mc.level.getBlockEntity(targetPos);

        updateProgress(pos, progress, originalState);

        // --- INITIALISATION ACCESSOR BLOC ---
        IBlockAccessor accessor = new IBlockAccessor(
                mc.level, mc.player, targetPos, finalState, finalBE,
                data, hitResult, new ItemStack[]{ItemStack.EMPTY},
                (List<ItemStack>[]) new List[]{new ArrayList<>()}, new String[]{null}
        );

        List<String> extraInfo = new ArrayList<>();
        for (IBlockTooltipProvider provider : TooltipProviderRegistry.getProviders()) {
            if (provider.isApplicable(finalState, finalBE)) {
                provider.addTooltip(extraInfo, accessor);
            }
        }

        var info = TooltipData.collect(mc, pos, finalState, mc.level.getFluidState(pos), extraInfo, accessor);

        if (info.icon().isEmpty() && accessor.getPreviewItems().isEmpty() && extraInfo.isEmpty()) return;

        renderFinal(event, mc, info, accessor, visualProgress);
    }

    /**
     * Gère le rendu pour les entités spécifiques via les Entity Providers
     */
    private static boolean handleSpecialEntity(RenderGuiEvent.Post event, Minecraft mc, Entity entity) {
        List<String> extraInfo = new ArrayList<>();
        List<ItemStack> previewItems = new ArrayList<>();

        // Initialisation de l'accessor avec les conteneurs requis par ton record
        IBlockAccessor accessor = new IBlockAccessor(
                mc.level, mc.player, entity.blockPosition(), null, null,
                null, null,
                new ItemStack[]{ItemStack.EMPTY},            // iconContainer
                (List<ItemStack>[]) new List[]{previewItems}, // inventoryContainer
                new String[]{null}                           // titleContainer
        );

        // Parcours des providers d'entités
        boolean foundProvider = false;
        for (IEntityTooltipProvider provider : TooltipEntityProviderRegistry.getProviders()) {
            if (provider.isApplicable(entity)) {
                provider.addTooltip(extraInfo, entity, accessor);
                foundProvider = true;
            }
        }

        // Si aucun provider n'a rien ajouté, on ne dessine rien (évite les tooltips vides sur les mobs)
        if (!foundProvider && extraInfo.isEmpty() && previewItems.isEmpty()) return false;

        String modId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace();

        // Récupération de l'icône :
        // 1. On regarde si un provider a fait accessor.setIcon(...)
        // 2. Sinon on utilise le résultat du "Pick Block" de l'entité
        ItemStack icon = accessor.getIcon();
        if (icon.isEmpty()) {
            icon = entity.getPickResult();
        }

        // Gestion du titre (soit un override du provider, soit le nom de l'entité)
        String title = accessor.getTitleOverride();
        if (title == null) {
            title = entity.getDisplayName().getString();
        }

        TooltipData.BlockInfo info = new TooltipData.BlockInfo(
                title,
                capitalize(modId),
                modId,
                icon,
                List.of(),
                extraInfo
        );

        renderFinal(event, mc, info, accessor, 0f);
        return true;
    }

    private static void handleItemEntity(RenderGuiEvent.Post event, Minecraft mc, ItemEntity targetEntity) {
        if (mc.level == null || mc.player == null) return;

        List<ItemEntity> nearby = mc.level.getEntitiesOfClass(
                ItemEntity.class,
                targetEntity.getBoundingBox().inflate(1.0D)
        );

        java.util.Map<Integer, ItemStack> mergedMap = new java.util.LinkedHashMap<>();
        List<String> tooltipLines = new ArrayList<>();
        net.minecraft.world.item.Item.TooltipContext tooltipContext = net.minecraft.world.item.Item.TooltipContext.of(mc.level);
        boolean isCtrlPressed = net.minecraft.client.gui.screens.Screen.hasControlDown();

        for (ItemEntity ie : nearby) {
            ItemStack stack = ie.getItem();
            if (stack.isEmpty()) continue;

            if (ie == targetEntity && !isCtrlPressed) {
                List<net.minecraft.network.chat.Component> lines = stack.getTooltipLines(
                        tooltipContext, mc.player,
                        mc.options.advancedItemTooltips ? net.minecraft.world.item.TooltipFlag.Default.ADVANCED : net.minecraft.world.item.TooltipFlag.Default.NORMAL
                );
                for (int i = 1; i < lines.size(); i++) {
                    tooltipLines.add(lines.get(i).getString());
                }
            }

            int hash = stack.getItem().hashCode();
            if (stack.getComponentsPatch() != null) {
                hash = 31 * hash + stack.getComponentsPatch().hashCode();
            }

            if (mergedMap.containsKey(hash)) {
                mergedMap.get(hash).grow(stack.getCount());
            } else {
                mergedMap.put(hash, stack.copy());
            }
        }

        List<ItemStack> previewList = new ArrayList<>(mergedMap.values());
        ItemStack targetStack = targetEntity.getItem();
        String modId = BuiltInRegistries.ITEM.getKey(targetStack.getItem()).getNamespace();

        TooltipData.BlockInfo info = new TooltipData.BlockInfo(
                targetStack.getHoverName().getString(),
                capitalize(modId),
                modId,
                ItemStack.EMPTY, // Évite le doublon d'icône avec le titre
                List.of(),
                tooltipLines
        );

        IBlockAccessor itemAccessor = new IBlockAccessor(
                mc.level, mc.player, targetEntity.blockPosition(), null, null,
                null, null, new ItemStack[]{targetStack},
                (List<ItemStack>[]) new List[]{previewList}, new String[]{null}
        );

        renderFinal(event, mc, info, itemAccessor, 0f);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "Minecraft";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private static void renderFinal(RenderGuiEvent.Post event, Minecraft mc, TooltipData.BlockInfo info, IBlockAccessor accessor, float progress) {
        long timeSinceFinish = System.currentTimeMillis() - finishTime;
        TooltipOverlayRenderer.render(
                event.getGuiGraphics(),
                mc.font,
                info,
                accessor,
                progress,
                timeSinceFinish,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight()
        );
    }

    private static void updateProgress(BlockPos pos, float currentProgress, BlockState state) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTimeMillis) / 1000f;
        lastTimeMillis = currentTime;

        Minecraft mc = Minecraft.getInstance();

        if (lastTargetPos != null && (pos == null || !pos.equals(lastTargetPos))) {
            if (mc.level != null && mc.level.getBlockState(lastTargetPos).isAir()) {
                visualProgress = 1.0f;
                finishTime = currentTime;
            }
        }

        if (pos != null && !pos.equals(lastTargetPos)) {
            if (visualProgress < 0.90f) visualProgress = 0f;
            lastTargetPos = pos;
        }

        if (currentProgress > 0 && state != null) {
            float destroySpeedPerTick = state.getDestroyProgress(mc.player, mc.level, pos);
            if (destroySpeedPerTick >= 1.0f) {
                visualProgress = 1.0f;
            } else {
                float speedPerSecond = destroySpeedPerTick * 20f;
                visualProgress += speedPerSecond * deltaTime;
                float maxAllowed = currentProgress + 0.1f;
                if (visualProgress > maxAllowed) visualProgress = maxAllowed;
                visualProgress = Math.min(visualProgress, 0.98f);
            }
        } else {
            long timeSinceFinish = currentTime - finishTime;
            if (timeSinceFinish > 150) {
                visualProgress = Math.max(0, visualProgress - (deltaTime * 4.0f));
            }
        }
    }
}