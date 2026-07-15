package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.Objects;
import java.util.Optional;

public record ImitatorActionResult(
        boolean accepted,
        Optional<ImitatorPendingRecord> pendingRecord,
        Optional<ImitatorForm> form,
        String message
) {
    public ImitatorActionResult {
        Objects.requireNonNull(pendingRecord, "pendingRecord");
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(message, "message");
        if (message.length() > 256) {
            throw new IllegalArgumentException("Action result message exceeds the configured limit");
        }
    }

    public static ImitatorActionResult accepted(String message) {
        return new ImitatorActionResult(true, Optional.empty(), Optional.empty(), message);
    }

    public static ImitatorActionResult recorded(ImitatorPendingRecord pendingRecord) {
        int precision = (int) Math.round(pendingRecord.precision() * 100D);
        return new ImitatorActionResult(true, Optional.of(pendingRecord), Optional.empty(), "Record captured with " + precision + "% precision; choose a form slot");
    }

    public static ImitatorActionResult form(ImitatorForm form, String message) {
        return new ImitatorActionResult(true, Optional.empty(), Optional.of(form), message);
    }

    public static ImitatorActionResult rejected(String message) {
        return new ImitatorActionResult(false, Optional.empty(), Optional.empty(), message);
    }
}
