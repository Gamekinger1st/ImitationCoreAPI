package com.github.gamekinger1st.imitationcoreapi.api.chat;

import java.util.Objects;
import java.util.Optional;

public record PersonaChatDecision(PersonaChatDisposition disposition, Optional<PersonaIdentity> persona, Optional<String> reason) {
    public PersonaChatDecision {
        Objects.requireNonNull(disposition, "disposition");
        Objects.requireNonNull(persona, "persona");
        Objects.requireNonNull(reason, "reason");
        reason = reason.map(String::strip).filter(value -> !value.isEmpty()).map(value -> value.length() > 256 ? value.substring(0, 256) : value);
        if (disposition == PersonaChatDisposition.REPLACE && persona.isEmpty()) {
            throw new IllegalArgumentException("Replacing chat requires a persona");
        }
        if (disposition != PersonaChatDisposition.REPLACE && persona.isPresent()) {
            throw new IllegalArgumentException("Only replacement chat may provide a persona");
        }
    }

    public static PersonaChatDecision passthrough() {
        return new PersonaChatDecision(PersonaChatDisposition.PASSTHROUGH, Optional.empty(), Optional.empty());
    }

    public static PersonaChatDecision replace(PersonaIdentity persona) {
        return new PersonaChatDecision(PersonaChatDisposition.REPLACE, Optional.of(persona), Optional.empty());
    }

    public static PersonaChatDecision block(String reason) {
        return new PersonaChatDecision(PersonaChatDisposition.BLOCK, Optional.empty(), Optional.of(reason));
    }
}
