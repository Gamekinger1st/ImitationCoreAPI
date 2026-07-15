package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DisguiseRenderRegistry {
    private final Map<ResourceLocation, DisguiseRenderAdapter> adapters = new LinkedHashMap<>();

    public synchronized DisguiseRenderRegistration register(DisguiseRenderAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        ResourceLocation id = Objects.requireNonNull(adapter.id(), "adapter.id");
        if (adapters.putIfAbsent(id, adapter) != null) {
            throw new IllegalArgumentException("A disguise render adapter is already registered for " + id);
        }
        return new RegisteredAdapter(this, id, adapter);
    }

    public boolean render(DisguiseRenderContext context) {
        Objects.requireNonNull(context, "context");
        for (DisguiseRenderAdapter adapter : orderedAdapters()) {
            try {
                if (adapter.supports(context) && adapter.render(context)) {
                    return true;
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return false;
    }

    private synchronized java.util.List<DisguiseRenderAdapter> orderedAdapters() {
        return adapters.values().stream()
                .sorted(Comparator.comparingInt(DisguiseRenderAdapter::priority).reversed().thenComparing(adapter -> adapter.id().toString()))
                .toList();
    }

    private synchronized boolean unregister(ResourceLocation id, DisguiseRenderAdapter adapter) {
        return adapters.remove(id, adapter);
    }

    private record RegisteredAdapter(DisguiseRenderRegistry registry, ResourceLocation id, DisguiseRenderAdapter adapter) implements DisguiseRenderRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, adapter);
        }
    }
}
