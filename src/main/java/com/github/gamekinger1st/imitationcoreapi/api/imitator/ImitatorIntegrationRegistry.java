package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaIdentity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ImitatorIntegrationRegistry {
    private final Map<ResourceLocation, ImitatorIntegration> integrations = new LinkedHashMap<>();

    public synchronized ImitatorIntegrationRegistration register(ImitatorIntegration integration) {
        Objects.requireNonNull(integration, "integration");
        ResourceLocation id = Objects.requireNonNull(integration.id(), "integration.id");
        if (integrations.putIfAbsent(id, integration) != null) {
            throw new IllegalArgumentException("An Imitator integration is already registered for " + id);
        }
        return new RegisteredIntegration(this, id, integration);
    }

    public Optional<UUID> activeSession(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        for (ImitatorIntegration integration : orderedIntegrations()) {
            try {
                Optional<UUID> session = Objects.requireNonNull(integration.activeSession(player), "active session");
                if (session.isPresent()) {
                    return session;
                }
            } catch (RuntimeException | LinkageError exception) {
                ImitationCoreApi.LOGGER.error("Imitator integration {} failed to resolve an active session", integration.id(), exception);
            }
        }
        return Optional.empty();
    }

    public Optional<PersonaIdentity> activePersona(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        for (ImitatorIntegration integration : orderedIntegrations()) {
            try {
                Optional<PersonaIdentity> persona = Objects.requireNonNull(integration.activePersona(player), "active persona");
                if (persona.isPresent()) {
                    return persona;
                }
            } catch (RuntimeException | LinkageError exception) {
                ImitationCoreApi.LOGGER.error("Imitator integration {} failed to resolve an active persona", integration.id(), exception);
            }
        }
        return Optional.empty();
    }

    private synchronized java.util.List<ImitatorIntegration> orderedIntegrations() {
        return integrations.values().stream().sorted(Comparator.comparingInt(ImitatorIntegration::priority).reversed().thenComparing(integration -> integration.id().toString())).toList();
    }

    private synchronized boolean unregister(ResourceLocation id, ImitatorIntegration integration) {
        return integrations.remove(id, integration);
    }

    private record RegisteredIntegration(ImitatorIntegrationRegistry registry, ResourceLocation id, ImitatorIntegration integration) implements ImitatorIntegrationRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, integration);
        }
    }
}
