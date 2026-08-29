package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityLevel;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorTransformationModifiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ClientDisguiseState(
        int entityId,
        UUID ownerId,
        UUID sessionId,
        UUID snapshotId,
        TransformationScope scope,
        ResourceLocation entityType,
        String displayName,
        CompoundTag entityData,
        CompoundTag visualData,
        CompatibilityLevel compatibility,
        long revision,
        Optional<PlayerDisguiseProfile> playerProfile,
        Optional<DisguiseAppraisalSnapshot> appraisal,
        ImitatorTransformationModifiers transformationModifiers
) {
    public ClientDisguiseState(
            int entityId,
            UUID ownerId,
            UUID sessionId,
            UUID snapshotId,
            TransformationScope scope,
            ResourceLocation entityType,
            String displayName,
            CompoundTag entityData,
            CompoundTag visualData,
            CompatibilityLevel compatibility,
            long revision,
            Optional<PlayerDisguiseProfile> playerProfile
    ) {
        this(entityId, ownerId, sessionId, snapshotId, scope, entityType, displayName, entityData, visualData, compatibility, revision, playerProfile, Optional.empty(), ImitatorTransformationModifiers.DEFAULT);
    }

    public ClientDisguiseState(
            int entityId,
            UUID ownerId,
            UUID sessionId,
            UUID snapshotId,
            TransformationScope scope,
            ResourceLocation entityType,
            String displayName,
            CompoundTag entityData,
            CompoundTag visualData,
            CompatibilityLevel compatibility,
            long revision
    ) {
        this(entityId, ownerId, sessionId, snapshotId, scope, entityType, displayName, entityData, visualData, compatibility, revision, Optional.empty(), Optional.empty(), ImitatorTransformationModifiers.DEFAULT);
    }

    public ClientDisguiseState(
            int entityId,
            UUID ownerId,
            UUID sessionId,
            UUID snapshotId,
            ResourceLocation entityType,
            String displayName,
            CompoundTag entityData,
            CompoundTag visualData,
            CompatibilityLevel compatibility,
            long revision
    ) {
        this(entityId, ownerId, sessionId, snapshotId, TransformationScope.GAMEPLAY, entityType, displayName, entityData, visualData, compatibility, revision, Optional.empty(), Optional.empty(), ImitatorTransformationModifiers.DEFAULT);
    }

    public ClientDisguiseState(
            int entityId,
            UUID ownerId,
            UUID sessionId,
            UUID snapshotId,
            TransformationScope scope,
            ResourceLocation entityType,
            String displayName,
            CompoundTag entityData,
            CompoundTag visualData,
            CompatibilityLevel compatibility,
            long revision,
            Optional<PlayerDisguiseProfile> playerProfile,
            Optional<DisguiseAppraisalSnapshot> appraisal
    ) {
        this(entityId, ownerId, sessionId, snapshotId, scope, entityType, displayName, entityData, visualData, compatibility, revision, playerProfile, appraisal, ImitatorTransformationModifiers.DEFAULT);
    }

    public ClientDisguiseState {
        if (entityId < 0 || revision < 0) {
            throw new IllegalArgumentException("entityId and revision cannot be negative");
        }
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(entityData, "entityData");
        Objects.requireNonNull(visualData, "visualData");
        Objects.requireNonNull(compatibility, "compatibility");
        Objects.requireNonNull(playerProfile, "playerProfile");
        Objects.requireNonNull(appraisal, "appraisal");
        Objects.requireNonNull(transformationModifiers, "transformationModifiers");
        displayName = displayName.strip();
        if (displayName.length() > 256) {
            throw new IllegalArgumentException("displayName exceeds the configured limit");
        }
        entityData = entityData.copy();
        visualData = visualData.copy();
    }

    @Override
    public CompoundTag entityData() {
        return entityData.copy();
    }

    @Override
    public CompoundTag visualData() {
        return visualData.copy();
    }
}
