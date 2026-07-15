package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotExtension;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TensuraStateExtensions {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "tensura_state");
    public static final int SCHEMA_VERSION = 1;

    private TensuraStateExtensions() {
    }

    public static SnapshotExtension create(TensuraStateSnapshot snapshot) {
        return new SnapshotExtension(ID, SCHEMA_VERSION, TensuraStateSnapshotSerialization.toTag(Objects.requireNonNull(snapshot, "snapshot")));
    }

    public static Optional<TensuraStateSnapshot> find(List<SnapshotExtension> extensions) {
        Objects.requireNonNull(extensions, "extensions");
        return extensions.stream()
                .filter(extension -> extension.adapterId().equals(ID))
                .filter(extension -> extension.schemaVersion() == SCHEMA_VERSION)
                .findFirst()
                .flatMap(extension -> {
                    try {
                        return Optional.of(TensuraStateSnapshotSerialization.fromTag(extension.payload()));
                    } catch (IllegalArgumentException exception) {
                        return Optional.empty();
                    }
                });
    }
}
