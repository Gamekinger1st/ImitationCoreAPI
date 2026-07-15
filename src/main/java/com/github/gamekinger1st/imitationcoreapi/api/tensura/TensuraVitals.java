package com.github.gamekinger1st.imitationcoreapi.api.tensura;

public record TensuraVitals(double ep, double magicule, double aura, double spiritualHealth) {
    public TensuraVitals {
        validate(ep, "ep");
        validate(magicule, "magicule");
        validate(aura, "aura");
        validate(spiritualHealth, "spiritualHealth");
    }

    private static void validate(double value, String name) {
        if (!Double.isFinite(value) || value < 0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    public TensuraVitals plus(TensuraVitals delta) {
        java.util.Objects.requireNonNull(delta, "delta");
        return new TensuraVitals(ep + delta.ep, magicule + delta.magicule, aura + delta.aura, spiritualHealth + delta.spiritualHealth);
    }

    public TensuraVitals positiveDeltaSince(TensuraVitals previous) {
        java.util.Objects.requireNonNull(previous, "previous");
        return new TensuraVitals(
                Math.max(0D, ep - previous.ep),
                Math.max(0D, magicule - previous.magicule),
                Math.max(0D, aura - previous.aura),
                Math.max(0D, spiritualHealth - previous.spiritualHealth)
        );
    }

    public boolean isZero() {
        return ep == 0D && magicule == 0D && aura == 0D && spiritualHealth == 0D;
    }
}
