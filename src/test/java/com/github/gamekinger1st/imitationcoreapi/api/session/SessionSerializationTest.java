package com.github.gamekinger1st.imitationcoreapi.api.session;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.BaselineSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionSerializationTest {
    @Test
    void preservesSchemaAndCleanupLedgerAcrossRoundTrips() {
        TransformationSession session = TransformationSession.begin(UUID.randomUUID(), UUID.randomUUID(), TransformationScope.GAMEPLAY, 0.75D, BaselineSnapshot.empty(), CompatibilityAssessment.full(), 1L)
                .transitionTo(TransformationState.VALIDATING, 2L, Optional.empty())
                .transitionTo(TransformationState.APPLYING, 3L, Optional.empty());
        session = session.addTemporaryState(new TemporaryStateReference(
                UUID.randomUUID(),
                session.sessionId(),
                TemporaryStateKinds.EFFECT,
                new CompoundTag(),
                TemporaryStateStatus.ACTIVE
        ), 4L);

        TransformationSession loaded = SessionSerialization.fromTag(SessionSerialization.toTag(session));

        assertEquals(session, loaded);
    }

    @Test
    void preservesTransformExpirationAcrossRoundTrips() {
        TransformationSession session = TransformationSession.begin(UUID.randomUUID(), UUID.randomUUID(), TransformationScope.GAMEPLAY, 1D, BaselineSnapshot.empty(), CompatibilityAssessment.full(), 10L)
                .withExpiresGameTime(OptionalLong.of(1_210L), 11L);

        TransformationSession loaded = SessionSerialization.fromTag(SessionSerialization.toTag(session));

        assertEquals(OptionalLong.of(1_210L), loaded.expiresGameTime());
        assertEquals(session, loaded);
    }

    @Test
    void migratesLegacyTemporaryStateToAnUnassignedHandler() {
        TransformationSession session = TransformationSession.begin(UUID.randomUUID(), UUID.randomUUID(), BaselineSnapshot.empty(), CompatibilityAssessment.full(), 1L)
                .transitionTo(TransformationState.VALIDATING, 2L, Optional.empty());
        session = session.addTemporaryState(new TemporaryStateReference(
                UUID.randomUUID(),
                session.sessionId(),
                TemporaryStateKinds.EFFECT,
                new CompoundTag(),
                TemporaryStateStatus.ACTIVE
        ), 3L);
        CompoundTag legacy = SessionSerialization.toTag(session);
        legacy.putInt("schema", 1);
        legacy.getList("temporary_state", Tag.TAG_COMPOUND).getCompound(0).remove("handler");

        TransformationSession loaded = SessionSerialization.fromTag(legacy);

        assertEquals(TemporaryStateKinds.UNASSIGNED_HANDLER, loaded.temporaryState().getFirst().handlerId());
        assertEquals(TransformationScope.GAMEPLAY, loaded.scope());
    }

    @Test
    void migratesSchemaTwoSessionsToGameplayScope() {
        TransformationSession session = TransformationSession.begin(UUID.randomUUID(), UUID.randomUUID(), BaselineSnapshot.empty(), CompatibilityAssessment.full(), 1L);
        CompoundTag legacy = SessionSerialization.toTag(session);
        legacy.putInt("schema", 2);
        legacy.remove("scope");

        TransformationSession loaded = SessionSerialization.fromTag(legacy);

        assertEquals(TransformationScope.GAMEPLAY, loaded.scope());
        assertEquals(1D, loaded.gameplayScale());
    }

    @Test
    void migratesSchemaThreeSessionsToUnitGameplayScale() {
        TransformationSession session = TransformationSession.begin(UUID.randomUUID(), UUID.randomUUID(), TransformationScope.GAMEPLAY, 0.75D, BaselineSnapshot.empty(), CompatibilityAssessment.full(), 1L);
        CompoundTag legacy = SessionSerialization.toTag(session);
        legacy.putInt("schema", 3);
        legacy.remove("gameplay_scale");

        TransformationSession loaded = SessionSerialization.fromTag(legacy);

        assertEquals(1D, loaded.gameplayScale());
    }

    @Test
    void migratesSchemaFourSessionsToUnlimitedDuration() {
        TransformationSession session = TransformationSession.begin(UUID.randomUUID(), UUID.randomUUID(), TransformationScope.GAMEPLAY, 1D, BaselineSnapshot.empty(), CompatibilityAssessment.full(), 1L)
                .withExpiresGameTime(OptionalLong.of(2_000L), 2L);
        CompoundTag legacy = SessionSerialization.toTag(session);
        legacy.putInt("schema", 4);
        legacy.remove("has_expiration");
        legacy.remove("expires_game_time");

        TransformationSession loaded = SessionSerialization.fromTag(legacy);

        assertEquals(OptionalLong.empty(), loaded.expiresGameTime());
    }
}
