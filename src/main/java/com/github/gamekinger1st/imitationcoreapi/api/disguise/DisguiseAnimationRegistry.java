package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DisguiseAnimationRegistry {
    private final Map<ResourceLocation, DisguiseAnimationAdapter> adapters = new LinkedHashMap<>();
    private final Map<ResourceLocation, DisguiseAnimationIntentProvider> intentProviders = new LinkedHashMap<>();

    public synchronized DisguiseAnimationRegistration register(DisguiseAnimationAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        ResourceLocation id = Objects.requireNonNull(adapter.id(), "adapter.id");
        if (adapters.putIfAbsent(id, adapter) != null) {
            throw new IllegalArgumentException("A disguise animation adapter is already registered for " + id);
        }
        return new RegisteredAdapter(this, id, adapter);
    }

    public synchronized DisguiseAnimationIntentRegistration registerIntentProvider(DisguiseAnimationIntentProvider provider) {
        Objects.requireNonNull(provider, "provider");
        ResourceLocation id = Objects.requireNonNull(provider.id(), "provider.id");
        if (intentProviders.putIfAbsent(id, provider) != null) {
            throw new IllegalArgumentException("A disguise animation intent provider is already registered for " + id);
        }
        return new RegisteredIntentProvider(this, id, provider);
    }

    public void synchronize(Entity imitation, Entity subject, ClientDisguiseState state, float partialTick) {
        synchronizeHandled(imitation, subject, state, partialTick);
    }

    public boolean synchronizeHandled(Entity imitation, Entity subject, ClientDisguiseState state, float partialTick) {
        Objects.requireNonNull(imitation, "imitation");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(state, "state");
        if (!Float.isFinite(partialTick) || partialTick < 0F || partialTick > 1F) {
            throw new IllegalArgumentException("partialTick must be between zero and one");
        }
        DisguiseAnimationIntent intent = intent(imitation, subject, state, partialTick);
        boolean handled = false;
        for (DisguiseAnimationAdapter adapter : orderedAdapters()) {
            try {
                if (adapter.supports(imitation, subject, state)) {
                    adapter.synchronize(imitation, subject, state, partialTick, intent);
                    handled = true;
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return handled;
    }

    public void clearSession(java.util.UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        for (DisguiseAnimationAdapter adapter : orderedAdapters()) {
            try {
                adapter.clearSession(sessionId);
            } catch (RuntimeException | LinkageError exception) {
            }
        }
    }

    public void clearAllSessions() {
        for (DisguiseAnimationAdapter adapter : orderedAdapters()) {
            try {
                adapter.clearAllSessions();
            } catch (RuntimeException | LinkageError exception) {
            }
        }
    }

    private DisguiseAnimationIntent intent(Entity imitation, Entity subject, ClientDisguiseState state, float partialTick) {
        DisguiseAnimationIntent intent = DisguiseAnimationIntent.from(subject, partialTick);
        for (DisguiseAnimationIntentProvider provider : orderedIntentProviders()) {
            try {
                if (provider.supports(imitation, subject, state, intent)) {
                    intent = intent.withAdditionalCustomTriggers(provider.customTriggers(imitation, subject, state, intent));
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return intent;
    }

    private synchronized java.util.List<DisguiseAnimationAdapter> orderedAdapters() {
        return adapters.values().stream()
                .sorted(Comparator.comparingInt(DisguiseAnimationAdapter::priority).reversed().thenComparing(adapter -> adapter.id().toString()))
                .toList();
    }

    private synchronized java.util.List<DisguiseAnimationIntentProvider> orderedIntentProviders() {
        return intentProviders.values().stream()
                .sorted(Comparator.comparingInt(DisguiseAnimationIntentProvider::priority).reversed().thenComparing(provider -> provider.id().toString()))
                .toList();
    }

    private synchronized boolean unregister(ResourceLocation id, DisguiseAnimationAdapter adapter) {
        return adapters.remove(id, adapter);
    }

    private synchronized boolean unregisterIntentProvider(ResourceLocation id, DisguiseAnimationIntentProvider provider) {
        return intentProviders.remove(id, provider);
    }

    private record RegisteredAdapter(DisguiseAnimationRegistry registry, ResourceLocation id, DisguiseAnimationAdapter adapter) implements DisguiseAnimationRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, adapter);
        }
    }

    private record RegisteredIntentProvider(DisguiseAnimationRegistry registry, ResourceLocation id, DisguiseAnimationIntentProvider provider) implements DisguiseAnimationIntentRegistration {
        @Override
        public boolean unregister() {
            return registry.unregisterIntentProvider(id, provider);
        }
    }
}
