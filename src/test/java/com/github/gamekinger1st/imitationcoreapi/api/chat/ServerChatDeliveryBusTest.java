package com.github.gamekinger1st.imitationcoreapi.api.chat;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerChatDeliveryBusTest {
    @Test
    void unregistersClosedListeners() {
        ServerChatDeliveryBus bus = new ServerChatDeliveryBus();
        AtomicInteger deliveries = new AtomicInteger();
        ServerChatDeliveryRegistration registration = bus.register(envelope -> deliveries.incrementAndGet());

        bus.post(systemMessage());
        registration.close();
        bus.post(systemMessage());

        assertEquals(1, deliveries.get());
    }

    private static ChatEnvelope systemMessage() {
        return new ChatEnvelope(UUID.randomUUID(), Instant.EPOCH, ChatChannels.SYSTEM, ChatChannelKind.SYSTEM, ChatMessageSource.SERVER_SYSTEM, Optional.empty(), Optional.empty(), Optional.empty(), "Hello");
    }
}
