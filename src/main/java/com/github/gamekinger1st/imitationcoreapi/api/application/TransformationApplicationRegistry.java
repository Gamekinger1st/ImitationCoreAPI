package com.github.gamekinger1st.imitationcoreapi.api.application;

import net.minecraft.resources.ResourceLocation;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TransformationApplicationRegistry {
    private final Map<ResourceLocation, TransformationApplicationAdapter> adapters = new LinkedHashMap<>();

    public synchronized TransformationApplicationRegistration register(TransformationApplicationAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        ResourceLocation id = Objects.requireNonNull(adapter.id(), "adapter.id");
        if (adapters.putIfAbsent(id, adapter) != null) {
            throw new IllegalArgumentException("A transformation application adapter is already registered for " + id);
        }
        return new RegisteredAdapter(this, id, adapter);
    }

    public synchronized Optional<TransformationApplicationAdapter> get(ResourceLocation id) {
        return Optional.ofNullable(adapters.get(Objects.requireNonNull(id, "id")));
    }

    public synchronized List<TransformationApplicationAdapter> applyOrder() {
        return adapters.values().stream()
                .sorted(Comparator.comparingInt(TransformationApplicationAdapter::priority).reversed().thenComparing(adapter -> adapter.id().toString()))
                .toList();
    }

    public synchronized List<TransformationApplicationAdapter> applyOrder(TransformationScope scope) {
        Objects.requireNonNull(scope, "scope");
        return adapters.values().stream()
                .filter(adapter -> adapter.appliesTo(scope))
                .sorted(Comparator.comparingInt(TransformationApplicationAdapter::priority).reversed().thenComparing(adapter -> adapter.id().toString()))
                .toList();
    }

    public synchronized List<TransformationApplicationAdapter> revertOrder() {
        return adapters.values().stream()
                .sorted(Comparator.comparingInt(TransformationApplicationAdapter::priority).thenComparing(adapter -> adapter.id().toString()))
                .toList();
    }

    public synchronized List<TransformationApplicationAdapter> revertOrder(TransformationScope scope) {
        Objects.requireNonNull(scope, "scope");
        return adapters.values().stream()
                .filter(adapter -> adapter.appliesTo(scope))
                .sorted(Comparator.comparingInt(TransformationApplicationAdapter::priority).thenComparing(adapter -> adapter.id().toString()))
                .toList();
    }

    private synchronized boolean unregister(ResourceLocation id, TransformationApplicationAdapter adapter) {
        return adapters.remove(id, adapter);
    }

    private record RegisteredAdapter(TransformationApplicationRegistry registry, ResourceLocation id, TransformationApplicationAdapter adapter) implements TransformationApplicationRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, adapter);
        }
    }
}
