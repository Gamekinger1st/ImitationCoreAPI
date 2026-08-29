package com.github.gamekinger1st.imitationcoreapi.api.chat;

import com.github.gamekinger1st.imitationcoreapi.api.network.ChatProtocolPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientChatProtocolStateTest {
    @Test
    void acceptsTheCurrentProtocolAndTracksItsActiveChannel() {
        ClientChatProtocolState state = new ClientChatProtocolState();

        state.accept(new ChatProtocolPayload(ChatProtocolPayload.CURRENT_PROTOCOL_VERSION, 200, ChatChannels.LOCAL));

        assertTrue(state.replacementEnabled());
        assertEquals(ChatChannels.LOCAL, state.activeChannel());
    }

    @Test
    void rejectsAnIncompatibleProtocolAndReturnsToVanillaDefaults() {
        ClientChatProtocolState state = new ClientChatProtocolState();
        state.accept(new ChatProtocolPayload(ChatProtocolPayload.CURRENT_PROTOCOL_VERSION, 200, ChatChannels.LOCAL));

        state.accept(new ChatProtocolPayload(ChatProtocolPayload.CURRENT_PROTOCOL_VERSION + 1, 200, ChatChannels.LOCAL));

        assertFalse(state.replacementEnabled());
        assertEquals(ChatChannels.GLOBAL, state.activeChannel());
    }
}
