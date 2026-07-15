package com.github.gamekinger1st.imitationcoreapi.api.chat;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PersonaIdentity(UUID personaId, String displayName, Optional<UUID> copiedPlayerId) {
    public PersonaIdentity {
        Objects.requireNonNull(personaId, "personaId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(copiedPlayerId, "copiedPlayerId");
        displayName = displayName.strip();
        if (displayName.isEmpty() || displayName.length() > 256) {
            throw new IllegalArgumentException("Persona display name must contain between 1 and 256 characters");
        }
    }
}
