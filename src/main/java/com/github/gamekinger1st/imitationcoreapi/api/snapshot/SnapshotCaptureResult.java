package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;

import java.util.Objects;

public record SnapshotCaptureResult(IdentitySnapshot snapshot, CompatibilityAssessment compatibility) {
    public SnapshotCaptureResult {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(compatibility, "compatibility");
    }
}
