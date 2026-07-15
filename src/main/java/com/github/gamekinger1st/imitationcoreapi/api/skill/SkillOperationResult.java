package com.github.gamekinger1st.imitationcoreapi.api.skill;

import java.util.Objects;

public record SkillOperationResult(boolean successful, String detail) {
    public SkillOperationResult {
        Objects.requireNonNull(detail, "detail");
    }

    public static SkillOperationResult success() {
        return new SkillOperationResult(true, "");
    }

    public static SkillOperationResult failure(String detail) {
        return new SkillOperationResult(false, detail);
    }
}
