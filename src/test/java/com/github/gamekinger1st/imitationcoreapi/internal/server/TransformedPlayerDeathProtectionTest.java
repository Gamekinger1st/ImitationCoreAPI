package com.github.gamekinger1st.imitationcoreapi.internal.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformedPlayerDeathProtectionTest {
    @Test
    void protectsOnlyDamageThatWouldConsumeHealthAndAbsorption() {
        assertFalse(TransformedPlayerDeathProtection.isLethal(5.0F, 3.0F, 7.99F));
        assertTrue(TransformedPlayerDeathProtection.isLethal(5.0F, 3.0F, 8.0F));
        assertTrue(TransformedPlayerDeathProtection.isLethal(5.0F, 0.0F, 6.0F));
    }

    @Test
    void ignoresNonPositiveOrNonFiniteDamage() {
        assertFalse(TransformedPlayerDeathProtection.isLethal(1.0F, 0.0F, 0.0F));
        assertFalse(TransformedPlayerDeathProtection.isLethal(1.0F, 0.0F, -1.0F));
        assertFalse(TransformedPlayerDeathProtection.isLethal(1.0F, 0.0F, Float.NaN));
    }

    @Test
    void deathEventFallbackUsesOneHealthBeforeReversion() {
        assertEquals(1.0F, TransformedPlayerDeathProtection.survivalHealth(20.0F));
        assertEquals(1.0F, TransformedPlayerDeathProtection.survivalHealth(0.5F));
    }
}
