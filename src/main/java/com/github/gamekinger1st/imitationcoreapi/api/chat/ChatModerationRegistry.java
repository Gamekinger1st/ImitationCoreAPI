package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ChatModerationRegistry {
    private final Map<ResourceLocation, ChatModerationProvider> providers = new LinkedHashMap<>();

    public synchronized ChatModerationRegistration register(ChatModerationProvider provider) {
        Objects.requireNonNull(provider, "provider");
        ResourceLocation id = Objects.requireNonNull(provider.id(), "provider.id");
        if (providers.putIfAbsent(id, provider) != null) {
            throw new IllegalArgumentException("A chat moderation provider is already registered for " + id);
        }
        return new RegisteredProvider(this, id, provider);
    }

    public ChatModerationDecision evaluate(ChatModerationContext context) {
        Objects.requireNonNull(context, "context");
        for (ChatModerationProvider provider : orderedProviders()) {
            ChatModerationDecision decision = Objects.requireNonNull(provider.evaluate(context), "chat moderation decision");
            if (!decision.allowed()) {
                return decision;
            }
        }
        return ChatModerationDecision.allow();
    }

    private synchronized java.util.List<ChatModerationProvider> orderedProviders() {
        return providers.values().stream()
                .sorted(Comparator.comparingInt(ChatModerationProvider::priority).reversed().thenComparing(provider -> provider.id().toString()))
                .toList();
    }

    private synchronized boolean unregister(ResourceLocation id, ChatModerationProvider provider) {
        return providers.remove(id, provider);
    }

    private record RegisteredProvider(ChatModerationRegistry registry, ResourceLocation id, ChatModerationProvider provider) implements ChatModerationRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, provider);
        }
    }
}
