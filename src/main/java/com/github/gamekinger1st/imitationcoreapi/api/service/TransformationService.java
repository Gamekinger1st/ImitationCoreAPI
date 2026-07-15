package com.github.gamekinger1st.imitationcoreapi.api.service;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.event.TransformationEvent;
import com.github.gamekinger1st.imitationcoreapi.api.event.TransformationEventType;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateStatus;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationLifecycleReason;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationState;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.BaselineSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class TransformationService {
    private final TransformationRepository repository;

    public TransformationService(TransformationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public IdentitySnapshot storeSnapshot(IdentitySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        repository.saveSnapshot(snapshot);
        ImitationApi.events().post(new TransformationEvent(TransformationEventType.SNAPSHOT_STORED, Optional.empty(), Optional.of(snapshot), Optional.empty(), Optional.empty()));
        return snapshot;
    }

    public Optional<IdentitySnapshot> snapshot(UUID snapshotId) {
        return repository.snapshot(Objects.requireNonNull(snapshotId, "snapshotId"));
    }

    public Optional<TransformationSession> session(UUID sessionId) {
        return repository.session(Objects.requireNonNull(sessionId, "sessionId"));
    }

    public Optional<TransformationSession> activeSessionForOwner(UUID ownerId) {
        return repository.sessionsForOwner(Objects.requireNonNull(ownerId, "ownerId")).stream()
                .filter(session -> session.state() == TransformationState.ACTIVE)
                .max(Comparator.comparing(TransformationSession::createdGameTime));
    }

    public Collection<TransformationSession> sessionsForOwner(UUID ownerId) {
        return repository.sessionsForOwner(Objects.requireNonNull(ownerId, "ownerId"));
    }

    public Collection<TransformationSession> activeSessions() {
        return repository.sessions().stream().filter(session -> session.state() == TransformationState.ACTIVE).toList();
    }

    public SessionTransitionResult beginSession(UUID ownerId, UUID snapshotId, BaselineSnapshot baseline, CompatibilityAssessment compatibility, long gameTime) {
        return beginSession(ownerId, snapshotId, TransformationScope.GAMEPLAY, baseline, compatibility, gameTime);
    }

    public SessionTransitionResult beginSession(UUID ownerId, UUID snapshotId, TransformationScope scope, BaselineSnapshot baseline, CompatibilityAssessment compatibility, long gameTime) {
        return beginSession(ownerId, snapshotId, scope, 1D, baseline, compatibility, gameTime);
    }

    public SessionTransitionResult beginSession(UUID ownerId, UUID snapshotId, TransformationScope scope, double gameplayScale, BaselineSnapshot baseline, CompatibilityAssessment compatibility, long gameTime) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(scope, "scope");
        if (!Double.isFinite(gameplayScale) || gameplayScale < 0D || gameplayScale > 1D) {
            throw new IllegalArgumentException("gameplayScale must be between zero and one");
        }
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(compatibility, "compatibility");
        if (repository.snapshot(snapshotId).isEmpty()) {
            return SessionTransitionResult.rejected("The requested snapshot does not exist");
        }
        if (!compatibility.level().isUsable()) {
            return SessionTransitionResult.rejected("The requested snapshot is unsupported: " + String.join("; ", compatibility.reasons()));
        }
        for (TransformationSession existing : repository.sessionsForOwner(ownerId)) {
            if (existing.state().requiresRecovery()) {
                return SessionTransitionResult.rejected("The owner already has a session that requires recovery");
            }
        }
        TransformationSession session = TransformationSession.begin(ownerId, snapshotId, scope, gameplayScale, baseline, compatibility, gameTime);
        repository.saveSession(session);
        ImitationApi.events().post(new TransformationEvent(TransformationEventType.SESSION_CREATED, Optional.of(ownerId), Optional.empty(), Optional.of(session), Optional.empty()));
        return SessionTransitionResult.accepted(session);
    }

    public SessionTransitionResult transition(UUID sessionId, TransformationState expectedState, TransformationState targetState, long gameTime) {
        return transition(sessionId, expectedState, targetState, gameTime, Optional.empty(), Optional.empty());
    }

    public SessionTransitionResult fail(UUID sessionId, TransformationState expectedState, String detail, long gameTime, TransformationLifecycleReason reason) {
        return transition(sessionId, expectedState, TransformationState.FAILED, gameTime, Optional.of(detail), Optional.of(reason));
    }

    public SessionTransitionResult requestCleanup(UUID sessionId, TransformationLifecycleReason reason, long gameTime) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(reason, "reason");
        Optional<TransformationSession> found = repository.session(sessionId);
        if (found.isEmpty()) {
            return SessionTransitionResult.rejected("The requested session does not exist");
        }
        TransformationSession session = found.get();
        if (session.state().isTerminal()) {
            return SessionTransitionResult.accepted(session);
        }
        SessionTransitionResult transitionResult;
        if (session.state() == TransformationState.REVERTING || session.state() == TransformationState.CLEANING) {
            transitionResult = SessionTransitionResult.accepted(session);
        } else {
            transitionResult = transition(sessionId, session.state(), TransformationState.REVERTING, gameTime, Optional.empty(), Optional.of(reason));
        }
        if (!transitionResult.accepted()) {
            return transitionResult;
        }
        TransformationSession updated = transitionResult.session().orElseThrow().requestTemporaryStateCleanup(gameTime);
        if (updated != transitionResult.session().orElseThrow()) {
            repository.saveSession(updated);
            ImitationApi.events().post(new TransformationEvent(TransformationEventType.TEMPORARY_STATE_UPDATED, Optional.of(updated.ownerId()), Optional.empty(), Optional.of(updated), Optional.of(reason)));
        }
        ImitationApi.events().post(new TransformationEvent(TransformationEventType.CLEANUP_REQUESTED, Optional.of(updated.ownerId()), Optional.empty(), Optional.of(updated), Optional.of(reason)));
        return SessionTransitionResult.accepted(updated);
    }

    public List<SessionTransitionResult> requestCleanupForOwner(UUID ownerId, TransformationLifecycleReason reason, long gameTime) {
        Objects.requireNonNull(ownerId, "ownerId");
        List<SessionTransitionResult> results = new ArrayList<>();
        for (TransformationSession session : repository.sessionsForOwner(ownerId)) {
            results.add(requestCleanup(session.sessionId(), reason, gameTime));
        }
        return List.copyOf(results);
    }

    public List<SessionTransitionResult> requestCleanupForAll(TransformationLifecycleReason reason, long gameTime) {
        Objects.requireNonNull(reason, "reason");
        List<TransformationSession> sessions = repository.sessions().stream()
                .sorted(Comparator.comparing(TransformationSession::createdGameTime))
                .toList();
        List<SessionTransitionResult> results = new ArrayList<>(sessions.size());
        for (TransformationSession session : sessions) {
            results.add(requestCleanup(session.sessionId(), reason, gameTime));
        }
        return List.copyOf(results);
    }

    public List<SessionTransitionResult> recoverInterruptedSessions(long gameTime) {
        List<SessionTransitionResult> results = new ArrayList<>();
        for (TransformationSession session : sessionsRequiringRecovery()) {
            SessionTransitionResult result = requestCleanup(session.sessionId(), TransformationLifecycleReason.SERVER_STARTUP_RECOVERY, gameTime);
            result.session().ifPresent(recovered -> ImitationApi.events().post(new TransformationEvent(TransformationEventType.RECOVERY_REQUIRED, Optional.of(recovered.ownerId()), Optional.empty(), Optional.of(recovered), Optional.of(TransformationLifecycleReason.SERVER_STARTUP_RECOVERY))));
            results.add(result);
        }
        return List.copyOf(results);
    }

    public List<SessionTransitionResult> recoverOwner(UUID ownerId, long gameTime) {
        Objects.requireNonNull(ownerId, "ownerId");
        List<SessionTransitionResult> results = new ArrayList<>();
        for (TransformationSession session : repository.sessionsForOwner(ownerId)) {
            if (!session.state().requiresRecovery()) {
                continue;
            }
            SessionTransitionResult result = requestCleanup(session.sessionId(), TransformationLifecycleReason.RECONNECT, gameTime);
            result.session().ifPresent(recovered -> ImitationApi.events().post(new TransformationEvent(TransformationEventType.RECOVERY_REQUIRED, Optional.of(recovered.ownerId()), Optional.empty(), Optional.of(recovered), Optional.of(TransformationLifecycleReason.RECONNECT))));
            results.add(result);
        }
        return List.copyOf(results);
    }

    public SessionTransitionResult addTemporaryState(UUID sessionId, TemporaryStateReference reference, long gameTime) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(reference, "reference");
        Optional<TransformationSession> found = repository.session(sessionId);
        if (found.isEmpty()) {
            return SessionTransitionResult.rejected("The requested session does not exist");
        }
        try {
            TransformationSession updated = found.get().addTemporaryState(reference, gameTime);
            repository.saveSession(updated);
            ImitationApi.events().post(new TransformationEvent(TransformationEventType.TEMPORARY_STATE_ADDED, Optional.of(updated.ownerId()), Optional.empty(), Optional.of(updated), Optional.empty()));
            return SessionTransitionResult.accepted(updated);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return SessionTransitionResult.rejected(exception.getMessage());
        }
    }

    public SessionTransitionResult updateTemporaryState(UUID sessionId, UUID referenceId, TemporaryStateStatus status, long gameTime) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(status, "status");
        Optional<TransformationSession> found = repository.session(sessionId);
        if (found.isEmpty()) {
            return SessionTransitionResult.rejected("The requested session does not exist");
        }
        try {
            TransformationSession updated = found.get().updateTemporaryState(referenceId, status, gameTime);
            repository.saveSession(updated);
            ImitationApi.events().post(new TransformationEvent(TransformationEventType.TEMPORARY_STATE_UPDATED, Optional.of(updated.ownerId()), Optional.empty(), Optional.of(updated), Optional.empty()));
            return SessionTransitionResult.accepted(updated);
        } catch (IllegalArgumentException exception) {
            return SessionTransitionResult.rejected(exception.getMessage());
        }
    }

    public SessionTransitionResult updateTemporaryStatePayload(UUID sessionId, UUID referenceId, CompoundTag payload, long gameTime) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(payload, "payload");
        Optional<TransformationSession> found = repository.session(sessionId);
        if (found.isEmpty()) {
            return SessionTransitionResult.rejected("The requested session does not exist");
        }
        try {
            TransformationSession updated = found.get().updateTemporaryStatePayload(referenceId, payload, gameTime);
            repository.saveSession(updated);
            ImitationApi.events().post(new TransformationEvent(TransformationEventType.TEMPORARY_STATE_UPDATED, Optional.of(updated.ownerId()), Optional.empty(), Optional.of(updated), Optional.empty()));
            return SessionTransitionResult.accepted(updated);
        } catch (IllegalArgumentException exception) {
            return SessionTransitionResult.rejected(exception.getMessage());
        }
    }

    public SessionTransitionResult updateExpiration(UUID sessionId, OptionalLong expiresGameTime, long gameTime) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(expiresGameTime, "expiresGameTime");
        Optional<TransformationSession> found = repository.session(sessionId);
        if (found.isEmpty()) {
            return SessionTransitionResult.rejected("The requested session does not exist");
        }
        try {
            TransformationSession updated = found.get().withExpiresGameTime(expiresGameTime, gameTime);
            repository.saveSession(updated);
            ImitationApi.events().post(new TransformationEvent(TransformationEventType.SESSION_UPDATED, Optional.of(updated.ownerId()), Optional.empty(), Optional.of(updated), Optional.empty()));
            return SessionTransitionResult.accepted(updated);
        } catch (IllegalArgumentException exception) {
            return SessionTransitionResult.rejected(exception.getMessage());
        }
    }

    public SessionTransitionResult completeReversion(UUID sessionId, long gameTime) {
        Optional<TransformationSession> found = repository.session(Objects.requireNonNull(sessionId, "sessionId"));
        if (found.isEmpty()) {
            return SessionTransitionResult.rejected("The requested session does not exist");
        }
        TransformationSession session = found.get();
        if (session.state() == TransformationState.REVERTED) {
            return SessionTransitionResult.accepted(session);
        }
        if (session.state() != TransformationState.REVERTING && session.state() != TransformationState.CLEANING) {
            return SessionTransitionResult.rejected("The session is not reverting");
        }
        if (session.hasOutstandingTemporaryState()) {
            if (session.state() == TransformationState.REVERTING) {
                return transition(sessionId, TransformationState.REVERTING, TransformationState.CLEANING, gameTime, Optional.empty(), Optional.empty());
            }
            return SessionTransitionResult.rejected("The session still has temporary state requiring reconciliation");
        }
        return transition(sessionId, session.state(), TransformationState.REVERTED, gameTime, Optional.empty(), Optional.empty());
    }

    public Collection<TransformationSession> sessionsRequiringRecovery() {
        return repository.sessions().stream().filter(session -> session.state().requiresRecovery()).toList();
    }

    private SessionTransitionResult transition(
            UUID sessionId,
            TransformationState expectedState,
            TransformationState targetState,
            long gameTime,
            Optional<String> failureDetail,
            Optional<TransformationLifecycleReason> lifecycleReason
    ) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(expectedState, "expectedState");
        Objects.requireNonNull(targetState, "targetState");
        Objects.requireNonNull(failureDetail, "failureDetail");
        Objects.requireNonNull(lifecycleReason, "lifecycleReason");
        Optional<TransformationSession> found = repository.session(sessionId);
        if (found.isEmpty()) {
            return SessionTransitionResult.rejected("The requested session does not exist");
        }
        TransformationSession current = found.get();
        if (current.state() != expectedState) {
            return SessionTransitionResult.rejected("The session state changed before this transition could be applied");
        }
        try {
            TransformationSession updated = current.transitionTo(targetState, gameTime, failureDetail);
            repository.saveSession(updated);
            ImitationApi.events().post(new TransformationEvent(TransformationEventType.SESSION_TRANSITIONED, Optional.of(updated.ownerId()), Optional.empty(), Optional.of(updated), lifecycleReason));
            return SessionTransitionResult.accepted(updated);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return SessionTransitionResult.rejected(exception.getMessage());
        }
    }
}
