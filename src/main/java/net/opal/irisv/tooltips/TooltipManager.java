package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
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

        // 1. Raycast
        var hitResult = mc.level.clip(new ClipContext(
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
        var state = mc.level.getBlockState(pos);
        var fluid = mc.level.getFluidState(pos);
        BlockEntity be = mc.level.getBlockEntity(pos);

        // 2. Gestion de la progression (Barre de cassage)
        var gameMode = (DestroyAccessor) mc.gameMode;
        float currentProgress = gameMode.getDestroyProgress();
        updateProgress(pos, currentProgress, state);

// --- 3. LOGIQUE D'API ---

// On initialise avec une liste mutable pour la preview
        List<ItemStack> itemsList = new ArrayList<>();
        IBlockAccessor accessor = new IBlockAccessor(
                mc.level,
                mc.player,
                pos,
                state,
                be,
                ClientDataCache.get(pos),
                hitResult,
                new ItemStack[]{ItemStack.EMPTY},      // iconContainer
                new List[]{new ArrayList<>()},         // inventoryContainer (on initialise une liste vide)
                new String[]{null}                     // titleContainer (null par défaut pour garder le nom original)
        );

        List<String> extraInfo = new ArrayList<>();
        for (IBlockTooltipProvider provider : TooltipProviderRegistry.getProviders()) {
            if (provider.isApplicable(state, be)) {
                provider.addTooltip(extraInfo, accessor);
            }
        }

// IMPORTANT : On récupère les items mis à jour par le provider AVANT le collect ou le render
        var finalPreviewItems = accessor.getPreviewItems();

        var info = TooltipData.collect(mc, pos, state, fluid, extraInfo);

        // 4. Collecte finale des données pour le rendu
        long timeSinceFinish = System.currentTimeMillis() - finishTime;

        // --- 5. RENDU FINAL (MAJ ICI) ---
        // On passe l'accessor au lieu du state pour que le renderer puisse voir l'icône modifiée
        TooltipOverlayRenderer.render(
                event.getGuiGraphics(),
                mc.font,
                info,
                accessor, // <--- CHANGEMENT ICI
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