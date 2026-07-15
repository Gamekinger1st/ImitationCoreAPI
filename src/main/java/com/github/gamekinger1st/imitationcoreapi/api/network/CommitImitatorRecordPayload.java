package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CommitImitatorRecordPayload(int slot) implements CustomPacketPayload {
    public static final Type<CommitImitatorRecordPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "commit_imitator_record"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CommitImitatorRecordPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.slot),
            buffer -> new CommitImitatorRecordPayload(buffer.readVarInt())
    );

    public CommitImitatorRecordPayload {
        if (slot < 0 || slot > 255) {
            throw new IllegalArgumentException("slot must be between 0 and 255");
        }
    }

    @Override
    public Type<CommitImitatorRecordPayload> type() {
        return TYPE;
    }
}
