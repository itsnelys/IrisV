package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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

        // 1. RAYCAST
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