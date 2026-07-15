package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

import net.minecraft.world.entity.Entity;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record SnapshotCaptureContext(Entity subject, Optional<UUID> requesterId, long capturedGameTime, SnapshotLimits limits) {
    public SnapshotCaptureContext {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(requesterId, "requesterId");
        Objects.requireNonNull(limits, "limits");
        if (capturedGameTime < 0) {
            throw new IllegalArgumentException("capturedGameTime cannot be negative");
        }
    }
}
