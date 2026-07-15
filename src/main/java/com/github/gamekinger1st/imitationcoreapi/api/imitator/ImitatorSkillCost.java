package com.github.gamekinger1st.imitationcoreapi.api.imitator;

public record ImitatorSkillCost(long resourceCost, int cooldownTicks, long requiredMastery) {
    public ImitatorSkillCost {
        if (resourceCost < 0L || cooldownTicks < 0 || requiredMastery < 0L) {
            throw new IllegalArgumentException("Imitator skill costs cannot be negative");
        }
    }

    public static ImitatorSkillCost free() {
        return new ImitatorSkillCost(0L, 0, 0L);
    }
}
