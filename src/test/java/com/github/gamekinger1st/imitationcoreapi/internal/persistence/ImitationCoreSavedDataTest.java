package com.github.gamekinger1st.imitationcoreapi.internal.persistence;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorForm;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormLibraryState;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorSkillMode;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationState;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.BaselineSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotSerialization;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitationCoreSavedDataTest {
    @Test
    void futureDataIsQuarantinedWithoutPreventingWorldLoad() throws Exception {
        CompoundTag future = new CompoundTag();
        future.putInt("data_version", 99);
        future.putString("future_field", "preserve me");
        java.lang.reflect.Method migrate = ImitationCoreSavedData.class.getDeclaredMethod("migrate", CompoundTag.class);
        migrate.setAccessible(true);

        CompoundTag result = (CompoundTag)migrate.invoke(null, future);

        assertEquals(4, result.getInt("data_version"));
        assertEquals("preserve me", result.getCompound("preserved_future_data").getString("future_field"));
        assertEquals(0, result.getList("sessions", Tag.TAG_COMPOUND).size());
    }

    @Test
    void pruningPreservesRecoveryAndFormDataWhileBoundingHistory() {
        ImitationCoreSavedData data = new ImitationCoreSavedData();
        UUID ownerId = UUID.randomUUID();
        IdentitySnapshot activeSnapshot = snapshot(0L);
        IdentitySnapshot formSnapshot = snapshot(1L);
        data.saveSnapshot(activeSnapshot);
        data.saveSnapshot(formSnapshot);
        data.saveFormLibrary(ownerId, new ImitatorFormLibraryState(
                ImitatorFormLibraryState.CURRENT_SCHEMA_VERSION,
                Map.of(0, new ImitatorForm(formSnapshot.snapshotId(), 1D, true, true)),
                OptionalInt.of(0),
                Optional.empty(),
                List.of(),
                ImitatorSkillMode.TRANSFORM,
                true
        ));
        for (int index = 0; index < 300; index++) {
            data.saveSnapshot(snapshot(index + 2L));
        }
        for (int index = 0; index < 520; index++) {
            data.saveSession(session(ownerId, activeSnapshot.snapshotId(), TransformationState.REVERTED, index));
        }
        TransformationSession recovery = session(ownerId, activeSnapshot.snapshotId(), TransformationState.ACTIVE, 1_000L);
        data.saveSession(recovery);

        CompoundTag saved = data.save(new CompoundTag(), null);

        assertEquals(513, saved.getList("sessions", Tag.TAG_COMPOUND).size());
        assertEquals(258, saved.getList("snapshots", Tag.TAG_COMPOUND).size());
        Set<UUID> snapshotIds = saved.getList("snapshots", Tag.TAG_COMPOUND).stream()
                .map(CompoundTag.class::cast)
                .map(SnapshotSerialization::identityFromTag)
                .map(IdentitySnapshot::snapshotId)
                .collect(Collectors.toSet());
        assertTrue(snapshotIds.contains(activeSnapshot.snapshotId()));
        assertTrue(snapshotIds.contains(formSnapshot.snapshotId()));
        assertTrue(data.session(recovery.sessionId()).isPresent());
    }

    private static IdentitySnapshot snapshot(long gameTime) {
        return IdentitySnapshot.builder(ResourceLocation.withDefaultNamespace("chicken"), gameTime)
                .displayName("Chicken")
                .build();
    }

    private static TransformationSession session(UUID ownerId, UUID snapshotId, TransformationState state, long gameTime) {
        return new TransformationSession(
                UUID.randomUUID(),
                ownerId,
                snapshotId,
                TransformationScope.SURFACE,
                1D,
                BaselineSnapshot.empty(),
                CompatibilityAssessment.full(),
                state,
                0L,
                gameTime,
                0L,
                OptionalLong.empty(),
                Optional.empty(),
                List.of()
        );
    }
}
