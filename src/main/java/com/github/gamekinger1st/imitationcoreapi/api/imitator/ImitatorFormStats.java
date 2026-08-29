package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotExtension;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ImitatorFormStats(Optional<DisguiseAppraisalSnapshot> appraisal, java.util.Map<net.minecraft.resources.ResourceLocation, Double> attributeBaseValues) {
    public static final ImitatorFormStats EMPTY = new ImitatorFormStats(Optional.empty(), java.util.Map.of());
    private static final String APPRAISAL = "appraisal";
    private static final String ATTRIBUTES = "attributes";

    public ImitatorFormStats(Optional<DisguiseAppraisalSnapshot> appraisal) {
        this(appraisal, java.util.Map.of());
    }

    public ImitatorFormStats {
        Objects.requireNonNull(appraisal, "appraisal");
        Objects.requireNonNull(attributeBaseValues, "attributeBaseValues");
        attributeBaseValues = java.util.Map.copyOf(attributeBaseValues);
    }

    public static ImitatorFormStats empty() {
        return EMPTY;
    }

    public static ImitatorFormStats fromSnapshot(IdentitySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        CompoundTag visualData = snapshot.visualData();
        java.util.Map<net.minecraft.resources.ResourceLocation, Double> attributes = visualData.contains(ATTRIBUTES, Tag.TAG_COMPOUND)
                ? attributes(visualData.getCompound(ATTRIBUTES))
                : java.util.Map.of();
        return new ImitatorFormStats(DisguiseAppraisalExtensions.find(snapshot.extensions()), attributes);
    }

    public static ImitatorFormStats fromAppraisal(DisguiseAppraisalSnapshot appraisal) {
        return new ImitatorFormStats(Optional.of(Objects.requireNonNull(appraisal, "appraisal")), java.util.Map.of());
    }

    public ImitatorFormStats apply(ImitatorFormStatDelta delta) {
        Objects.requireNonNull(delta, "delta");
        if (delta.isEmpty()) {
            return this;
        }
        Optional<DisguiseAppraisalSnapshot> updatedAppraisal = appraisal.map(current -> new DisguiseAppraisalSnapshot(
                    current.health() + delta.health(),
                    current.maxHealth() + delta.maxHealth(),
                    current.armorValue() + delta.armorValue(),
                    merge(current.tensuraVitals(), delta.tensuraVitals())
            ));
        java.util.LinkedHashMap<net.minecraft.resources.ResourceLocation, Double> attributes = new java.util.LinkedHashMap<>(attributeBaseValues);
        delta.attributeBaseValues().forEach((id, value) -> attributes.merge(id, value, Double::sum));
        return new ImitatorFormStats(updatedAppraisal, attributes);
    }

    public boolean isEmpty() {
        return appraisal.isEmpty() && attributeBaseValues.isEmpty();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        appraisal.ifPresent(snapshot -> tag.put(APPRAISAL, DisguiseAppraisalExtensions.create(snapshot).payload()));
        CompoundTag attributes = new CompoundTag();
        attributeBaseValues.forEach((id, value) -> attributes.putDouble(id.toString(), value));
        if (!attributes.isEmpty()) {
            tag.put(ATTRIBUTES, attributes);
        }
        return tag;
    }

    public static ImitatorFormStats fromTag(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.contains(APPRAISAL, Tag.TAG_COMPOUND)) {
            return tag.contains(ATTRIBUTES, Tag.TAG_COMPOUND) ? new ImitatorFormStats(Optional.empty(), attributes(tag.getCompound(ATTRIBUTES))) : EMPTY;
        }
        SnapshotExtension extension = new SnapshotExtension(DisguiseAppraisalExtensions.ID, DisguiseAppraisalExtensions.SCHEMA_VERSION, tag.getCompound(APPRAISAL));
        java.util.Map<net.minecraft.resources.ResourceLocation, Double> attributes = tag.contains(ATTRIBUTES, Tag.TAG_COMPOUND)
                ? attributes(tag.getCompound(ATTRIBUTES))
                : java.util.Map.of();
        return new ImitatorFormStats(DisguiseAppraisalExtensions.find(List.of(extension)), attributes);
    }

    public IdentitySnapshot mergeInto(IdentitySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (appraisal.isEmpty()) {
            return snapshot;
        }
        List<SnapshotExtension> extensions = new ArrayList<>();
        for (SnapshotExtension extension : snapshot.extensions()) {
            if (!extension.adapterId().equals(DisguiseAppraisalExtensions.ID) && !extension.adapterId().equals(TensuraStateExtensions.ID)) {
                extensions.add(extension);
            }
        }
        extensions.add(DisguiseAppraisalExtensions.create(appraisal.get()));
        Optional<TensuraStateSnapshot> tensuraState = TensuraStateExtensions.find(snapshot.extensions());
        if (tensuraState.isPresent() && appraisal.get().tensuraVitals().isPresent()) {
            TensuraStateSnapshot current = tensuraState.get();
            extensions.add(TensuraStateExtensions.create(new TensuraStateSnapshot(current.bridgeId(), current.schemaVersion(), appraisal.get().tensuraVitals().get(), current.sections())));
        } else {
            snapshot.extensions().stream()
                    .filter(extension -> extension.adapterId().equals(TensuraStateExtensions.ID))
                    .findFirst()
                    .ifPresent(extensions::add);
        }
        CompoundTag visualData = snapshot.visualData();
        if (!attributeBaseValues.isEmpty()) {
            CompoundTag attributeTag = visualData.contains(ATTRIBUTES, Tag.TAG_COMPOUND) ? visualData.getCompound(ATTRIBUTES).copy() : new CompoundTag();
            net.minecraft.nbt.ListTag values = new net.minecraft.nbt.ListTag();
            attributeBaseValues.forEach((id, value) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("id", id.toString());
                entry.putDouble("base", value);
                entry.putDouble("value", value);
                values.add(entry);
            });
            attributeTag.put("values", values);
            visualData.put(ATTRIBUTES, attributeTag);
            Double maxHealth = attributeBaseValues.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("generic.max_health"));
            if (maxHealth != null) {
                float previousMax = Math.max(1F, visualData.getFloat("max_health"));
                float previousHealth = Math.max(0F, visualData.getFloat("health"));
                float newMax = Math.max(1F, maxHealth.floatValue());
                visualData.putFloat("max_health", newMax);
                visualData.putFloat("health", Math.min(newMax, previousHealth + Math.max(0F, newMax - previousMax)));
            }
        }
        IdentitySnapshot.Builder builder = IdentitySnapshot.builder(snapshot.entityType(), snapshot.capturedGameTime())
                .snapshotId(snapshot.snapshotId())
                .schemaVersion(snapshot.schemaVersion())
                .displayName(snapshot.displayName())
                .entityData(snapshot.entityData())
                .visualData(visualData);
        extensions.forEach(builder::extension);
        return builder.build();
    }

    private static Optional<com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals> merge(
            Optional<com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals> current,
            Optional<com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals> delta
    ) {
        if (current.isEmpty()) {
            return delta;
        }
        if (delta.isEmpty()) {
            return current;
        }
        return Optional.of(current.get().plus(delta.get()));
    }

    private static java.util.Map<net.minecraft.resources.ResourceLocation, Double> attributes(CompoundTag source) {
        java.util.LinkedHashMap<net.minecraft.resources.ResourceLocation, Double> result = new java.util.LinkedHashMap<>();
        if (source.contains("values", Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag values = source.getList("values", Tag.TAG_COMPOUND);
            for (int index = 0; index < values.size(); index++) {
                CompoundTag entry = values.getCompound(index);
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(entry.getString("id"));
                if (id != null && entry.contains("base", Tag.TAG_DOUBLE)) {
                    result.put(id, entry.getDouble("base"));
                }
            }
            return java.util.Map.copyOf(result);
        }
        for (String key : source.getAllKeys()) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(key);
            if (id != null && source.contains(key, Tag.TAG_DOUBLE)) {
                result.put(id, source.getDouble(key));
            }
        }
        return java.util.Map.copyOf(result);
    }
}
