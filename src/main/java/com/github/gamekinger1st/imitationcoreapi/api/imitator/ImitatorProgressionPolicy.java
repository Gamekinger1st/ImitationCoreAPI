package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.Objects;
import java.util.UUID;

public record ImitatorProgressionPolicy(
        double minimumPrecision,
        double maximumInitialPrecision,
        double distancePenalty,
        double motionPenalty,
        double powerDeficitPenalty,
        double maximumAnalysisBonus,
        double masteredRecordingBonus,
        double transformPrecisionGain,
        double replicaPrecisionGain,
        double perfectThreshold,
        double mirrorSyncThreshold,
        double perfectCostMultiplier,
        double minimumReproductionScale,
        double masteredReproductionBonus,
        int recordMasteryReward,
        int transformMasteryReward,
        int replicaMasteryReward,
        int perfectFormMasteryBonus
) {
    public static final ImitatorProgressionPolicy DEFAULT = new ImitatorProgressionPolicy(
            0.35D,
            0.90D,
            0.25D,
            0.20D,
            0.25D,
            0.20D,
            0.10D,
            0.02D,
            0.01D,
            0.999D,
            0.85D,
            0.50D,
            0.25D,
            0.15D,
            2,
            5,
            3,
            10
    );

    public ImitatorProgressionPolicy {
        validateRatio(minimumPrecision, "minimumPrecision");
        validateRatio(maximumInitialPrecision, "maximumInitialPrecision");
        validateRatio(distancePenalty, "distancePenalty");
        validateRatio(motionPenalty, "motionPenalty");
        validateRatio(powerDeficitPenalty, "powerDeficitPenalty");
        validateRatio(maximumAnalysisBonus, "maximumAnalysisBonus");
        validateRatio(masteredRecordingBonus, "masteredRecordingBonus");
        validateRatio(transformPrecisionGain, "transformPrecisionGain");
        validateRatio(replicaPrecisionGain, "replicaPrecisionGain");
        validateRatio(perfectThreshold, "perfectThreshold");
        validateRatio(mirrorSyncThreshold, "mirrorSyncThreshold");
        validateRatio(perfectCostMultiplier, "perfectCostMultiplier");
        validateRatio(minimumReproductionScale, "minimumReproductionScale");
        validateRatio(masteredReproductionBonus, "masteredReproductionBonus");
        if (minimumPrecision > maximumInitialPrecision || maximumInitialPrecision >= perfectThreshold || mirrorSyncThreshold < minimumPrecision || mirrorSyncThreshold > perfectThreshold) {
            throw new IllegalArgumentException("Imitator precision thresholds are inconsistent");
        }
        if (recordMasteryReward < 0 || transformMasteryReward < 0 || replicaMasteryReward < 0 || perfectFormMasteryBonus < 0) {
            throw new IllegalArgumentException("Imitator mastery rewards cannot be negative");
        }
    }

    public ImitatorForm initialForm(UUID snapshotId, ImitatorRecordingContext context) {
        Objects.requireNonNull(snapshotId, "snapshotId");
        context = Objects.requireNonNull(context, "context");
        return form(snapshotId, initialPrecision(context), true);
    }

    public double initialPrecision(ImitatorRecordingContext context) {
        Objects.requireNonNull(context, "context");
        double precision = 1D
                - context.distanceRatio() * distancePenalty
                - context.targetMotionRatio() * motionPenalty
                - (1D - context.powerRatio()) * powerDeficitPenalty
                + Math.min(context.analysisBonus(), maximumAnalysisBonus)
                + (context.mastered() ? masteredRecordingBonus : 0D);
        return clamp(precision, minimumPrecision, maximumInitialPrecision);
    }

    public ImitatorFormProgression refine(ImitatorForm form, ImitatorProgressionAction action) {
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(action, "action");
        double gain = switch (action) {
            case TRANSFORM -> transformPrecisionGain;
            case REPLICA -> replicaPrecisionGain;
            case RECORD -> 0D;
        };
        ImitatorForm refined = form(form.snapshotId(), Math.min(1D, form.precision() + gain), form.skillCopyAllowed()).withStats(form.stats());
        return new ImitatorFormProgression(action, form, refined, !form.perfect() && refined.perfect());
    }

    public boolean allowsMirrorSync(ImitatorForm form) {
        return Objects.requireNonNull(form, "form").precision() >= mirrorSyncThreshold;
    }

    public long adjustedResourceCost(long baseCost, ImitatorForm form) {
        if (baseCost < 0L) {
            throw new IllegalArgumentException("Base cost cannot be negative");
        }
        Objects.requireNonNull(form, "form");
        return form.perfect() ? (long) Math.ceil(baseCost * perfectCostMultiplier) : baseCost;
    }

    public double reproductionScale(double ownerPower, double targetPower, boolean mastered) {
        if (!Double.isFinite(ownerPower) || !Double.isFinite(targetPower) || ownerPower < 0D || targetPower < 0D) {
            throw new IllegalArgumentException("Reproduction power values cannot be negative or non-finite");
        }
        if (targetPower == 0D || ownerPower >= targetPower) {
            return 1D;
        }
        double scale = ownerPower == 0D ? minimumReproductionScale : ownerPower / targetPower;
        if (mastered) {
            scale += masteredReproductionBonus;
        }
        return clamp(scale, minimumReproductionScale, 1D);
    }

    public int masteryReward(ImitatorProgressionAction action, boolean becamePerfect) {
        Objects.requireNonNull(action, "action");
        int reward = switch (action) {
            case RECORD -> recordMasteryReward;
            case TRANSFORM -> transformMasteryReward;
            case REPLICA -> replicaMasteryReward;
        };
        return becamePerfect ? reward + perfectFormMasteryBonus : reward;
    }

    private ImitatorForm form(UUID snapshotId, double precision, boolean skillCopyAllowed) {
        boolean perfect = precision >= perfectThreshold;
        return new ImitatorForm(snapshotId, precision, perfect, precision >= mirrorSyncThreshold, skillCopyAllowed);
    }

    private static void validateRatio(double value, String name) {
        if (!Double.isFinite(value) || value < 0D || value > 1D) {
            throw new IllegalArgumentException(name + " must be between zero and one");
        }
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
