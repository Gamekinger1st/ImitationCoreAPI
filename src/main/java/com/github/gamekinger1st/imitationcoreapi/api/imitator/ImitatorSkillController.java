package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnostics;
import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationLifecycleReason;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;

import java.util.Objects;
import java.util.Optional;

public final class ImitatorSkillController {
    private final ImitatorHandlerService handlers;
    private final ImitatorSynchronizer synchronizer;

    public ImitatorSkillController(ImitatorHandlerService handlers) {
        this(handlers, ImitatorSynchronizer.NO_OP);
    }

    public ImitatorSkillController(ImitatorHandlerService handlers, ImitatorSynchronizer synchronizer) {
        this.handlers = Objects.requireNonNull(handlers, "handlers");
        this.synchronizer = Objects.requireNonNull(synchronizer, "synchronizer");
    }

    public ImitatorSkillMode mode(ServerPlayer player) {
        return handlers.formsFor(Objects.requireNonNull(player, "player")).skillMode();
    }

    public ImitatorSkillMode cycleMode(ServerPlayer player) {
        ImitatorSkillMode mode = handlers.formsFor(Objects.requireNonNull(player, "player")).cycleSkillMode();
        synchronizer.syncFormLibrary(player);
        return mode;
    }

    public void setMode(ServerPlayer player, ImitatorSkillMode mode) {
        handlers.formsFor(Objects.requireNonNull(player, "player")).setSkillMode(Objects.requireNonNull(mode, "mode"));
        synchronizer.syncFormLibrary(player);
    }

    public boolean mirrorSyncEnabled(ServerPlayer player) {
        return handlers.formsFor(Objects.requireNonNull(player, "player")).mirrorSyncEnabled();
    }

    public void setMirrorSyncEnabled(ServerPlayer player, boolean enabled) {
        handlers.formsFor(Objects.requireNonNull(player, "player")).setMirrorSyncEnabled(enabled);
        synchronizer.syncFormLibrary(player);
    }

    public ImitatorActionResult commitRecord(ServerPlayer player, int slot) {
        ImitatorActionResult result = handlers.commitPendingRecord(Objects.requireNonNull(player, "player"), slot);
        synchronizer.syncFormLibrary(player);
        return result;
    }

    public ImitatorActionResult selectForm(ServerPlayer player, int slot) {
        ImitatorActionResult result = handlers.selectForm(Objects.requireNonNull(player, "player"), slot);
        synchronizer.syncFormLibrary(player);
        return result;
    }

    public ImitatorActionResult clearForm(ServerPlayer player, int slot) {
        ImitatorActionResult result = handlers.clearForm(Objects.requireNonNull(player, "player"), slot);
        synchronizer.syncFormLibrary(player);
        return result;
    }

    public ImitatorActionResult clearAllForms(ServerPlayer player) {
        ImitatorActionResult result = handlers.clearAllForms(Objects.requireNonNull(player, "player"));
        synchronizer.syncFormLibrary(player);
        return result;
    }

    public ImitatorActionResult activateFormAbility(ServerPlayer player) {
        return handlers.activateFormAbility(Objects.requireNonNull(player, "player"));
    }

    public void tickFormTraits(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        Optional<SessionTransitionResult> removed = handlers.revertIfControllerSkillMissing(player);
        if (removed.isPresent()) {
            removed.get().session().ifPresent(session -> synchronizer.syncSession(player, session));
            return;
        }
        Optional<SessionTransitionResult> expired = handlers.expireTransformDuration(player);
        if (expired.isPresent()) {
            expired.get().session().ifPresent(session -> synchronizer.syncSession(player, session));
            return;
        }
        Optional<ImitatorFormStatDelta> delta = handlers.tickFormTraits(player);
        if (delta.isPresent()) {
            synchronizer.syncFormLibrary(player);
            handlers.activeSessionFor(player).ifPresent(session -> synchronizer.syncSession(player, session));
        }
    }

    public void openMenu(ServerPlayer player, ImitatorMenuRequest request) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(request, "request");
        ImitationApi.imitatorMenuContinuations().clear(player.getUUID());
        if (request != ImitatorMenuRequest.NONE) {
            synchronizer.syncFormLibrary(player);
            synchronizer.openMenu(player, request);
        }
    }

    public void openMenu(ServerPlayer player, ImitatorMenuRequest request, ImitatorMenuContinuation continuation) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(continuation, "continuation");
        ImitationApi.imitatorMenuContinuations().stage(player, request, mode(player), continuation);
        synchronizer.syncFormLibrary(player);
        synchronizer.openMenu(player, request);
    }

    public SessionTransitionResult revert(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return handlers.sessionFor(player)
                .map(session -> {
                    SessionTransitionResult result = handlers.requestReversion(player, session.sessionId(), TransformationLifecycleReason.FORCE_REVERT);
                    result.session().ifPresent(updated -> synchronizer.syncSession(player, updated));
                    return result;
                })
                .orElseGet(() -> SessionTransitionResult.rejected("There is no active Imitator transformation to revert"));
    }

    public ImitatorSkillUseResult preflight(ServerPlayer player, Optional<Entity> target, long mastery, ImitatorSkillDefinition definition) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(definition, "definition");
        if (mastery < 0L) {
            throw new IllegalArgumentException("Mastery cannot be negative");
        }
        ImitatorSkillMode mode = mode(player);
        ImitatorSkillCost cost = definition.cost(mode);
        ImitatorSkillUseResult result;
        if (mastery < cost.requiredMastery()) {
            result = ImitatorSkillUseResult.rejected(mode, cost, ImitatorMenuRequest.NONE, "This Imitator mode requires more mastery");
        } else {
            result = switch (mode) {
                case RECORD -> target.map(entity -> recordPreflight(player, entity, mode, cost))
                        .orElseGet(() -> ImitatorSkillUseResult.rejected(mode, cost, ImitatorMenuRequest.NONE, "Select a target to record"));
                case TRANSFORM -> transformPreflight(player, mode, cost, definition.progressionPolicy(), definition.mirrorSyncPolicy());
                case REPLICA -> replicaPreflight(player, mode, cost, definition.progressionPolicy(), definition.replicaPolicy());
            };
        }
        diagnoseRejected(player, result);
        return result;
    }

    public ImitatorSkillUseResult execute(ServerPlayer player, Optional<Entity> target, long mastery, ImitatorSkillDefinition definition) {
        ImitatorSkillUseResult preflight = preflight(player, target, mastery, definition);
        if (!preflight.accepted()) {
            openMenu(player, preflight.menuRequest());
            return preflight;
        }
        return executeValidated(player, target, preflight, definition, mastery);
    }

    public ImitatorSkillUseResult execute(ServerPlayer player, Optional<Entity> target, ImitatorSkillDefinition definition, ImitatorSkillHost host) {
        Objects.requireNonNull(host, "host");
        ImitatorSkillUseResult preflight = preflight(player, target, host.mastery(player), definition);
        if (!preflight.accepted()) {
            openMenu(player, preflight.menuRequest());
            return preflight;
        }
        if (!host.canUse(player, preflight.mode(), preflight.cost())) {
            ImitatorSkillUseResult rejected = ImitatorSkillUseResult.rejected(preflight.mode(), preflight.cost(), ImitatorMenuRequest.NONE, "The skill host rejected this Imitator action");
            diagnoseRejected(player, rejected);
            return rejected;
        }
        ImitatorSkillUseResult result = executeValidated(player, target, preflight, definition, host.mastery(player));
        if (result.accepted()) {
            host.onUseAccepted(player, result.mode(), result.cost());
        }
        return result;
    }

    private ImitatorSkillUseResult executeValidated(ServerPlayer player, Optional<Entity> target, ImitatorSkillUseResult preflight, ImitatorSkillDefinition definition, long mastery) {
        ImitatorSkillUseResult result = switch (preflight.mode()) {
            case RECORD -> {
                ImitatorActionResult recorded = handlers.stageRecord(player, target.orElseThrow(), definition.progressionPolicy(), handlers.recordingContext(player, target.orElseThrow(), mastery >= definition.maximumMastery()));
                yield ImitatorSkillUseResult.record(preflight.mode(), preflight.cost(), recorded, definition.progressionPolicy().masteryReward(ImitatorProgressionAction.RECORD, false));
            }
            case TRANSFORM -> {
                ImitatorTransformOutcome outcome = handlers.beginSelectedTransform(
                        player,
                        definition.progressionPolicy(),
                        definition.mirrorSyncPolicy(),
                        definition.skillCopyPolicy(),
                        mastery >= definition.maximumMastery(),
                        definition.skillId(),
                        definition.transformDurationPolicy(),
                        definition.transformationModifiers()
                );
                int reward = outcome.progression()
                        .map(progression -> definition.progressionPolicy().masteryReward(ImitatorProgressionAction.TRANSFORM, progression.becamePerfect()))
                        .orElse(0);
                String started = outcome.transition().session()
                        .map(session -> session.scope() == TransformationScope.SURFACE ? "Surface imitation started" : "Transformation started")
                        .orElse(outcome.transition().message());
                String message = outcome.progression()
                        .map(progression -> transformMessage(progression, started))
                        .orElse(outcome.transition().accepted() ? started : outcome.transition().message());
                if (outcome.transition().accepted() && outcome.copiedSkillCount() > 0) {
                    message += "; copied " + outcome.copiedSkillCount() + " temporary skills";
                }
                yield ImitatorSkillUseResult.transform(preflight.mode(), preflight.cost(), outcome.transition(), reward, message);
            }
            case REPLICA -> {
                ImitatorReplicaOutcome outcome = handlers.beginSelectedReplica(player, definition.progressionPolicy(), definition.replicaPolicy(), mastery >= definition.maximumMastery());
                int reward = outcome.progression()
                        .map(progression -> definition.progressionPolicy().masteryReward(ImitatorProgressionAction.REPLICA, progression.becamePerfect()))
                        .orElse(0);
                String started = outcome.replicaId().map(id -> "Replica created").orElse(outcome.transition().accepted() ? "Replica created" : outcome.transition().message());
                String message = outcome.progression()
                        .map(progression -> replicaMessage(progression, started))
                        .orElse(outcome.transition().accepted() ? started : outcome.transition().message());
                yield ImitatorSkillUseResult.replica(preflight.mode(), preflight.cost(), outcome.transition(), reward, message);
            }
        };
        synchronizer.syncFormLibrary(player);
        result.transformationResult().flatMap(SessionTransitionResult::session).ifPresent(session -> synchronizer.syncSession(player, session));
        if (result.menuRequest() != ImitatorMenuRequest.NONE) {
            synchronizer.openMenu(player, result.menuRequest());
        }
        diagnoseRejected(player, result);
        return result;
    }

    private ImitatorSkillUseResult recordPreflight(ServerPlayer player, Entity target, ImitatorSkillMode mode, ImitatorSkillCost cost) {
        ImitatorActionResult validation = handlers.validateRecord(player, target);
        return validation.accepted()
                ? ImitatorSkillUseResult.record(mode, cost, validation)
                : ImitatorSkillUseResult.rejected(mode, cost, ImitatorMenuRequest.NONE, validation.message());
    }

    private ImitatorSkillUseResult transformPreflight(ServerPlayer player, ImitatorSkillMode mode, ImitatorSkillCost cost, ImitatorProgressionPolicy progressionPolicy, ImitatorMirrorSyncPolicy mirrorSyncPolicy) {
        ImitatorActionResult validation = handlers.validateSelectedTransform(player, progressionPolicy, mirrorSyncPolicy);
        if (validation.accepted()) {
            ImitatorForm selected = handlers.formsFor(player).selectedForm().orElseThrow();
            ImitatorSkillCost adjustedCost = new ImitatorSkillCost(progressionPolicy.adjustedResourceCost(cost.resourceCost(), selected), cost.cooldownTicks(), cost.requiredMastery());
            return new ImitatorSkillUseResult(true, mode, adjustedCost, ImitatorMenuRequest.NONE, Optional.empty(), Optional.empty(), validation.message());
        }
        ImitatorMenuRequest menu = validation.message().equals("No form slot is selected") ? ImitatorMenuRequest.SELECT_TRANSFORM_FORM : ImitatorMenuRequest.NONE;
        return ImitatorSkillUseResult.rejected(mode, cost, menu, validation.message());
    }

    private ImitatorSkillUseResult replicaPreflight(ServerPlayer player, ImitatorSkillMode mode, ImitatorSkillCost cost, ImitatorProgressionPolicy progressionPolicy, ImitatorReplicaPolicy replicaPolicy) {
        ImitatorActionResult validation = handlers.validateSelectedReplica(player, progressionPolicy, replicaPolicy);
        if (validation.accepted()) {
            ImitatorForm selected = handlers.formsFor(player).selectedForm().orElseThrow();
            ImitatorSkillCost adjustedCost = new ImitatorSkillCost(progressionPolicy.adjustedResourceCost(cost.resourceCost(), selected), cost.cooldownTicks(), cost.requiredMastery());
            return new ImitatorSkillUseResult(true, mode, adjustedCost, ImitatorMenuRequest.NONE, Optional.empty(), Optional.empty(), validation.message());
        }
        ImitatorMenuRequest menu = validation.message().equals("No form slot is selected") ? ImitatorMenuRequest.SELECT_TRANSFORM_FORM : ImitatorMenuRequest.NONE;
        return ImitatorSkillUseResult.rejected(mode, cost, menu, validation.message());
    }

    private static String transformMessage(ImitatorFormProgression progression, String fallback) {
        int precision = (int) Math.round(progression.currentForm().precision() * 100D);
        if (progression.becamePerfect()) {
            return "Transformation started; form precision reached perfection";
        }
        return fallback.isBlank() ? "Transformation started; form precision refined to " + precision + "%" : fallback + "; form precision refined to " + precision + "%";
    }

    private static String replicaMessage(ImitatorFormProgression progression, String fallback) {
        int precision = (int) Math.round(progression.currentForm().precision() * 100D);
        if (progression.becamePerfect()) {
            return "Replica created; form precision reached perfection";
        }
        return fallback.isBlank() ? "Replica created; form precision refined to " + precision + "%" : fallback + "; form precision refined to " + precision + "%";
    }

    private static void diagnoseRejected(ServerPlayer player, ImitatorSkillUseResult result) {
        if (!result.accepted()) {
            ImitationDiagnostics.rejected(player, result.message());
        }
    }
}
