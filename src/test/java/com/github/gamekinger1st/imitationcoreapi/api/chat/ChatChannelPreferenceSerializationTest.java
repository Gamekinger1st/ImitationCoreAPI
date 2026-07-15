package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatChannelPreferenceSerializationTest {
    @Test
    void roundTripsAnActivePlayerChannel() {
        UUID playerId = UUID.randomUUID();

        ChatChannelPreferenceSerialization.Entry entry = ChatChannelPreferenceSerialization.fromTag(ChatChannelPreferenceSerialization.toTag(playerId, ChatChannels.LOCAL));

        assertEquals(playerId, entry.playerId());
        assertEquals(ChatChannels.LOCAL, entry.channelId());
    }

    @Test
    void rejectsMalformedChannelPreferences() {
        CompoundTag invalid = new CompoundTag();
        invalid.putUUID("player", UUID.randomUUID());
        invalid.putString("channel", "not a resource location");

        assertThrows(IllegalArgumentException.class, () -> ChatChannelPreferenceSerialization.fromTag(invalid));
    }
}
