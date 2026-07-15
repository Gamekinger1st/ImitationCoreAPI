package com.github.gamekinger1st.imitationcoreapi.api.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SkillUpdateRequest(
        ResourceLocation skillId,
        Optional<Double> mastery,
        Optional<Boolean> toggled,
        Optional<List<Integer>> cooldowns,
        Optional<Integer> temporaryRemoveTime
) {
    public SkillUpdateRequest {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(mastery, "mastery");
        Objects.requireNonNull(toggled, "toggled");
        Objects.requireNonNull(cooldowns, "cooldowns");
        Objects.requireNonNull(temporaryRemoveTime, "temporaryRemoveTime");
        mastery.ifPresent(value -> {
            if (!Double.isFinite(value) || value < 0) {
                throw new IllegalArgumentException("mastery must be finite and non-negative");
            }
        });
        cooldowns = cooldowns.map(List::copyOf);
        temporaryRemoveTime.ifPresent(value -> {
            if (value < 0) {
                throw new IllegalArgumentException("temporaryRemoveTime cannot be negative");
            }
        });
        if (mastery.isEmpty() && toggled.isEmpty() && cooldowns.isEmpty() && temporaryRemoveTime.isEmpty()) {
            throw new IllegalArgumentException("A skill update request must change at least one value");
        }
    }
}
