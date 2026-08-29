package com.github.gamekinger1st.imitationcoreapi.api.session;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.BaselineSnapshot;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationSessionTest {
    @Test
    void transitionsThroughAReversibleLifecycle() {
        TransformationSession session = TransformationSession.begin(UUID.randomUUID(), UUID.randomUUID(), BaselineSnapshot.empty(), CompatibilityAssessment.full(), 10L);

        session = session.transitionTo(TransformationState.VALIDATING, 11L, Optional.empty());
        session = session.transitionTo(TransformationState.APPLYING, 12L, Optional.empty());
        session = session.transitionTo(TransformationState.ACTIVE, 13L, Optional.empty());
        session = session.transitionTo(TransformationState.REVERTING, 14L, Optional.empty());
        session = session.transitionTo(TransformationState.REVERTED, 15L, Optional.empty());

        assertEquals(TransformationState.REVERTED, session.state());
        assertTrue(session.state().isTerminal());
        assertFalse(session.state().requiresRecovery());
    }

    @Test
    void preservesSurfaceScopeAcrossTransitions() {
        TransformationSession session = TransformationSession.begin(UUID.randomUUID(), UUID.randomUUID(), TransformationScope.SURFACE, BaselineSnapshot.empty(), CompatibilityAssessment.visual("surface"), 10L);

        session = session.transitionTo(TransformationState.VALIDATING, 11L, Optional.empty());

        assertEquals(TransformationScope.SURFACE, session.scope());
    }

    @Test
    void preservesGameplayScaleAcrossTransitions() {
        TransformationSession session = TransformationSession.begin(UUID.randomUUID(), UUID.randomUUID(), TransformationScope.GAMEPLAY, 0.85D, BaselineSnapshot.empty(), CompatibilityAssessment.full(), 10L);

        session = session.transitionTo(TransformationState.VALIDATING, 11L, Optional.empty());

        assertEquals(0.85D, session.gameplayScale());
    }

    @Test
    void rejectsInvalidTransitionsAndCrossSessionTemporaryState() {
        TransformationSession session = TransformationSession.begin(UUID.randomUUID(), UUID.randomUUID(), BaselineSnapshot.empty(), CompatibilityAssessment.full(), 10L);
        assertThrows(IllegalStateException.class, () -> session.transitionTo(TransformationState.ACTIVE, 11L, Optional.empty()));

        TemporaryStateReference reference = new TemporaryStateReference(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TemporaryStateKinds.EFFECT,
                new CompoundTag(),
                TemporaryStateStatus.ACTIVE
        );
        assertThrows(IllegalArgumentException.class, () -> session.addTemporaryState(reference, 11L));
    }

    @Test
    void rejectsTemporaryStateRegressionsAfterCleanup() {
        TransformationSession session = TransformationSession.begin(UUID.randomUUID(), UUID.randomUUID(), BaselineSnapshot.empty(), CompatibilityAssessment.full(), 10L);
        UUID referenceId = UUID.randomUUID();
        TemporaryStateReference reference = new TemporaryStateReference(referenceId, session.sessionId(), TemporaryStateKinds.EFFECT, new CompoundTag(), TemporaryStateStatus.PREPARED);
        TransformationSession cleaned = session.addTemporaryState(reference, 11L).updateTemporaryState(referenceId, TemporaryStateStatus.CLEANED, 12L);

        assertThrows(IllegalArgumentException.class, () -> cleaned.updateTemporaryState(referenceId, TemporaryStateStatus.ACTIVE, 13L));
    }

    @Test
    void rejectsOversizedTemporaryStatePayloads() {
        CompoundTag payload = new CompoundTag();
        payload.putString("data", "x".repeat(TemporaryStateReference.MAX_PAYLOAD_BYTES));

        assertThrows(IllegalArgumentException.class, () -> new TemporaryStateReference(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TemporaryStateKinds.EFFECT,
                payload,
                TemporaryStateStatus.PREPARED
        ));
    }
}
