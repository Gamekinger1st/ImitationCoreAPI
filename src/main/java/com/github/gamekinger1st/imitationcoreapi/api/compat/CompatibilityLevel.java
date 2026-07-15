package com.github.gamekinger1st.imitationcoreapi.api.compat;

public enum CompatibilityLevel {
    FULL(3),
    VISUAL(2),
    FALLBACK(1),
    UNSUPPORTED(0);

    private final int rank;

    CompatibilityLevel(int rank) {
        this.rank = rank;
    }

    public boolean isUsable() {
        return this != UNSUPPORTED;
    }

    public CompatibilityLevel combine(CompatibilityLevel other) {
        return rank <= other.rank ? this : other;
    }
}
