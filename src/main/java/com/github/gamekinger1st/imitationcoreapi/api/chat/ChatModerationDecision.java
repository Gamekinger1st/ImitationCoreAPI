package com.github.gamekinger1st.imitationcoreapi.api.chat;

import java.util.Objects;
import java.util.Optional;

public record ChatModerationDecision(boolean allowed, Optional<String> reason) {
    public ChatModerationDecision {
        Objects.requireNonNull(reason, "reason");
        reason = reason.map(String::strip).filter(value -> !value.isEmpty()).map(value -> value.length() > 256 ? value.substring(0, 256) : value);
        if (allowed && reason.isPresent()) {
            throw new IllegalArgumentException("Allowed chat decisions cannot include a rejection reason");
        }
    }

    public static ChatModerationDecision allow() {
        return new ChatModerationDecision(true, Optional.empty());
    }

    public static ChatModerationDecision block(String reason) {
        return new ChatModerationDecision(false, Optional.ofNullable(reason));
    }
}
