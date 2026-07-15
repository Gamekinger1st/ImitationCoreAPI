package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChatProtocolHelloPayload(int protocolVersion) implements CustomPacketPayload {
    public static final Type<ChatProtocolHelloPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "chat_protocol_hello"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChatProtocolHelloPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> buffer.writeVarInt(payload.protocolVersion), buffer -> new ChatProtocolHelloPayload(buffer.readVarInt()));

    public ChatProtocolHelloPayload {
        if (protocolVersion <= 0 || protocolVersion > ChatProtocolPayload.CURRENT_PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Unsupported chat protocol version");
        }
    }

    @Override
    public Type<ChatProtocolHelloPayload> type() {
        return TYPE;
    }
}
