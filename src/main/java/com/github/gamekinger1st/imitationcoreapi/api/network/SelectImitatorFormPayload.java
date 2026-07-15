package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectImitatorFormPayload(int slot) implements CustomPacketPayload {
    public static final Type<SelectImitatorFormPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "select_imitator_form"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectImitatorFormPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.slot),
            buffer -> new SelectImitatorFormPayload(buffer.readVarInt())
    );

    public SelectImitatorFormPayload {
        if (slot < 0 || slot > 255) {
            throw new IllegalArgumentException("slot must be between 0 and 255");
        }
    }

    @Override
    public Type<SelectImitatorFormPayload> type() {
        return TYPE;
    }
}
