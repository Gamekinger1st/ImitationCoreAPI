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

public record ImitatorFormStats(Optional<DisguiseAppraisalSnapshot> appraisal) {
    public static final ImitatorFormStats EMPTY = new ImitatorFormStats(Optional.empty());
    private static final String APPRAISAL = "appraisal";

    public ImitatorFormStats {
        Objects.requireNonNull(appraisal, "appraisal");
    }

    public static ImitatorFormStats empty() {
        return EMPTY;
    }

    public static ImitatorFormStats fromSnapshot(IdentitySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new ImitatorFormStats(DisguiseAppraisalExtensions.find(snapshot.extensions()));
    }

    public static ImitatorFormStats fromAppraisal(DisguiseAppraisalSnapshot appraisal) {
        return new ImitatorFormStats(Optional.of(Objects.requireNonNull(appraisal, "appraisal")));
    }

    public ImitatorFormStats apply(ImitatorFormStatDelta delta) {
        Objects.requireNonNull(delta, "delta");
        if (delta.isEmpty() || appraisal.isEmpty()) {
            return this;
        }
        DisguiseAppraisalSnapshot current = appraisal.get();
        return new ImitatorFormStats(Optional.of(new DisguiseAppraisalSnapshot(
                current.health() + delta.health(),
                current.maxHealth() + delta.maxHealth(),
                current.armorValue() + delta.armorValue(),
                merge(current.tensuraVitals(), delta.tensuraVitals())
        )));
    }

    public boolean isEmpty() {
        return appraisal.isEmpty();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        appraisal.ifPresent(snapshot -> tag.put(APPRAISAL, DisguiseAppraisalExtensions.create(snapshot).payload()));
        return tag;
    }

    public static ImitatorFormStats fromTag(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.contains(APPRAISAL, Tag.TAG_COMPOUND)) {
            return EMPTY;
        }
        SnapshotExtension extension = new SnapshotExtension(DisguiseAppraisalExtensions.ID, DisguiseAppraisalExtensions.SCHEMA_VERSION, tag.getCompound(APPRAISAL));
        return new ImitatorFormStats(DisguiseAppraisalExtensions.find(List.of(extension)));
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
        IdentitySnapshot.Builder builder = IdentitySnapshot.builder(snapshot.entityType(), snapshot.capturedGameTime())
                .snapshotId(snapshot.snapshotId())
                .schemaVersion(snapshot.schemaVersion())
                .displayName(snapshot.displayName())
                .entityData(snapshot.entityData())
                .visualData(snapshot.visualData());
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
}
