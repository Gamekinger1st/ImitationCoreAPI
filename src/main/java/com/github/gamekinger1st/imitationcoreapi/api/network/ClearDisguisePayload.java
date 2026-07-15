package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

public record ClearDisguisePayload(int entityId, UUID ownerId) implements CustomPacketPayload {
    public static final Type<ClearDisguisePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "clear_disguise"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearDisguisePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entityId);
                buffer.writeUUID(payload.ownerId);
            },
            buffer -> new ClearDisguisePayload(buffer.readVarInt(), buffer.readUUID())
    );

    public ClearDisguisePayload {
        if (entityId < 0) {
            throw new IllegalArgumentException("entityId cannot be negative");
        }
        Objects.requireNonNull(ownerId, "ownerId");
    }

    @Override
    public Type<ClearDisguisePayload> type() {
        return TYPE;
    }
}
