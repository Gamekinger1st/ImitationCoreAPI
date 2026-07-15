package com.github.gamekinger1st.imitationcoreapi.api.chat;

import com.github.gamekinger1st.imitationcoreapi.api.network.PersonaChatPayload;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersonaChatPayloadTest {
    @Test
    void buildsAnUnsignedPersonaPresentationWithoutChangingTheRealSender() {
        UUID realSender = UUID.randomUUID();
        PersonaIdentity persona = new PersonaIdentity(UUID.randomUUID(), "Rimuru", Optional.of(UUID.randomUUID()));
        PersonaChatPayload payload = new PersonaChatPayload(UUID.randomUUID(), realSender, persona, "Hello");

        assertEquals(realSender, payload.realSenderId());
        assertEquals("<Rimuru> Hello", payload.displayComponent().getString());
    }

    @Test
    void enforcesBoundedPersonaChatContent() {
        PersonaIdentity persona = new PersonaIdentity(UUID.randomUUID(), "Rimuru", Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new PersonaChatPayload(UUID.randomUUID(), UUID.randomUUID(), persona, "   "));
        assertThrows(IllegalArgumentException.class, () -> new PersonaChatPayload(UUID.randomUUID(), UUID.randomUUID(), persona, "x".repeat(PersonaChatPayload.MAX_MESSAGE_LENGTH + 1)));
    }
}
