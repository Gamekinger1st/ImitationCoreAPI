package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;

import java.util.Objects;
import java.util.Optional;

public record ImitatorSkillUseResult(
        boolean accepted,
        ImitatorSkillMode mode,
        ImitatorSkillCost cost,
        ImitatorMenuRequest menuRequest,
        Optional<ImitatorActionResult> recordResult,
        Optional<SessionTransitionResult> transformationResult,
        String message,
        int masteryReward
) {
    public ImitatorSkillUseResult(boolean accepted, ImitatorSkillMode mode, ImitatorSkillCost cost, ImitatorMenuRequest menuRequest, Optional<ImitatorActionResult> recordResult, Optional<SessionTransitionResult> transformationResult, String message) {
        this(accepted, mode, cost, menuRequest, recordResult, transformationResult, message, 0);
    }

    public ImitatorSkillUseResult {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(menuRequest, "menuRequest");
        Objects.requireNonNull(recordResult, "recordResult");
        Objects.requireNonNull(transformationResult, "transformationResult");
        Objects.requireNonNull(message, "message");
        message = message.strip();
        if (message.isEmpty() || message.length() > 256) {
            throw new IllegalArgumentException("Imitator skill result message is outside the supported bounds");
        }
        if (recordResult.isPresent() && transformationResult.isPresent()) {
            throw new IllegalArgumentException("An Imitator skill result cannot contain two action results");
        }
        if (masteryReward < 0) {
            throw new IllegalArgumentException("Imitator mastery reward cannot be negative");
        }
    }

    public static ImitatorSkillUseResult rejected(ImitatorSkillMode mode, ImitatorSkillCost cost, ImitatorMenuRequest menuRequest, String message) {
        return new ImitatorSkillUseResult(false, mode, cost, menuRequest, Optional.empty(), Optional.empty(), message);
    }

    public static ImitatorSkillUseResult record(ImitatorSkillMode mode, ImitatorSkillCost cost, ImitatorActionResult result) {
        return record(mode, cost, result, 0);
    }

    public static ImitatorSkillUseResult record(ImitatorSkillMode mode, ImitatorSkillCost cost, ImitatorActionResult result, int masteryReward) {
        return new ImitatorSkillUseResult(result.accepted(), mode, cost, result.accepted() ? ImitatorMenuRequest.COMMIT_RECORD : ImitatorMenuRequest.NONE, Optional.of(result), Optional.empty(), result.message(), result.accepted() ? masteryReward : 0);
    }

    public static ImitatorSkillUseResult transform(ImitatorSkillMode mode, ImitatorSkillCost cost, SessionTransitionResult result) {
        return transform(mode, cost, result, 0, result.accepted() ? "Transformation started" : result.message());
    }

    public static ImitatorSkillUseResult transform(ImitatorSkillMode mode, ImitatorSkillCost cost, SessionTransitionResult result, int masteryReward, String message) {
        return new ImitatorSkillUseResult(result.accepted(), mode, cost, ImitatorMenuRequest.NONE, Optional.empty(), Optional.of(result), message, result.accepted() ? masteryReward : 0);
    }

    public static ImitatorSkillUseResult replica(ImitatorSkillMode mode, ImitatorSkillCost cost, SessionTransitionResult result, int masteryReward, String message) {
        return new ImitatorSkillUseResult(result.accepted(), mode, cost, ImitatorMenuRequest.NONE, Optional.empty(), Optional.empty(), message, result.accepted() ? masteryReward : 0);
    }
}
