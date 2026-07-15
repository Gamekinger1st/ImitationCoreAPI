package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public interface ImitatorFormLibrary {
    int slotCapacity();

    List<ImitatorFormSlot> occupiedSlots();

    List<ImitatorForm> forms();

    Optional<ImitatorForm> form(int slot);

    Optional<ImitatorForm> setForm(int slot, ImitatorForm form);

    Optional<ImitatorForm> clearForm(int slot);

    OptionalInt selectedSlot();

    Optional<ImitatorForm> selectedForm();

    boolean selectForm(int slot);

    void clearSelectedForm();

    Optional<ImitatorPendingRecord> pendingRecord();

    void setPendingRecord(ImitatorPendingRecord pendingRecord);

    Optional<ImitatorPendingRecord> clearPendingRecord();

    boolean expirePendingRecord(long gameTime);

    List<UUID> seenSnapshotIds();

    boolean rememberSeenSnapshot(UUID snapshotId);

    void clearSeenSnapshots();

    ImitatorSkillMode skillMode();

    ImitatorSkillMode cycleSkillMode();

    void setSkillMode(ImitatorSkillMode mode);

    boolean mirrorSyncEnabled();

    void setMirrorSyncEnabled(boolean enabled);
}
