package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

public final class SnapshotCaptureException extends RuntimeException {
    public SnapshotCaptureException(String message) {
        super(message);
    }

    public SnapshotCaptureException(String message, Throwable cause) {
        super(message, cause);
    }
}
