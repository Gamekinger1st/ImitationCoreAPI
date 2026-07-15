package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class PersistentImitatorFormLibrary implements ImitatorFormLibrary {
    private final UUID ownerId;
    private final ImitatorFormRepository repository;
    private final ImitatorFormLibraryLimits limits;

    public PersistentImitatorFormLibrary(UUID ownerId, ImitatorFormRepository repository, ImitatorFormLibraryLimits limits) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    @Override
    public int slotCapacity() {
        return limits.slotCapacity();
    }

    @Override
    public List<ImitatorFormSlot> occupiedSlots() {
        return state().forms().entrySet().stream()
                .map(entry -> new ImitatorFormSlot(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(ImitatorFormSlot::index))
                .toList();
    }

    @Override
    public List<ImitatorForm> forms() {
        return occupiedSlots().stream().map(ImitatorFormSlot::form).toList();
    }

    @Override
    public Optional<ImitatorForm> form(int slot) {
        validateSlot(slot);
        return Optional.ofNullable(state().forms().get(slot));
    }

    @Override
    public Optional<ImitatorForm> setForm(int slot, ImitatorForm form) {
        validateSlot(slot);
        Objects.requireNonNull(form, "form");
        ImitatorFormLibraryState state = state();
        state.forms().entrySet().stream()
                .filter(entry -> entry.getValue().snapshotId().equals(form.snapshotId()) && entry.getKey() != slot)
                .findFirst()
                .ifPresent(entry -> {
                    throw new IllegalArgumentException("The form is already stored in slot " + displaySlot(entry.getKey()));
                });
        Optional<ImitatorForm> previous = Optional.ofNullable(state.forms().get(slot));
        save(state.withForm(slot, form));
        return previous;
    }

    @Override
    public Optional<ImitatorForm> clearForm(int slot) {
        validateSlot(slot);
        ImitatorFormLibraryState state = state();
        Optional<ImitatorForm> previous = Optional.ofNullable(state.forms().get(slot));
        if (previous.isPresent()) {
            save(state.withoutForm(slot));
        }
        return previous;
    }

    @Override
    public OptionalInt selectedSlot() {
        return state().selectedSlot();
    }

    @Override
    public Optional<ImitatorForm> selectedForm() {
        ImitatorFormLibraryState state = state();
        return state.selectedSlot().isPresent() ? Optional.ofNullable(state.forms().get(state.selectedSlot().getAsInt())) : Optional.empty();
    }

    @Override
    public boolean selectForm(int slot) {
        validateSlot(slot);
        ImitatorFormLibraryState state = state();
        if (!state.forms().containsKey(slot)) {
            return false;
        }
        save(state.withSelectedSlot(slot));
        return true;
    }

    @Override
    public void clearSelectedForm() {
        ImitatorFormLibraryState state = state();
        if (state.selectedSlot().isPresent()) {
            save(state.withoutSelectedSlot());
        }
    }

    @Override
    public Optional<ImitatorPendingRecord> pendingRecord() {
        return state().pendingRecord();
    }

    @Override
    public void setPendingRecord(ImitatorPendingRecord pendingRecord) {
        Objects.requireNonNull(pendingRecord, "pendingRecord");
        if (pendingRecord.expiresGameTime() - pendingRecord.createdGameTime() > limits.maxPendingDurationTicks()) {
            throw new IllegalArgumentException("The pending record duration exceeds the configured limit");
        }
        save(state().withPendingRecord(pendingRecord));
    }

    @Override
    public Optional<ImitatorPendingRecord> clearPendingRecord() {
        ImitatorFormLibraryState state = state();
        Optional<ImitatorPendingRecord> previous = state.pendingRecord();
        if (previous.isPresent()) {
            save(state.withoutPendingRecord());
        }
        return previous;
    }

    @Override
    public boolean expirePendingRecord(long gameTime) {
        Optional<ImitatorPendingRecord> pending = pendingRecord();
        if (pending.isEmpty() || !pending.get().isExpired(gameTime)) {
            return false;
        }
        clearPendingRecord();
        return true;
    }

    @Override
    public List<UUID> seenSnapshotIds() {
        return state().seenSnapshotIds();
    }

    @Override
    public boolean rememberSeenSnapshot(UUID snapshotId) {
        Objects.requireNonNull(snapshotId, "snapshotId");
        ImitatorFormLibraryState state = state();
        boolean changed = !state.seenSnapshotIds().contains(snapshotId) || !state.seenSnapshotIds().getLast().equals(snapshotId);
        if (changed) {
            save(state.withSeenSnapshot(snapshotId, limits.maxSeenForms()));
        }
        return changed;
    }

    @Override
    public void clearSeenSnapshots() {
        ImitatorFormLibraryState state = state();
        if (!state.seenSnapshotIds().isEmpty()) {
            save(state.withoutSeenSnapshots());
        }
    }

    @Override
    public ImitatorSkillMode skillMode() {
        return state().skillMode();
    }

    @Override
    public ImitatorSkillMode cycleSkillMode() {
        ImitatorFormLibraryState state = state();
        ImitatorSkillMode mode = state.skillMode().next();
        save(state.withSkillMode(mode));
        return mode;
    }

    @Override
    public void setSkillMode(ImitatorSkillMode mode) {
        ImitatorFormLibraryState state = state();
        if (state.skillMode() != mode) {
            save(state.withSkillMode(mode));
        }
    }

    @Override
    public boolean mirrorSyncEnabled() {
        return state().mirrorSyncEnabled();
    }

    @Override
    public void setMirrorSyncEnabled(boolean enabled) {
        ImitatorFormLibraryState state = state();
        if (state.mirrorSyncEnabled() != enabled) {
            save(state.withMirrorSyncEnabled(enabled));
        }
    }

    private ImitatorFormLibraryState state() {
        return repository.formLibrary(ownerId);
    }

    private void save(ImitatorFormLibraryState state) {
        repository.saveFormLibrary(ownerId, state);
    }

    private void validateSlot(int slot) {
        if (slot < 0 || slot >= limits.slotCapacity()) {
            throw new IllegalArgumentException("Form slot must be between 1 and " + limits.slotCapacity());
        }
    }

    private static int displaySlot(int slot) {
        return slot + 1;
    }
}
