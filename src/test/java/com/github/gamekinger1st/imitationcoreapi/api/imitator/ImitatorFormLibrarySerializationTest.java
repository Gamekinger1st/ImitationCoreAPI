package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.nbt.CompoundTag;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitatorFormLibrarySerializationTest {
    @Test
    void roundTripsAllPersistentLibraryState() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ImitatorFormLibraryState state = new ImitatorFormLibraryState(
                ImitatorFormLibraryState.CURRENT_SCHEMA_VERSION,
                Map.of(2, new ImitatorForm(first, 0.75D, false, true, false, ImitatorFormStats.fromAppraisal(new DisguiseAppraisalSnapshot(10F, 20F, 2, Optional.of(new TensuraVitals(100D, 40D, 60D, 12D)))))),
                OptionalInt.of(2),
                Optional.of(new ImitatorPendingRecord(second, 10L, 20L)),
                List.of(first, second),
                ImitatorSkillMode.TRANSFORM,
                true
        );

        CompoundTag serialized = ImitatorFormLibrarySerialization.toTag(state);

        assertEquals(state, ImitatorFormLibrarySerialization.fromTag(serialized));
    }

    @Test
    void rejectsDuplicateStoredSlots() {
        CompoundTag serialized = ImitatorFormLibrarySerialization.toTag(ImitatorFormLibraryState.empty());
        CompoundTag form = new CompoundTag();
        form.putInt("slot", 0);
        form.putUUID("snapshot_id", UUID.randomUUID());
        form.putDouble("precision", 0.5D);
        serialized.getList("forms", net.minecraft.nbt.Tag.TAG_COMPOUND).add(form);
        serialized.getList("forms", net.minecraft.nbt.Tag.TAG_COMPOUND).add(form.copy());

        assertThrows(IllegalArgumentException.class, () -> ImitatorFormLibrarySerialization.fromTag(serialized));
    }

    @Test
    void migratesModeAndMirrorSyncPreferencesFromTheOriginalLibrarySchema() {
        CompoundTag serialized = ImitatorFormLibrarySerialization.toTag(ImitatorFormLibraryState.empty());
        serialized.putInt("schema", 1);
        serialized.remove("skill_mode");
        serialized.remove("mirror_sync_enabled");

        ImitatorFormLibraryState migrated = ImitatorFormLibrarySerialization.fromTag(serialized);

        assertEquals(ImitatorSkillMode.RECORD, migrated.skillMode());
        org.junit.jupiter.api.Assertions.assertFalse(migrated.mirrorSyncEnabled());
    }

    @Test
    void migratesPendingRecordPrecisionFromSchemaTwo() {
        UUID snapshotId = UUID.randomUUID();
        CompoundTag serialized = ImitatorFormLibrarySerialization.toTag(ImitatorFormLibraryState.empty());
        serialized.putInt("schema", 2);
        CompoundTag pending = new CompoundTag();
        pending.putUUID("snapshot_id", snapshotId);
        pending.putLong("created_game_time", 10L);
        pending.putLong("expires_game_time", 20L);
        serialized.put("pending_record", pending);

        ImitatorPendingRecord migrated = ImitatorFormLibrarySerialization.fromTag(serialized).pendingRecord().orElseThrow();

        assertEquals(snapshotId, migrated.snapshotId());
        assertEquals(ImitatorProgressionPolicy.DEFAULT.minimumPrecision(), migrated.precision());
        org.junit.jupiter.api.Assertions.assertFalse(migrated.mirrorSyncAllowed());
    }

    @Test
    void migratesSkillCopyEligibilityFromSchemaThree() {
        UUID formId = UUID.randomUUID();
        UUID pendingId = UUID.randomUUID();
        ImitatorFormLibraryState state = new ImitatorFormLibraryState(
                ImitatorFormLibraryState.CURRENT_SCHEMA_VERSION,
                Map.of(0, new ImitatorForm(formId, 0.9D, false, true, true)),
                OptionalInt.of(0),
                Optional.of(new ImitatorPendingRecord(pendingId, 10L, 20L, 0.9D, true, true)),
                List.of(),
                ImitatorSkillMode.RECORD,
                false
        );
        CompoundTag serialized = ImitatorFormLibrarySerialization.toTag(state);
        serialized.putInt("schema", 3);
        serialized.getList("forms", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).remove("skill_copy_allowed");
        serialized.getCompound("pending_record").remove("skill_copy_allowed");

        ImitatorFormLibraryState migrated = ImitatorFormLibrarySerialization.fromTag(serialized);

        org.junit.jupiter.api.Assertions.assertTrue(migrated.forms().get(0).skillCopyAllowed());
        org.junit.jupiter.api.Assertions.assertTrue(migrated.pendingRecord().orElseThrow().skillCopyAllowed());
    }

    @Test
    void migratesFormStatsFromSchemaFourAsEmpty() {
        UUID formId = UUID.randomUUID();
        ImitatorFormLibraryState state = new ImitatorFormLibraryState(
                ImitatorFormLibraryState.CURRENT_SCHEMA_VERSION,
                Map.of(0, new ImitatorForm(formId, 0.9D, false, true, true)),
                OptionalInt.of(0),
                Optional.empty(),
                List.of(),
                ImitatorSkillMode.RECORD,
                false
        );
        CompoundTag serialized = ImitatorFormLibrarySerialization.toTag(state);
        serialized.putInt("schema", 4);

        ImitatorFormLibraryState migrated = ImitatorFormLibrarySerialization.fromTag(serialized);

        assertTrue(migrated.forms().get(0).stats().isEmpty());
    }
}
