package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.Objects;

public record ImitatorFormProgression(ImitatorProgressionAction action, ImitatorForm previousForm, ImitatorForm currentForm, boolean becamePerfect) {
    public ImitatorFormProgression {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(previousForm, "previousForm");
        Objects.requireNonNull(currentForm, "currentForm");
        if (!previousForm.snapshotId().equals(currentForm.snapshotId())) {
            throw new IllegalArgumentException("Form progression cannot change the snapshot identity");
        }
        if (currentForm.precision() < previousForm.precision()) {
            throw new IllegalArgumentException("Form progression cannot reduce precision");
        }
        if (becamePerfect && (!currentForm.perfect() || previousForm.perfect())) {
            throw new IllegalArgumentException("Perfect-form transition is invalid");
        }
    }
}
