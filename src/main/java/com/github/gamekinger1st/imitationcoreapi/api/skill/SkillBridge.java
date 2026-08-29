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

    default SkillOperationResult restoreSkill(LivingEntity entity, SkillState state) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(state, "state");
        return SkillOperationResult.failure("This skill bridge cannot restore an individual skill");
    }

    default SkillOperationResult removeSkill(LivingEntity entity, ResourceLocation skillId) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(skillId, "skillId");
        return SkillOperationResult.failure("This skill bridge cannot remove an individual skill");
    }

    SkillOperationResult grantTemporary(LivingEntity entity, ResourceLocation skillId, int removeTime, TemporarySkillOwnership ownership);

    SkillOperationResult revokeTemporary(LivingEntity entity, ResourceLocation skillId, TemporarySkillOwnership ownership);

    SkillOperationResult update(LivingEntity entity, SkillUpdateRequest request);
}
