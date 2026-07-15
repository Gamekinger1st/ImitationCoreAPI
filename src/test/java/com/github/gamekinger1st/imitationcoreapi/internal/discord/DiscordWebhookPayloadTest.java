package com.github.gamekinger1st.imitationcoreapi.internal.discord;

import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelKind;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannels;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatEnvelope;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatMessageSource;
import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaIdentity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscordWebhookPayloadTest {
    @Test
    void formatsPersonaChatWithTheRealSender() {
        ChatEnvelope envelope = new ChatEnvelope(
                UUID.randomUUID(),
                Instant.EPOCH,
                ChatChannels.GLOBAL,
                ChatChannelKind.GLOBAL,
                ChatMessageSource.VANILLA_SIGNED_PLAYER,
                Optional.of(UUID.randomUUID()),
                Optional.of("RealPlayer"),
                Optional.of(new PersonaIdentity(UUID.randomUUID(), "ImitatedPlayer", Optional.empty())),
                "Hello"
        );

        assertEquals("[global] <ImitatedPlayer via RealPlayer> Hello", DiscordWebhookPayload.format(envelope));
    }

    @Test
    void escapesWebhookContentAndDisablesMentions() {
        assertEquals("{\"username\":\"Minecraft\",\"allowed_mentions\":{\"parse\":[]},\"content\":\"\\\"quoted\\\\path\\n\"}", DiscordWebhookPayload.json("\"quoted\\path\n"));
    }
}
