package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record ImitatorCopiedSkill(ResourceLocation skillId, double mastery) {
    public ImitatorCopiedSkill {
        Objects.requireNonNull(skillId, "skillId");
        if (!Double.isFinite(mastery) || mastery < 0D) {
            throw new IllegalArgumentException("mastery must be finite and non-negative");
        }
    }
}
