package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record SelectChatChannelPayload(ResourceLocation channelId) implements CustomPacketPayload {
    public static final Type<SelectChatChannelPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "select_chat_channel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectChatChannelPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeResourceLocation(payload.channelId),
            buffer -> new SelectChatChannelPayload(buffer.readResourceLocation())
    );

    public SelectChatChannelPayload {
        Objects.requireNonNull(channelId, "channelId");
    }

    @Override
    public Type<SelectChatChannelPayload> type() {
        return TYPE;
    }
}
