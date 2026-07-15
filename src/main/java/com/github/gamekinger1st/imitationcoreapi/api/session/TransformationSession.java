package com.github.gamekinger1st.imitationcoreapi.api.session;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.BaselineSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public record TransformationSession(
        UUID sessionId,
        UUID ownerId,
        UUID snapshotId,
        TransformationScope scope,
        double gameplayScale,
        BaselineSnapshot baseline,
        CompatibilityAssessment compatibility,
        TransformationState state,
        long createdGameTime,
        long updatedGameTime,
        long revision,
        OptionalLong expiresGameTime,
        Optional<String> failureDetail,
        List<TemporaryStateReference> temporaryState
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public TransformationSession(
            UUID sessionId,
            UUID ownerId,
            UUID snapshotId,
            BaselineSnapshot baseline,
            CompatibilityAssessment compatibility,
            TransformationState state,
            long createdGameTime,
            long updatedGameTime,
            long revision,
            Optional<String> failureDetail,
            List<TemporaryStateReference> temporaryState
    ) {
        this(sessionId, ownerId, snapshotId, TransformationScope.GAMEPLAY, 1D, baseline, compatibility, state, createdGameTime, updatedGameTime, revision, OptionalLong.empty(), failureDetail, temporaryState);
    }

    public TransformationSession(
            UUID sessionId,
            UUID ownerId,
            UUID snapshotId,
            TransformationScope scope,
            double gameplayScale,
            BaselineSnapshot baseline,
            CompatibilityAssessment compatibility,
            TransformationState state,
            long createdGameTime,
            long updatedGameTime,
            long revision,
            Optional<String> failureDetail,
            List<TemporaryStateReference> temporaryState
    ) {
        this(sessionId, ownerId, snapshotId, scope, gameplayScale, baseline, compatibility, state, createdGameTime, updatedGameTime, revision, OptionalLong.empty(), failureDetail, temporaryState);
    }

    public TransformationSession {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(scope, "scope");
        if (!Double.isFinite(gameplayScale) || gameplayScale < 0D || gameplayScale > 1D) {
            throw new IllegalArgumentException("gameplayScale must be between zero and one");
        }
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(compatibility, "compatibility");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(expiresGameTime, "expiresGameTime");
        Objects.requireNonNull(failureDetail, "failureDetail");
        Objects.requireNonNull(temporaryState, "temporaryState");
        if (createdGameTime < 0 || updatedGameTime < createdGameTime || revision < 0) {
            throw new IllegalArgumentException("Invalid transformation session timing or revision");
        }
        if (expiresGameTime.isPresent() && expiresGameTime.getAsLong() < createdGameTime) {
            throw new IllegalArgumentException("Session expiration cannot be before creation");
        }
        failureDetail = failureDetail.map(String::strip).filter(value -> !value.isEmpty()).map(value -> value.length() > 512 ? value.substring(0, 512) : value);
        temporaryState = List.copyOf(new ArrayList<>(temporaryState));
        for (TemporaryStateReference reference : temporaryState) {
            if (!sessionId.equals(reference.sessionId())) {
                throw new IllegalArgumentException("Temporary state belongs to another session");
            }
        }
    }

    public static TransformationSession begin(UUID ownerId, UUID snapshotId, BaselineSnapshot baseline, CompatibilityAssessment compatibility, long gameTime) {
        return begin(ownerId, snapshotId, TransformationScope.GAMEPLAY, 1D, baseline, compatibility, gameTime);
    }

    public static TransformationSession begin(UUID ownerId, UUID snapshotId, TransformationScope scope, BaselineSnapshot baseline, CompatibilityAssessment compatibility, long gameTime) {
        return begin(ownerId, snapshotId, scope, 1D, baseline, compatibility, gameTime);
    }

    public static TransformationSession begin(UUID ownerId, UUID snapshotId, TransformationScope scope, double gameplayScale, BaselineSnapshot baseline, CompatibilityAssessment compatibility, long gameTime) {
        return new TransformationSession(UUID.randomUUID(), ownerId, snapshotId, scope, gameplayScale, baseline, compatibility, TransformationState.CAPTURING, gameTime, gameTime, 0, OptionalLong.empty(), Optional.empty(), List.of());
    }

    public TransformationSession transitionTo(TransformationState target, long gameTime, Optional<String> failureDetail) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(failureDetail, "failureDetail");
        if (!state.canTransitionTo(target)) {
            throw new IllegalStateException("Illegal transformation transition: " + state + " -> " + target);
        }
        if (gameTime < updatedGameTime) {
            throw new IllegalArgumentException("Transformation session time cannot move backwards");
        }
        if (target == TransformationState.FAILED && failureDetail.isEmpty()) {
            throw new IllegalArgumentException("Failed sessions require a failure detail");
        }
        return new TransformationSession(sessionId, ownerId, snapshotId, scope, gameplayScale, baseline, compatibility, target, createdGameTime, gameTime, revision + 1, expiresGameTime, failureDetail, temporaryState);
    }

    public TransformationSession withExpiresGameTime(OptionalLong expiration, long gameTime) {
        Objects.requireNonNull(expiration, "expiration");
        return new TransformationSession(sessionId, ownerId, snapshotId, scope, gameplayScale, baseline, compatibility, state, createdGameTime, nextGameTime(gameTime), revision + 1, expiration, failureDetail, temporaryState);
    }

    public boolean expiresAtOrBefore(long gameTime) {
        return state == TransformationState.ACTIVE && expiresGameTime.isPresent() && gameTime >= expiresGameTime.getAsLong();
    }

    public TransformationSession addTemporaryState(TemporaryStateReference reference, long gameTime) {
        Objects.requireNonNull(reference, "reference");
        if (!sessionId.equals(reference.sessionId())) {
            throw new IllegalArgumentException("Temporary state belongs to another session");
        }
        if (state.isTerminal()) {
            throw new IllegalStateException("Cannot add temporary state to a reverted session");
        }
        if (temporaryState.stream().anyMatch(existing -> existing.referenceId().equals(reference.referenceId()))) {
            throw new IllegalArgumentException("Duplicate temporary state reference");
        }
        List<TemporaryStateReference> references = new ArrayList<>(temporaryState);
        references.add(reference);
        return new TransformationSession(sessionId, ownerId, snapshotId, scope, gameplayScale, baseline, compatibility, state, createdGameTime, nextGameTime(gameTime), revision + 1, expiresGameTime, failureDetail, references);
    }

    public TransformationSession updateTemporaryState(UUID referenceId, TemporaryStateStatus status, long gameTime) {
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(status, "status");
        List<TemporaryStateReference> references = new ArrayList<>(temporaryState.size());
        boolean found = false;
        for (TemporaryStateReference reference : temporaryState) {
            if (reference.referenceId().equals(referenceId)) {
                references.add(reference.withStatus(status));
                found = true;
            } else {
                references.add(reference);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown temporary state reference: " + referenceId);
        }
        return new TransformationSession(sessionId, ownerId, snapshotId, scope, gameplayScale, baseline, compatibility, state, createdGameTime, nextGameTime(gameTime), revision + 1, expiresGameTime, failureDetail, references);
    }

    public TransformationSession updateTemporaryStatePayload(UUID referenceId, CompoundTag payload, long gameTime) {
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(payload, "payload");
        List<TemporaryStateReference> references = new ArrayList<>(temporaryState.size());
        boolean found = false;
        for (TemporaryStateReference reference : temporaryState) {
            if (reference.referenceId().equals(referenceId)) {
                references.add(reference.withPayload(payload));
                found = true;
            } else {
                references.add(reference);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown temporary state reference: " + referenceId);
        }
        return new TransformationSession(sessionId, ownerId, snapshotId, scope, gameplayScale, baseline, compatibility, state, createdGameTime, nextGameTime(gameTime), revision + 1, expiresGameTime, failureDetail, references);
    }

    public TransformationSession requestTemporaryStateCleanup(long gameTime) {
        List<TemporaryStateReference> references = new ArrayList<>(temporaryState.size());
        boolean changed = false;
        for (TemporaryStateReference reference : temporaryState) {
            if (reference.status() == TemporaryStateStatus.PREPARED || reference.status() == TemporaryStateStatus.ACTIVE) {
                references.add(reference.withStatus(TemporaryStateStatus.CLEANUP_REQUESTED));
                changed = true;
            } else {
                references.add(reference);
            }
        }
        if (!changed) {
            return this;
        }
        return new TransformationSession(sessionId, ownerId, snapshotId, scope, gameplayScale, baseline, compatibility, state, createdGameTime, nextGameTime(gameTime), revision + 1, expiresGameTime, failureDetail, references);
    }

    public boolean hasOutstandingTemporaryState() {
        return temporaryState.stream().anyMatch(reference -> reference.status().requiresReconciliation());
    }

    private long nextGameTime(long gameTime) {
        if (gameTime < updatedGameTime) {
            throw new IllegalArgumentException("Transformation session time cannot move backwards");
        }
        return gameTime;
    }
}
