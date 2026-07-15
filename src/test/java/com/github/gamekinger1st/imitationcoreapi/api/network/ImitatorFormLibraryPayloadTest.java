package com.github.gamekinger1st.imitationcoreapi.api.network;

import org.junit.jupiter.api.Test;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorSkillMode;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImitatorFormLibraryPayloadTest {
    @Test
    void normalizesFormSlotsAndPreservesPendingRecord() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ImitatorFormLibraryPayload payload = new ImitatorFormLibraryPayload(
                4,
                List.of(
                        new ImitatorFormLibraryPayload.FormSlot(3, second, 1D, true, true),
                        new ImitatorFormLibraryPayload.FormSlot(1, first, 0.5D, false, false)
                ),
                OptionalInt.of(3),
                Optional.of(new ImitatorFormLibraryPayload.PendingRecord(first, 10L, 20L)),
                ImitatorSkillMode.TRANSFORM,
                true
        );

        assertEquals(1, payload.forms().getFirst().slot());
        assertEquals(3, payload.selectedSlot().getAsInt());
        assertEquals(first, payload.pendingRecord().orElseThrow().snapshotId());
        assertEquals(0.35D, payload.pendingRecord().orElseThrow().precision());
        assertEquals(ImitatorSkillMode.TRANSFORM, payload.skillMode());
        org.junit.jupiter.api.Assertions.assertTrue(payload.mirrorSyncEnabled());
    }

    @Test
    void rejectsInvalidMenuState() {
        UUID snapshot = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new ImitatorFormLibraryPayload(
                1,
                List.of(new ImitatorFormLibraryPayload.FormSlot(0, snapshot, 0D, false, false)),
                OptionalInt.of(1),
                Optional.empty(),
                ImitatorSkillMode.RECORD,
                false
        ));
    }
}
