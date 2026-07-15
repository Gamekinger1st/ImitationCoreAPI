package com.github.gamekinger1st.imitationcoreapi.api.diagnostic;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ImitationDiagnostics {
    private ImitationDiagnostics() {
    }

    public static void rejected(ServerPlayer player, String message) {
        rejected(player, classify(message), message);
    }

    public static void rejected(ServerPlayer player, ImitationDiagnosticCategory category, String message) {
        publish(player, category, ImitationDiagnosticSeverity.WARNING, message);
    }

    public static void cleanupFailed(Optional<ServerPlayer> player, UUID sessionId, String message, long gameTime) {
        publish(
                ImitationDiagnosticCategory.TEMPORARY_CLEANUP_FAILED,
                ImitationDiagnosticSeverity.ERROR,
                message,
                player.map(ServerPlayer::getUUID),
                Optional.of(sessionId),
                Optional.empty(),
                Optional.empty(),
                gameTime
        );
    }

    public static void compatibility(ServerPlayer player, CompatibilityAssessment assessment) {
        Objects.requireNonNull(assessment, "assessment");
        if (assessment.level() == CompatibilityLevel.FULL) {
            return;
        }
        ImitationDiagnosticSeverity severity = assessment.level().isUsable() ? ImitationDiagnosticSeverity.WARNING : ImitationDiagnosticSeverity.ERROR;
        publish(player, compatibilityCategory(assessment), severity, compatibilityMessage(assessment));
    }

    public static void publish(ServerPlayer player, ImitationDiagnosticCategory category, ImitationDiagnosticSeverity severity, String message) {
        Objects.requireNonNull(player, "player");
        publish(
                category,
                severity,
                message,
                Optional.of(player.getUUID()),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                player.level().getGameTime()
        );
    }

    public static void publish(
            ImitationDiagnosticCategory category,
            ImitationDiagnosticSeverity severity,
            String message,
            Optional<UUID> playerId,
            Optional<UUID> sessionId,
            Optional<UUID> snapshotId,
            Optional<ResourceLocation> subjectId,
            long gameTime
    ) {
        ImitationApi.diagnostics().post(ImitationDiagnostic.create(category, severity, message, playerId, sessionId, snapshotId, subjectId, gameTime));
    }

    public static ImitationDiagnosticCategory classify(String message) {
        String normalized = Objects.requireNonNull(message, "message").toLowerCase(Locale.ROOT);
        if (normalized.contains("protocol")) {
            return ImitationDiagnosticCategory.CLIENT_PROTOCOL_MISMATCH;
        }
        if (normalized.contains("gecko")) {
            return ImitationDiagnosticCategory.MISSING_GECKOLIB_BRIDGE;
        }
        if (normalized.contains("tensura")) {
            return ImitationDiagnosticCategory.MISSING_TENSURA_BRIDGE;
        }
        if (normalized.contains("snapshot") || normalized.contains("no longer available") || normalized.contains("does not exist")) {
            return ImitationDiagnosticCategory.INVALID_SNAPSHOT;
        }
        if (normalized.contains("perfect form") || normalized.contains("mirror sync")) {
            return ImitationDiagnosticCategory.UNSAFE_PERFECT_FORM;
        }
        if (normalized.contains("no form slot is selected")) {
            return ImitationDiagnosticCategory.NO_SELECTED_FORM;
        }
        if (normalized.contains("replica") || normalized.contains("living copy")) {
            return ImitationDiagnosticCategory.REPLICA_UNSUPPORTED;
        }
        if (normalized.contains("renderer") || normalized.contains("render")) {
            return ImitationDiagnosticCategory.UNSUPPORTED_RENDERER;
        }
        if (normalized.contains("faction") || normalized.contains("resolver")) {
            return ImitationDiagnosticCategory.FACTION_RESOLVER_MISSING;
        }
        if (normalized.contains("cleanup") || normalized.contains("temporary") || normalized.contains("reversion") || normalized.contains("quarantine")) {
            return ImitationDiagnosticCategory.TEMPORARY_CLEANUP_FAILED;
        }
        return ImitationDiagnosticCategory.ACTION_REJECTED;
    }

    public static ImitationDiagnosticCategory compatibilityCategory(CompatibilityAssessment assessment) {
        String reasons = String.join(" ", assessment.reasons()).toLowerCase(Locale.ROOT);
        if (reasons.contains("renderer") || reasons.contains("render")) {
            return ImitationDiagnosticCategory.UNSUPPORTED_RENDERER;
        }
        if (reasons.contains("gecko")) {
            return ImitationDiagnosticCategory.MISSING_GECKOLIB_BRIDGE;
        }
        if (reasons.contains("tensura")) {
            return ImitationDiagnosticCategory.MISSING_TENSURA_BRIDGE;
        }
        return ImitationDiagnosticCategory.COMPATIBILITY_DEGRADED;
    }

    private static String compatibilityMessage(CompatibilityAssessment assessment) {
        if (assessment.reasons().isEmpty()) {
            return "Compatibility level is " + assessment.level().name().toLowerCase(Locale.ROOT);
        }
        return "Compatibility level is " + assessment.level().name().toLowerCase(Locale.ROOT) + ": " + String.join("; ", assessment.reasons());
    }
}
