package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.Objects;
import java.util.UUID;

public final class ImitatorFormService {
    private final ImitatorFormRepository repository;
    private final ImitatorFormLibraryLimits limits;

    public ImitatorFormService(ImitatorFormRepository repository) {
        this(repository, ImitatorFormLibraryLimits.DEFAULT);
    }

    public ImitatorFormService(ImitatorFormRepository repository, ImitatorFormLibraryLimits limits) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public ImitatorFormLibrary library(UUID ownerId) {
        return new PersistentImitatorFormLibrary(Objects.requireNonNull(ownerId, "ownerId"), repository, limits);
    }

    public ImitatorPendingRecord stagePendingRecord(UUID ownerId, UUID snapshotId, long gameTime) {
        return stagePendingRecord(ownerId, snapshotId, gameTime, ImitatorProgressionPolicy.DEFAULT.initialForm(snapshotId, ImitatorRecordingContext.DEFAULT));
    }

    public ImitatorPendingRecord stagePendingRecord(UUID ownerId, UUID snapshotId, long gameTime, ImitatorForm form) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(form, "form");
        if (!snapshotId.equals(form.snapshotId())) {
            throw new IllegalArgumentException("Pending form must use the staged snapshot identity");
        }
        if (gameTime < 0) {
            throw new IllegalArgumentException("gameTime cannot be negative");
        }
        ImitatorPendingRecord pending = new ImitatorPendingRecord(snapshotId, gameTime, gameTime + limits.maxPendingDurationTicks(), form.precision(), form.mirrorSyncAllowed(), form.skillCopyAllowed());
        library(ownerId).setPendingRecord(pending);
        return pending;
    }
}
