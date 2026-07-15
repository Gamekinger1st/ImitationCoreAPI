package com.github.gamekinger1st.imitationcoreapi.api.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface SkillClassificationProvider {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    Optional<SkillClassification> classify(SkillClassificationContext context);
}
