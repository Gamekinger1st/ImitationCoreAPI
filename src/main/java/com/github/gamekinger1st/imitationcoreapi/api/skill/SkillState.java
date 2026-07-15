package com.github.gamekinger1st.imitationcoreapi.api.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public record SkillState(ResourceLocation skillId, CompoundTag serializedData, double mastery, boolean toggled, List<Integer> cooldowns, boolean temporary) {
    public SkillState {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(serializedData, "serializedData");
        Objects.requireNonNull(cooldowns, "cooldowns");
        if (!Double.isFinite(mastery) || mastery < 0) {
            throw new IllegalArgumentException("mastery must be finite and non-negative");
        }
        serializedData = serializedData.copy();
        cooldowns = List.copyOf(cooldowns);
    }

    @Override
    public CompoundTag serializedData() {
        return serializedData.copy();
    }
}
