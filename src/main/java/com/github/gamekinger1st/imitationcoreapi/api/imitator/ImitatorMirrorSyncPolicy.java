package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraCopyPolicy;

import java.util.Objects;

public record ImitatorMirrorSyncPolicy(TensuraCopyPolicy tensuraCopyPolicy) {
    public static final ImitatorMirrorSyncPolicy DEFAULT = new ImitatorMirrorSyncPolicy(TensuraCopyPolicy.DEFAULT);

    public ImitatorMirrorSyncPolicy {
        Objects.requireNonNull(tensuraCopyPolicy, "tensuraCopyPolicy");
    }
}
