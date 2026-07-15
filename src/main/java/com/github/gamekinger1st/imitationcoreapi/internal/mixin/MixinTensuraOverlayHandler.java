package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseQueries;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseState;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.internal.client.ImitatorAppraisalRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Pseudo
@Mixin(targets = "io.github.manasmods.tensura.handler.client.OverlayHandler", remap = false)
public abstract class MixinTensuraOverlayHandler {
    @Shadow
    private static int analysisLevel;

    @Shadow
    private static Font font;

    @Shadow
    private static GuiGraphics graphics;

    @Shadow
    private static float analysisOpacity;

    @Shadow
    private static float analysisScale;

    @Shadow
    private static float[] analysisPositions;

    @Shadow
    private static boolean isLeftSide(int element) {
        throw new AssertionError();
    }

    @Inject(method = "renderEntityAnalysis", at = @At("HEAD"), cancellable = true)
    private static void imitationcoreapi$renderCopiedAppraisal(LivingEntity target, CallbackInfo callback) {
        if (analysisLevel >= 20 || !(target instanceof Player)) {
            return;
        }
        Optional<ClientDisguiseState> state = ClientDisguiseQueries.state(target);
        Optional<DisguiseAppraisalSnapshot> appraisal = state.flatMap(ClientDisguiseState::appraisal);
        if (state.isEmpty() || appraisal.isEmpty()) {
            return;
        }
        float x = analysisPositions != null && analysisPositions.length > 0 ? analysisPositions[0] : 0F;
        float y = analysisPositions != null && analysisPositions.length > 1 ? analysisPositions[1] : 0F;
        ImitatorAppraisalRenderer.render(graphics, font, analysisOpacity, analysisScale, x, y, isLeftSide(3), state.get(), appraisal.get());
        callback.cancel();
    }
}
