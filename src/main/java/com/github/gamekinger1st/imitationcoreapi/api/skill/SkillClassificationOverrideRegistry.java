package com.github.gamekinger1st.imitationcoreapi.api.skill;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class SkillClassificationOverrideRegistry {
    private final Map<ResourceLocation, SkillClassificationProvider> providers = new LinkedHashMap<>();
    private final Map<OverrideKey, OverrideEntry> overrides = new LinkedHashMap<>();

    public synchronized SkillClassificationRegistration register(SkillClassificationProvider provider) {
        Objects.requireNonNull(provider, "provider");
        ResourceLocation id = Objects.requireNonNull(provider.id(), "provider.id");
        if (providers.putIfAbsent(id, provider) != null) {
            throw new IllegalArgumentException("A skill classification provider is already registered for " + id);
        }
        return () -> unregisterProvider(id, provider);
    }

    public SkillClassificationRegistration override(ResourceLocation skillId, SkillClassification classification) {
        return override(null, skillId, classification);
    }

    public synchronized SkillClassificationRegistration override(ResourceLocation bridgeId, ResourceLocation skillId, SkillClassification classification) {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(classification, "classification");
        OverrideKey key = new OverrideKey(Optional.ofNullable(bridgeId), skillId);
        OverrideEntry entry = new OverrideEntry(UUID.randomUUID(), classification);
        overrides.put(key, entry);
        return () -> unregisterOverride(key, entry);
    }

    public SkillClassification resolve(ResourceLocation bridgeId, ResourceLocation skillId, SkillClassification originalClassification) {
        Objects.requireNonNull(bridgeId, "bridgeId");
        Objects.requireNonNull(skillId, "skillId");
        SkillClassification original = Objects.requireNonNullElse(originalClassification, SkillClassification.UNKNOWN);
        Optional<SkillClassification> exact = explicit(Optional.of(bridgeId), skillId);
        if (exact.isPresent()) {
            return exact.get();
        }
        Optional<SkillClassification> global = explicit(Optional.empty(), skillId);
        if (global.isPresent()) {
            return global.get();
        }
        SkillClassificationContext context = new SkillClassificationContext(bridgeId, skillId, original);
        for (SkillClassificationProvider provider : orderedProviders()) {
            try {
                Optional<SkillClassification> classification = Objects.requireNonNull(provider.classify(context), "provider classification");
                if (classification.isPresent()) {
                    return Objects.requireNonNull(classification.get(), "classification");
                }
            } catch (RuntimeException | LinkageError exception) {
                ImitationCoreApi.LOGGER.error("Skill classification provider {} failed", provider.id(), exception);
            }
        }
        return original;
    }

    private synchronized Optional<SkillClassification> explicit(Optional<ResourceLocation> bridgeId, ResourceLocation skillId) {
        OverrideEntry entry = overrides.get(new OverrideKey(bridgeId, skillId));
        return entry == null ? Optional.empty() : Optional.of(entry.classification());
    }

    private synchronized java.util.List<SkillClassificationProvider> orderedProviders() {
        return providers.values().stream().sorted(Comparator.comparingInt(SkillClassificationProvider::priority).reversed().thenComparing(provider -> provider.id().toString())).toList();
    }

    private synchronized boolean unregisterProvider(ResourceLocation id, SkillClassificationProvider provider) {
        return providers.remove(id, provider);
    }

    private synchronized boolean unregisterOverride(OverrideKey key, OverrideEntry entry) {
        return overrides.remove(key, entry);
    }

    private record OverrideKey(Optional<ResourceLocation> bridgeId, ResourceLocation skillId) {
        private OverrideKey {
            Objects.requireNonNull(bridgeId, "bridgeId");
            Objects.requireNonNull(skillId, "skillId");
        }
    }

    private record OverrideEntry(UUID token, SkillClassification classification) {
        private OverrideEntry {
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(classification, "classification");
        }
    }
}
