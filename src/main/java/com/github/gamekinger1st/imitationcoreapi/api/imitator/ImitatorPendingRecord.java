package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.Objects;
import java.util.UUID;

public record ImitatorPendingRecord(UUID snapshotId, long createdGameTime, long expiresGameTime, double precision, boolean mirrorSyncAllowed, boolean skillCopyAllowed) {
    public ImitatorPendingRecord(UUID snapshotId, long createdGameTime, long expiresGameTime, double precision, boolean mirrorSyncAllowed) {
        this(snapshotId, createdGameTime, expiresGameTime, precision, mirrorSyncAllowed, false);
    }

    public ImitatorPendingRecord(UUID snapshotId, long createdGameTime, long expiresGameTime) {
        this(snapshotId, createdGameTime, expiresGameTime, ImitatorProgressionPolicy.DEFAULT.minimumPrecision(), false, false);
    }

    public ImitatorPendingRecord {
        Objects.requireNonNull(snapshotId, "snapshotId");
        if (createdGameTime < 0) {
            throw new IllegalArgumentException("createdGameTime cannot be negative");
        }
        if (expiresGameTime <= createdGameTime) {
            throw new IllegalArgumentException("expiresGameTime must be later than createdGameTime");
        }
        if (!Double.isFinite(precision) || precision < 0D || precision > 1D) {
            throw new IllegalArgumentException("precision must be between zero and one");
        }
    }

    public boolean isExpired(long gameTime) {
        return gameTime >= expiresGameTime;
    }
}
