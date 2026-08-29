package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActivateFormAbilityPayload() implements CustomPacketPayload {
    public static final ActivateFormAbilityPayload INSTANCE = new ActivateFormAbilityPayload();
    public static final Type<ActivateFormAbilityPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "activate_form_ability"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateFormAbilityPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<ActivateFormAbilityPayload> type() {
        return TYPE;
    }
}
