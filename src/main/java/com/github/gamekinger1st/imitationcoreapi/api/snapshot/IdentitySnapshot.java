package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record IdentitySnapshot(
        UUID snapshotId,
        int schemaVersion,
        ResourceLocation entityType,
        String displayName,
        CompoundTag entityData,
        CompoundTag visualData,
        List<SnapshotExtension> extensions,
        long capturedGameTime
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public IdentitySnapshot {
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(entityData, "entityData");
        Objects.requireNonNull(visualData, "visualData");
        Objects.requireNonNull(extensions, "extensions");
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported identity snapshot schema version: " + schemaVersion);
        }
        if (capturedGameTime < 0) {
            throw new IllegalArgumentException("capturedGameTime cannot be negative");
        }
        displayName = displayName.strip();
        entityData = entityData.copy();
        visualData = visualData.copy();
        extensions = List.copyOf(new ArrayList<>(extensions));
        SnapshotLimits.DEFAULT.validateIdentity(displayName, entityData, visualData, extensions);
    }

    @Override
    public CompoundTag entityData() {
        return entityData.copy();
    }

    @Override
    public CompoundTag visualData() {
        return visualData.copy();
    }

    public static Builder builder(ResourceLocation entityType, long capturedGameTime) {
        return new Builder(entityType, capturedGameTime);
    }

    public static final class Builder {
        private UUID snapshotId = UUID.randomUUID();
        private int schemaVersion = CURRENT_SCHEMA_VERSION;
        private final ResourceLocation entityType;
        private final long capturedGameTime;
        private String displayName = "";
        private CompoundTag entityData = new CompoundTag();
        private CompoundTag visualData = new CompoundTag();
        private final List<SnapshotExtension> extensions = new ArrayList<>();

        private Builder(ResourceLocation entityType, long capturedGameTime) {
            this.entityType = Objects.requireNonNull(entityType, "entityType");
            this.capturedGameTime = capturedGameTime;
        }

        public Builder snapshotId(UUID snapshotId) {
            this.snapshotId = Objects.requireNonNull(snapshotId, "snapshotId");
            return this;
        }

        public Builder schemaVersion(int schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        public Builder entityData(CompoundTag entityData) {
            this.entityData = Objects.requireNonNull(entityData, "entityData").copy();
            return this;
        }

        public Builder visualData(CompoundTag visualData) {
            this.visualData = Objects.requireNonNull(visualData, "visualData").copy();
            return this;
        }

        public Builder extension(SnapshotExtension extension) {
            extensions.add(Objects.requireNonNull(extension, "extension"));
            return this;
        }

        public IdentitySnapshot build() {
            return build(SnapshotLimits.DEFAULT);
        }

        public IdentitySnapshot build(SnapshotLimits limits) {
            IdentitySnapshot snapshot = new IdentitySnapshot(snapshotId, schemaVersion, entityType, displayName, entityData, visualData, extensions, capturedGameTime);
            Objects.requireNonNull(limits, "limits").validate(snapshot);
            return snapshot;
        }
    }
}
