package com.github.gamekinger1st.imitationcoreapi.internal.server;

final class TransformedPlayerDeathProtection {
    private TransformedPlayerDeathProtection() {
    }

    static boolean isLethal(float health, float absorption, float damage) {
        if (!Float.isFinite(damage) || damage <= 0.0F) {
            return false;
        }
        return damage >= Math.max(0.0F, health) + Math.max(0.0F, absorption);
    }

    static float survivalHealth(float maxHealth) {
        return Math.max(1.0F, Math.min(1.0F, maxHealth));
    }
}
