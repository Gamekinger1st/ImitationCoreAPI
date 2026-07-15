package com.github.gamekinger1st.imitationcoreapi.api.targeting;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MobFactionRegistryTest {
    @Test
    void usesTheHighestPriorityResolverAndFallsBackToTheEntityType() {
        MobFactionRegistry registry = new MobFactionRegistry();
        ResourceLocation zombie = ResourceLocation.withDefaultNamespace("zombie");
        ResourceLocation undead = ResourceLocation.fromNamespaceAndPath("test", "undead");
        ResourceLocation override = ResourceLocation.fromNamespaceAndPath("test", "override");
        registry.register(new Resolver(ResourceLocation.fromNamespaceAndPath("test", "base"), 0, zombie, undead));
        registry.register(new Resolver(ResourceLocation.fromNamespaceAndPath("test", "override"), 10, zombie, override));

        assertEquals(override, registry.resolve(zombie));
        assertEquals(ResourceLocation.withDefaultNamespace("cow"), registry.resolve(ResourceLocation.withDefaultNamespace("cow")));
        assertEquals(Optional.of(ResourceLocation.fromNamespaceAndPath("test", "override")), registry.resolveWithStatus(zombie).resolverId());
        assertEquals(Optional.empty(), registry.resolveWithStatus(ResourceLocation.withDefaultNamespace("cow")).resolverId());
    }

    @Test
    void rejectsDuplicateResolverIdsAndAllowsUnregistration() {
        MobFactionRegistry registry = new MobFactionRegistry();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("test", "resolver");
        MobFactionRegistration registration = registry.register(new Resolver(id, 0, ResourceLocation.withDefaultNamespace("zombie"), ResourceLocation.fromNamespaceAndPath("test", "undead")));

        assertThrows(IllegalArgumentException.class, () -> registry.register(new Resolver(id, 1, ResourceLocation.withDefaultNamespace("zombie"), ResourceLocation.fromNamespaceAndPath("test", "override"))));
        assertEquals(true, registration.unregister());
        assertFalse(registration.unregister());
    }

    @Test
    void ignoresAFailingResolverAndContinuesWithTheNextResolver() {
        MobFactionRegistry registry = new MobFactionRegistry();
        ResourceLocation zombie = ResourceLocation.withDefaultNamespace("zombie");
        ResourceLocation undead = ResourceLocation.fromNamespaceAndPath("test", "undead");
        registry.register(new MobFactionResolver() {
            @Override
            public ResourceLocation id() {
                return ResourceLocation.fromNamespaceAndPath("test", "failing");
            }

            @Override
            public int priority() {
                return 10;
            }

            @Override
            public Optional<ResourceLocation> resolve(ResourceLocation entityType) {
                throw new IllegalStateException("broken integration");
            }
        });
        registry.register(new Resolver(ResourceLocation.fromNamespaceAndPath("test", "fallback"), 0, zombie, undead));

        assertEquals(undead, registry.resolve(zombie));
    }

    @Test
    void groupsDifferentMobTypesIntoTheSameFactionWithoutSpecificMobExceptions() {
        MobFactionRegistry registry = new MobFactionRegistry();
        ResourceLocation skeleton = ResourceLocation.withDefaultNamespace("skeleton");
        ResourceLocation stray = ResourceLocation.withDefaultNamespace("stray");
        ResourceLocation undead = ResourceLocation.fromNamespaceAndPath("test", "undead");
        registry.register(new MobFactionResolver() {
            @Override
            public ResourceLocation id() {
                return ResourceLocation.fromNamespaceAndPath("test", "undead_group");
            }

            @Override
            public int priority() {
                return 0;
            }

            @Override
            public Optional<ResourceLocation> resolve(ResourceLocation entityType) {
                return entityType.equals(skeleton) || entityType.equals(stray) ? Optional.of(undead) : Optional.empty();
            }
        });

        assertEquals(registry.resolve(skeleton), registry.resolve(stray));
        assertEquals(undead, registry.resolveWithStatus(skeleton).factionId());
        assertEquals(Optional.of(ResourceLocation.fromNamespaceAndPath("test", "undead_group")), registry.resolveWithStatus(stray).resolverId());
    }

    private record Resolver(ResourceLocation id, int priority, ResourceLocation entityType, ResourceLocation faction) implements MobFactionResolver {
        @Override
        public Optional<ResourceLocation> resolve(ResourceLocation entityType) {
            return this.entityType.equals(entityType) ? Optional.of(faction) : Optional.empty();
        }
    }
}
