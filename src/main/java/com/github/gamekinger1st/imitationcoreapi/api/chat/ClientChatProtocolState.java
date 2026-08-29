package com.github.gamekinger1st.imitationcoreapi.api.chat;

import com.github.gamekinger1st.imitationcoreapi.api.network.ChatProtocolPayload;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import java.util.concurrent.atomic.AtomicReference;

public final class ClientChatProtocolState {
    private final AtomicReference<ChatProtocolPayload> protocol = new AtomicReference<>();

    public Optional<ChatProtocolPayload> protocol() {
        return Optional.ofNullable(protocol.get());
    }

    public boolean replacementEnabled() {
        return protocol().map(value -> value.protocolVersion() == ChatProtocolPayload.CURRENT_PROTOCOL_VERSION).orElse(false);
    }

    public ResourceLocation activeChannel() {
        return protocol().map(ChatProtocolPayload::activeChannel).orElse(ChatChannels.GLOBAL);
    }

    public void accept(ChatProtocolPayload payload) {
        if (payload.protocolVersion() == ChatProtocolPayload.CURRENT_PROTOCOL_VERSION) {
            protocol.set(payload);
        } else {
            clear();
        }
    }

    public void clear() {
        protocol.set(null);
    }
}
