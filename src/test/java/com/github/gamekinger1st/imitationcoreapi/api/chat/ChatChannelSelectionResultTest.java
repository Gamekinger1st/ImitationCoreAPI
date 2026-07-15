package com.github.gamekinger1st.imitationcoreapi.api.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatChannelSelectionResultTest {
    @Test
    void distinguishesAcceptedAndRejectedSelections() {
        ChatChannelSelectionResult selected = ChatChannelSelectionResult.selected(ChatChannels.LOCAL);
        ChatChannelSelectionResult rejected = ChatChannelSelectionResult.rejected("Unavailable");

        assertTrue(selected.accepted());
        assertEquals(ChatChannels.LOCAL, selected.channelId().orElseThrow());
        assertFalse(rejected.accepted());
    }
}
