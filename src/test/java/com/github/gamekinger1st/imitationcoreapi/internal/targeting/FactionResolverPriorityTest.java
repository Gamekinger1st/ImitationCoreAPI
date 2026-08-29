package com.github.gamekinger1st.imitationcoreapi.internal.targeting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionResolverPriorityTest {
    @Test
    void loadedModFactionTagsOverrideVanillaFamilyFallbacks() {
        assertTrue(new TagBackedMobFactionResolver().priority() > new VanillaMobFactionResolver().priority());
    }
}
