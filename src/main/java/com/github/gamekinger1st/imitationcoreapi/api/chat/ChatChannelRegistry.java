package com.github.gamekinger1st.imitationcoreapi.api.chat;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ChatChannelRegistry {
    private final Map<ResourceLocation, ChatChannelProvider> providers = new LinkedHashMap<>();

    public synchronized ChatChannelRegistration register(ChatChannelProvider provider) {
        Objects.requireNonNull(provider, "provider");
        ResourceLocation id = Objects.requireNonNull(provider.id(), "provider.id");
        if (providers.putIfAbsent(id, provider) != null) {
            throw new IllegalArgumentException("A chat channel provider is already registered for " + id);
        }
        return new RegisteredProvider(this, id, provider);
    }

    public Optional<ChatChannelProvider> provider(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        synchronized (this) {
            return Optional.ofNullable(providers.get(id));
        }
    }

    public Optional<ChatDelivery> route(ChatChannelRequest request) {
        Objects.requireNonNull(request, "request");
        Optional<ChatChannelProvider> provider = provider(request.channelId());
        if (provider.isEmpty()) {
            return Optional.empty();
        }
        try {
            return provider.get().acceptsPlayerMessages()
                    ? Objects.requireNonNull(provider.get().route(request), "chat channel route result")
                    : Optional.empty();
        } catch (RuntimeException | LinkageError exception) {
            ImitationCoreApi.LOGGER.error("Chat channel provider {} failed", request.channelId(), exception);
            return Optional.empty();
        }
    }

    public synchronized java.util.List<ChatChannelProvider> providers() {
        return providers.values().stream()
                .sorted(Comparator.comparingInt(ChatChannelProvider::priority).reversed().thenComparing(provider -> provider.id().toString()))
                .toList();
    }

    private synchronized boolean unregister(ResourceLocation id, ChatChannelProvider provider) {
        return providers.remove(id, provider);
    }

    private record RegisteredProvider(ChatChannelRegistry registry, ResourceLocation id, ChatChannelProvider provider) implements ChatChannelRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, provider);
        }
    }
}
