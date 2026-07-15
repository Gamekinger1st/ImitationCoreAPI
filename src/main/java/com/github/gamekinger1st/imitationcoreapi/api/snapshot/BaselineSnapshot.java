package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record BaselineSnapshot(int schemaVersion, CompoundTag playerData, List<SnapshotExtension> extensions) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public BaselineSnapshot {
        Objects.requireNonNull(playerData, "playerData");
        Objects.requireNonNull(extensions, "extensions");
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported baseline snapshot schema version: " + schemaVersion);
        }
        playerData = playerData.copy();
        extensions = List.copyOf(new ArrayList<>(extensions));
        SnapshotLimits.DEFAULT.validateBaseline(playerData, extensions);
    }

    @Override
    public CompoundTag playerData() {
        return playerData.copy();
    }

    public static BaselineSnapshot empty() {
        return new BaselineSnapshot(CURRENT_SCHEMA_VERSION, new CompoundTag(), List.of());
    }
}
