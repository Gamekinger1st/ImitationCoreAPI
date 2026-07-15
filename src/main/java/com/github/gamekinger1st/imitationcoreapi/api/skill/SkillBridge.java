package com.github.gamekinger1st.imitationcoreapi.api.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.Optional;

public interface SkillBridge {
    ResourceLocation id();

    int priority();

    boolean isAvailable();

    default SkillClassification classify(ResourceLocation skillId) {
        Objects.requireNonNull(skillId, "skillId");
        return SkillClassification.UNKNOWN;
    }

    default SkillOperationResult alterClassification(ResourceLocation skillId, SkillClassification classification) {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(classification, "classification");
        return SkillOperationResult.failure("This skill bridge cannot alter skill classifications");
    }

    Optional<SkillSnapshot> capture(LivingEntity entity);

    SkillOperationResult restore(LivingEntity entity, SkillSnapshot snapshot);

    SkillOperationResult grantTemporary(LivingEntity entity, ResourceLocation skillId, int removeTime, TemporarySkillOwnership ownership);

    SkillOperationResult revokeTemporary(LivingEntity entity, ResourceLocation skillId, TemporarySkillOwnership ownership);

    SkillOperationResult update(LivingEntity entity, SkillUpdateRequest request);
}
