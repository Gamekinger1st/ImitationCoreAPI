package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestImitatorFormLibraryPayload() implements CustomPacketPayload {
    public static final Type<RequestImitatorFormLibraryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "request_imitator_form_library"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestImitatorFormLibraryPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> {
    }, buffer -> new RequestImitatorFormLibraryPayload());

    @Override
    public Type<RequestImitatorFormLibraryPayload> type() {
        return TYPE;
    }
}
