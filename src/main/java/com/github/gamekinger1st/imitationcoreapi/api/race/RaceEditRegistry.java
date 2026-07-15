package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RaceEditRegistry {
    private final Map<ResourceLocation, RaceEditProvider> providers = new LinkedHashMap<>();
    private final Map<ResourceLocation, RaceFunctionHandler> functionHandlers = new LinkedHashMap<>();
    private final Map<OverrideKey, OverrideEntry> overrides = new LinkedHashMap<>();

    public synchronized RaceEditRegistration register(RaceEditProvider provider) {
        Objects.requireNonNull(provider, "provider");
        ResourceLocation id = Objects.requireNonNull(provider.id(), "provider.id");
        if (providers.putIfAbsent(id, provider) != null) {
            throw new IllegalArgumentException("A race edit provider is already registered for " + id);
        }
        return () -> unregisterProvider(id, provider);
    }

    public synchronized RaceEditRegistration registerFunction(RaceFunctionHandler handler) {
        Objects.requireNonNull(handler, "handler");
        ResourceLocation id = Objects.requireNonNull(handler.id(), "handler.id");
        if (functionHandlers.putIfAbsent(id, handler) != null) {
            throw new IllegalArgumentException("A race function handler is already registered for " + id);
        }
        return () -> unregisterFunctionHandler(id, handler);
    }

    public RaceEditRegistration override(RaceEditProfile profile) {
        return override(null, profile);
    }

    public synchronized RaceEditRegistration override(ResourceLocation bridgeId, RaceEditProfile profile) {
        Objects.requireNonNull(profile, "profile");
        OverrideKey key = new OverrideKey(Optional.ofNullable(bridgeId), profile.raceId());
        OverrideEntry entry = new OverrideEntry(UUID.randomUUID(), profile);
        overrides.put(key, entry);
        return () -> unregisterOverride(key, entry);
    }

    public RaceEditProfile profile(ResourceLocation bridgeId, ResourceLocation raceId) {
        RaceEditContext context = new RaceEditContext(bridgeId, raceId);
        RaceEditBuilder builder = RaceEditProfile.builder(raceId);
        for (RaceEditProvider provider : orderedProvidersAscending()) {
            try {
                Optional<RaceEditProfile> edit = Objects.requireNonNull(provider.edit(context), "provider edit");
                edit.filter(profile -> profile.raceId().equals(raceId)).ifPresent(builder::merge);
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        explicit(Optional.empty(), raceId).ifPresent(builder::merge);
        explicit(Optional.of(bridgeId), raceId).ifPresent(builder::merge);
        return builder.build();
    }

    public Optional<Double> stat(ResourceLocation bridgeId, ResourceLocation raceId, ResourceLocation key) {
        return profile(bridgeId, raceId).stat(key);
    }

    public Optional<Component> line(ResourceLocation bridgeId, ResourceLocation raceId, ResourceLocation key) {
        return profile(bridgeId, raceId).line(key);
    }

    public Optional<CompoundTag> data(ResourceLocation bridgeId, ResourceLocation raceId, ResourceLocation key) {
        return profile(bridgeId, raceId).data(key);
    }

    public RaceFunctionResult handleFunction(RaceFunctionContext context) {
        Objects.requireNonNull(context, "context");
        for (RaceFunctionHandler handler : orderedFunctionHandlersDescending()) {
            try {
                RaceFunctionResult result = Objects.requireNonNull(handler.handle(context), "function result");
                if (result.handled()) {
                    return result;
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return RaceFunctionResult.pass();
    }

    private synchronized java.util.List<RaceEditProvider> orderedProvidersAscending() {
        return providers.values().stream().sorted(Comparator.comparingInt(RaceEditProvider::priority).thenComparing(provider -> provider.id().toString())).toList();
    }

    private synchronized java.util.List<RaceFunctionHandler> orderedFunctionHandlersDescending() {
        return functionHandlers.values().stream().sorted(Comparator.comparingInt(RaceFunctionHandler::priority).reversed().thenComparing(handler -> handler.id().toString())).toList();
    }

    private synchronized boolean unregisterProvider(ResourceLocation id, RaceEditProvider provider) {
        return providers.remove(id, provider);
    }

    private synchronized boolean unregisterFunctionHandler(ResourceLocation id, RaceFunctionHandler handler) {
        return functionHandlers.remove(id, handler);
    }

    private synchronized Optional<RaceEditProfile> explicit(Optional<ResourceLocation> bridgeId, ResourceLocation raceId) {
        OverrideEntry entry = overrides.get(new OverrideKey(bridgeId, raceId));
        return entry == null ? Optional.empty() : Optional.of(entry.profile());
    }

    private synchronized boolean unregisterOverride(OverrideKey key, OverrideEntry entry) {
        return overrides.remove(key, entry);
    }

    private record OverrideKey(Optional<ResourceLocation> bridgeId, ResourceLocation raceId) {
        private OverrideKey {
            Objects.requireNonNull(bridgeId, "bridgeId");
            Objects.requireNonNull(raceId, "raceId");
        }
    }

    private record OverrideEntry(UUID token, RaceEditProfile profile) {
        private OverrideEntry {
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(profile, "profile");
        }
    }
}
