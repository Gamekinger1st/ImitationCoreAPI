package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record SnapshotExtension(ResourceLocation adapterId, int schemaVersion, CompoundTag payload) {
    public SnapshotExtension {
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(payload, "payload");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        payload = payload.copy();
    }

    @Override
    public CompoundTag payload() {
        return payload.copy();
    }
}
