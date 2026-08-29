package com.github.gamekinger1st.imitationcoreapi.api.targeting;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnosticCategory;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnosticSeverity;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnostics;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MobImitationTargetingService {
    public static final double DEFAULT_RECONCILIATION_RANGE = 32D;
    private static final String ACTIVE_FORM_TAG = "ImitationCoreAPI.ActiveForm";
    private static final Map<ResourceLocation, Boolean> MOB_FORM_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> FACTION_RESOLVER_DIAGNOSTICS = ConcurrentHashMap.newKeySet();
    private static final net.minecraft.tags.TagKey<EntityType<?>> TENSURA_HOSTILE = entityTag("tensura", "hostile_monster");
    private static final net.minecraft.tags.TagKey<EntityType<?>> TENSURA_NEUTRAL = entityTag("tensura", "neutral_monster");
    private static final net.minecraft.tags.TagKey<EntityType<?>> TENSURA_ANIMAL_PREY = entityTag("tensura", "animal_prey");

    private final TransformationService transformations;
    private final MobFactionRegistry factions;

    public MobImitationTargetingService(TransformationService transformations) {
        this(transformations, ImitationApi.mobFactions());
    }

    public MobImitationTargetingService(TransformationService transformations, MobFactionRegistry factions) {
        this.transformations = Objects.requireNonNull(transformations, "transformations");
        this.factions = Objects.requireNonNull(factions, "factions");
    }

    public boolean shouldSuppress(Mob aggressor, ServerPlayer target) {
        Objects.requireNonNull(aggressor, "aggressor");
        Objects.requireNonNull(target, "target");
        Optional<ResourceLocation> formType = imitatedMobType(target);
        if (formType.isEmpty()) {
            return false;
        }
        if (isRetaliatingAgainst(aggressor, target)) {
            return false;
        }
        ResourceLocation aggressorType = BuiltInRegistries.ENTITY_TYPE.getKey(aggressor.getType());
        if (aggressorType == null) {
            return true;
        }
        MobFactionResolution aggressorFaction = factions.resolveWithStatus(aggressorType);
        MobFactionResolution formFaction = factions.resolveWithStatus(formType.get());
        diagnoseFallbackFaction(target, aggressorFaction, formFaction);
        if (isNaturalPrey(aggressorType, formType.get())) {
            return false;
        }
        return shouldSuppressResolvedTarget(aggressorFaction, formFaction, false);
    }

    public int clearExistingTargets(ServerPlayer target) {
        return clearExistingTargets(target, DEFAULT_RECONCILIATION_RANGE);
    }

    public int clearExistingTargets(ServerPlayer target, double range) {
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(range) || range <= 0D || range > 256D) {
            throw new IllegalArgumentException("range must be between zero and 256");
        }
        if (imitatedMobType(target).isEmpty()) {
            return 0;
        }
        int cleared = 0;
        for (Mob mob : target.level().getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(range), mob -> targetsPlayer(mob, target) && shouldSuppress(mob, target))) {
            clearSuppressedTarget(mob, target);
            cleared++;
        }
        return cleared;
    }

    public boolean clearSuppressedTarget(Mob aggressor, ServerPlayer target) {
        Objects.requireNonNull(aggressor, "aggressor");
        Objects.requireNonNull(target, "target");
        if (!targetsPlayer(aggressor, target) || !shouldSuppress(aggressor, target)) {
            return false;
        }
        aggressor.setTarget(null);
        if (hasAttackTargetMemory(aggressor)) {
            aggressor.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
        aggressor.getNavigation().stop();
        return true;
    }

    public boolean sharesFaction(ResourceLocation aggressorType, ResourceLocation formType) {
        Objects.requireNonNull(aggressorType, "aggressorType");
        Objects.requireNonNull(formType, "formType");
        return factions.resolveWithStatus(aggressorType).factionId().equals(factions.resolveWithStatus(formType).factionId());
    }

    public boolean shouldSuppressResolvedTarget(ResourceLocation aggressorType, ResourceLocation formType, boolean retaliating) {
        Objects.requireNonNull(aggressorType, "aggressorType");
        Objects.requireNonNull(formType, "formType");
        return shouldSuppressResolvedTarget(factions.resolveWithStatus(aggressorType), factions.resolveWithStatus(formType), retaliating);
    }

    public Optional<ResourceLocation> imitatedMobType(ServerPlayer target) {
        Optional<ResourceLocation> markerType = activeFormType(target)
                .filter(entityType -> isMobForm(entityType, target.serverLevel()));
        if (markerType.isPresent()) {
            return markerType;
        }
        return transformations.activeSessionForOwner(target.getUUID())
                .filter(session -> session.scope() != TransformationScope.REPLICA)
                .flatMap(session -> transformations.snapshot(session.snapshotId()))
                .filter(snapshot -> isMobForm(snapshot, target.serverLevel()))
                .map(IdentitySnapshot::entityType);
    }

    private static Optional<ResourceLocation> activeFormType(ServerPlayer target) {
        if (!target.getPersistentData().contains(ACTIVE_FORM_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag marker = target.getPersistentData().getCompound(ACTIVE_FORM_TAG);
        if (!marker.getBoolean("changes_owner_presentation")) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(marker.getString("entity_type")));
    }

    private static boolean isMobForm(IdentitySnapshot snapshot, ServerLevel level) {
        return isMobForm(snapshot.entityType(), level);
    }

    private static boolean isMobForm(ResourceLocation entityType, ServerLevel level) {
        return MOB_FORM_CACHE.computeIfAbsent(entityType, value -> BuiltInRegistries.ENTITY_TYPE.getOptional(value)
                .map(type -> {
                    if (Mob.class.isAssignableFrom(type.getBaseClass())) {
                        return true;
                    }
                    Entity entity;
                    try {
                        entity = type.create(level);
                    } catch (RuntimeException | LinkageError exception) {
                        return false;
                    }
                    try {
                        return entity instanceof Mob;
                    } finally {
                        if (entity != null) {
                            entity.discard();
                        }
                    }
                })
                .orElse(false));
    }

    private static boolean isRetaliatingAgainst(Mob aggressor, ServerPlayer target) {
        if (aggressor instanceof NeutralMob neutralMob && neutralMob.isAngryAt(target)) {
            return true;
        }
        if (aggressor.getLastHurtByMob() != target) {
            return false;
        }
        int lastHurtTick = aggressor.getLastHurtByMobTimestamp();
        return lastHurtTick <= 0 || aggressor.tickCount - lastHurtTick <= 20 * 30;
    }

    private static boolean shouldSuppressResolvedTarget(MobFactionResolution aggressorFaction, MobFactionResolution formFaction, boolean retaliating) {
        if (retaliating) {
            return false;
        }
        return aggressorFaction.factionId().equals(formFaction.factionId());
    }

    private static boolean isNaturalPrey(ResourceLocation aggressorType, ResourceLocation formType) {
        Optional<EntityType<?>> aggressor = BuiltInRegistries.ENTITY_TYPE.getOptional(aggressorType);
        Optional<EntityType<?>> form = BuiltInRegistries.ENTITY_TYPE.getOptional(formType);
        return aggressor.filter(type -> type.is(TENSURA_HOSTILE) || type.is(TENSURA_NEUTRAL)).isPresent()
                && form.filter(type -> type.is(TENSURA_ANIMAL_PREY)).isPresent();
    }

    private static net.minecraft.tags.TagKey<EntityType<?>> entityTag(String namespace, String path) {
        return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static void diagnoseFallbackFaction(ServerPlayer target, MobFactionResolution aggressorFaction, MobFactionResolution formFaction) {
        if (!aggressorFaction.usedFallback() && !formFaction.usedFallback()) {
            return;
        }
        if (FACTION_RESOLVER_DIAGNOSTICS.size() > 1024) {
            FACTION_RESOLVER_DIAGNOSTICS.clear();
        }
        String key = target.getUUID() + "|" + aggressorFaction.entityType() + "|" + formFaction.entityType();
        if (!FACTION_RESOLVER_DIAGNOSTICS.add(key)) {
            return;
        }
        ImitationDiagnostics.publish(
                ImitationDiagnosticCategory.FACTION_RESOLVER_MISSING,
                ImitationDiagnosticSeverity.WARNING,
                "Faction resolver fallback was used for " + aggressorFaction.entityType() + " against copied form " + formFaction.entityType(),
                Optional.of(target.getUUID()),
                Optional.empty(),
                Optional.empty(),
                Optional.of(formFaction.entityType()),
                target.level().getGameTime()
        );
    }

    private static boolean targetsPlayer(Mob aggressor, ServerPlayer target) {
        if (aggressor.getTarget() == target) {
            return true;
        }
        if (!hasAttackTargetMemory(aggressor)) {
            return false;
        }
        return aggressor.getBrain()
                .getMemory(MemoryModuleType.ATTACK_TARGET)
                .map(target::equals)
                .orElse(false);
    }

    private static boolean hasAttackTargetMemory(Mob aggressor) {
        return aggressor.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED);
    }
}
