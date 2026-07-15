package com.github.gamekinger1st.imitationcoreapi.api.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record SkillClassificationContext(
        ResourceLocation bridgeId,
        ResourceLocation skillId,
        SkillClassification originalClassification
) {
    public SkillClassificationContext {
        Objects.requireNonNull(bridgeId, "bridgeId");
        Objects.requireNonNull(skillId, "skillId");
        originalClassification = Objects.requireNonNullElse(originalClassification, SkillClassification.UNKNOWN);
    }
}
