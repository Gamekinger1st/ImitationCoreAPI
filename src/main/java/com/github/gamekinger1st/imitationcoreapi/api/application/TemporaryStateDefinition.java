package com.github.gamekinger1st.imitationcoreapi.api.application;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record TemporaryStateDefinition(ResourceLocation kind, CompoundTag payload) {
    public TemporaryStateDefinition {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(payload, "payload");
        payload = payload.copy();
    }

    @Override
    public CompoundTag payload() {
        return payload.copy();
    }
}
