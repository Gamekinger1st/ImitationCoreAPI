package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;

import java.util.Objects;
import java.util.Optional;

public record ImitatorTransformOutcome(SessionTransitionResult transition, Optional<ImitatorFormProgression> progression, int copiedSkillCount) {
    public ImitatorTransformOutcome(SessionTransitionResult transition, Optional<ImitatorFormProgression> progression) {
        this(transition, progression, 0);
    }

    public ImitatorTransformOutcome {
        Objects.requireNonNull(transition, "transition");
        Objects.requireNonNull(progression, "progression");
        if (!transition.accepted() && progression.isPresent()) {
            throw new IllegalArgumentException("Rejected transformations cannot refine a form");
        }
        if (copiedSkillCount < 0 || !transition.accepted() && copiedSkillCount > 0) {
            throw new IllegalArgumentException("Rejected transformations cannot copy skills");
        }
    }
}
