package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import java.util.Objects;

public record TensuraStateOperationResult(boolean successful, String detail) {
    public TensuraStateOperationResult {
        Objects.requireNonNull(detail, "detail");
    }

    public static TensuraStateOperationResult success() {
        return new TensuraStateOperationResult(true, "");
    }

    public static TensuraStateOperationResult failure(String detail) {
        return new TensuraStateOperationResult(false, Objects.requireNonNull(detail, "detail"));
    }
}
