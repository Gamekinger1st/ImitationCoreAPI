package com.github.gamekinger1st.imitationcoreapi.internal.disguise;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnosticCategory;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnosticSeverity;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnostics;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseState;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAnimationAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAnimationIntent;
import com.github.gamekinger1st.imitationcoreapi.api.gecko.GeckoControllerSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GeckoDisguiseAnimationAdapter implements DisguiseAnimationAdapter {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "geckolib_disguise_animation");
    private static final int MAX_TRACKED_SESSIONS = 512;
    private static final int ONE_SHOT_HOLD_TICKS = 24;
    private final Map<UUID, Map<String, String>> activeTriggeredAnimations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> activeTriggerHoldTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> lastForcedTriggerTicks = new ConcurrentHashMap<>();
    private final Set<UUID> missingBridgeDiagnostics = ConcurrentHashMap.newKeySet();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE - 100;
    }

    @Override
    public boolean supports(Entity imitation, Entity subject, ClientDisguiseState state) {
        CompoundTag visualData = state.visualData();
        return visualData.contains("gecko_controller_states", Tag.TAG_COMPOUND) || visualData.contains("gecko_controllers", Tag.TAG_COMPOUND);
    }

    @Override
    public void synchronize(Entity imitation, Entity subject, ClientDisguiseState state, float partialTick) {
        synchronize(imitation, subject, state, partialTick, DisguiseAnimationIntent.from(subject, partialTick));
    }

    @Override
    public void synchronize(Entity imitation, Entity subject, ClientDisguiseState state, float partialTick, DisguiseAnimationIntent intent) {
        if (activeTriggeredAnimations.size() > MAX_TRACKED_SESSIONS) {
            activeTriggeredAnimations.clear();
            activeTriggerHoldTicks.clear();
            lastForcedTriggerTicks.clear();
            missingBridgeDiagnostics.clear();
        }
        List<GeckoControllerSnapshot> controllers = controllersFromVisualData(state.visualData());
        Map<String, String> active = activeTriggeredAnimations.computeIfAbsent(state.sessionId(), ignored -> new ConcurrentHashMap<>());
        Map<String, Integer> heldUntil = activeTriggerHoldTicks.computeIfAbsent(state.sessionId(), ignored -> new ConcurrentHashMap<>());
        Map<String, Integer> forcedTicks = lastForcedTriggerTicks.computeIfAbsent(state.sessionId(), ignored -> new ConcurrentHashMap<>());
        Set<String> seen = new HashSet<>();
        for (GeckoControllerSnapshot controller : controllers) {
            Optional<String> trigger = triggerName(controller, intent);
            if (trigger.isEmpty()) {
                if (active.containsKey(controller.controllerName()) && heldUntil.getOrDefault(controller.controllerName(), -1) >= subject.tickCount) {
                    seen.add(controller.controllerName());
                }
                continue;
            }
            seen.add(controller.controllerName());
            boolean oneShot = oneShotTrigger(trigger.get());
            if (oneShot) {
                heldUntil.put(controller.controllerName(), subject.tickCount + ONE_SHOT_HOLD_TICKS);
            }
            String previous = active.put(controller.controllerName(), trigger.get());
            boolean force = shouldForceRetrigger(trigger.get(), intent, subject.tickCount, controller.controllerName(), forcedTicks);
            if (!trigger.get().equals(previous) || force) {
                if (!ImitationApi.geckoAnimations().trigger(imitation, controller.controllerName(), trigger.get())) {
                    diagnoseMissingBridge(state, subject);
                }
            }
        }
        for (Map.Entry<String, String> entry : List.copyOf(active.entrySet())) {
            if (!seen.contains(entry.getKey())) {
                if (!ImitationApi.geckoAnimations().stop(imitation, entry.getKey(), entry.getValue())) {
                    diagnoseMissingBridge(state, subject);
                }
                active.remove(entry.getKey(), entry.getValue());
                heldUntil.remove(entry.getKey());
                forcedTicks.remove(entry.getKey());
            }
        }
        if (active.isEmpty()) {
            activeTriggeredAnimations.remove(state.sessionId(), active);
            activeTriggerHoldTicks.remove(state.sessionId(), heldUntil);
            lastForcedTriggerTicks.remove(state.sessionId(), forcedTicks);
        }
    }

    private void diagnoseMissingBridge(ClientDisguiseState state, Entity subject) {
        if (!missingBridgeDiagnostics.add(state.sessionId())) {
            return;
        }
        ImitationDiagnostics.publish(
                ImitationDiagnosticCategory.MISSING_GECKOLIB_BRIDGE,
                ImitationDiagnosticSeverity.WARNING,
                "GeckoLib animation data was recorded, but no loaded bridge could trigger the copied form animation",
                Optional.of(state.ownerId()),
                Optional.of(state.sessionId()),
                Optional.of(state.snapshotId()),
                Optional.of(state.entityType()),
                subject.level().getGameTime()
        );
    }

    private Optional<String> triggerName(GeckoControllerSnapshot controller, DisguiseAnimationIntent intent) {
        return inferredTrigger(controller, intent);
    }

    static Optional<String> playbackTrigger(GeckoControllerSnapshot controller, boolean livePlaybackState) {
        if (!livePlaybackState) {
            return Optional.empty();
        }
        return controller.activeTriggeredAnimationName()
                .or(() -> controller.playingTriggeredAnimation() ? controller.triggerableAnimationNames().stream().findFirst() : Optional.empty());
    }

    static Optional<String> inferredTrigger(GeckoControllerSnapshot controller, DisguiseAnimationIntent intent) {
        List<String> triggerable = controller.triggerableAnimationNames();
        if (triggerable.isEmpty()) {
            return Optional.empty();
        }
        for (List<String> keywords : intent.triggerKeywordGroups()) {
            Optional<String> match = matchingTrigger(triggerable, keywords);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> matchingTrigger(List<String> triggerable, List<String> keywords) {
        for (String keyword : keywords) {
            Optional<String> match = triggerable.stream()
                    .filter(name -> name.toLowerCase(java.util.Locale.ROOT).contains(keyword))
                    .findFirst();
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private boolean shouldForceRetrigger(String trigger, DisguiseAnimationIntent intent, int tick, String controller, Map<String, Integer> forcedTicks) {
        if (!oneShotTrigger(trigger) || !intent.attacking() || intent.swingTime() > 1) {
            return false;
        }
        Integer previousTick = forcedTicks.put(controller, tick);
        return previousTick == null || previousTick != tick;
    }

    private static boolean oneShotTrigger(String trigger) {
        String normalized = trigger.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("death")
                || normalized.contains("dead")
                || normalized.contains("die")
                || normalized.contains("hurt")
                || normalized.contains("damage")
                || normalized.contains("hit")
                || normalized.contains("attack")
                || normalized.contains("bite")
                || normalized.contains("claw")
                || normalized.contains("tail")
                || normalized.contains("roar")
                || normalized.contains("shoot")
                || normalized.contains("cannon")
                || normalized.contains("leap")
                || normalized.contains("spit")
                || normalized.contains("slam")
                || normalized.contains("use")
                || normalized.contains("eat")
                || normalized.contains("drink")
                || normalized.contains("cast");
    }

    private List<GeckoControllerSnapshot> controllersFromVisualData(CompoundTag visualData) {
        if (visualData.contains("gecko_controller_states", Tag.TAG_COMPOUND)) {
            return controllersFromStateTag(visualData.getCompound("gecko_controller_states"));
        }
        if (visualData.contains("gecko_controllers", Tag.TAG_COMPOUND)) {
            return visualData.getCompound("gecko_controllers").getAllKeys().stream()
                    .sorted(Comparator.comparingInt(GeckoDisguiseAnimationAdapter::numericKey))
                    .map(key -> visualData.getCompound("gecko_controllers").getString(key))
                    .filter(name -> !name.isBlank())
                    .map(GeckoControllerSnapshot::new)
                    .toList();
        }
        return List.of();
    }

    private List<GeckoControllerSnapshot> controllersFromStateTag(CompoundTag states) {
        return states.getAllKeys().stream()
                .sorted(Comparator.comparingInt(GeckoDisguiseAnimationAdapter::numericKey))
                .filter(key -> states.contains(key, Tag.TAG_COMPOUND))
                .map(key -> controllerFromTag(states.getCompound(key)))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<GeckoControllerSnapshot> controllerFromTag(CompoundTag tag) {
        if (!tag.contains("name", Tag.TAG_STRING)) {
            return Optional.empty();
        }
        String name = tag.getString("name");
        String state = tag.contains("state", Tag.TAG_STRING) ? tag.getString("state") : "UNKNOWN";
        List<String> animations = tag.contains("animations", Tag.TAG_COMPOUND) ? stringList(tag.getCompound("animations")) : List.of();
        List<String> triggerableAnimations = tag.contains("triggerable_animations", Tag.TAG_COMPOUND) ? stringList(tag.getCompound("triggerable_animations")) : List.of();
        double speed = tag.contains("speed", Tag.TAG_DOUBLE) ? tag.getDouble("speed") : 1D;
        double transition = tag.contains("transition", Tag.TAG_DOUBLE) ? tag.getDouble("transition") : 0D;
        return Optional.of(new GeckoControllerSnapshot(name, state, animations, triggerableAnimations, speed, transition, "", false));
    }

    private List<String> stringList(CompoundTag tag) {
        return tag.getAllKeys().stream()
                .sorted(Comparator.comparingInt(GeckoDisguiseAnimationAdapter::numericKey))
                .filter(key -> tag.contains(key, Tag.TAG_STRING))
                .map(tag::getString)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private static int numericKey(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }
}
