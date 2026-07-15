package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record TensuraStateSnapshot(ResourceLocation bridgeId, int schemaVersion, TensuraVitals vitals, Map<ResourceLocation, CompoundTag> sections) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final int MAX_SECTIONS = 16;
    private static final int MAX_SECTION_BYTES = 262_144;

    public TensuraStateSnapshot {
        Objects.requireNonNull(bridgeId, "bridgeId");
        Objects.requireNonNull(vitals, "vitals");
        Objects.requireNonNull(sections, "sections");
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported Tensura state snapshot schema version: " + schemaVersion);
        }
        if (sections.size() > MAX_SECTIONS) {
            throw new IllegalArgumentException("Tensura state snapshot has too many sections");
        }
        Map<ResourceLocation, CompoundTag> copied = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, CompoundTag> entry : sections.entrySet()) {
            ResourceLocation section = Objects.requireNonNull(entry.getKey(), "section");
            CompoundTag value = Objects.requireNonNull(entry.getValue(), "section data").copy();
            if (value.sizeInBytes() > MAX_SECTION_BYTES) {
                throw new IllegalArgumentException("Tensura state section exceeds the configured limit: " + section);
            }
            copied.put(section, value);
        }
        sections = Map.copyOf(copied);
    }

    @Override
    public Map<ResourceLocation, CompoundTag> sections() {
        Map<ResourceLocation, CompoundTag> copied = new LinkedHashMap<>();
        sections.forEach((section, data) -> copied.put(section, data.copy()));
        return Map.copyOf(copied);
    }
}
