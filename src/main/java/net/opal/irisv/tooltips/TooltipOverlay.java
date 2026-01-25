package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.opal.irisv.mixin.DestroyAccessor;
import net.opal.irisv.option.ConfigOptions;

public class TooltipOverlay {

    private static BlockPos lastTargetPos = null;
    private static float visualProgress = 0f;
    private static long lastTimeMillis = System.currentTimeMillis();
    private static long finishTime = 0;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ConfigOptions.getInstance().enableBlockTooltipOverlay) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || mc.gameMode == null) return;

        // Raycast pour trouver le bloc regardé
        var hitResult = mc.level.clip(new ClipContext(
                mc.player.getEyePosition(1f),
                mc.player.getEyePosition(1f).add(mc.player.getViewVector(1f).scale(5)),
                ClipContext.Block.OUTLINE,
                mc.player.isUnderWater() || mc.player.isInLava() ? ClipContext.Fluid.NONE : ClipContext.Fluid.ANY,
                mc.player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            updateProgress(null, 0, null); // Permet la descente même si on ne regarde rien
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos pos = blockHit.getBlockPos();
        var state = mc.level.getBlockState(pos);
        var fluid = mc.level.getFluidState(pos);

        // Récupération de la progression réelle via le Mixin
        var gameMode = (DestroyAccessor) mc.gameMode;
        float currentProgress = gameMode.getDestroyProgress();

        updateProgress(pos, currentProgress, state);

        var info = TooltipData.collect(mc, pos, state, fluid);
        long timeSinceFinish = System.currentTimeMillis() - finishTime;

        TooltipOverlayRenderer.render(
                event.getGuiGraphics(),
                mc.font,
                info,
                state,
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

        // 1. GESTION DU CHANGEMENT DE BLOC / CASSE
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

        // 2. LOGIQUE DE MONTÉE
        if (currentProgress > 0 && state != null) {
            // On récupère la vitesse théorique du bloc (ex: 0.05 par tick)
            float destroySpeedPerTick = state.getDestroyProgress(mc.player, mc.level, pos);

            if (destroySpeedPerTick >= 1.0f) {
                visualProgress = 1.0f;
            } else {
                // Conversion en vitesse par seconde (Tick * 20)
                float speedPerSecond = destroySpeedPerTick * 20f;

                // AVANCEE LINÉAIRE : On avance en fonction du temps réel (FPS)
                // Cela rend la montée parfaitement fluide, comme la descente.
                visualProgress += speedPerSecond * deltaTime;

                // SÉCURITÉ : On ne laisse pas la barre dépasser la progression réelle + un petit bonus
                // Cela évite que la barre n'arrive à 100% alors que le serveur dit 80%
                float maxAllowed = currentProgress + 0.1f;
                if (visualProgress > maxAllowed) visualProgress = maxAllowed;

                // On sature à 0.98 pour attendre le vrai bris du bloc
                visualProgress = Math.min(visualProgress, 0.98f);
            }
        }
        // 3. LOGIQUE DE DESCENTE / FLASH
        else {
            long timeSinceFinish = currentTime - finishTime;
            if (timeSinceFinish > 150) {
                // Utilisation du deltaTime identique à la montée
                visualProgress = Math.max(0, visualProgress - (deltaTime * 4.0f));
            }
        }
    }
}