package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record ImitatorSkillCopySnapshot(ResourceLocation bridgeId, int schemaVersion, List<ImitatorCopiedSkill> skills) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MAX_SKILLS = 64;

    public ImitatorSkillCopySnapshot {
        Objects.requireNonNull(bridgeId, "bridgeId");
        Objects.requireNonNull(skills, "skills");
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported Imitator skill copy snapshot schema version: " + schemaVersion);
        }
        skills = skills.stream()
                .map(skill -> Objects.requireNonNull(skill, "skill"))
                .sorted(Comparator.comparing(skill -> skill.skillId().toString()))
                .toList();
        if (skills.size() > MAX_SKILLS) {
            throw new IllegalArgumentException("Too many copied skills in one snapshot");
        }
        for (int index = 1; index < skills.size(); index++) {
            if (skills.get(index - 1).skillId().equals(skills.get(index).skillId())) {
                throw new IllegalArgumentException("Copied skill snapshots cannot contain duplicate skill ids");
            }
        }
    }
}
