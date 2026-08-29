package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateStatus;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class ImitatorFormProgressionService {
    public static final ResourceLocation HANDLER_ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "imitator_form_progression");
    private final TransformationService transformations;
    private final ImitatorFormService forms;

    public ImitatorFormProgressionService(TransformationService transformations, ImitatorFormService forms) {
        this.transformations = Objects.requireNonNull(transformations, "transformations");
        this.forms = Objects.requireNonNull(forms, "forms");
    }

    public Optional<ImitatorFormStatDelta> reconcile(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        Optional<TransformationSession> session = transformations.activeSessionForOwner(player.getUUID())
                .filter(value -> value.scope().changesOwnerPresentation());
        if (session.isEmpty()) {
            return Optional.empty();
        }
        Optional<IdentitySnapshot> snapshot = transformations.snapshot(session.get().snapshotId());
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }
        DisguiseAppraisalSnapshot current = currentAppraisal(player);
        java.util.Map<ResourceLocation, Double> currentAttributes = currentAttributes(player);
        TemporaryStateReference reference = progressionReference(session.get())
                .orElseGet(() -> createProgressionReference(session.get(), player.level().getGameTime()).orElse(null));
        if (reference == null) {
            return Optional.empty();
        }
        ImitatorFormProgressionState state = ImitatorFormProgressionState.fromTag(reference.payload());
        ImitatorFormStatDelta delta = state.lastDelta(current, currentAttributes);
        ImitatorFormProgressionState updated = state.observe(current, currentAttributes);
        SessionTransitionResult updatedState = transformations.updateTemporaryStatePayload(session.get().sessionId(), reference.referenceId(), updated.toTag(), player.level().getGameTime());
        if (!updatedState.accepted()) {
            return Optional.empty();
        }
        if (delta.isEmpty()) {
            return Optional.empty();
        }
        applyDeltaToForm(player.getUUID(), snapshot.get(), delta);
        return Optional.of(delta);
    }

    public Optional<ImitatorFormStatDelta> applyDelta(UUID ownerId, UUID snapshotId, ImitatorFormStatDelta delta) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(delta, "delta");
        if (delta.isEmpty()) {
            return Optional.empty();
        }
        return transformations.snapshot(snapshotId)
                .map(snapshot -> {
                    applyDeltaToForm(ownerId, snapshot, delta);
                    return delta;
                });
    }

    private void applyDeltaToForm(UUID ownerId, IdentitySnapshot snapshot, ImitatorFormStatDelta delta) {
        ImitatorFormLibrary library = forms.library(ownerId);
        OptionalInt slot = selectedMatchingSlot(library, snapshot.snapshotId());
        if (slot.isEmpty()) {
            return;
        }
        ImitatorForm form = library.form(slot.getAsInt()).orElseThrow();
        ImitatorFormStats baseStats = form.stats().isEmpty() ? ImitatorFormStats.fromSnapshot(snapshot) : form.stats();
        ImitatorFormStats updatedStats = baseStats.apply(delta);
        if (updatedStats.equals(form.stats())) {
            return;
        }
        library.setForm(slot.getAsInt(), form.withStats(updatedStats));
        transformations.storeSnapshot(updatedStats.mergeInto(snapshot));
    }

    private OptionalInt selectedMatchingSlot(ImitatorFormLibrary library, UUID snapshotId) {
        if (library.selectedSlot().isPresent()) {
            int selected = library.selectedSlot().getAsInt();
            if (library.form(selected).map(form -> form.snapshotId().equals(snapshotId)).orElse(false)) {
                return OptionalInt.of(selected);
            }
        }
        return library.occupiedSlots().stream()
                .filter(slot -> slot.form().snapshotId().equals(snapshotId))
                .mapToInt(ImitatorFormSlot::index)
                .findFirst();
    }

    private Optional<TemporaryStateReference> progressionReference(TransformationSession session) {
        return session.temporaryState().stream()
                .filter(reference -> reference.handlerId().equals(HANDLER_ID))
                .filter(reference -> reference.kind().equals(TemporaryStateKinds.FORM_PROGRESSION))
                .findFirst();
    }

    private Optional<TemporaryStateReference> createProgressionReference(TransformationSession session, long gameTime) {
        CompoundTag payload = ImitatorFormProgressionState.empty(session.snapshotId()).toTag();
        TemporaryStateReference reference = new TemporaryStateReference(
                UUID.randomUUID(),
                session.sessionId(),
                HANDLER_ID,
                TemporaryStateKinds.FORM_PROGRESSION,
                payload,
                TemporaryStateStatus.ACTIVE
        );
        SessionTransitionResult added = transformations.addTemporaryState(session.sessionId(), reference, gameTime);
        return added.accepted() ? Optional.of(reference) : Optional.empty();
    }

    private DisguiseAppraisalSnapshot currentAppraisal(ServerPlayer player) {
        return new DisguiseAppraisalSnapshot(
                player.getHealth(),
                player.getMaxHealth(),
                player.getArmorValue(),
                ImitationApi.tensuraStates().capture(player).map(state -> state.vitals())
        );
    }

    private java.util.Map<ResourceLocation, Double> currentAttributes(ServerPlayer player) {
        java.util.LinkedHashMap<ResourceLocation, Double> attributes = new java.util.LinkedHashMap<>();
        net.minecraft.nbt.ListTag values = player.getAttributes().save();
        for (int index = 0; index < values.size(); index++) {
            CompoundTag value = values.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(value.getString("id"));
            if (id != null && value.contains("base", net.minecraft.nbt.Tag.TAG_DOUBLE)) {
                attributes.put(id, value.getDouble("base"));
            }
        }
        return java.util.Map.copyOf(attributes);
    }
}
