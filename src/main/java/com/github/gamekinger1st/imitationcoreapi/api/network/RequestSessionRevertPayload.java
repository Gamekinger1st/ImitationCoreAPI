package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

public record RequestSessionRevertPayload(UUID sessionId, long expectedRevision) implements CustomPacketPayload {
    public static final Type<RequestSessionRevertPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "request_session_revert"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSessionRevertPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.sessionId);
                buffer.writeVarLong(payload.expectedRevision);
            },
            buffer -> new RequestSessionRevertPayload(buffer.readUUID(), buffer.readVarLong())
    );

    public RequestSessionRevertPayload {
        Objects.requireNonNull(sessionId, "sessionId");
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("expectedRevision cannot be negative");
        }
    }

    @Override
    public Type<RequestSessionRevertPayload> type() {
        return TYPE;
    }
}
