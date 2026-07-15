package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.Objects;

public record ImitatorFormSlot(int index, ImitatorForm form) {
    public ImitatorFormSlot {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }
        Objects.requireNonNull(form, "form");
    }
}
