package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
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

        HitResult hitResult = mc.level.clip(new ClipContext(
                mc.player.getEyePosition(1f),
                mc.player.getEyePosition(1f).add(mc.player.getViewVector(1f).scale(5)),
                ClipContext.Block.OUTLINE,
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

        // --- 1. SÉCURITÉ "CLEAN DESTRUCTION" (Comme Jade) ---
        // Si le bloc est de l'air ou si la progression de minage est terminée, on stoppe tout de suite.
        // Ça évite de voir le tooltip pendant 1 ou 2 frames alors que le bloc a disparu.
        var gameMode = (DestroyAccessor) mc.gameMode;
        float progress = gameMode.getDestroyProgress();

        if (originalState.isAir() || progress >= 1.0f) {
            ClientDataCache.remove(pos);
            updateProgress(null, 0, null);
            return; // ON ARRÊTE LE RENDU ICI
        }

        // --- 1. DONNÉES DE BASE DU BLOC VISÉ ---
        BlockEntity originalBE = mc.level.getBlockEntity(pos);
        var fluid = mc.level.getFluidState(pos);

        // --- 2. PROGRESSION DE CASSAGE ---
        updateProgress(pos, gameMode.getDestroyProgress(), originalState);

        // --- 3. LOGIQUE DE REDIRECTION (MASTER/SLAVE) ---
        BlockPos targetPos = pos;
        CompoundTag data = ClientDataCache.get(pos);
        BlockEntity finalBE = originalBE;
        BlockState finalState = originalState;

        // Si le bloc visé est vide, on cherche un Master autour
        if (data == null || data.isEmpty()) {
            for (Direction dir : Direction.values()) {
                BlockPos nPos = pos.relative(dir);
                CompoundTag neighborData = ClientDataCache.get(nPos);

                if (neighborData != null && !neighborData.isEmpty()) {
                    // On vérifie si c'est bien un inventaire (Items ou inventory)
                    if (neighborData.contains("Items") || neighborData.contains("inventory") || neighborData.contains("Storage")) {
                        data = neighborData;
                        targetPos = nPos;
                        finalBE = mc.level.getBlockEntity(nPos);
                        finalState = mc.level.getBlockState(nPos);
                        break;
                    }
                }
            }
        }

        // --- 4. INITIALISATION DE L'ACCESSOR ---
        ItemStack[] iconContainer = new ItemStack[]{ItemStack.EMPTY};
        List<ItemStack>[] inventoryContainer = (List<ItemStack>[]) new List[]{new ArrayList<>()};
        String[] titleContainer = new String[]{null};

        IBlockAccessor accessor = new IBlockAccessor(
                mc.level,
                mc.player,
                targetPos, // On pointe vers le Master
                finalState,
                finalBE,
                data,
                hitResult,
                iconContainer,
                inventoryContainer,
                titleContainer
        );

        // --- 5. APPEL DES PROVIDERS ---
        List<String> extraInfo = new ArrayList<>();
        for (IBlockTooltipProvider provider : TooltipProviderRegistry.getProviders()) {
            // IMPORTANT: On utilise finalState et finalBE pour que le provider
            // reconnaisse que le bloc est un inventaire même si on regarde l'esclave.
            if (provider.isApplicable(finalState, finalBE)) {
                provider.addTooltip(extraInfo, accessor);
            }
        }

// --- 6. COLLECTE ET FILTRAGE FINAL ---
        var info = TooltipData.collect(mc, pos, finalState, fluid, extraInfo, accessor);

        // SÉCURITÉ JADE : Si l'icône est "Forbidden" (Air/Barrière) et qu'il n'y a pas d'items
        // dans l'inventaire, on considère que le tooltip n'a rien à afficher de propre.
        if (info.icon().isEmpty() && accessor.getPreviewItems().isEmpty() && extraInfo.isEmpty()) {
            return;
        }

        // --- 7. RENDU ---
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

    private static void updateProgress(BlockPos pos, float currentProgress, net.minecraft.world.level.block.state.BlockState state) {
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