package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatEnvelopeTest {
    @Test
    void preservesTheRealSenderWhenRenderingAPersona() {
        UUID sender = UUID.randomUUID();
        ChatEnvelope envelope = new ChatEnvelope(
                UUID.randomUUID(),
                Instant.EPOCH,
                ChatChannels.GLOBAL,
                ChatChannelKind.GLOBAL,
                ChatMessageSource.VANILLA_SIGNED_PLAYER,
                Optional.of(sender),
                Optional.of("RealPlayer"),
                Optional.of(new PersonaIdentity(UUID.randomUUID(), "ImitatedPlayer", Optional.empty())),
                "Hello"
        );

        assertEquals(sender, envelope.realSenderId().orElseThrow());
        assertEquals("ImitatedPlayer", envelope.displayComponent().getString().replace("[global] <", "").replace("> Hello", ""));
        assertEquals("[global] <ImitatedPlayer via RealPlayer> Hello", envelope.vanillaFallbackComponent().getString());
    }

    @Test
    void rejectsPlayerMessagesThatLoseAuthenticatedSenderIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new ChatEnvelope(
                UUID.randomUUID(),
                Instant.EPOCH,
                ResourceLocation.withDefaultNamespace("global"),
                ChatChannelKind.GLOBAL,
                ChatMessageSource.CORE_UNSIGNED_PLAYER,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "Hello"
        ));
    }

    @Test
    void rejectsSystemMessagesThatClaimAPlayerSender() {
        assertThrows(IllegalArgumentException.class, () -> new ChatEnvelope(
                UUID.randomUUID(),
                Instant.EPOCH,
                ChatChannels.SYSTEM,
                ChatChannelKind.SYSTEM,
                ChatMessageSource.SERVER_SYSTEM,
                Optional.of(UUID.randomUUID()),
                Optional.of("Player"),
                Optional.empty(),
                "Hello"
        ));
    }

    @Test
    void supportsExplicitExternalChatSources() {
        UUID discordIdentity = UUID.randomUUID();
        ChatEnvelope envelope = new ChatEnvelope(
                UUID.randomUUID(),
                Instant.EPOCH,
                ChatChannels.GLOBAL,
                ChatChannelKind.GLOBAL,
                ChatMessageSource.DISCORD_BRIDGE,
                Optional.of(discordIdentity),
                Optional.of("Discord: Player"),
                Optional.empty(),
                "Hello"
        );

        assertEquals(discordIdentity, envelope.realSenderId().orElseThrow());
        assertEquals("[global] <Discord: Player> Hello", envelope.displayComponent().getString());
    }
}
