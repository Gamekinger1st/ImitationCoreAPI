package com.github.gamekinger1st.imitationcoreapi.api.skill;

public enum SkillClassification {
    STANDARD,
    RESISTANCE,
    INTRINSIC,
    COMMON,
    EXTRA,
    UNIQUE,
    ULTIMATE,
    UNKNOWN;

    public boolean standardLike() {
        return switch (this) {
            case STANDARD, RESISTANCE, INTRINSIC, COMMON, EXTRA -> true;
            case UNIQUE, ULTIMATE, UNKNOWN -> false;
        };
    }
}
