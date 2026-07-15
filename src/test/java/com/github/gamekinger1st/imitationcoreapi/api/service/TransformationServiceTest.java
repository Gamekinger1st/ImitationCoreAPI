package com.github.gamekinger1st.imitationcoreapi.api.service;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateStatus;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationLifecycleReason;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationState;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.BaselineSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationServiceTest {
    @Test
    void requiresTemporaryStateToBeReconciledBeforeReversionCompletes() {
        InMemoryRepository repository = new InMemoryRepository();
        TransformationService service = new TransformationService(repository);
        IdentitySnapshot snapshot = IdentitySnapshot.builder(ResourceLocation.withDefaultNamespace("zombie"), 10L)
                .displayName("Zombie")
                .entityData(new CompoundTag())
                .visualData(new CompoundTag())
                .build();
        service.storeSnapshot(snapshot);

        UUID ownerId = UUID.randomUUID();
        TransformationSession session = service.beginSession(ownerId, snapshot.snapshotId(), BaselineSnapshot.empty(), CompatibilityAssessment.full(), 10L).session().orElseThrow();
        session = service.transition(session.sessionId(), TransformationState.CAPTURING, TransformationState.VALIDATING, 11L).session().orElseThrow();
        session = service.transition(session.sessionId(), TransformationState.VALIDATING, TransformationState.APPLYING, 12L).session().orElseThrow();
        session = service.transition(session.sessionId(), TransformationState.APPLYING, TransformationState.ACTIVE, 13L).session().orElseThrow();

        TemporaryStateReference reference = new TemporaryStateReference(
                UUID.randomUUID(),
                session.sessionId(),
                ResourceLocation.withDefaultNamespace("temporary_item"),
                new CompoundTag(),
                TemporaryStateStatus.ACTIVE
        );
        session = service.addTemporaryState(session.sessionId(), reference, 14L).session().orElseThrow();
        session = service.requestCleanup(session.sessionId(), TransformationLifecycleReason.FORCE_REVERT, 15L).session().orElseThrow();
        session = service.completeReversion(session.sessionId(), 16L).session().orElseThrow();

        assertEquals(TransformationState.CLEANING, session.state());
        assertTrue(session.hasOutstandingTemporaryState());

        session = service.updateTemporaryState(session.sessionId(), reference.referenceId(), TemporaryStateStatus.CLEANED, 17L).session().orElseThrow();
        session = service.completeReversion(session.sessionId(), 18L).session().orElseThrow();

        assertEquals(TransformationState.REVERTED, session.state());
        assertFalse(session.hasOutstandingTemporaryState());
    }

    @Test
    void startupRecoveryPersistsCleanupIntentForEveryInterruptedSession() {
        InMemoryRepository repository = new InMemoryRepository();
        TransformationService service = new TransformationService(repository);
        IdentitySnapshot snapshot = IdentitySnapshot.builder(ResourceLocation.withDefaultNamespace("zombie"), 10L)
                .displayName("Zombie")
                .entityData(new CompoundTag())
                .visualData(new CompoundTag())
                .build();
        service.storeSnapshot(snapshot);

        UUID ownerId = UUID.randomUUID();
        TransformationSession session = service.beginSession(ownerId, snapshot.snapshotId(), BaselineSnapshot.empty(), CompatibilityAssessment.full(), 10L).session().orElseThrow();
        session = service.transition(session.sessionId(), TransformationState.CAPTURING, TransformationState.VALIDATING, 11L).session().orElseThrow();
        session = service.transition(session.sessionId(), TransformationState.VALIDATING, TransformationState.APPLYING, 12L).session().orElseThrow();
        session = service.transition(session.sessionId(), TransformationState.APPLYING, TransformationState.ACTIVE, 13L).session().orElseThrow();
        TemporaryStateReference reference = new TemporaryStateReference(
                UUID.randomUUID(),
                session.sessionId(),
                ResourceLocation.withDefaultNamespace("temporary_item"),
                new CompoundTag(),
                TemporaryStateStatus.ACTIVE
        );
        service.addTemporaryState(session.sessionId(), reference, 14L);

        TransformationSession recovered = service.recoverInterruptedSessions(20L).getFirst().session().orElseThrow();

        assertEquals(TransformationState.REVERTING, recovered.state());
        assertEquals(TemporaryStateStatus.CLEANUP_REQUESTED, recovered.temporaryState().getFirst().status());
        assertTrue(recovered.revision() > session.revision());
    }

    @Test
    void updatesTemporaryStatePayloadsWithoutChangingStatus() {
        InMemoryRepository repository = new InMemoryRepository();
        TransformationService service = new TransformationService(repository);
        IdentitySnapshot snapshot = IdentitySnapshot.builder(ResourceLocation.withDefaultNamespace("zombie"), 10L)
                .displayName("Zombie")
                .entityData(new CompoundTag())
                .visualData(new CompoundTag())
                .build();
        service.storeSnapshot(snapshot);
        TransformationSession session = service.beginSession(UUID.randomUUID(), snapshot.snapshotId(), BaselineSnapshot.empty(), CompatibilityAssessment.full(), 10L).session().orElseThrow();
        CompoundTag payload = new CompoundTag();
        payload.putString("value", "old");
        TemporaryStateReference reference = new TemporaryStateReference(UUID.randomUUID(), session.sessionId(), ResourceLocation.withDefaultNamespace("temporary_item"), payload, TemporaryStateStatus.ACTIVE);
        session = service.addTemporaryState(session.sessionId(), reference, 11L).session().orElseThrow();
        CompoundTag updatedPayload = new CompoundTag();
        updatedPayload.putString("value", "new");

        TransformationSession updated = service.updateTemporaryStatePayload(session.sessionId(), reference.referenceId(), updatedPayload, 12L).session().orElseThrow();

        assertEquals(TemporaryStateStatus.ACTIVE, updated.temporaryState().getFirst().status());
        assertEquals("new", updated.temporaryState().getFirst().payload().getString("value"));
    }

    @Test
    void updatesPersistedSessionExpiration() {
        InMemoryRepository repository = new InMemoryRepository();
        TransformationService service = new TransformationService(repository);
        IdentitySnapshot snapshot = IdentitySnapshot.builder(ResourceLocation.withDefaultNamespace("zombie"), 10L)
                .displayName("Zombie")
                .entityData(new CompoundTag())
                .visualData(new CompoundTag())
                .build();
        service.storeSnapshot(snapshot);
        TransformationSession session = service.beginSession(UUID.randomUUID(), snapshot.snapshotId(), BaselineSnapshot.empty(), CompatibilityAssessment.full(), 10L).session().orElseThrow();

        TransformationSession updated = service.updateExpiration(session.sessionId(), OptionalLong.of(1_210L), 11L).session().orElseThrow();

        assertEquals(OptionalLong.of(1_210L), updated.expiresGameTime());
        assertEquals(session.revision() + 1, updated.revision());
    }

    @Test
    void rejectsUnsupportedCompatibilityAndExistingRecoverySession() {
        InMemoryRepository repository = new InMemoryRepository();
        TransformationService service = new TransformationService(repository);
        IdentitySnapshot snapshot = IdentitySnapshot.builder(ResourceLocation.withDefaultNamespace("zombie"), 10L)
                .displayName("Zombie")
                .build();
        service.storeSnapshot(snapshot);
        UUID ownerId = UUID.randomUUID();

        SessionTransitionResult unsupported = service.beginSession(ownerId, snapshot.snapshotId(), BaselineSnapshot.empty(), CompatibilityAssessment.unsupported("missing gameplay adapter"), 10L);

        assertFalse(unsupported.accepted());
        assertEquals("The requested snapshot is unsupported: missing gameplay adapter", unsupported.message());

        TransformationSession active = service.beginSession(ownerId, snapshot.snapshotId(), BaselineSnapshot.empty(), CompatibilityAssessment.full(), 11L).session().orElseThrow();
        service.requestCleanup(active.sessionId(), TransformationLifecycleReason.FORCE_REVERT, 12L);

        SessionTransitionResult blocked = service.beginSession(ownerId, snapshot.snapshotId(), BaselineSnapshot.empty(), CompatibilityAssessment.full(), 13L);

        assertFalse(blocked.accepted());
        assertEquals("The owner already has a session that requires recovery", blocked.message());
    }

    private static final class InMemoryRepository implements TransformationRepository {
        private final Map<UUID, IdentitySnapshot> snapshots = new LinkedHashMap<>();
        private final Map<UUID, TransformationSession> sessions = new LinkedHashMap<>();

        @Override
        public Optional<IdentitySnapshot> snapshot(UUID snapshotId) {
            return Optional.ofNullable(snapshots.get(snapshotId));
        }

        @Override
        public void saveSnapshot(IdentitySnapshot snapshot) {
            snapshots.put(snapshot.snapshotId(), snapshot);
        }

        @Override
        public Optional<TransformationSession> session(UUID sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        @Override
        public Collection<TransformationSession> sessionsForOwner(UUID ownerId) {
            return sessions.values().stream().filter(session -> session.ownerId().equals(ownerId)).toList();
        }

        @Override
        public Collection<TransformationSession> sessions() {
            return sessions.values();
        }

        @Override
        public void saveSession(TransformationSession session) {
            sessions.put(session.sessionId(), session);
        }
    }
}
