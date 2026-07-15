package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotExtension;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ImitatorFormProgressionState(
        UUID snapshotId,
        Optional<DisguiseAppraisalSnapshot> lastObservedAppraisal,
        ImitatorFormStatDelta accumulatedDelta
) {
    private static final String SNAPSHOT_ID = "snapshot_id";
    private static final String LAST_APPRAISAL = "last_appraisal";
    private static final String ACCUMULATED_DELTA = "accumulated_delta";
    private static final String HEALTH = "health";
    private static final String MAX_HEALTH = "max_health";
    private static final String ARMOR = "armor";
    private static final String TENSURA = "tensura";
    private static final String EP = "ep";
    private static final String MAGICULE = "magicule";
    private static final String AURA = "aura";
    private static final String SPIRITUAL_HEALTH = "spiritual_health";

    public ImitatorFormProgressionState {
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(lastObservedAppraisal, "lastObservedAppraisal");
        Objects.requireNonNull(accumulatedDelta, "accumulatedDelta");
    }

    public static ImitatorFormProgressionState empty(UUID snapshotId) {
        return new ImitatorFormProgressionState(snapshotId, Optional.empty(), ImitatorFormStatDelta.EMPTY);
    }

    public ImitatorFormProgressionState observe(DisguiseAppraisalSnapshot current) {
        Objects.requireNonNull(current, "current");
        if (lastObservedAppraisal.isEmpty()) {
            return new ImitatorFormProgressionState(snapshotId, Optional.of(current), accumulatedDelta);
        }
        ImitatorFormStatDelta delta = ImitatorFormStatDelta.positiveBetween(lastObservedAppraisal.get(), current);
        DisguiseAppraisalSnapshot nextLast = ImitatorFormStats.fromAppraisal(lastObservedAppraisal.get()).apply(delta).appraisal().orElse(current);
        return new ImitatorFormProgressionState(snapshotId, Optional.of(nextLast), accumulatedDelta.plus(delta));
    }

    public ImitatorFormStatDelta lastDelta(DisguiseAppraisalSnapshot current) {
        Objects.requireNonNull(current, "current");
        return lastObservedAppraisal.map(previous -> ImitatorFormStatDelta.positiveBetween(previous, current)).orElse(ImitatorFormStatDelta.EMPTY);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(SNAPSHOT_ID, snapshotId);
        lastObservedAppraisal.ifPresent(appraisal -> tag.put(LAST_APPRAISAL, DisguiseAppraisalExtensions.create(appraisal).payload()));
        if (!accumulatedDelta.isEmpty()) {
            tag.put(ACCUMULATED_DELTA, deltaToTag(accumulatedDelta));
        }
        return tag;
    }

    public static ImitatorFormProgressionState fromTag(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.hasUUID(SNAPSHOT_ID)) {
            throw new IllegalArgumentException("Missing form progression snapshot id");
        }
        Optional<DisguiseAppraisalSnapshot> last = tag.contains(LAST_APPRAISAL, Tag.TAG_COMPOUND)
                ? appraisalFromTag(tag.getCompound(LAST_APPRAISAL))
                : Optional.empty();
        ImitatorFormStatDelta delta = tag.contains(ACCUMULATED_DELTA, Tag.TAG_COMPOUND)
                ? deltaFromTag(tag.getCompound(ACCUMULATED_DELTA))
                : ImitatorFormStatDelta.EMPTY;
        return new ImitatorFormProgressionState(tag.getUUID(SNAPSHOT_ID), last, delta);
    }

    private static Optional<DisguiseAppraisalSnapshot> appraisalFromTag(CompoundTag tag) {
        return DisguiseAppraisalExtensions.find(List.of(new SnapshotExtension(DisguiseAppraisalExtensions.ID, DisguiseAppraisalExtensions.SCHEMA_VERSION, tag)));
    }

    private static CompoundTag deltaToTag(ImitatorFormStatDelta delta) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(HEALTH, delta.health());
        tag.putFloat(MAX_HEALTH, delta.maxHealth());
        tag.putInt(ARMOR, delta.armorValue());
        delta.tensuraVitals().ifPresent(vitals -> {
            CompoundTag tensura = new CompoundTag();
            tensura.putDouble(EP, vitals.ep());
            tensura.putDouble(MAGICULE, vitals.magicule());
            tensura.putDouble(AURA, vitals.aura());
            tensura.putDouble(SPIRITUAL_HEALTH, vitals.spiritualHealth());
            tag.put(TENSURA, tensura);
        });
        return tag;
    }

    private static ImitatorFormStatDelta deltaFromTag(CompoundTag tag) {
        Optional<com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals> vitals = Optional.empty();
        if (tag.contains(TENSURA, Tag.TAG_COMPOUND)) {
            CompoundTag tensura = tag.getCompound(TENSURA);
            vitals = Optional.of(new com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals(
                    tensura.getDouble(EP),
                    tensura.getDouble(MAGICULE),
                    tensura.getDouble(AURA),
                    tensura.getDouble(SPIRITUAL_HEALTH)
            ));
        }
        return new ImitatorFormStatDelta(
                tag.contains(HEALTH, Tag.TAG_FLOAT) ? tag.getFloat(HEALTH) : 0F,
                tag.contains(MAX_HEALTH, Tag.TAG_FLOAT) ? tag.getFloat(MAX_HEALTH) : 0F,
                tag.contains(ARMOR, Tag.TAG_INT) ? tag.getInt(ARMOR) : 0,
                vitals
        );
    }
}
