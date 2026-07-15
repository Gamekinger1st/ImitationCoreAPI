package com.github.gamekinger1st.imitationcoreapi.api.chat;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientChatStoreTest {
    @Test
    void deduplicatesAndBoundsClientHistory() {
        ClientChatStore store = new ClientChatStore();
        UUID firstId = UUID.randomUUID();
        store.onChatMessage(message(firstId, "first"));
        store.onChatMessage(message(firstId, "first"));
        for (int index = 0; index <= ClientChatStore.MAX_HISTORY_SIZE; index++) {
            store.onChatMessage(message(UUID.randomUUID(), "message-" + index));
        }

        assertEquals(ClientChatStore.MAX_HISTORY_SIZE, store.history().size());
        assertEquals(ClientChatStore.MAX_HISTORY_SIZE, store.unread(ChatChannels.GLOBAL));
        store.markRead(ChatChannels.GLOBAL);
        assertEquals(0, store.unread(ChatChannels.GLOBAL));
    }

    private static ChatEnvelope message(UUID messageId, String message) {
        return new ChatEnvelope(messageId, Instant.EPOCH, ChatChannels.GLOBAL, ChatChannelKind.GLOBAL, ChatMessageSource.SERVER_SYSTEM, Optional.empty(), Optional.empty(), Optional.empty(), message);
    }
}
