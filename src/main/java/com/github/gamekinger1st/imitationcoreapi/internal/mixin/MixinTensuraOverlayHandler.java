package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseQueries;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseState;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.internal.client.ImitatorAppraisalRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
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
    private static boolean isLeftSide(int element) {
        throw new AssertionError();
    }

    @Shadow
    private static void renderWithFlipping(ResourceLocation texture, float x, float y, int width, int height, float alpha, boolean isLeftSide) {
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
        boolean leftSide = isLeftSide(3);
        renderWithFlipping(ImitatorAppraisalRenderer.ENTITY_ANALYSIS, 0F, 0F, 95, 156, analysisOpacity, leftSide);
        ImitatorAppraisalRenderer.render(graphics, font, state.get(), appraisal.get(), target, leftSide);
        callback.cancel();
    }
}
