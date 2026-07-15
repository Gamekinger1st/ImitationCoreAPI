package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatEnvelope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record SendCoreChatPayload(ResourceLocation channelId, String message, Optional<UUID> targetPlayerId) implements CustomPacketPayload {
    public static final Type<SendCoreChatPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "send_core_chat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SendCoreChatPayload> STREAM_CODEC = StreamCodec.of(SendCoreChatPayload::encode, SendCoreChatPayload::decode);

    public SendCoreChatPayload {
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(targetPlayerId, "targetPlayerId");
        message = message.strip();
        if (message.isEmpty() || message.length() > ChatEnvelope.MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Chat message must contain between 1 and " + ChatEnvelope.MAX_MESSAGE_LENGTH + " characters");
        }
    }

    private static void encode(RegistryFriendlyByteBuf buffer, SendCoreChatPayload payload) {
        buffer.writeResourceLocation(payload.channelId);
        buffer.writeUtf(payload.message, ChatEnvelope.MAX_MESSAGE_LENGTH);
        buffer.writeBoolean(payload.targetPlayerId.isPresent());
        payload.targetPlayerId.ifPresent(buffer::writeUUID);
    }

    private static SendCoreChatPayload decode(RegistryFriendlyByteBuf buffer) {
        ResourceLocation channelId = buffer.readResourceLocation();
        String message = buffer.readUtf(ChatEnvelope.MAX_MESSAGE_LENGTH);
        Optional<UUID> targetPlayerId = buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
        return new SendCoreChatPayload(channelId, message, targetPlayerId);
    }

    @Override
    public Type<SendCoreChatPayload> type() {
        return TYPE;
    }
}
