package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

public record SnapshotLimits(
        int maxEntityDataBytes,
        int maxVisualDataBytes,
        int maxExtensions,
        int maxExtensionDataBytes,
        int maxDisplayNameCharacters
) {
    public static final SnapshotLimits DEFAULT = new SnapshotLimits(262_144, 131_072, 32, 131_072, 256);

    public SnapshotLimits {
        if (maxEntityDataBytes < 1 || maxVisualDataBytes < 1 || maxExtensions < 0 || maxExtensionDataBytes < 1 || maxDisplayNameCharacters < 1) {
            throw new IllegalArgumentException("Snapshot limits must be positive");
        }
    }

    public void validate(IdentitySnapshot snapshot) {
        validateIdentity(snapshot.displayName(), snapshot.entityData(), snapshot.visualData(), snapshot.extensions());
    }

    void validateIdentity(String displayName, net.minecraft.nbt.CompoundTag entityData, net.minecraft.nbt.CompoundTag visualData, java.util.List<SnapshotExtension> extensions) {
        if (entityData.sizeInBytes() > maxEntityDataBytes) {
            throw new IllegalArgumentException("Entity data exceeds the configured snapshot limit");
        }
        if (visualData.sizeInBytes() > maxVisualDataBytes) {
            throw new IllegalArgumentException("Visual data exceeds the configured snapshot limit");
        }
        if (extensions.size() > maxExtensions) {
            throw new IllegalArgumentException("Snapshot has too many extensions");
        }
        if (displayName.length() > maxDisplayNameCharacters) {
            throw new IllegalArgumentException("Snapshot display name exceeds the configured limit");
        }
        validateExtensions(extensions);
    }

    public void validate(BaselineSnapshot snapshot) {
        validateBaseline(snapshot.playerData(), snapshot.extensions());
    }

    void validateBaseline(net.minecraft.nbt.CompoundTag playerData, java.util.List<SnapshotExtension> extensions) {
        if (playerData.sizeInBytes() > maxEntityDataBytes) {
            throw new IllegalArgumentException("Baseline player data exceeds the configured limit");
        }
        if (extensions.size() > maxExtensions) {
            throw new IllegalArgumentException("Baseline has too many extensions");
        }
        validateExtensions(extensions);
    }

    private void validateExtensions(java.util.List<SnapshotExtension> extensions) {
        for (SnapshotExtension extension : extensions) {
            if (extension.payload().sizeInBytes() > maxExtensionDataBytes) {
                throw new IllegalArgumentException("Snapshot extension exceeds the configured data limit");
            }
        }
    }
}
