package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorMenuRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record OpenImitatorMenuPayload(ImitatorMenuRequest request) implements CustomPacketPayload {
    public static final Type<OpenImitatorMenuPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "open_imitator_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenImitatorMenuPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.request.ordinal()),
            buffer -> new OpenImitatorMenuPayload(requestAt(buffer.readVarInt()))
    );

    public OpenImitatorMenuPayload {
        Objects.requireNonNull(request, "request");
        if (request == ImitatorMenuRequest.NONE) {
            throw new IllegalArgumentException("An Imitator menu payload requires a menu request");
        }
    }

    @Override
    public Type<OpenImitatorMenuPayload> type() {
        return TYPE;
    }

    private static ImitatorMenuRequest requestAt(int index) {
        ImitatorMenuRequest[] requests = ImitatorMenuRequest.values();
        if (index < 0 || index >= requests.length || requests[index] == ImitatorMenuRequest.NONE) {
            throw new IllegalArgumentException("Invalid Imitator menu request");
        }
        return requests[index];
    }
}
