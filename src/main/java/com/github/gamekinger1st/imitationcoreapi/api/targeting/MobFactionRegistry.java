package com.github.gamekinger1st.imitationcoreapi.api.targeting;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class MobFactionRegistry {
    private final Map<ResourceLocation, MobFactionResolver> resolvers = new LinkedHashMap<>();

    public synchronized MobFactionRegistration register(MobFactionResolver resolver) {
        Objects.requireNonNull(resolver, "resolver");
        ResourceLocation id = Objects.requireNonNull(resolver.id(), "resolver.id");
        if (resolvers.containsKey(id)) {
            throw new IllegalArgumentException("A mob faction resolver is already registered for " + id);
        }
        resolvers.put(id, resolver);
        return new RegisteredResolver(this, id, resolver);
    }

    public synchronized ResourceLocation resolve(ResourceLocation entityType) {
        return resolveWithStatus(entityType).factionId();
    }

    public synchronized MobFactionResolution resolveWithStatus(ResourceLocation entityType) {
        Objects.requireNonNull(entityType, "entityType");
        for (MobFactionResolver resolver : resolvers.values().stream()
                .sorted(Comparator.comparingInt(MobFactionResolver::priority).reversed().thenComparing(resolver -> resolver.id().toString()))
                .toList()) {
            try {
                java.util.Optional<ResourceLocation> resolved = Objects.requireNonNull(resolver.resolve(entityType), "resolver result");
                if (resolved.isPresent()) {
                    return new MobFactionResolution(entityType, resolved.get(), java.util.Optional.of(resolver.id()));
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return new MobFactionResolution(entityType, entityType, java.util.Optional.empty());
    }

    private synchronized boolean unregister(ResourceLocation id, MobFactionResolver resolver) {
        return resolvers.remove(id, resolver);
    }

    private record RegisteredResolver(MobFactionRegistry registry, ResourceLocation id, MobFactionResolver resolver) implements MobFactionRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, resolver);
        }
    }
}
