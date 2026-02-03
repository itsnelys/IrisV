package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
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
// On cherche d'abord s'il y a un item n'importe où dans le champ de vision
        AABB searchBox = mc.player.getBoundingBox().expandTowards(viewVec.scale(reach)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.player, eyePos, endPos, searchBox,
                entity -> entity instanceof ItemEntity, reach * reach // On check sur toute la portée
        );

// SI ON TROUVE UN ITEM -> ON L'AFFICHE DIRECTEMENT
        if (entityHit != null && entityHit.getEntity() instanceof ItemEntity itemEntity) {
            handleItemEntity(event, mc, itemEntity);
            updateProgress(null, 0, null);
            return; // On arrête là, l'item gagne sur le bloc
        }

// 3. RAYCAST DES BLOCS (Seulement si aucun item n'a été trouvé)
        HitResult hitResult = mc.level.clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.OUTLINE,
                mc.player.isUnderWater() || mc.player.isInLava() ? ClipContext.Fluid.NONE : ClipContext.Fluid.ANY,
                mc.player
        ));

        // --- 3. LOGIQUE POUR LES BLOCS (Ton code original) ---
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            updateProgress(null, 0, null);
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos pos = blockHit.getBlockPos();
        BlockState originalState = mc.level.getBlockState(pos);

        // À partir d'ici, tu gardes ton code original : var gameMode = (DestroyAccessor)...

        // --- SÉCURITÉ "CLEAN DESTRUCTION" ---
        var gameMode = (DestroyAccessor) mc.gameMode;
        float progress = gameMode.getDestroyProgress();

        if (originalState.isAir() || progress >= 1.0f) {
            ClientDataCache.remove(pos);
            updateProgress(null, 0, null);
            return; // Coupe le rendu instantanément
        }

        // --- LOGIQUE DE REDIRECTION (MASTER/SLAVE) ---
        // On utilise StorageUtils pour pointer immédiatement vers le bloc maître (Vanilla ou Moddé)
        BlockPos targetPos = StorageUtils.getActualTarget(mc.level, pos, originalState);
        CompoundTag data = ClientDataCache.get(targetPos);

        BlockState finalState = (targetPos.equals(pos)) ? originalState : mc.level.getBlockState(targetPos);
        BlockEntity finalBE = (targetPos.equals(pos)) ? mc.level.getBlockEntity(pos) : mc.level.getBlockEntity(targetPos);

        // Mise à jour de la barre de progression
        updateProgress(pos, progress, originalState);

        // --- INITIALISATION ACCESSOR ---
        ItemStack[] iconContainer = new ItemStack[]{ItemStack.EMPTY};
        List<ItemStack>[] inventoryContainer = (List<ItemStack>[]) new List[]{new ArrayList<>()};
        String[] titleContainer = new String[]{null};

        IBlockAccessor accessor = new IBlockAccessor(
                mc.level, mc.player, targetPos, finalState, finalBE,
                data, hitResult, iconContainer, inventoryContainer, titleContainer
        );

        // --- APPEL DES PROVIDERS ---
        List<String> extraInfo = new ArrayList<>();
        for (IBlockTooltipProvider provider : TooltipProviderRegistry.getProviders()) {
            if (provider.isApplicable(finalState, finalBE)) {
                provider.addTooltip(extraInfo, accessor);
            }
        }

        // --- COLLECTE ET FILTRAGE FINAL ---
        var info = TooltipData.collect(mc, pos, finalState, mc.level.getFluidState(pos), extraInfo, accessor);

        // Si l'icône est interdite (Air/Barrière) et qu'il n'y a rien d'autre, on ne dessine rien.
        if (info.icon().isEmpty() && accessor.getPreviewItems().isEmpty() && extraInfo.isEmpty()) {
            return;
        }

        // --- RENDU ---
        long timeSinceFinish = System.currentTimeMillis() - finishTime;
        TooltipOverlayRenderer.render(
                event.getGuiGraphics(),
                mc.font,
                info,
                accessor,
                visualProgress,
                timeSinceFinish,
                mc.getWindow().getGuiScaledWidth()
        );
    }

    private static void handleItemEntity(RenderGuiEvent.Post event, Minecraft mc, net.minecraft.world.entity.item.ItemEntity targetEntity) {
        if (mc.level == null || mc.player == null) return;

        // 1. Scan de la zone (3 blocs)
        List<net.minecraft.world.entity.item.ItemEntity> nearby = mc.level.getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                targetEntity.getBoundingBox().inflate(1.0D)
        );

        // 2. Fusion des stacks et Tooltip conditionnel
        java.util.Map<Integer, ItemStack> mergedMap = new java.util.LinkedHashMap<>();
        List<String> tooltipLines = new ArrayList<>();
        net.minecraft.world.item.Item.TooltipContext tooltipContext = net.minecraft.world.item.Item.TooltipContext.of(mc.level);

        // VERIFICATION DE LA TOUCHE CTRL
        boolean isCtrlPressed = net.minecraft.client.gui.screens.Screen.hasControlDown();

        for (net.minecraft.world.entity.item.ItemEntity ie : nearby) {
            ItemStack stack = ie.getItem();
            if (stack.isEmpty()) continue;

            // On récupère les lignes UNIQUEMENT si CTRL n'est PAS pressé
            if (ie == targetEntity && !isCtrlPressed) {
                List<net.minecraft.network.chat.Component> lines = stack.getTooltipLines(
                        tooltipContext, mc.player,
                        mc.options.advancedItemTooltips ? net.minecraft.world.item.TooltipFlag.Default.ADVANCED : net.minecraft.world.item.TooltipFlag.Default.NORMAL
                );
                // On ignore l'index 0 (nom de l'item)
                for (int i = 1; i < lines.size(); i++) {
                    tooltipLines.add(lines.get(i).getString());
                }
            }

            // Fusion des stacks (Hash Item + NBT)
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

        // 3. Préparation des données
        ItemStack targetStack = targetEntity.getItem();
        String modId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(targetStack.getItem()).getNamespace();

        TooltipData.BlockInfo info = new TooltipData.BlockInfo(
                targetStack.getHoverName().getString(),
                capitalize(modId),
                modId,
                new ItemStack(net.minecraft.world.item.Items.AIR), // Fix icône doublon
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
                mc.getWindow().getGuiScaledWidth()
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