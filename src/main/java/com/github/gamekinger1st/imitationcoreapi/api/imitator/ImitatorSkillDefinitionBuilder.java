package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Objects;

public final class ImitatorSkillDefinitionBuilder {
    private final ResourceLocation skillId;
    private String displayName;
    private String description;
    private long learningCost;
    private long masteryRequirement;
    private long maximumMastery;
    private final EnumMap<ImitatorSkillMode, ImitatorSkillCost> modeCosts = new EnumMap<>(ImitatorSkillMode.class);
    private ImitatorProgressionPolicy progressionPolicy = ImitatorProgressionPolicy.DEFAULT;
    private ImitatorMirrorSyncPolicy mirrorSyncPolicy = ImitatorMirrorSyncPolicy.DEFAULT;
    private ImitatorSkillCopyPolicy skillCopyPolicy = ImitatorSkillCopyPolicy.DISABLED;
    private ImitatorReplicaPolicy replicaPolicy = ImitatorReplicaPolicy.DEFAULT;
    private ImitatorTransformDurationPolicy transformDurationPolicy = ImitatorTransformDurationPolicy.unlimited();
    private ImitatorFormLibraryLimits formLibraryLimits = ImitatorFormLibraryLimits.DEFAULT;
    private ImitatorTransformationModifiers transformationModifiers = ImitatorTransformationModifiers.DEFAULT;

    public ImitatorSkillDefinitionBuilder(ResourceLocation skillId, String displayName, String description) {
        this.skillId = Objects.requireNonNull(skillId, "skillId");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.description = Objects.requireNonNull(description, "description");
        for (ImitatorSkillMode mode : ImitatorSkillMode.values()) {
            modeCosts.put(mode, ImitatorSkillCost.free());
        }
    }

    public static ImitatorSkillDefinitionBuilder from(ImitatorSkillDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        ImitatorSkillDefinitionBuilder builder = new ImitatorSkillDefinitionBuilder(definition.skillId(), definition.displayName(), definition.description());
        builder.learningCost(definition.learningCost());
        builder.mastery(definition.masteryRequirement(), definition.maximumMastery());
        for (ImitatorSkillMode mode : ImitatorSkillMode.values()) {
            builder.modeCost(mode, definition.cost(mode));
        }
        builder.progressionPolicy(definition.progressionPolicy());
        builder.perfectFormPolicy(definition.mirrorSyncPolicy());
        builder.copiedSkillPolicy(definition.skillCopyPolicy());
        builder.replicaPolicy(definition.replicaPolicy());
        builder.transformDurationPolicy(definition.transformDurationPolicy());
        builder.formLibraryLimits(definition.formLibraryLimits());
        builder.transformationModifiers(definition.transformationModifiers());
        return builder;
    }

    public ImitatorSkillDefinitionBuilder displayName(String displayName) {
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        return this;
    }

    public ImitatorSkillDefinitionBuilder description(String description) {
        this.description = Objects.requireNonNull(description, "description");
        return this;
    }

    public ImitatorSkillDefinitionBuilder learningCost(long learningCost) {
        this.learningCost = learningCost;
        return this;
    }

    public ImitatorSkillDefinitionBuilder mastery(long masteryRequirement, long maximumMastery) {
        this.masteryRequirement = masteryRequirement;
        this.maximumMastery = maximumMastery;
        return this;
    }

    public ImitatorSkillDefinitionBuilder maximumMastery(long maximumMastery) {
        this.maximumMastery = maximumMastery;
        return this;
    }

    public ImitatorSkillDefinitionBuilder modeCost(ImitatorSkillMode mode, ImitatorSkillCost cost) {
        modeCosts.put(Objects.requireNonNull(mode, "mode"), Objects.requireNonNull(cost, "cost"));
        return this;
    }

    public ImitatorSkillDefinitionBuilder modeCost(ImitatorSkillMode mode, long resourceCost, int cooldownTicks, long requiredMastery) {
        return modeCost(mode, new ImitatorSkillCost(resourceCost, cooldownTicks, requiredMastery));
    }

    public ImitatorSkillDefinitionBuilder recordCost(long resourceCost, int cooldownTicks, long requiredMastery) {
        return modeCost(ImitatorSkillMode.RECORD, resourceCost, cooldownTicks, requiredMastery);
    }

    public ImitatorSkillDefinitionBuilder transformCost(long resourceCost, int cooldownTicks, long requiredMastery) {
        return modeCost(ImitatorSkillMode.TRANSFORM, resourceCost, cooldownTicks, requiredMastery);
    }

    public ImitatorSkillDefinitionBuilder replicaCost(long resourceCost, int cooldownTicks, long requiredMastery) {
        return modeCost(ImitatorSkillMode.REPLICA, resourceCost, cooldownTicks, requiredMastery);
    }

    public ImitatorSkillDefinitionBuilder progressionPolicy(ImitatorProgressionPolicy progressionPolicy) {
        this.progressionPolicy = Objects.requireNonNull(progressionPolicy, "progressionPolicy");
        return this;
    }

    public ImitatorSkillDefinitionBuilder mirrorSyncPolicy(ImitatorMirrorSyncPolicy mirrorSyncPolicy) {
        return perfectFormPolicy(mirrorSyncPolicy);
    }

    public ImitatorSkillDefinitionBuilder perfectFormPolicy(ImitatorMirrorSyncPolicy mirrorSyncPolicy) {
        this.mirrorSyncPolicy = Objects.requireNonNull(mirrorSyncPolicy, "mirrorSyncPolicy");
        return this;
    }

    public ImitatorSkillDefinitionBuilder skillCopyPolicy(ImitatorSkillCopyPolicy skillCopyPolicy) {
        return copiedSkillPolicy(skillCopyPolicy);
    }

    public ImitatorSkillDefinitionBuilder copiedSkillPolicy(ImitatorSkillCopyPolicy skillCopyPolicy) {
        this.skillCopyPolicy = Objects.requireNonNull(skillCopyPolicy, "skillCopyPolicy");
        return this;
    }

    public ImitatorSkillDefinitionBuilder replicaPolicy(ImitatorReplicaPolicy replicaPolicy) {
        this.replicaPolicy = Objects.requireNonNull(replicaPolicy, "replicaPolicy");
        return this;
    }

    public ImitatorSkillDefinitionBuilder transformDurationPolicy(ImitatorTransformDurationPolicy transformDurationPolicy) {
        this.transformDurationPolicy = Objects.requireNonNull(transformDurationPolicy, "transformDurationPolicy");
        return this;
    }

    public ImitatorSkillDefinitionBuilder transformDurationMinutes(int minutes) {
        return transformDurationPolicy(ImitatorTransformDurationPolicy.minutes(minutes));
    }

    public ImitatorSkillDefinitionBuilder formLibraryLimits(ImitatorFormLibraryLimits formLibraryLimits) {
        this.formLibraryLimits = Objects.requireNonNull(formLibraryLimits, "formLibraryLimits");
        return this;
    }

    public ImitatorSkillDefinitionBuilder transformationModifiers(ImitatorTransformationModifiers transformationModifiers) {
        this.transformationModifiers = Objects.requireNonNull(transformationModifiers, "transformationModifiers");
        return this;
    }

    public ImitatorSkillDefinitionBuilder autoJumpOverride(ImitatorAutoJumpOverride autoJumpOverride) {
        return transformationModifiers(new ImitatorTransformationModifiers(autoJumpOverride));
    }

    public ImitatorSkillDefinitionBuilder forceAutoJump(boolean enabled) {
        return transformationModifiers(ImitatorTransformationModifiers.forceAutoJump(enabled));
    }

    public ImitatorSkillDefinition build() {
        return new ImitatorSkillDefinition(
                skillId,
                displayName,
                description,
                learningCost,
                masteryRequirement,
                maximumMastery,
                modeCosts,
                progressionPolicy,
                mirrorSyncPolicy,
                skillCopyPolicy,
                replicaPolicy,
                transformDurationPolicy,
                formLibraryLimits,
                transformationModifiers
        );
    }
}
