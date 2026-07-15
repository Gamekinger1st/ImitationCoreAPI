package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentImitatorFormLibraryTest {
    @Test
    void persistsSlotsAndClearsSelectionWhenTheSelectedFormIsRemoved() {
        InMemoryRepository repository = new InMemoryRepository();
        ImitatorFormLibrary library = new PersistentImitatorFormLibrary(UUID.randomUUID(), repository, new ImitatorFormLibraryLimits(2, 2, 20));
        ImitatorForm first = new ImitatorForm(UUID.randomUUID(), 0.25D, false, false);
        ImitatorForm second = new ImitatorForm(UUID.randomUUID(), 1D, true, true);

        assertTrue(library.setForm(0, first).isEmpty());
        assertTrue(library.setForm(1, second).isEmpty());
        assertTrue(library.selectForm(1));
        assertEquals(second, library.selectedForm().orElseThrow());
        assertEquals(2, library.occupiedSlots().size());
        assertThrows(IllegalArgumentException.class, () -> library.setForm(0, second));

        assertEquals(second, library.clearForm(1).orElseThrow());
        assertTrue(library.selectedSlot().isEmpty());
        assertTrue(library.selectedForm().isEmpty());
    }

    @Test
    void boundsPendingRecordsAndSeenFormHistory() {
        InMemoryRepository repository = new InMemoryRepository();
        ImitatorFormLibrary library = new PersistentImitatorFormLibrary(UUID.randomUUID(), repository, new ImitatorFormLibraryLimits(2, 2, 20));
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();

        library.setPendingRecord(new ImitatorPendingRecord(first, 10L, 30L));
        assertFalse(library.expirePendingRecord(29L));
        assertTrue(library.expirePendingRecord(30L));
        assertTrue(library.pendingRecord().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> library.setPendingRecord(new ImitatorPendingRecord(first, 10L, 31L)));

        assertTrue(library.rememberSeenSnapshot(first));
        assertTrue(library.rememberSeenSnapshot(second));
        assertTrue(library.rememberSeenSnapshot(third));
        assertEquals(java.util.List.of(second, third), library.seenSnapshotIds());
        assertTrue(library.rememberSeenSnapshot(second));
        assertEquals(java.util.List.of(third, second), library.seenSnapshotIds());
    }

    private static final class InMemoryRepository implements ImitatorFormRepository {
        private final Map<UUID, ImitatorFormLibraryState> libraries = new LinkedHashMap<>();

        @Override
        public ImitatorFormLibraryState formLibrary(UUID ownerId) {
            return libraries.getOrDefault(ownerId, ImitatorFormLibraryState.empty());
        }

        @Override
        public void saveFormLibrary(UUID ownerId, ImitatorFormLibraryState library) {
            libraries.put(ownerId, library);
        }
    }
}
