package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

public record DisguiseRenderContext(
        Entity subject,
        Entity imitation,
        ClientDisguiseState state,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight
) {
    public DisguiseRenderContext {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(imitation, "imitation");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(poseStack, "poseStack");
        Objects.requireNonNull(bufferSource, "bufferSource");
        if (!Float.isFinite(entityYaw)) {
            throw new IllegalArgumentException("entityYaw must be finite");
        }
        if (!Float.isFinite(partialTick) || partialTick < 0F || partialTick > 1F) {
            throw new IllegalArgumentException("partialTick must be between zero and one");
        }
    }
}
