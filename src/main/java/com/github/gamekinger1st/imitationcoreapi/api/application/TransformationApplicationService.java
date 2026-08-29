package com.github.gamekinger1st.imitationcoreapi.api.application;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnostics;
import com.github.gamekinger1st.imitationcoreapi.api.event.TransformationEvent;
import com.github.gamekinger1st.imitationcoreapi.api.event.TransformationEventType;
import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateStatus;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationLifecycleReason;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationState;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TransformationApplicationService {
    private final MinecraftServer server;
    private final TransformationService transformations;
    private final TransformationApplicationRegistry adapters;

    public TransformationApplicationService(MinecraftServer server, TransformationService transformations, TransformationApplicationRegistry adapters) {
        this.server = Objects.requireNonNull(server, "server");
        this.transformations = Objects.requireNonNull(transformations, "transformations");
        this.adapters = Objects.requireNonNull(adapters, "adapters");
    }

    public SessionTransitionResult apply(ServerPlayer owner, UUID sessionId) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(sessionId, "sessionId");
        Optional<TransformationSession> found = transformations.session(sessionId);
        if (found.isEmpty()) {
            return SessionTransitionResult.rejected("The requested session does not exist");
        }
        TransformationSession session = found.get();
        if (!session.ownerId().equals(owner.getUUID())) {
            return SessionTransitionResult.rejected("The requested session does not belong to this player");
        }
        if (session.state() != TransformationState.CAPTURING) {
            return SessionTransitionResult.rejected("The requested session is not ready to apply");
        }
        Optional<IdentitySnapshot> snapshot = transformations.snapshot(session.snapshotId());
        if (snapshot.isEmpty()) {
            return failAndRevert(owner, sessionId, TransformationState.CAPTURING, "The transformation snapshot is unavailable", owner.level().getGameTime());
        }
        long gameTime = owner.level().getGameTime();
        SessionTransitionResult validating = transformations.transition(sessionId, TransformationState.CAPTURING, TransformationState.VALIDATING, gameTime);
        if (!validating.accepted()) {
            return validating;
        }
        TransformationApplicationContext validationContext = context(owner, validating.session().orElseThrow(), snapshot.get(), gameTime);
        for (TransformationApplicationAdapter adapter : adapters.applyOrder(session.scope())) {
            try {
                Optional<String> rejection = Objects.requireNonNull(adapter.validate(validationContext), "adapter validation result");
                if (rejection.isPresent()) {
                    return failAndRevert(owner, sessionId, TransformationState.VALIDATING, boundedMessage(rejection.get()), gameTime);
                }
            } catch (RuntimeException | LinkageError exception) {
                return failAndRevert(owner, sessionId, TransformationState.VALIDATING, adapter.id() + " validation failed: " + message(exception), gameTime);
            }
        }
        ImitationApi.events().post(new TransformationEvent(TransformationEventType.TRANSFORMATION_VALIDATED, Optional.of(owner.getUUID()), snapshot, validating.session(), Optional.empty()));
        SessionTransitionResult applying = transformations.transition(sessionId, TransformationState.VALIDATING, TransformationState.APPLYING, gameTime);
        if (!applying.accepted()) {
            return applying;
        }
        for (TransformationApplicationAdapter adapter : adapters.applyOrder(session.scope())) {
            TransformationSession current = transformations.session(sessionId).orElseThrow();
            TransformationApplicationContext applicationContext = context(owner, current, snapshot.get(), gameTime);
            List<TemporaryStateReference> references;
            try {
                references = registerPreparedState(sessionId, adapter, applicationContext, gameTime);
            } catch (RuntimeException | LinkageError exception) {
                return failAndRevert(owner, sessionId, TransformationState.APPLYING, adapter.id() + " preparation failed: " + message(exception), gameTime);
            }
            try {
                TransformationSession prepared = transformations.session(sessionId).orElseThrow();
                adapter.apply(context(owner, prepared, snapshot.get(), gameTime), List.copyOf(references));
            } catch (RuntimeException | LinkageError exception) {
                return failAndRevert(owner, sessionId, TransformationState.APPLYING, adapter.id() + " application failed: " + message(exception), gameTime);
            }
            for (TemporaryStateReference reference : references) {
                SessionTransitionResult activated = transformations.updateTemporaryState(sessionId, reference.referenceId(), TemporaryStateStatus.ACTIVE, gameTime);
                if (!activated.accepted()) {
                    return failAndRevert(owner, sessionId, TransformationState.APPLYING, "Could not activate temporary state: " + activated.message(), gameTime);
                }
            }
        }
        SessionTransitionResult active = transformations.transition(sessionId, TransformationState.APPLYING, TransformationState.ACTIVE, gameTime);
        active.session().ifPresent(updated -> ImitationApi.events().post(new TransformationEvent(TransformationEventType.TRANSFORMATION_APPLIED, Optional.of(owner.getUUID()), snapshot, Optional.of(updated), Optional.empty())));
        return active;
    }

    public SessionTransitionResult requestReversion(Optional<ServerPlayer> owner, UUID sessionId, TransformationLifecycleReason reason, long gameTime) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(reason, "reason");
        Optional<TransformationSession> requested = transformations.session(sessionId);
        if (requested.isEmpty()) {
            return SessionTransitionResult.rejected("The requested session does not exist");
        }
        if (owner.isPresent() && !owner.get().getUUID().equals(requested.get().ownerId())) {
            return SessionTransitionResult.rejected("The requested session does not belong to this player");
        }
        SessionTransitionResult cleanup = transformations.requestCleanup(sessionId, reason, gameTime);
        if (!cleanup.accepted()) {
            ImitationDiagnostics.cleanupFailed(owner, sessionId, cleanup.message(), gameTime);
            return cleanup;
        }
        cleanup.session().ifPresent(session -> ImitationApi.events().post(new TransformationEvent(TransformationEventType.REVERSION_REQUESTED, Optional.of(session.ownerId()), Optional.empty(), Optional.of(session), Optional.of(reason))));
        TransformationSession initial = cleanup.session().orElseThrow();
        if (initial.state().isTerminal()) {
            return cleanup;
        }
        Optional<IdentitySnapshot> snapshot = transformations.snapshot(initial.snapshotId());
        for (TransformationApplicationAdapter adapter : adapters.revertOrder(initial.scope())) {
            TransformationSession current = transformations.session(sessionId).orElseThrow();
            List<TemporaryStateReference> references = current.temporaryState().stream()
                    .filter(reference -> reference.handlerId().equals(adapter.id()))
                    .filter(reference -> reference.status().requiresReconciliation())
                    .toList();
            if (references.isEmpty()) {
                continue;
            }
            TransformationReversionContext context = new TransformationReversionContext(server, owner, current, snapshot, reason, gameTime);
            try {
                adapter.revert(context, references);
            } catch (RuntimeException | LinkageError exception) {
                quarantine(sessionId, references, gameTime);
                String detail = adapter.id() + " reversion failed: " + message(exception);
                ImitationDiagnostics.cleanupFailed(owner, sessionId, detail, gameTime);
                return SessionTransitionResult.rejected(detail);
            }
            for (TemporaryStateReference reference : references) {
                SessionTransitionResult cleaned = transformations.updateTemporaryState(sessionId, reference.referenceId(), TemporaryStateStatus.CLEANED, gameTime);
                if (!cleaned.accepted()) {
                    ImitationDiagnostics.cleanupFailed(owner, sessionId, cleaned.message(), gameTime);
                    return cleaned;
                }
            }
        }
        TransformationSession afterAdapters = transformations.session(sessionId).orElseThrow();
        List<TemporaryStateReference> unhandled = afterAdapters.temporaryState().stream()
                .filter(reference -> reference.status().requiresReconciliation())
                .toList();
        if (!unhandled.isEmpty()) {
            quarantine(sessionId, unhandled, gameTime);
            String detail = "Some temporary transformation state has no registered cleanup adapter";
            ImitationDiagnostics.cleanupFailed(owner, sessionId, detail, gameTime);
            return SessionTransitionResult.rejected(detail);
        }
        SessionTransitionResult completed = transformations.completeReversion(sessionId, gameTime);
        if (!completed.accepted()) {
            ImitationDiagnostics.cleanupFailed(owner, sessionId, completed.message(), gameTime);
        }
        completed.session().ifPresent(session -> {
            ImitationApi.events().post(new TransformationEvent(TransformationEventType.REVERSION_COMPLETED, Optional.of(session.ownerId()), snapshot, Optional.of(session), Optional.of(reason)));
            ImitationApi.events().post(new TransformationEvent(TransformationEventType.CLEANUP_COMPLETED, Optional.of(session.ownerId()), snapshot, Optional.of(session), Optional.of(reason)));
        });
        return completed;
    }

    public List<SessionTransitionResult> requestReversionForOwner(Optional<ServerPlayer> owner, UUID ownerId, TransformationLifecycleReason reason, long gameTime) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(reason, "reason");
        List<SessionTransitionResult> results = new ArrayList<>();
        for (TransformationSession session : sessionsForOwner(ownerId)) {
            results.add(requestReversion(owner, session.sessionId(), reason, gameTime));
        }
        return List.copyOf(results);
    }

    public List<SessionTransitionResult> recoverInterruptedSessions(long gameTime) {
        List<SessionTransitionResult> results = new ArrayList<>();
        for (TransformationSession session : transformations.sessionsRequiringRecovery()) {
            Optional<ServerPlayer> owner = Optional.ofNullable(server.getPlayerList().getPlayer(session.ownerId()));
            results.add(requestReversion(owner, session.sessionId(), TransformationLifecycleReason.SERVER_STARTUP_RECOVERY, gameTime));
        }
        return List.copyOf(results);
    }

    public List<SessionTransitionResult> requestReversionForAll(TransformationLifecycleReason reason, long gameTime) {
        Objects.requireNonNull(reason, "reason");
        List<SessionTransitionResult> results = new ArrayList<>();
        for (TransformationSession session : transformations.sessionsRequiringRecovery()) {
            Optional<ServerPlayer> owner = Optional.ofNullable(server.getPlayerList().getPlayer(session.ownerId()));
            results.add(requestReversion(owner, session.sessionId(), reason, gameTime));
        }
        return List.copyOf(results);
    }

    private List<TemporaryStateReference> registerPreparedState(UUID sessionId, TransformationApplicationAdapter adapter, TransformationApplicationContext context, long gameTime) {
        List<TemporaryStateDefinition> definitions = new ArrayList<>();
        definitions.add(new TemporaryStateDefinition(TemporaryStateKinds.APPLICATION_MARKER, new CompoundTag()));
        definitions.addAll(Objects.requireNonNull(adapter.prepare(context), "adapter preparation result"));
        List<TemporaryStateReference> references = new ArrayList<>(definitions.size());
        for (TemporaryStateDefinition definition : definitions) {
            TemporaryStateReference reference = new TemporaryStateReference(
                    UUID.randomUUID(),
                    sessionId,
                    adapter.id(),
                    definition.kind(),
                    definition.payload(),
                    TemporaryStateStatus.PREPARED
            );
            SessionTransitionResult added = transformations.addTemporaryState(sessionId, reference, gameTime);
            if (!added.accepted()) {
                throw new IllegalStateException(added.message());
            }
            references.add(reference);
        }
        return references;
    }

    private SessionTransitionResult failAndRevert(ServerPlayer owner, UUID sessionId, TransformationState expectedState, String detail, long gameTime) {
        ImitationDiagnostics.cleanupFailed(Optional.of(owner), sessionId, boundedMessage(detail), gameTime);
        SessionTransitionResult failed = transformations.fail(sessionId, expectedState, boundedMessage(detail), gameTime, TransformationLifecycleReason.APPLY_FAILURE);
        if (failed.accepted()) {
            requestReversion(Optional.of(owner), sessionId, TransformationLifecycleReason.APPLY_FAILURE, gameTime);
        }
        return SessionTransitionResult.rejected(boundedMessage(detail));
    }

    private void quarantine(UUID sessionId, List<TemporaryStateReference> references, long gameTime) {
        for (TemporaryStateReference reference : references) {
            transformations.updateTemporaryState(sessionId, reference.referenceId(), TemporaryStateStatus.QUARANTINED, gameTime);
        }
    }

    private Collection<TransformationSession> sessionsForOwner(UUID ownerId) {
        return transformations.sessionsRequiringRecovery().stream().filter(session -> session.ownerId().equals(ownerId)).toList();
    }

    private TransformationApplicationContext context(ServerPlayer owner, TransformationSession session, IdentitySnapshot snapshot, long gameTime) {
        return new TransformationApplicationContext(server, owner, session, snapshot, gameTime);
    }

    private String message(Throwable exception) {
        return message(exception.getMessage());
    }

    private String message(String value) {
        return value == null || value.isBlank() ? "unknown error" : boundedMessage(value);
    }

    private String boundedMessage(String value) {
        String normalized = Objects.requireNonNull(value, "value").strip();
        return normalized.length() > 384 ? normalized.substring(0, 384) : normalized;
    }
}
