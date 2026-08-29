package com.github.gamekinger1st.imitationcoreapi.internal.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatDraftStoreTest {
    @AfterEach
    void clearDraft() {
        ChatDraftStore.clear();
    }

    @Test
    void restoresSavedDraftForOrdinaryChatOpen() {
        ChatDraftStore.save("unfinished message");

        assertEquals("unfinished message", ChatDraftStore.restore(""));
    }

    @Test
    void explicitInitialTextTakesPriorityOverSavedDraft() {
        ChatDraftStore.save("unfinished message");

        assertEquals("/", ChatDraftStore.restore("/"));
    }

    @Test
    void clearingRemovesSubmittedDraft() {
        ChatDraftStore.save("/unfinished command");

        ChatDraftStore.clear();

        assertEquals("", ChatDraftStore.restore(""));
    }

    @Test
    void boundsStoredDraftToVanillaChatLength() {
        ChatDraftStore.save("x".repeat(300));

        assertEquals(256, ChatDraftStore.restore("").length());
    }

    @Test
    void disconnectDiscardCannotBeOverwrittenByAClosingOldScreen() {
        ChatDraftStore.save("private draft");

        ChatDraftStore.discard();
        ChatDraftStore.save("private draft");

        assertEquals("", ChatDraftStore.restore(""));
        ChatDraftStore.save("new session draft");
        assertEquals("new session draft", ChatDraftStore.restore(""));
    }
}
