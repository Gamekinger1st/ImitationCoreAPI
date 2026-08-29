package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChatProtocolPayload(int protocolVersion, int maximumHistory, ResourceLocation activeChannel) implements CustomPacketPayload {
    public static final int CURRENT_PROTOCOL_VERSION = 2;
    public static final Type<ChatProtocolPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "chat_protocol"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChatProtocolPayload> STREAM_CODEC = StreamCodec.of(ChatProtocolPayload::encode, ChatProtocolPayload::decode);

    public ChatProtocolPayload {
        java.util.Objects.requireNonNull(activeChannel, "activeChannel");
        if (protocolVersion <= 0 || maximumHistory <= 0 || maximumHistory > 10_000) {
            throw new IllegalArgumentException("Invalid chat protocol limits");
        }
    }

    public ChatProtocolPayload(int protocolVersion, int maximumHistory) {
        this(protocolVersion, maximumHistory, com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannels.GLOBAL);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ChatProtocolPayload payload) {
        buffer.writeVarInt(payload.protocolVersion);
        buffer.writeVarInt(payload.maximumHistory);
        buffer.writeResourceLocation(payload.activeChannel);
    }

    private static ChatProtocolPayload decode(RegistryFriendlyByteBuf buffer) {
        return new ChatProtocolPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readResourceLocation());
    }

    @Override
    public Type<ChatProtocolPayload> type() {
        return TYPE;
    }
}
