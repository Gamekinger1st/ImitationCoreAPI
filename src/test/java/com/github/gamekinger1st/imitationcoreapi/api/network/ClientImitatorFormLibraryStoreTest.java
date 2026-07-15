package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorSkillMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientImitatorFormLibraryStoreTest {
    @Test
    void tracksAndClearsTheLatestServerLibrary() {
        ClientImitatorFormLibraryStore store = new ClientImitatorFormLibraryStore();
        ImitatorFormLibraryPayload payload = new ImitatorFormLibraryPayload(1, List.of(), OptionalInt.empty(), Optional.empty(), ImitatorSkillMode.RECORD, false);

        store.onFormLibrary(payload);

        assertEquals(payload, store.current().orElseThrow());
        store.clear();
        assertTrue(store.current().isEmpty());
    }
}
