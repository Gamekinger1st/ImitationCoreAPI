package com.github.gamekinger1st.imitationcoreapi.api.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public record SkillSnapshot(ResourceLocation bridgeId, int schemaVersion, List<SkillState> skills) {
    public SkillSnapshot {
        Objects.requireNonNull(bridgeId, "bridgeId");
        Objects.requireNonNull(skills, "skills");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        skills = List.copyOf(skills);
    }
}
