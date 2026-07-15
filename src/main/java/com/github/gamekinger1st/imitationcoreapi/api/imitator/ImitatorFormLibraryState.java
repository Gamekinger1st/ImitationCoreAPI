package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public record ImitatorFormLibraryState(
        int schemaVersion,
        Map<Integer, ImitatorForm> forms,
        OptionalInt selectedSlot,
        Optional<ImitatorPendingRecord> pendingRecord,
        List<UUID> seenSnapshotIds,
        ImitatorSkillMode skillMode,
        boolean mirrorSyncEnabled
) {
    public static final int CURRENT_SCHEMA_VERSION = 5;

    public ImitatorFormLibraryState {
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported form library schema version: " + schemaVersion);
        }
        Objects.requireNonNull(forms, "forms");
        Objects.requireNonNull(selectedSlot, "selectedSlot");
        Objects.requireNonNull(pendingRecord, "pendingRecord");
        Objects.requireNonNull(seenSnapshotIds, "seenSnapshotIds");
        Objects.requireNonNull(skillMode, "skillMode");
        Map<Integer, ImitatorForm> copiedForms = new LinkedHashMap<>();
        for (Map.Entry<Integer, ImitatorForm> entry : forms.entrySet()) {
            Integer slot = Objects.requireNonNull(entry.getKey(), "form slot");
            if (slot < 0 || slot > 255) {
                throw new IllegalArgumentException("Form slot must be between 0 and 255");
            }
            copiedForms.put(slot, Objects.requireNonNull(entry.getValue(), "form"));
        }
        if (selectedSlot.isPresent() && !copiedForms.containsKey(selectedSlot.getAsInt())) {
            throw new IllegalArgumentException("The selected form slot must contain a form");
        }
        List<UUID> copiedSeen = new ArrayList<>();
        for (UUID snapshotId : seenSnapshotIds) {
            UUID value = Objects.requireNonNull(snapshotId, "seenSnapshotId");
            if (!copiedSeen.contains(value)) {
                copiedSeen.add(value);
            }
        }
        if (copiedSeen.size() > 4_096) {
            throw new IllegalArgumentException("Too many seen forms");
        }
        forms = Map.copyOf(copiedForms);
        pendingRecord = pendingRecord.map(value -> value);
        seenSnapshotIds = List.copyOf(copiedSeen);
    }

    public static ImitatorFormLibraryState empty() {
        return new ImitatorFormLibraryState(CURRENT_SCHEMA_VERSION, Map.of(), OptionalInt.empty(), Optional.empty(), List.of(), ImitatorSkillMode.RECORD, false);
    }

    public ImitatorFormLibraryState withForm(int slot, ImitatorForm form) {
        Map<Integer, ImitatorForm> updated = new LinkedHashMap<>(forms);
        updated.put(slot, Objects.requireNonNull(form, "form"));
        return new ImitatorFormLibraryState(schemaVersion, updated, selectedSlot, pendingRecord, seenSnapshotIds, skillMode, mirrorSyncEnabled);
    }

    public ImitatorFormLibraryState withoutForm(int slot) {
        if (!forms.containsKey(slot)) {
            return this;
        }
        Map<Integer, ImitatorForm> updated = new LinkedHashMap<>(forms);
        updated.remove(slot);
        OptionalInt selected = selectedSlot.isPresent() && selectedSlot.getAsInt() == slot ? OptionalInt.empty() : selectedSlot;
        return new ImitatorFormLibraryState(schemaVersion, updated, selected, pendingRecord, seenSnapshotIds, skillMode, mirrorSyncEnabled);
    }

    public ImitatorFormLibraryState withSelectedSlot(int slot) {
        return new ImitatorFormLibraryState(schemaVersion, forms, OptionalInt.of(slot), pendingRecord, seenSnapshotIds, skillMode, mirrorSyncEnabled);
    }

    public ImitatorFormLibraryState withoutSelectedSlot() {
        return new ImitatorFormLibraryState(schemaVersion, forms, OptionalInt.empty(), pendingRecord, seenSnapshotIds, skillMode, mirrorSyncEnabled);
    }

    public ImitatorFormLibraryState withPendingRecord(ImitatorPendingRecord pending) {
        return new ImitatorFormLibraryState(schemaVersion, forms, selectedSlot, Optional.of(Objects.requireNonNull(pending, "pending")), seenSnapshotIds, skillMode, mirrorSyncEnabled);
    }

    public ImitatorFormLibraryState withoutPendingRecord() {
        return new ImitatorFormLibraryState(schemaVersion, forms, selectedSlot, Optional.empty(), seenSnapshotIds, skillMode, mirrorSyncEnabled);
    }

    public ImitatorFormLibraryState withSeenSnapshot(UUID snapshotId, int maxSeenForms) {
        Objects.requireNonNull(snapshotId, "snapshotId");
        List<UUID> updated = new ArrayList<>(seenSnapshotIds);
        updated.remove(snapshotId);
        updated.add(snapshotId);
        while (updated.size() > maxSeenForms) {
            updated.removeFirst();
        }
        return new ImitatorFormLibraryState(schemaVersion, forms, selectedSlot, pendingRecord, updated, skillMode, mirrorSyncEnabled);
    }

    public ImitatorFormLibraryState withoutSeenSnapshots() {
        return new ImitatorFormLibraryState(schemaVersion, forms, selectedSlot, pendingRecord, List.of(), skillMode, mirrorSyncEnabled);
    }

    public ImitatorFormLibraryState withSkillMode(ImitatorSkillMode mode) {
        return new ImitatorFormLibraryState(schemaVersion, forms, selectedSlot, pendingRecord, seenSnapshotIds, Objects.requireNonNull(mode, "mode"), mirrorSyncEnabled);
    }

    public ImitatorFormLibraryState withMirrorSyncEnabled(boolean enabled) {
        return new ImitatorFormLibraryState(schemaVersion, forms, selectedSlot, pendingRecord, seenSnapshotIds, skillMode, enabled);
    }
}
