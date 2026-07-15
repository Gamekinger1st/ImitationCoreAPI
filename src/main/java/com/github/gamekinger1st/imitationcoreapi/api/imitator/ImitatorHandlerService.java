package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.adapter.AdapterKind;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationService;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityLevel;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnostics;
import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationLifecycleReason;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationState;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.BaselineSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotExtension;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotCaptureResult;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotCaptureService;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotLimits;
import com.github.gamekinger1st.imitationcoreapi.api.skill.TemporarySkillGrantResult;
import com.github.gamekinger1st.imitationcoreapi.api.skill.OwnerSkillSuppressionService;
import com.github.gamekinger1st.imitationcoreapi.api.skill.TemporarySkillService;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraCopyPolicy;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.targeting.MobImitationTargetingService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ImitatorHandlerService {
    private static final ResourceLocation PLAYER_ENTITY_TYPE = ResourceLocation.withDefaultNamespace("player");
    private final TransformationService transformations;
    private final SnapshotCaptureService captures;
    private final ImitatorFormService forms;
    private final ImitatorActionPolicy actionPolicy;
    private final TransformationApplicationService applications;

    public ImitatorHandlerService(TransformationService transformations, ImitatorFormService forms, TransformationApplicationService applications) {
        this(transformations, forms, applications, ImitatorActionPolicy.DEFAULT);
    }

    public ImitatorHandlerService(TransformationService transformations, ImitatorFormService forms, TransformationApplicationService applications, ImitatorActionPolicy actionPolicy) {
        this.transformations = Objects.requireNonNull(transformations, "transformations");
        this.captures = new SnapshotCaptureService(ImitationApi.adapters(), SnapshotLimits.DEFAULT);
        this.forms = Objects.requireNonNull(forms, "forms");
        this.applications = Objects.requireNonNull(applications, "applications");
        this.actionPolicy = Objects.requireNonNull(actionPolicy, "actionPolicy");
    }

    public SnapshotCaptureResult record(ServerPlayer requester, Entity target) {
        Objects.requireNonNull(requester, "requester");
        Objects.requireNonNull(target, "target");
        SnapshotCaptureResult result = captures.capture(target, Optional.of(requester.getUUID()), requester.level().getGameTime());
        transformations.storeSnapshot(result.snapshot());
        return result;
    }

    public ImitatorFormLibrary formsFor(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return forms.library(player.getUUID());
    }

    public ImitatorActionResult stageRecord(ServerPlayer requester, Entity target) {
        return stageRecord(requester, target, ImitatorProgressionPolicy.DEFAULT, recordingContext(requester, target, false));
    }

    public ImitatorActionResult stageRecord(ServerPlayer requester, Entity target, ImitatorProgressionPolicy progressionPolicy, ImitatorRecordingContext recordingContext) {
        Objects.requireNonNull(requester, "requester");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(progressionPolicy, "progressionPolicy");
        Objects.requireNonNull(recordingContext, "recordingContext");
        ImitatorActionResult validation = validateRecord(requester, target);
        if (!validation.accepted()) {
            ImitationDiagnostics.rejected(requester, validation.message());
            return validation;
        }
        try {
            SnapshotCaptureResult captured = record(requester, target);
            ImitatorForm form = progressionPolicy.initialForm(captured.snapshot().snapshotId(), recordingContext);
            ImitatorPendingRecord pending = forms.stagePendingRecord(requester.getUUID(), captured.snapshot().snapshotId(), requester.level().getGameTime(), form);
            return ImitatorActionResult.recorded(pending);
        } catch (RuntimeException exception) {
            return rejectedAction(requester, "The target could not be recorded: " + message(exception));
        }
    }

    public ImitatorActionResult commitPendingRecord(ServerPlayer player, int slot) {
        Objects.requireNonNull(player, "player");
        ImitatorFormLibrary library = formsFor(player);
        library.expirePendingRecord(player.level().getGameTime());
        Optional<ImitatorPendingRecord> pending = library.pendingRecord();
        if (pending.isEmpty()) {
            return rejectedAction(player, "There is no pending record to store");
        }
        if (transformations.snapshot(pending.get().snapshotId()).isEmpty()) {
            library.clearPendingRecord();
            return rejectedAction(player, "The pending record is no longer available");
        }
        ImitatorForm form = new ImitatorForm(pending.get().snapshotId(), pending.get().precision(), false, pending.get().mirrorSyncAllowed(), pending.get().skillCopyAllowed());
        try {
            library.setForm(slot, form);
            library.clearPendingRecord();
            library.rememberSeenSnapshot(form.snapshotId());
            library.selectForm(slot);
            return ImitatorActionResult.form(form, "Record stored and selected in slot " + displaySlot(slot));
        } catch (IllegalArgumentException exception) {
            return rejectedAction(player, message(exception));
        }
    }

    public ImitatorActionResult selectForm(ServerPlayer player, int slot) {
        Objects.requireNonNull(player, "player");
        ImitatorFormLibrary library = formsFor(player);
        try {
            if (!library.selectForm(slot)) {
                return rejectedAction(player, "The requested form slot is empty");
            }
            return ImitatorActionResult.form(library.selectedForm().orElseThrow(), "Selected form slot " + displaySlot(slot));
        } catch (IllegalArgumentException exception) {
            return rejectedAction(player, message(exception));
        }
    }

    public ImitatorActionResult clearForm(ServerPlayer player, int slot) {
        Objects.requireNonNull(player, "player");
        ImitatorFormLibrary library = formsFor(player);
        try {
            Optional<ImitatorForm> removed = library.clearForm(slot);
            return removed.isPresent()
                    ? ImitatorActionResult.accepted("Cleared form slot " + displaySlot(slot))
                    : rejectedAction(player, "The requested form slot is already empty");
        } catch (IllegalArgumentException exception) {
            return rejectedAction(player, message(exception));
        }
    }

    public ImitatorActionResult clearAllForms(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ImitatorFormLibrary library = formsFor(player);
        int cleared = 0;
        for (ImitatorFormSlot slot : library.occupiedSlots()) {
            if (library.clearForm(slot.index()).isPresent()) {
                cleared++;
            }
        }
        library.clearSelectedForm();
        library.clearPendingRecord();
        return ImitatorActionResult.accepted("Cleared " + cleared + " form slot" + (cleared == 1 ? "" : "s"));
    }

    public SessionTransitionResult beginSelectedTransform(ServerPlayer player) {
        return beginSelectedTransform(player, ImitatorProgressionPolicy.DEFAULT, ImitatorMirrorSyncPolicy.DEFAULT, ImitatorSkillCopyPolicy.DISABLED, false, ResourceLocation.withDefaultNamespace("imitator")).transition();
    }

    public ImitatorTransformOutcome beginSelectedTransform(ServerPlayer player, ImitatorProgressionPolicy progressionPolicy) {
        return beginSelectedTransform(player, progressionPolicy, ImitatorMirrorSyncPolicy.DEFAULT, ImitatorSkillCopyPolicy.DISABLED, false, ResourceLocation.withDefaultNamespace("imitator"));
    }

    public ImitatorTransformOutcome beginSelectedTransform(ServerPlayer player, ImitatorProgressionPolicy progressionPolicy, ImitatorMirrorSyncPolicy mirrorSyncPolicy) {
        return beginSelectedTransform(player, progressionPolicy, mirrorSyncPolicy, ImitatorSkillCopyPolicy.DISABLED, false, ResourceLocation.withDefaultNamespace("imitator"));
    }

    public ImitatorTransformOutcome beginSelectedTransform(ServerPlayer player, ImitatorProgressionPolicy progressionPolicy, ImitatorMirrorSyncPolicy mirrorSyncPolicy, ImitatorSkillCopyPolicy skillCopyPolicy, boolean mastered, ResourceLocation imitatorSkillId) {
        return beginSelectedTransform(player, progressionPolicy, mirrorSyncPolicy, skillCopyPolicy, mastered, imitatorSkillId, ImitatorTransformDurationPolicy.unlimited());
    }

    public ImitatorTransformOutcome beginSelectedTransform(ServerPlayer player, ImitatorProgressionPolicy progressionPolicy, ImitatorMirrorSyncPolicy mirrorSyncPolicy, ImitatorSkillCopyPolicy skillCopyPolicy, boolean mastered, ResourceLocation imitatorSkillId, ImitatorTransformDurationPolicy durationPolicy) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(progressionPolicy, "progressionPolicy");
        Objects.requireNonNull(mirrorSyncPolicy, "mirrorSyncPolicy");
        Objects.requireNonNull(skillCopyPolicy, "skillCopyPolicy");
        Objects.requireNonNull(imitatorSkillId, "imitatorSkillId");
        Objects.requireNonNull(durationPolicy, "durationPolicy");
        ImitatorActionResult validation = validateSelectedTransform(player, progressionPolicy, mirrorSyncPolicy);
        if (!validation.accepted()) {
            ImitationDiagnostics.rejected(player, validation.message());
            return new ImitatorTransformOutcome(SessionTransitionResult.rejected(validation.message()), Optional.empty());
        }
        ImitatorFormLibrary library = formsFor(player);
        ImitatorForm form = library.selectedForm().orElseThrow();
        Optional<IdentitySnapshot> snapshot = transformations.snapshot(form.snapshotId());
        if (snapshot.isEmpty()) {
            ImitationDiagnostics.rejected(player, "The selected form is no longer available");
            return new ImitatorTransformOutcome(SessionTransitionResult.rejected("The selected form is no longer available"), Optional.empty());
        }
        IdentitySnapshot effectiveSnapshot = effectiveSnapshot(form, snapshot.get());
        if (effectiveSnapshot != snapshot.get()) {
            transformations.storeSnapshot(effectiveSnapshot);
        }
        double gameplayScale = mirrorScale(player, effectiveSnapshot, form, library.mirrorSyncEnabled(), mirrorSyncPolicy.tensuraCopyPolicy());
        SessionTransitionResult started = beginTransform(player, form.snapshotId(), scopeFor(library), gameplayScale);
        if (!started.accepted()) {
            return new ImitatorTransformOutcome(started, Optional.empty());
        }
        SessionTransitionResult applied = applications.apply(player, started.session().orElseThrow().sessionId());
        if (!applied.accepted()) {
            return new ImitatorTransformOutcome(applied, Optional.empty());
        }
        SessionTransitionResult suppression = new OwnerSkillSuppressionService(transformations).suppressOriginalSkills(player, applied.session().orElseThrow().sessionId(), List.of(imitatorSkillId));
        if (!suppression.accepted()) {
            requestReversion(player, applied.session().orElseThrow().sessionId(), TransformationLifecycleReason.APPLY_FAILURE);
            String message = "Owner skill suppression failed: " + suppression.message();
            ImitationDiagnostics.rejected(player, message);
            return new ImitatorTransformOutcome(SessionTransitionResult.rejected(message), Optional.empty());
        }
        SessionTransitionResult policyAttached = attachSkillCopyPolicy(player, applied.session().orElseThrow().sessionId(), skillCopyPolicy);
        if (!policyAttached.accepted()) {
            requestReversion(player, applied.session().orElseThrow().sessionId(), TransformationLifecycleReason.APPLY_FAILURE);
            String message = "Copied skill policy failed: " + policyAttached.message();
            ImitationDiagnostics.rejected(player, message);
            return new ImitatorTransformOutcome(SessionTransitionResult.rejected(message), Optional.empty());
        }
        SessionTransitionResult durationAttached = attachDurationPolicy(player, policyAttached.session().orElseThrow().sessionId(), durationPolicy);
        if (!durationAttached.accepted()) {
            requestReversion(player, applied.session().orElseThrow().sessionId(), TransformationLifecycleReason.APPLY_FAILURE);
            String message = "Duration policy failed: " + durationAttached.message();
            ImitationDiagnostics.rejected(player, message);
            return new ImitatorTransformOutcome(SessionTransitionResult.rejected(message), Optional.empty());
        }
        new MobImitationTargetingService(transformations).clearExistingTargets(player);
        int copiedSkills = copyFormSkills(player, durationAttached.session().orElseThrow(), skillCopyPolicy, imitatorSkillId);
        ImitatorFormProgression progression = refineSelectedForm(player, progressionPolicy, ImitatorProgressionAction.TRANSFORM);
        return new ImitatorTransformOutcome(durationAttached, Optional.of(progression), copiedSkills);
    }

    public ImitatorReplicaOutcome beginSelectedReplica(ServerPlayer player, ImitatorProgressionPolicy progressionPolicy, ImitatorReplicaPolicy replicaPolicy, boolean mastered) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(progressionPolicy, "progressionPolicy");
        Objects.requireNonNull(replicaPolicy, "replicaPolicy");
        ImitatorActionResult validation = validateSelectedReplica(player, progressionPolicy, replicaPolicy);
        if (!validation.accepted()) {
            ImitationDiagnostics.rejected(player, validation.message());
            return new ImitatorReplicaOutcome(SessionTransitionResult.rejected(validation.message()), Optional.empty());
        }
        ImitatorForm form = formsFor(player).selectedForm().orElseThrow();
        Optional<IdentitySnapshot> snapshot = transformations.snapshot(form.snapshotId());
        if (snapshot.isEmpty()) {
            ImitationDiagnostics.rejected(player, "The selected form is no longer available");
            return new ImitatorReplicaOutcome(SessionTransitionResult.rejected("The selected form is no longer available"), Optional.empty());
        }
        SessionTransitionResult started = beginReplica(player, form.snapshotId(), replicaPolicy, replicaScale(player, snapshot.get(), form, progressionPolicy, mastered));
        if (!started.accepted()) {
            return new ImitatorReplicaOutcome(started, Optional.empty());
        }
        SessionTransitionResult applied = applications.apply(player, started.session().orElseThrow().sessionId());
        if (!applied.accepted()) {
            return new ImitatorReplicaOutcome(applied, Optional.empty());
        }
        ImitatorFormProgression progression = refineSelectedForm(player, progressionPolicy, ImitatorProgressionAction.REPLICA);
        return new ImitatorReplicaOutcome(applied, Optional.of(progression), replicaId(applied.session().orElseThrow()));
    }

    public ImitatorActionResult validateRecord(ServerPlayer requester, Entity target) {
        Objects.requireNonNull(requester, "requester");
        Objects.requireNonNull(target, "target");
        ImitatorFormLibrary library = formsFor(requester);
        library.expirePendingRecord(requester.level().getGameTime());
        if (library.pendingRecord().isPresent()) {
            return rejectedAction(requester, "Commit or cancel the existing pending record first");
        }
        return recordRejection(requester, target)
                .map(message -> rejectedAction(requester, message))
                .orElseGet(() -> ImitatorActionResult.accepted("Record target accepted"));
    }

    public ImitatorActionResult validateSelectedTransform(ServerPlayer player) {
        return validateSelectedTransform(player, ImitatorProgressionPolicy.DEFAULT, ImitatorMirrorSyncPolicy.DEFAULT);
    }

    public ImitatorActionResult validateSelectedTransform(ServerPlayer player, ImitatorProgressionPolicy progressionPolicy) {
        return validateSelectedTransform(player, progressionPolicy, ImitatorMirrorSyncPolicy.DEFAULT);
    }

    public ImitatorActionResult validateSelectedTransform(ServerPlayer player, ImitatorProgressionPolicy progressionPolicy, ImitatorMirrorSyncPolicy mirrorSyncPolicy) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(progressionPolicy, "progressionPolicy");
        Objects.requireNonNull(mirrorSyncPolicy, "mirrorSyncPolicy");
        ImitatorFormLibrary library = formsFor(player);
        library.expirePendingRecord(player.level().getGameTime());
        if (sessionFor(player).isPresent()) {
            return rejectedAction(player, "End the active Imitator transformation before starting another one");
        }
        if (library.pendingRecord().isPresent()) {
            return rejectedAction(player, "Commit or cancel the existing pending record before transforming");
        }
        Optional<ImitatorForm> selected = library.selectedForm();
        if (selected.isEmpty()) {
            return rejectedAction(player, "No form slot is selected");
        }
        if (selected.get().precision() < progressionPolicy.minimumPrecision()) {
            return rejectedAction(player, "The selected form does not have enough precision to transform");
        }
        if (library.mirrorSyncEnabled() && !progressionPolicy.allowsMirrorSync(selected.get())) {
            return rejectedAction(player, "Perfect Form requires a more precise recorded form");
        }
        Optional<IdentitySnapshot> snapshot = transformations.snapshot(selected.get().snapshotId());
        if (snapshot.isEmpty()) {
            return rejectedAction(player, "The selected form is no longer available");
        }
        if (library.mirrorSyncEnabled()) {
            Optional<TensuraStateSnapshot> targetState = TensuraStateExtensions.find(snapshot.get().extensions());
            Optional<TensuraStateSnapshot> ownerState = ImitationApi.tensuraStates().capture(player);
            if (targetState.isEmpty() || ownerState.isEmpty()) {
                return rejectedAction(player, "Perfect Form requires compatible Tensura state for both forms");
            }
            TensuraCopyPolicy.TensuraCopyPolicyDecision decision = mirrorSyncPolicy.tensuraCopyPolicy().evaluate(ownerState.get().vitals(), targetState.get().vitals(), selected.get().precision(), true);
            if (!decision.accepted()) {
                return rejectedAction(player, decision.reasons().getFirst());
            }
        }
        CompatibilityAssessment compatibility = compatibilityFor(snapshot.get(), scopeFor(library));
        if (scopeFor(library) == TransformationScope.GAMEPLAY && compatibility.level() != CompatibilityLevel.FULL) {
            ImitationDiagnostics.compatibility(player, compatibility);
        }
        return compatibility.level().isUsable()
                ? ImitatorActionResult.accepted("Selected form accepted")
                : rejectedAction(player, "The selected form is unsupported: " + String.join("; ", compatibility.reasons()));
    }

    public ImitatorActionResult validateSelectedReplica(ServerPlayer player, ImitatorProgressionPolicy progressionPolicy, ImitatorReplicaPolicy replicaPolicy) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(progressionPolicy, "progressionPolicy");
        Objects.requireNonNull(replicaPolicy, "replicaPolicy");
        ImitatorFormLibrary library = formsFor(player);
        library.expirePendingRecord(player.level().getGameTime());
        if (sessionFor(player).isPresent()) {
            return rejectedAction(player, "End the active Imitator session before creating a replica");
        }
        if (library.pendingRecord().isPresent()) {
            return rejectedAction(player, "Commit or cancel the existing pending record before creating a replica");
        }
        Optional<ImitatorForm> selected = library.selectedForm();
        if (selected.isEmpty()) {
            return rejectedAction(player, "No form slot is selected");
        }
        if (selected.get().precision() < progressionPolicy.minimumPrecision()) {
            return rejectedAction(player, "The selected form does not have enough precision to create a replica");
        }
        Optional<IdentitySnapshot> snapshot = transformations.snapshot(selected.get().snapshotId());
        if (snapshot.isEmpty()) {
            return rejectedAction(player, "The selected form is no longer available");
        }
        if (!replicaEntitySupported(snapshot.get(), replicaPolicy, player.serverLevel())) {
            return rejectedAction(player, "The selected form cannot produce a living replica");
        }
        return ImitatorActionResult.accepted("Selected form accepted");
    }

    public SessionTransitionResult beginTransform(ServerPlayer player, UUID snapshotId) {
        return beginTransform(player, snapshotId, TransformationScope.GAMEPLAY);
    }

    public SessionTransitionResult beginSurfaceImitation(ServerPlayer player, UUID snapshotId) {
        return beginTransform(player, snapshotId, TransformationScope.SURFACE, 0D);
    }

    public SessionTransitionResult beginReplica(ServerPlayer player, UUID snapshotId, ImitatorReplicaPolicy replicaPolicy, double reproductionScale) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(replicaPolicy, "replicaPolicy");
        if (!Double.isFinite(reproductionScale) || reproductionScale < 0D || reproductionScale > 1D) {
            throw new IllegalArgumentException("reproductionScale must be between zero and one");
        }
        Optional<IdentitySnapshot> snapshot = transformations.snapshot(snapshotId);
        if (snapshot.isEmpty()) {
            ImitationDiagnostics.rejected(player, "The requested snapshot does not exist");
            return SessionTransitionResult.rejected("The requested snapshot does not exist");
        }
        if (!replicaEntitySupported(snapshot.get(), replicaPolicy, player.serverLevel())) {
            ImitationDiagnostics.rejected(player, "The requested snapshot cannot create a replica");
            return SessionTransitionResult.rejected("The requested snapshot cannot create a replica");
        }
        BaselineSnapshot baseline = new BaselineSnapshot(BaselineSnapshot.CURRENT_SCHEMA_VERSION, replicaPolicy.toTag(), List.of());
        return transformations.beginSession(player.getUUID(), snapshotId, TransformationScope.REPLICA, reproductionScale, baseline, compatibilityFor(snapshot.get(), TransformationScope.REPLICA), player.level().getGameTime());
    }

    public SessionTransitionResult beginTransform(ServerPlayer player, UUID snapshotId, TransformationScope scope) {
        return beginTransform(player, snapshotId, scope, 1D);
    }

    public SessionTransitionResult beginTransform(ServerPlayer player, UUID snapshotId, TransformationScope scope, double gameplayScale) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(scope, "scope");
        if (!Double.isFinite(gameplayScale) || gameplayScale < 0D || gameplayScale > 1D) {
            throw new IllegalArgumentException("gameplayScale must be between zero and one");
        }
        Optional<IdentitySnapshot> snapshot = transformations.snapshot(snapshotId);
        if (snapshot.isEmpty()) {
            ImitationDiagnostics.rejected(player, "The requested snapshot does not exist");
            return SessionTransitionResult.rejected("The requested snapshot does not exist");
        }
        CompatibilityAssessment compatibility = compatibilityFor(snapshot.get(), scope);
        if (scope == TransformationScope.GAMEPLAY && compatibility.level() != CompatibilityLevel.FULL) {
            ImitationDiagnostics.compatibility(player, compatibility);
        }
        return transformations.beginSession(player.getUUID(), snapshotId, scope, gameplayScale, captureBaseline(player), compatibility, player.level().getGameTime());
    }

    public TransformationScope selectedTransformationScope(ServerPlayer player) {
        return scopeFor(formsFor(Objects.requireNonNull(player, "player")));
    }

    public SessionTransitionResult transition(ServerPlayer player, UUID sessionId, TransformationState expectedState, TransformationState targetState) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(expectedState, "expectedState");
        Objects.requireNonNull(targetState, "targetState");
        if (!owns(player, sessionId)) {
            ImitationDiagnostics.rejected(player, "The requested session does not belong to this player");
            return SessionTransitionResult.rejected("The requested session does not belong to this player");
        }
        return transformations.transition(sessionId, expectedState, targetState, player.level().getGameTime());
    }

    public SessionTransitionResult requestReversion(ServerPlayer player, UUID sessionId, TransformationLifecycleReason reason) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(reason, "reason");
        if (!owns(player, sessionId)) {
            ImitationDiagnostics.rejected(player, "The requested session does not belong to this player");
            return SessionTransitionResult.rejected("The requested session does not belong to this player");
        }
        return applications.requestReversion(Optional.of(player), sessionId, reason, player.level().getGameTime());
    }

    public Optional<TransformationSession> sessionFor(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return transformations.sessionsRequiringRecovery().stream()
                .filter(session -> session.ownerId().equals(player.getUUID()))
                .max(java.util.Comparator.comparing(TransformationSession::createdGameTime));
    }

    public Optional<TransformationSession> activeSessionFor(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return transformations.activeSessionForOwner(player.getUUID());
    }

    public ImitatorActionResult activateFormAbility(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        Optional<TransformationSession> session = activePresentationSession(player);
        if (session.isEmpty()) {
            return rejectedAction(player, "There is no active Imitator transformation");
        }
        Optional<IdentitySnapshot> snapshot = transformations.snapshot(session.get().snapshotId());
        if (snapshot.isEmpty()) {
            return rejectedAction(player, "The active Imitator form is no longer available");
        }
        return ImitatorFormAbilities.activate(player, snapshot.get(), activeSkillCopyPolicy(session.get()), copyAccess(player, snapshot.get()));
    }

    public Optional<ImitatorFormStatDelta> tickFormTraits(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        Optional<ImitatorFormStatDelta> delta = new ImitatorFormProgressionService(transformations, forms).reconcile(player);
        activePresentationSession(player).ifPresent(session -> transformations.snapshot(session.snapshotId())
                .ifPresent(snapshot -> ImitatorFormAbilities.tick(player, snapshot, activeSkillCopyPolicy(session), copyAccess(player, snapshot))));
        return delta;
    }

    public Optional<SessionTransitionResult> expireTransformDuration(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        long gameTime = player.level().getGameTime();
        return activeSessionFor(player)
                .filter(session -> session.expiresAtOrBefore(gameTime))
                .map(session -> requestReversion(player, session.sessionId(), TransformationLifecycleReason.DURATION_EXPIRED));
    }

    public List<TransformationSession> sessionsFor(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return transformations.sessionsForOwner(player.getUUID()).stream()
                .sorted(java.util.Comparator.comparing(TransformationSession::createdGameTime).reversed())
                .toList();
    }

    public BaselineSnapshot captureBaseline(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        CompoundTag playerData = player.saveWithoutId(new CompoundTag());
        List<SnapshotExtension> extensions = ImitationApi.tensuraStates().capture(player)
                .map(TensuraStateExtensions::create)
                .map(List::of)
                .orElseGet(List::of);
        return new BaselineSnapshot(BaselineSnapshot.CURRENT_SCHEMA_VERSION, playerData, extensions);
    }

    private boolean owns(ServerPlayer player, UUID sessionId) {
        return transformations.session(sessionId).map(session -> session.ownerId().equals(player.getUUID())).orElse(false);
    }

    private Optional<TransformationSession> activePresentationSession(ServerPlayer player) {
        return transformations.activeSessionForOwner(player.getUUID())
                .filter(session -> session.scope().changesOwnerPresentation())
                .filter(session -> transformations.snapshot(session.snapshotId()).isPresent());
    }

    private ImitatorFormProgression refineSelectedForm(ServerPlayer player, ImitatorProgressionPolicy progressionPolicy, ImitatorProgressionAction action) {
        ImitatorFormLibrary library = formsFor(player);
        int slot = library.selectedSlot().orElseThrow();
        ImitatorForm current = library.selectedForm().orElseThrow();
        ImitatorFormProgression progression = progressionPolicy.refine(current, action);
        library.setForm(slot, progression.currentForm());
        return progression;
    }

    private static IdentitySnapshot effectiveSnapshot(ImitatorForm form, IdentitySnapshot snapshot) {
        return form.stats().isEmpty() ? snapshot : form.stats().mergeInto(snapshot);
    }

    private int copyFormSkills(ServerPlayer player, TransformationSession session, ImitatorSkillCopyPolicy policy, ResourceLocation imitatorSkillId) {
        if (!policy.enabled()) {
            return 0;
        }
        Optional<IdentitySnapshot> snapshot = transformations.snapshot(session.snapshotId());
        Optional<ImitatorSkillCopySnapshot> copiedSkills = snapshot.flatMap(value -> ImitatorSkillCopyExtensions.find(value.extensions()));
        if (copiedSkills.isEmpty()) {
            return 0;
        }
        ImitatorSkillCopySnapshot copySnapshot = copiedSkills.get();
        if (!ImitationApi.skillBridges().activeBridgeId().filter(copySnapshot.bridgeId()::equals).isPresent()) {
            return 0;
        }
        ImitatorSkillCopyAccess access = snapshot.map(value -> copyAccess(player, value)).orElse(ImitatorSkillCopyAccess.INFERIOR_OR_EQUAL_EP);
        TemporarySkillService temporarySkills = new TemporarySkillService(transformations);
        int granted = 0;
        for (ImitatorCopiedSkill copiedSkill : policy.select(copySnapshot, imitatorSkillId, skillId -> ImitationApi.skillBridges().classify(copySnapshot.bridgeId(), skillId), access)) {
            TemporarySkillGrantResult result = temporarySkills.grant(player, session.sessionId(), copiedSkill.skillId(), policy.temporaryRemoveTime(), copiedSkill.mastery());
            if (result.operation().successful()) {
                granted++;
            }
        }
        return granted;
    }

    private SessionTransitionResult attachSkillCopyPolicy(ServerPlayer player, UUID sessionId, ImitatorSkillCopyPolicy policy) {
        return transformations.addTemporaryState(sessionId, ImitatorSkillCopyPolicyState.create(sessionId, policy), player.level().getGameTime());
    }

    private SessionTransitionResult attachDurationPolicy(ServerPlayer player, UUID sessionId, ImitatorTransformDurationPolicy durationPolicy) {
        long gameTime = player.level().getGameTime();
        return transformations.updateExpiration(sessionId, durationPolicy.expiresFrom(gameTime), gameTime);
    }

    private static ImitatorSkillCopyPolicy activeSkillCopyPolicy(TransformationSession session) {
        return ImitatorSkillCopyPolicyState.find(session).orElse(ImitatorSkillCopyPolicy.DEFAULT_FORM_ABILITY_POLICY);
    }

    private static ImitatorSkillCopyAccess copyAccess(ServerPlayer player, IdentitySnapshot targetSnapshot) {
        Optional<TensuraStateSnapshot> targetState = TensuraStateExtensions.find(targetSnapshot.extensions());
        Optional<TensuraStateSnapshot> ownerState = ImitationApi.tensuraStates().capture(player);
        if (targetState.isPresent() && ownerState.isPresent() && ownerState.get().vitals().ep() > targetState.get().vitals().ep()) {
            return ImitatorSkillCopyAccess.SUPERIOR_EP;
        }
        return ImitatorSkillCopyAccess.INFERIOR_OR_EQUAL_EP;
    }

    private static TransformationScope scopeFor(ImitatorFormLibrary library) {
        return library.mirrorSyncEnabled() ? TransformationScope.GAMEPLAY : TransformationScope.SURFACE;
    }

    private static CompatibilityAssessment compatibilityFor(IdentitySnapshot snapshot, TransformationScope scope) {
        return switch (scope) {
            case SURFACE -> CompatibilityAssessment.visual("Surface imitation applies no gameplay state");
            case REPLICA -> CompatibilityAssessment.visual("Replica spawns a temporary entity copy");
            case GAMEPLAY -> ImitationApi.adapters().assess(snapshot, List.of(AdapterKind.GAMEPLAY));
        };
    }

    private static double mirrorScale(ServerPlayer player, IdentitySnapshot snapshot, ImitatorForm form, boolean mirrorSyncEnabled, TensuraCopyPolicy policy) {
        if (!mirrorSyncEnabled) {
            return 0D;
        }
        TensuraStateSnapshot targetState = TensuraStateExtensions.find(snapshot.extensions()).orElseThrow(() -> new IllegalStateException("Perfect Form target state is unavailable"));
        TensuraStateSnapshot ownerState = ImitationApi.tensuraStates().capture(player).orElseThrow(() -> new IllegalStateException("Perfect Form owner state is unavailable"));
        TensuraCopyPolicy.TensuraCopyPolicyDecision decision = policy.evaluate(ownerState.vitals(), targetState.vitals(), form.precision(), true);
        if (!decision.accepted()) {
            throw new IllegalStateException(decision.reasons().getFirst());
        }
        return decision.scale();
    }

    private static double replicaScale(ServerPlayer player, IdentitySnapshot snapshot, ImitatorForm form, ImitatorProgressionPolicy progressionPolicy, boolean mastered) {
        double scale = form.precision();
        Optional<TensuraStateSnapshot> targetState = TensuraStateExtensions.find(snapshot.extensions());
        Optional<TensuraStateSnapshot> ownerState = ImitationApi.tensuraStates().capture(player);
        if (targetState.isPresent() && ownerState.isPresent()) {
            scale = Math.min(scale, progressionPolicy.reproductionScale(ownerState.get().vitals().ep(), targetState.get().vitals().ep(), mastered));
        }
        return Math.max(0D, Math.min(1D, scale));
    }

    private static Optional<UUID> replicaId(TransformationSession session) {
        return session.temporaryState().stream()
                .filter(reference -> reference.kind().equals(TemporaryStateKinds.REPLICA_ENTITY))
                .map(reference -> reference.payload())
                .filter(payload -> payload.hasUUID("entity"))
                .map(payload -> payload.getUUID("entity"))
                .findFirst();
    }

    private static boolean replicaEntitySupported(IdentitySnapshot snapshot, ImitatorReplicaPolicy policy, ServerLevel level) {
        if (snapshot.entityType().equals(PLAYER_ENTITY_TYPE)) {
            return policy.fallbackPlayerForms();
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(snapshot.entityType())
                .map(type -> type != EntityType.PLAYER && createsLivingEntity(type, level))
                .orElse(false);
    }

    private static boolean createsLivingEntity(EntityType<?> type, ServerLevel level) {
        Entity entity;
        try {
            entity = type.create(level);
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
        try {
            return entity instanceof LivingEntity;
        } finally {
            if (entity != null) {
                entity.discard();
            }
        }
    }

    public ImitatorRecordingContext recordingContext(ServerPlayer requester, Entity target, boolean mastered) {
        double range = Math.max(1D, actionPolicy.maxRecordDistance() - 3D);
        double distanceRatio = clamp((requester.distanceTo(target) - 3D) / range);
        double motionRatio = clamp(target.getDeltaMovement().length() / 0.7D);
        double powerRatio = target instanceof LivingEntity living
                ? ImitationApi.tensuraStates().capture(requester)
                        .flatMap(owner -> ImitationApi.tensuraStates().capture(living).map(targetState -> TensuraCopyPolicy.DEFAULT.powerRatio(owner.vitals(), targetState.vitals())))
                        .orElse(1D)
                : 1D;
        return new ImitatorRecordingContext(distanceRatio, motionRatio, powerRatio, 0D, mastered);
    }

    private Optional<String> recordRejection(ServerPlayer requester, Entity target) {
        if (requester.isSpectator()) {
            return Optional.of("Spectators cannot record forms");
        }
        if (target == requester) {
            return Optional.of("You cannot record your own form");
        }
        if (target.level() != requester.level()) {
            return Optional.of("The target is in another level");
        }
        if (target.isRemoved()) {
            return Optional.of("The target is no longer available");
        }
        if (!target.isAlive()) {
            return Optional.of("The target is no longer alive");
        }
        if (!actionPolicy.allowPlayerTargets() && target instanceof Player) {
            return Optional.of("Player forms are disabled by server policy");
        }
        if (requester.distanceToSqr(target) > actionPolicy.maxRecordDistanceSqr()) {
            return Optional.of("The target is out of recording range");
        }
        if (actionPolicy.requireLineOfSight() && !requester.hasLineOfSight(target)) {
            return Optional.of("The target must be visible to record it");
        }
        return Optional.empty();
    }

    private String message(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "unknown error" : message.length() > 192 ? message.substring(0, 192) : message;
    }

    private static ImitatorActionResult rejectedAction(ServerPlayer player, String message) {
        ImitationDiagnostics.rejected(player, message);
        return ImitatorActionResult.rejected(message);
    }

    private static double clamp(double value) {
        return Math.max(0D, Math.min(1D, value));
    }

    private static int displaySlot(int slot) {
        return slot + 1;
    }
}
