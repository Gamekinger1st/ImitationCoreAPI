package com.github.gamekinger1st.imitationcoreapi.api.skill;

import java.util.Objects;

public record OwnerSkillUseDecision(boolean allowed, String detail) {
    public OwnerSkillUseDecision {
        Objects.requireNonNull(detail, "detail");
    }

    public static OwnerSkillUseDecision allow() {
        return new OwnerSkillUseDecision(true, "");
    }

    public static OwnerSkillUseDecision denied(String detail) {
        String normalized = Objects.requireNonNull(detail, "detail").strip();
        return new OwnerSkillUseDecision(false, normalized.isEmpty() ? "The original skill is suppressed by the active copied form" : normalized);
    }
}
