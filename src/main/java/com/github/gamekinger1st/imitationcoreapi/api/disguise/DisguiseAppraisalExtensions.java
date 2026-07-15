package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotExtension;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DisguiseAppraisalExtensions {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "disguise_appraisal");
    public static final int SCHEMA_VERSION = 1;
    private static final String HEALTH = "health";
    private static final String MAX_HEALTH = "max_health";
    private static final String ARMOR = "armor";
    private static final String TENSURA = "tensura";
    private static final String EP = "ep";
    private static final String MAGICULE = "magicule";
    private static final String AURA = "aura";
    private static final String SPIRITUAL_HEALTH = "spiritual_health";

    private DisguiseAppraisalExtensions() {
    }

    public static SnapshotExtension create(DisguiseAppraisalSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        CompoundTag tag = new CompoundTag();
        tag.putFloat(HEALTH, snapshot.health());
        tag.putFloat(MAX_HEALTH, snapshot.maxHealth());
        tag.putInt(ARMOR, snapshot.armorValue());
        snapshot.tensuraVitals().ifPresent(vitals -> {
            CompoundTag tensura = new CompoundTag();
            tensura.putDouble(EP, vitals.ep());
            tensura.putDouble(MAGICULE, vitals.magicule());
            tensura.putDouble(AURA, vitals.aura());
            tensura.putDouble(SPIRITUAL_HEALTH, vitals.spiritualHealth());
            tag.put(TENSURA, tensura);
        });
        return new SnapshotExtension(ID, SCHEMA_VERSION, tag);
    }

    public static Optional<DisguiseAppraisalSnapshot> find(List<SnapshotExtension> extensions) {
        Objects.requireNonNull(extensions, "extensions");
        return extensions.stream()
                .filter(extension -> extension.adapterId().equals(ID))
                .filter(extension -> extension.schemaVersion() == SCHEMA_VERSION)
                .findFirst()
                .flatMap(extension -> fromTag(extension.payload()));
    }

    private static Optional<DisguiseAppraisalSnapshot> fromTag(CompoundTag tag) {
        try {
            if (!tag.contains(HEALTH, Tag.TAG_FLOAT)
                    || !tag.contains(MAX_HEALTH, Tag.TAG_FLOAT)
                    || !tag.contains(ARMOR, Tag.TAG_INT)) {
                return Optional.empty();
            }
            Optional<TensuraVitals> vitals = Optional.empty();
            if (tag.contains(TENSURA, Tag.TAG_COMPOUND)) {
                CompoundTag tensura = tag.getCompound(TENSURA);
                if (!tensura.contains(EP, Tag.TAG_DOUBLE)
                        || !tensura.contains(MAGICULE, Tag.TAG_DOUBLE)
                        || !tensura.contains(AURA, Tag.TAG_DOUBLE)
                        || !tensura.contains(SPIRITUAL_HEALTH, Tag.TAG_DOUBLE)) {
                    return Optional.empty();
                }
                vitals = Optional.of(new TensuraVitals(
                        tensura.getDouble(EP),
                        tensura.getDouble(MAGICULE),
                        tensura.getDouble(AURA),
                        tensura.getDouble(SPIRITUAL_HEALTH)
                ));
            }
            return Optional.of(new DisguiseAppraisalSnapshot(tag.getFloat(HEALTH), tag.getFloat(MAX_HEALTH), tag.getInt(ARMOR), vitals));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
