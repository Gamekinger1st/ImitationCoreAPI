package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorForm;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormLibrary;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormSlot;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorPendingRecord;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorProgressionPolicy;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorSkillMode;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;

public record ImitatorFormLibraryPayload(
        int slotCapacity,
        List<FormSlot> forms,
        OptionalInt selectedSlot,
        Optional<PendingRecord> pendingRecord,
        ImitatorSkillMode skillMode,
        boolean mirrorSyncEnabled
) implements CustomPacketPayload {
    public static final Type<ImitatorFormLibraryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "imitator_form_library"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImitatorFormLibraryPayload> STREAM_CODEC = StreamCodec.of(
            ImitatorFormLibraryPayload::encode,
            ImitatorFormLibraryPayload::decode
    );

    public ImitatorFormLibraryPayload {
        if (slotCapacity < 1 || slotCapacity > 256) {
            throw new IllegalArgumentException("slotCapacity must be between 1 and 256");
        }
        Objects.requireNonNull(forms, "forms");
        Objects.requireNonNull(selectedSlot, "selectedSlot");
        Objects.requireNonNull(pendingRecord, "pendingRecord");
        Objects.requireNonNull(skillMode, "skillMode");
        if (forms.size() > slotCapacity) {
            throw new IllegalArgumentException("Form count exceeds slot capacity");
        }
        forms = forms.stream().sorted(Comparator.comparingInt(FormSlot::slot)).toList();
        for (int index = 0; index < forms.size(); index++) {
            FormSlot form = forms.get(index);
            if (form.slot() >= slotCapacity || index > 0 && forms.get(index - 1).slot() == form.slot()) {
                throw new IllegalArgumentException("Invalid or duplicate form slot");
            }
        }
        if (selectedSlot.isPresent() && forms.stream().noneMatch(form -> form.slot() == selectedSlot.getAsInt())) {
            throw new IllegalArgumentException("The selected slot must contain a form");
        }
    }

    public static ImitatorFormLibraryPayload from(ImitatorFormLibrary library) {
        return from(library, ignored -> Optional.empty());
    }

    public static ImitatorFormLibraryPayload from(ImitatorFormLibrary library, Function<UUID, Optional<IdentitySnapshot>> snapshots) {
        Objects.requireNonNull(library, "library");
        Objects.requireNonNull(snapshots, "snapshots");
        List<FormSlot> forms = library.occupiedSlots().stream().map(slot -> FormSlot.from(slot, snapshots)).toList();
        Optional<PendingRecord> pending = library.pendingRecord().map(PendingRecord::from);
        return new ImitatorFormLibraryPayload(library.slotCapacity(), forms, library.selectedSlot(), pending, library.skillMode(), library.mirrorSyncEnabled());
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ImitatorFormLibraryPayload payload) {
        buffer.writeVarInt(payload.slotCapacity);
        buffer.writeVarInt(payload.forms.size());
        for (FormSlot form : payload.forms) {
            buffer.writeVarInt(form.slot);
            buffer.writeUUID(form.snapshotId);
            buffer.writeDouble(form.precision);
            buffer.writeBoolean(form.perfect);
            buffer.writeBoolean(form.mirrorSyncAllowed);
            buffer.writeBoolean(form.skillCopyAllowed);
            buffer.writeUtf(form.displayName, 256);
        }
        buffer.writeBoolean(payload.selectedSlot.isPresent());
        if (payload.selectedSlot.isPresent()) {
            buffer.writeVarInt(payload.selectedSlot.getAsInt());
        }
        buffer.writeBoolean(payload.pendingRecord.isPresent());
        if (payload.pendingRecord.isPresent()) {
            PendingRecord pending = payload.pendingRecord.get();
            buffer.writeUUID(pending.snapshotId);
            buffer.writeVarLong(pending.createdGameTime);
            buffer.writeVarLong(pending.expiresGameTime);
            buffer.writeDouble(pending.precision);
            buffer.writeBoolean(pending.mirrorSyncAllowed);
            buffer.writeBoolean(pending.skillCopyAllowed);
        }
        buffer.writeVarInt(payload.skillMode.ordinal());
        buffer.writeBoolean(payload.mirrorSyncEnabled);
    }

    private static ImitatorFormLibraryPayload decode(RegistryFriendlyByteBuf buffer) {
        int slotCapacity = buffer.readVarInt();
        if (slotCapacity < 1 || slotCapacity > 256) {
            throw new IllegalArgumentException("Invalid form-library slot capacity");
        }
        int count = buffer.readVarInt();
        if (count < 0 || count > slotCapacity) {
            throw new IllegalArgumentException("Invalid form-library form count");
        }
        java.util.ArrayList<FormSlot> forms = new java.util.ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            forms.add(new FormSlot(buffer.readVarInt(), buffer.readUUID(), buffer.readDouble(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readUtf(256)));
        }
        OptionalInt selected = buffer.readBoolean() ? OptionalInt.of(buffer.readVarInt()) : OptionalInt.empty();
        Optional<PendingRecord> pending = buffer.readBoolean()
                ? Optional.of(new PendingRecord(buffer.readUUID(), buffer.readVarLong(), buffer.readVarLong(), buffer.readDouble(), buffer.readBoolean(), buffer.readBoolean()))
                : Optional.empty();
        int modeIndex = buffer.readVarInt();
        ImitatorSkillMode[] modes = ImitatorSkillMode.values();
        if (modeIndex < 0 || modeIndex >= modes.length) {
            throw new IllegalArgumentException("Invalid Imitator skill mode");
        }
        return new ImitatorFormLibraryPayload(slotCapacity, forms, selected, pending, modes[modeIndex], buffer.readBoolean());
    }

    @Override
    public Type<ImitatorFormLibraryPayload> type() {
        return TYPE;
    }

    public record FormSlot(int slot, UUID snapshotId, double precision, boolean perfect, boolean mirrorSyncAllowed, boolean skillCopyAllowed, String displayName) {
        public FormSlot(int slot, UUID snapshotId, double precision, boolean perfect, boolean mirrorSyncAllowed) {
            this(slot, snapshotId, precision, perfect, mirrorSyncAllowed, false, "");
        }

        public FormSlot(int slot, UUID snapshotId, double precision, boolean perfect, boolean mirrorSyncAllowed, String displayName) {
            this(slot, snapshotId, precision, perfect, mirrorSyncAllowed, false, displayName);
        }

        public FormSlot {
            if (slot < 0) {
                throw new IllegalArgumentException("slot cannot be negative");
            }
            Objects.requireNonNull(snapshotId, "snapshotId");
            if (!Double.isFinite(precision) || precision < 0D || precision > 1D) {
                throw new IllegalArgumentException("precision must be between zero and one");
            }
            Objects.requireNonNull(displayName, "displayName");
            displayName = displayName.strip();
            if (displayName.length() > 256) {
                throw new IllegalArgumentException("displayName exceeds the configured limit");
            }
        }

        private static FormSlot from(ImitatorFormSlot slot, Function<UUID, Optional<IdentitySnapshot>> snapshots) {
            ImitatorForm form = slot.form();
            String displayName = snapshots.apply(form.snapshotId())
                    .map(IdentitySnapshot::displayName)
                    .filter(name -> !name.isBlank())
                    .orElse("");
            return new FormSlot(slot.index(), form.snapshotId(), form.precision(), form.perfect(), form.mirrorSyncAllowed(), form.skillCopyAllowed(), displayName);
        }
    }

    public record PendingRecord(UUID snapshotId, long createdGameTime, long expiresGameTime, double precision, boolean mirrorSyncAllowed, boolean skillCopyAllowed) {
        public PendingRecord(UUID snapshotId, long createdGameTime, long expiresGameTime) {
            this(snapshotId, createdGameTime, expiresGameTime, ImitatorProgressionPolicy.DEFAULT.minimumPrecision(), false, false);
        }

        public PendingRecord(UUID snapshotId, long createdGameTime, long expiresGameTime, double precision, boolean mirrorSyncAllowed) {
            this(snapshotId, createdGameTime, expiresGameTime, precision, mirrorSyncAllowed, false);
        }

        public PendingRecord {
            Objects.requireNonNull(snapshotId, "snapshotId");
            if (createdGameTime < 0 || expiresGameTime <= createdGameTime) {
                throw new IllegalArgumentException("Invalid pending-record timing");
            }
            if (!Double.isFinite(precision) || precision < 0D || precision > 1D) {
                throw new IllegalArgumentException("Pending-record precision must be between zero and one");
            }
        }

        private static PendingRecord from(ImitatorPendingRecord record) {
            return new PendingRecord(record.snapshotId(), record.createdGameTime(), record.expiresGameTime(), record.precision(), record.mirrorSyncAllowed(), record.skillCopyAllowed());
        }
    }
}
