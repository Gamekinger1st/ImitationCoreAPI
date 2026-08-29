package com.github.gamekinger1st.imitationcoreapi.api.chat;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PersonaChatRegistry {
    private final Map<ResourceLocation, PersonaChatProvider> providers = new LinkedHashMap<>();

    public synchronized PersonaChatRegistration register(PersonaChatProvider provider) {
        Objects.requireNonNull(provider, "provider");
        ResourceLocation id = Objects.requireNonNull(provider.id(), "provider.id");
        if (providers.putIfAbsent(id, provider) != null) {
            throw new IllegalArgumentException("A persona chat provider is already registered for " + id);
        }
        return new RegisteredProvider(this, id, provider);
    }

    public PersonaChatDecision resolve(ServerPlayer sender, String rawText) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(rawText, "rawText");
        for (PersonaChatProvider provider : orderedProviders()) {
            try {
                PersonaChatDecision decision = Objects.requireNonNull(provider.resolve(sender, rawText), "persona chat decision");
                if (decision.disposition() != PersonaChatDisposition.PASSTHROUGH) {
                    return decision;
                }
            } catch (RuntimeException | LinkageError exception) {
                ImitationCoreApi.LOGGER.error("Persona chat provider {} failed", provider.id(), exception);
            }
        }
        return PersonaChatDecision.passthrough();
    }

    private synchronized java.util.List<PersonaChatProvider> orderedProviders() {
        return providers.values().stream()
                .sorted(Comparator.comparingInt(PersonaChatProvider::priority).reversed().thenComparing(provider -> provider.id().toString()))
                .toList();
    }

    private synchronized boolean unregister(ResourceLocation id, PersonaChatProvider provider) {
        return providers.remove(id, provider);
    }

    private record RegisteredProvider(PersonaChatRegistry registry, ResourceLocation id, PersonaChatProvider provider) implements PersonaChatRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, provider);
        }
    }
}
