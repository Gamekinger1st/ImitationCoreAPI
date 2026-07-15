package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DisguisePresentationRegistry {
    private final Map<ResourceLocation, DisguisePresentationAdapter> adapters = new LinkedHashMap<>();

    public synchronized DisguisePresentationRegistration register(DisguisePresentationAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        ResourceLocation id = Objects.requireNonNull(adapter.id(), "adapter.id");
        if (adapters.putIfAbsent(id, adapter) != null) {
            throw new IllegalArgumentException("A disguise presentation adapter is already registered for " + id);
        }
        return new RegisteredAdapter(this, id, adapter);
    }

    public DisguisePresentation resolve(Entity subject, ClientDisguiseState state) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(state, "state");
        for (DisguisePresentationAdapter adapter : orderedAdapters()) {
            try {
                if (adapter.supports(subject, state)) {
                    return Objects.requireNonNull(adapter.presentation(subject, state), "adapter presentation");
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return DisguisePresentation.fallback();
    }

    private synchronized java.util.List<DisguisePresentationAdapter> orderedAdapters() {
        return adapters.values().stream().sorted(Comparator.comparingInt(DisguisePresentationAdapter::priority).reversed().thenComparing(adapter -> adapter.id().toString())).toList();
    }

    private synchronized boolean unregister(ResourceLocation id, DisguisePresentationAdapter adapter) {
        return adapters.remove(id, adapter);
    }

    private record RegisteredAdapter(DisguisePresentationRegistry registry, ResourceLocation id, DisguisePresentationAdapter adapter) implements DisguisePresentationRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, adapter);
        }
    }
}
