package com.github.gamekinger1st.imitationcoreapi.api.adapter;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ImitationAdapterRegistry {
    private final Map<ResourceLocation, ImitationAdapter> adapters = new LinkedHashMap<>();

    public synchronized AdapterRegistration register(ImitationAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        ResourceLocation id = Objects.requireNonNull(adapter.id(), "adapter.id");
        if (adapters.containsKey(id)) {
            throw new IllegalArgumentException("An imitation adapter is already registered for " + id);
        }
        adapters.put(id, adapter);
        return new RegisteredAdapter(this, id, adapter);
    }

    public synchronized Optional<ImitationAdapter> get(ResourceLocation id) {
        return Optional.ofNullable(adapters.get(Objects.requireNonNull(id, "id")));
    }

    public synchronized List<ImitationAdapter> adapters(AdapterKind kind) {
        Objects.requireNonNull(kind, "kind");
        return adapters.values().stream()
                .filter(adapter -> adapter.kind() == kind)
                .sorted(Comparator.comparing(adapter -> adapter.id().toString()))
                .toList();
    }

    public synchronized List<ImitationAdapter> all() {
        return adapters.values().stream()
                .sorted(Comparator.comparing(adapter -> adapter.id().toString()))
                .toList();
    }

    public CompatibilityAssessment assess(IdentitySnapshot snapshot, Collection<AdapterKind> requiredKinds) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(requiredKinds, "requiredKinds");
        CompatibilityAssessment assessment = CompatibilityAssessment.full();
        List<AdapterKind> missing = new ArrayList<>();
        for (AdapterKind kind : requiredKinds) {
            List<ImitationAdapter> matching = adapters(kind);
            if (matching.isEmpty()) {
                missing.add(kind);
                continue;
            }
            for (ImitationAdapter adapter : matching) {
                try {
                    assessment = assessment.combine(Objects.requireNonNull(adapter.assess(snapshot), "adapter assessment"));
                } catch (RuntimeException | LinkageError exception) {
                    assessment = assessment.combine(CompatibilityAssessment.fallback("An imitation adapter assessment failed"));
                }
            }
        }
        for (AdapterKind kind : missing) {
            assessment = assessment.combine(CompatibilityAssessment.fallback("No " + kind.name().toLowerCase() + " adapter is registered"));
        }
        return assessment;
    }

    private synchronized boolean unregister(ResourceLocation id, ImitationAdapter adapter) {
        return adapters.remove(id, adapter);
    }

    private record RegisteredAdapter(ImitationAdapterRegistry registry, ResourceLocation id, ImitationAdapter adapter) implements AdapterRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, adapter);
        }
    }
}
