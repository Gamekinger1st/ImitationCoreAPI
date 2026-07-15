package com.github.gamekinger1st.imitationcoreapi.api.session;

import java.util.EnumSet;

public enum TransformationState {
    CAPTURING,
    VALIDATING,
    CHANNELING,
    APPLYING,
    ACTIVE,
    REVERTING,
    CLEANING,
    REVERTED,
    FAILED;

    public boolean canTransitionTo(TransformationState target) {
        return switch (this) {
            case CAPTURING -> EnumSet.of(VALIDATING, REVERTING, FAILED).contains(target);
            case VALIDATING -> EnumSet.of(CHANNELING, APPLYING, REVERTING, FAILED).contains(target);
            case CHANNELING -> EnumSet.of(APPLYING, REVERTING, FAILED).contains(target);
            case APPLYING -> EnumSet.of(ACTIVE, REVERTING, FAILED).contains(target);
            case ACTIVE -> EnumSet.of(REVERTING, FAILED).contains(target);
            case REVERTING -> EnumSet.of(CLEANING, REVERTED, FAILED).contains(target);
            case CLEANING -> EnumSet.of(REVERTED, FAILED).contains(target);
            case REVERTED -> false;
            case FAILED -> EnumSet.of(REVERTING, CLEANING).contains(target);
        };
    }

    public boolean isTerminal() {
        return this == REVERTED;
    }

    public boolean requiresRecovery() {
        return this != REVERTED;
    }
}
