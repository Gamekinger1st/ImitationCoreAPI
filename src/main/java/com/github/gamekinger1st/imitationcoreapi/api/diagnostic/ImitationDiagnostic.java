package com.github.gamekinger1st.imitationcoreapi.api.diagnostic;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ImitationDiagnostic(
        ImitationDiagnosticCategory category,
        ImitationDiagnosticSeverity severity,
        String message,
        Optional<UUID> playerId,
        Optional<UUID> sessionId,
        Optional<UUID> snapshotId,
        Optional<ResourceLocation> subjectId,
        long gameTime
) {
    public ImitationDiagnostic {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(subjectId, "subjectId");
        if (gameTime < 0L) {
            throw new IllegalArgumentException("gameTime cannot be negative");
        }
        message = bounded(message);
    }

    public static ImitationDiagnostic create(
            ImitationDiagnosticCategory category,
            ImitationDiagnosticSeverity severity,
            String message,
            Optional<UUID> playerId,
            Optional<UUID> sessionId,
            Optional<UUID> snapshotId,
            Optional<ResourceLocation> subjectId,
            long gameTime
    ) {
        return new ImitationDiagnostic(category, severity, message, playerId, sessionId, snapshotId, subjectId, gameTime);
    }

    private static String bounded(String value) {
        String normalized = value.strip();
        return normalized.length() > 512 ? normalized.substring(0, 512) : normalized;
    }
}
