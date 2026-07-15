package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import java.util.List;

public record TensuraCopyPolicy(double maximumTargetEpRatio, double minimumMirrorPrecision, boolean mirrorSyncEnabled) {
    public static final TensuraCopyPolicy DEFAULT = new TensuraCopyPolicy(1D, 1D, false);

    public TensuraCopyPolicy {
        if (!Double.isFinite(maximumTargetEpRatio) || maximumTargetEpRatio <= 0D) {
            throw new IllegalArgumentException("maximumTargetEpRatio must be finite and positive");
        }
        if (!Double.isFinite(minimumMirrorPrecision) || minimumMirrorPrecision < 0D || minimumMirrorPrecision > 1D) {
            throw new IllegalArgumentException("minimumMirrorPrecision must be between zero and one");
        }
    }

    public TensuraCopyPolicyDecision evaluate(TensuraVitals imitator, TensuraVitals target, double precision, boolean requestedMirrorSync) {
        java.util.Objects.requireNonNull(imitator, "imitator");
        java.util.Objects.requireNonNull(target, "target");
        if (!Double.isFinite(precision) || precision < 0D || precision > 1D) {
            throw new IllegalArgumentException("precision must be between zero and one");
        }
        if (!requestedMirrorSync) {
            return TensuraCopyPolicyDecision.accepted(0D);
        }
        if (!mirrorSyncEnabled) {
            return TensuraCopyPolicyDecision.rejected("Perfect Form is disabled by policy");
        }
        if (precision < minimumMirrorPrecision) {
            return TensuraCopyPolicyDecision.rejected("The selected form does not meet the Perfect Form precision requirement");
        }
        if (target.ep() > imitator.ep() * maximumTargetEpRatio) {
            return TensuraCopyPolicyDecision.rejected("The target exceeds the configured EP copy limit");
        }
        return TensuraCopyPolicyDecision.accepted(Math.min(precision, powerRatio(imitator, target)));
    }

    public double powerRatio(TensuraVitals imitator, TensuraVitals target) {
        java.util.Objects.requireNonNull(imitator, "imitator");
        java.util.Objects.requireNonNull(target, "target");
        if (target.ep() == 0D) {
            return 1D;
        }
        return Math.max(0D, Math.min(1D, imitator.ep() / target.ep()));
    }

    public record TensuraCopyPolicyDecision(boolean accepted, double scale, List<String> reasons) {
        public TensuraCopyPolicyDecision {
            if (!Double.isFinite(scale) || scale < 0D || scale > 1D) {
                throw new IllegalArgumentException("scale must be between zero and one");
            }
            reasons = List.copyOf(reasons);
        }

        public static TensuraCopyPolicyDecision accepted(double scale) {
            return new TensuraCopyPolicyDecision(true, scale, List.of());
        }

        public static TensuraCopyPolicyDecision rejected(String reason) {
            return new TensuraCopyPolicyDecision(false, 0D, List.of(reason));
        }
    }
}
