package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.Objects;
import java.util.UUID;

public record ImitatorForm(UUID snapshotId, double precision, boolean perfect, boolean mirrorSyncAllowed, boolean skillCopyAllowed, ImitatorFormStats stats) {
    public ImitatorForm(UUID snapshotId, double precision, boolean perfect, boolean mirrorSyncAllowed) {
        this(snapshotId, precision, perfect, mirrorSyncAllowed, false, ImitatorFormStats.empty());
    }

    public ImitatorForm(UUID snapshotId, double precision, boolean perfect, boolean mirrorSyncAllowed, boolean skillCopyAllowed) {
        this(snapshotId, precision, perfect, mirrorSyncAllowed, skillCopyAllowed, ImitatorFormStats.empty());
    }

    public ImitatorForm {
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(stats, "stats");
        if (!Double.isFinite(precision) || precision < 0 || precision > 1) {
            throw new IllegalArgumentException("precision must be between zero and one");
        }
    }

    public ImitatorForm withStats(ImitatorFormStats stats) {
        return new ImitatorForm(snapshotId, precision, perfect, mirrorSyncAllowed, skillCopyAllowed, stats);
    }
}
