package com.github.gamekinger1st.imitationcoreapi.api.chat;

import java.util.Objects;
import java.util.Optional;

public record ChatDeliveryResult(boolean accepted, Optional<ChatEnvelope> envelope, Optional<String> reason) {
    public ChatDeliveryResult {
        Objects.requireNonNull(envelope, "envelope");
        Objects.requireNonNull(reason, "reason");
        if (accepted != envelope.isPresent()) {
            throw new IllegalArgumentException("Accepted chat delivery results must include exactly one envelope");
        }
        if (accepted && reason.isPresent()) {
            throw new IllegalArgumentException("Accepted chat delivery results cannot include a rejection reason");
        }
        reason = reason.map(String::strip).filter(value -> !value.isEmpty()).map(value -> value.length() > 256 ? value.substring(0, 256) : value);
    }

    public static ChatDeliveryResult accepted(ChatEnvelope envelope) {
        return new ChatDeliveryResult(true, Optional.of(envelope), Optional.empty());
    }

    public static ChatDeliveryResult rejected(String reason) {
        return new ChatDeliveryResult(false, Optional.empty(), Optional.ofNullable(reason));
    }
}
