package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TensuraStateSnapshotSerialization {
    private TensuraStateSnapshotSerialization() {
    }

    public static CompoundTag toTag(TensuraStateSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putString("bridge", snapshot.bridgeId().toString());
        tag.putInt("schema", snapshot.schemaVersion());
        tag.putDouble("ep", snapshot.vitals().ep());
        tag.putDouble("magicule", snapshot.vitals().magicule());
        tag.putDouble("aura", snapshot.vitals().aura());
        tag.putDouble("spiritual_health", snapshot.vitals().spiritualHealth());
        ListTag sections = new ListTag();
        snapshot.sections().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag section = new CompoundTag();
            section.putString("id", entry.getKey().toString());
            section.put("data", entry.getValue());
            sections.add(section);
        });
        tag.put("sections", sections);
        return tag;
    }

    public static TensuraStateSnapshot fromTag(CompoundTag tag) {
        ResourceLocation bridge = resourceLocation(tag, "bridge");
        if (!tag.contains("schema", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Missing Tensura state snapshot schema version");
        }
        TensuraVitals vitals = new TensuraVitals(
                tag.getDouble("ep"),
                tag.getDouble("magicule"),
                tag.getDouble("aura"),
                tag.getDouble("spiritual_health")
        );
        Map<ResourceLocation, CompoundTag> sections = new LinkedHashMap<>();
        ListTag tags = tag.getList("sections", Tag.TAG_COMPOUND);
        for (int index = 0; index < tags.size(); index++) {
            CompoundTag section = tags.getCompound(index);
            ResourceLocation id = resourceLocation(section, "id");
            if (!section.contains("data", Tag.TAG_COMPOUND) || sections.putIfAbsent(id, section.getCompound("data")) != null) {
                throw new IllegalArgumentException("Invalid or duplicate Tensura state snapshot section: " + id);
            }
        }
        return new TensuraStateSnapshot(bridge, tag.getInt("schema"), vitals, sections);
    }

    private static ResourceLocation resourceLocation(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_STRING)) {
            throw new IllegalArgumentException("Missing resource location field: " + key);
        }
        ResourceLocation value = ResourceLocation.tryParse(tag.getString(key));
        if (value == null) {
            throw new IllegalArgumentException("Invalid resource location field: " + key);
        }
        return value;
    }
}
