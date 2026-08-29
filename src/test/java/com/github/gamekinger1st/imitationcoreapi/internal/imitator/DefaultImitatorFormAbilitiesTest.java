package com.github.gamekinger1st.imitationcoreapi.internal.imitator;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormAbility;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultImitatorFormAbilitiesTest {
    @Test
    void registersTheDefaultCopiedFormAbilityCatalog() {
        Map<ResourceLocation, ImitatorFormAbility> abilities = DefaultImitatorFormAbilities.create().stream()
                .collect(Collectors.toMap(ImitatorFormAbility::id, Function.identity()));

        assertEquals(6, abilities.size());
        assertTrue(abilities.containsKey(id("skeleton_archer")));
        assertTrue(abilities.containsKey(id("enderman_teleport")));
        assertTrue(abilities.containsKey(id("creeper_explosion")));
        assertTrue(abilities.containsKey(id("blade_tiger_voice_cannon")));
        assertTrue(abilities.containsKey(id("spider_climb")));
        assertTrue(abilities.containsKey(id("sun_sensitive_undead")));
    }

    @Test
    void activeAndTickingBuiltInsSupportTheExpectedEntityTypes() {
        Map<ResourceLocation, ImitatorFormAbility> abilities = DefaultImitatorFormAbilities.create().stream()
                .collect(Collectors.toMap(ImitatorFormAbility::id, Function.identity()));

        ImitatorFormAbility skeleton = abilities.get(id("skeleton_archer"));
        assertTrue(skeleton.hasActiveAbility(snapshot(ResourceLocation.withDefaultNamespace("skeleton"))));
        assertTrue(skeleton.hasActiveAbility(snapshot(ResourceLocation.withDefaultNamespace("bogged"))));
        assertFalse(skeleton.hasActiveAbility(snapshot(ResourceLocation.withDefaultNamespace("zombie"))));

        ImitatorFormAbility enderman = abilities.get(id("enderman_teleport"));
        assertTrue(enderman.hasActiveAbility(snapshot(ResourceLocation.withDefaultNamespace("enderman"))));
        assertFalse(enderman.hasActiveAbility(snapshot(ResourceLocation.withDefaultNamespace("skeleton"))));

        ImitatorFormAbility bladeTiger = abilities.get(id("blade_tiger_voice_cannon"));
        assertTrue(bladeTiger.hasActiveAbility(snapshot(ResourceLocation.fromNamespaceAndPath("tensura", "blade_tiger"))));
        assertFalse(bladeTiger.hasActiveAbility(snapshot(ResourceLocation.withDefaultNamespace("enderman"))));

        ImitatorFormAbility creeper = abilities.get(id("creeper_explosion"));
        assertTrue(creeper.hasActiveAbility(snapshot(ResourceLocation.withDefaultNamespace("creeper"))));
        assertFalse(creeper.hasActiveAbility(snapshot(ResourceLocation.withDefaultNamespace("zombie"))));

        ImitatorFormAbility spider = abilities.get(id("spider_climb"));
        assertTrue(spider.hasTickAbility(snapshot(ResourceLocation.withDefaultNamespace("spider"))));
        assertTrue(spider.hasTickAbility(snapshot(ResourceLocation.withDefaultNamespace("cave_spider"))));
        assertFalse(spider.hasActiveAbility(snapshot(ResourceLocation.withDefaultNamespace("spider"))));

        ImitatorFormAbility undead = abilities.get(id("sun_sensitive_undead"));
        assertTrue(undead.hasTickAbility(snapshot(ResourceLocation.withDefaultNamespace("zombie"))));
        assertTrue(undead.hasTickAbility(snapshot(ResourceLocation.withDefaultNamespace("phantom"))));
        assertFalse(undead.hasTickAbility(snapshot(ResourceLocation.withDefaultNamespace("spider"))));
    }

    @Test
    void defaultAbilityIdsAreStableAndPriorityOrderedForRegistration() {
        List<ResourceLocation> ids = DefaultImitatorFormAbilities.create().stream()
                .map(ImitatorFormAbility::id)
                .toList();

        assertEquals(List.of(
                id("skeleton_archer"),
                id("enderman_teleport"),
                id("creeper_explosion"),
                id("blade_tiger_voice_cannon"),
                id("spider_climb"),
                id("sun_sensitive_undead")
        ), ids);
    }

    private static IdentitySnapshot snapshot(ResourceLocation entityType) {
        return IdentitySnapshot.builder(entityType, 0L).displayName("Copied Form").build();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, path);
    }
}
