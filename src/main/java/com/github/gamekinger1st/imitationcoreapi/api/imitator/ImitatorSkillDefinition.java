package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record ImitatorSkillDefinition(
        ResourceLocation skillId,
        String displayName,
        String description,
        long learningCost,
        long masteryRequirement,
        long maximumMastery,
        Map<ImitatorSkillMode, ImitatorSkillCost> modeCosts,
        ImitatorProgressionPolicy progressionPolicy,
        ImitatorMirrorSyncPolicy mirrorSyncPolicy,
        ImitatorSkillCopyPolicy skillCopyPolicy,
        ImitatorReplicaPolicy replicaPolicy,
        ImitatorTransformDurationPolicy transformDurationPolicy,
        ImitatorFormLibraryLimits formLibraryLimits
) {
    public static ImitatorSkillDefinitionBuilder builder(ResourceLocation skillId, String displayName, String description) {
        return new ImitatorSkillDefinitionBuilder(skillId, displayName, description);
    }

    public ImitatorSkillDefinition(ResourceLocation skillId, String displayName, String description, long learningCost, long masteryRequirement, long maximumMastery, Map<ImitatorSkillMode, ImitatorSkillCost> modeCosts) {
        this(skillId, displayName, description, learningCost, masteryRequirement, maximumMastery, modeCosts, ImitatorProgressionPolicy.DEFAULT, ImitatorMirrorSyncPolicy.DEFAULT, ImitatorSkillCopyPolicy.DISABLED, ImitatorReplicaPolicy.DEFAULT, ImitatorTransformDurationPolicy.unlimited(), ImitatorFormLibraryLimits.DEFAULT);
    }

    public ImitatorSkillDefinition(ResourceLocation skillId, String displayName, String description, long learningCost, long masteryRequirement, long maximumMastery, Map<ImitatorSkillMode, ImitatorSkillCost> modeCosts, ImitatorProgressionPolicy progressionPolicy) {
        this(skillId, displayName, description, learningCost, masteryRequirement, maximumMastery, modeCosts, progressionPolicy, ImitatorMirrorSyncPolicy.DEFAULT, ImitatorSkillCopyPolicy.DISABLED, ImitatorReplicaPolicy.DEFAULT, ImitatorTransformDurationPolicy.unlimited(), ImitatorFormLibraryLimits.DEFAULT);
    }

    public ImitatorSkillDefinition(ResourceLocation skillId, String displayName, String description, long learningCost, long masteryRequirement, long maximumMastery, Map<ImitatorSkillMode, ImitatorSkillCost> modeCosts, ImitatorProgressionPolicy progressionPolicy, ImitatorMirrorSyncPolicy mirrorSyncPolicy) {
        this(skillId, displayName, description, learningCost, masteryRequirement, maximumMastery, modeCosts, progressionPolicy, mirrorSyncPolicy, ImitatorSkillCopyPolicy.DISABLED, ImitatorReplicaPolicy.DEFAULT, ImitatorTransformDurationPolicy.unlimited(), ImitatorFormLibraryLimits.DEFAULT);
    }

    public ImitatorSkillDefinition(ResourceLocation skillId, String displayName, String description, long learningCost, long masteryRequirement, long maximumMastery, Map<ImitatorSkillMode, ImitatorSkillCost> modeCosts, ImitatorProgressionPolicy progressionPolicy, ImitatorMirrorSyncPolicy mirrorSyncPolicy, ImitatorSkillCopyPolicy skillCopyPolicy) {
        this(skillId, displayName, description, learningCost, masteryRequirement, maximumMastery, modeCosts, progressionPolicy, mirrorSyncPolicy, skillCopyPolicy, ImitatorReplicaPolicy.DEFAULT, ImitatorTransformDurationPolicy.unlimited(), ImitatorFormLibraryLimits.DEFAULT);
    }

    public ImitatorSkillDefinition(ResourceLocation skillId, String displayName, String description, long learningCost, long masteryRequirement, long maximumMastery, Map<ImitatorSkillMode, ImitatorSkillCost> modeCosts, ImitatorProgressionPolicy progressionPolicy, ImitatorMirrorSyncPolicy mirrorSyncPolicy, ImitatorSkillCopyPolicy skillCopyPolicy, ImitatorReplicaPolicy replicaPolicy) {
        this(skillId, displayName, description, learningCost, masteryRequirement, maximumMastery, modeCosts, progressionPolicy, mirrorSyncPolicy, skillCopyPolicy, replicaPolicy, ImitatorTransformDurationPolicy.unlimited(), ImitatorFormLibraryLimits.DEFAULT);
    }

    public ImitatorSkillDefinition(ResourceLocation skillId, String displayName, String description, long learningCost, long masteryRequirement, long maximumMastery, Map<ImitatorSkillMode, ImitatorSkillCost> modeCosts, ImitatorProgressionPolicy progressionPolicy, ImitatorMirrorSyncPolicy mirrorSyncPolicy, ImitatorSkillCopyPolicy skillCopyPolicy, ImitatorReplicaPolicy replicaPolicy, ImitatorTransformDurationPolicy transformDurationPolicy) {
        this(skillId, displayName, description, learningCost, masteryRequirement, maximumMastery, modeCosts, progressionPolicy, mirrorSyncPolicy, skillCopyPolicy, replicaPolicy, transformDurationPolicy, ImitatorFormLibraryLimits.DEFAULT);
    }

    public ImitatorSkillDefinition {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(modeCosts, "modeCosts");
        Objects.requireNonNull(progressionPolicy, "progressionPolicy");
        Objects.requireNonNull(mirrorSyncPolicy, "mirrorSyncPolicy");
        Objects.requireNonNull(skillCopyPolicy, "skillCopyPolicy");
        Objects.requireNonNull(replicaPolicy, "replicaPolicy");
        Objects.requireNonNull(transformDurationPolicy, "transformDurationPolicy");
        Objects.requireNonNull(formLibraryLimits, "formLibraryLimits");
        displayName = displayName.strip();
        description = description.strip();
        if (displayName.isEmpty() || displayName.length() > 128 || description.length() > 1_024) {
            throw new IllegalArgumentException("Imitator skill display data is outside the supported bounds");
        }
        if (learningCost < 0L || masteryRequirement < 0L || maximumMastery < masteryRequirement) {
            throw new IllegalArgumentException("Imitator skill progression bounds are invalid");
        }
        EnumMap<ImitatorSkillMode, ImitatorSkillCost> copied = new EnumMap<>(ImitatorSkillMode.class);
        copied.putAll(modeCosts);
        for (ImitatorSkillMode mode : ImitatorSkillMode.values()) {
            if (!copied.containsKey(mode)) {
                throw new IllegalArgumentException("Imitator skill definition is missing a cost for " + mode);
            }
            Objects.requireNonNull(copied.get(mode), "mode cost");
        }
        modeCosts = Map.copyOf(copied);
    }

    public ImitatorSkillCost cost(ImitatorSkillMode mode) {
        return modeCosts.get(Objects.requireNonNull(mode, "mode"));
    }
}
