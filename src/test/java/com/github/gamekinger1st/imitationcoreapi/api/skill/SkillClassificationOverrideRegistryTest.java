package com.github.gamekinger1st.imitationcoreapi.api.skill;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillClassificationOverrideRegistryTest {
    @Test
    void resolvesBridgeSpecificThenGlobalThenProviderThenOriginal() {
        SkillClassificationOverrideRegistry registry = new SkillClassificationOverrideRegistry();
        ResourceLocation bridge = ResourceLocation.fromNamespaceAndPath("test", "bridge");
        ResourceLocation otherBridge = ResourceLocation.fromNamespaceAndPath("test", "other_bridge");
        ResourceLocation skill = ResourceLocation.fromNamespaceAndPath("test", "skill");

        registry.register(new Provider(ResourceLocation.fromNamespaceAndPath("test", "provider"), 0, SkillClassification.EXTRA));
        SkillClassificationRegistration global = registry.override(skill, SkillClassification.COMMON);
        SkillClassificationRegistration specific = registry.override(bridge, skill, SkillClassification.INTRINSIC);

        assertEquals(SkillClassification.INTRINSIC, registry.resolve(bridge, skill, SkillClassification.UNIQUE));
        assertEquals(SkillClassification.COMMON, registry.resolve(otherBridge, skill, SkillClassification.UNIQUE));

        specific.unregister();

        assertEquals(SkillClassification.COMMON, registry.resolve(bridge, skill, SkillClassification.UNIQUE));

        global.unregister();

        assertEquals(SkillClassification.EXTRA, registry.resolve(bridge, skill, SkillClassification.UNIQUE));
    }

    @Test
    void ignoresFailingProvidersAndKeepsOriginal() {
        SkillClassificationOverrideRegistry registry = new SkillClassificationOverrideRegistry();
        ResourceLocation bridge = ResourceLocation.fromNamespaceAndPath("test", "bridge");
        ResourceLocation skill = ResourceLocation.fromNamespaceAndPath("test", "skill");

        registry.register(new FailingProvider(ResourceLocation.fromNamespaceAndPath("test", "failing")));

        assertEquals(SkillClassification.UNIQUE, registry.resolve(bridge, skill, SkillClassification.UNIQUE));
    }

    private record Provider(ResourceLocation id, int priority, SkillClassification classification) implements SkillClassificationProvider {
        @Override
        public Optional<SkillClassification> classify(SkillClassificationContext context) {
            return Optional.of(classification);
        }
    }

    private record FailingProvider(ResourceLocation id) implements SkillClassificationProvider {
        @Override
        public Optional<SkillClassification> classify(SkillClassificationContext context) {
            throw new IllegalStateException();
        }
    }
}
