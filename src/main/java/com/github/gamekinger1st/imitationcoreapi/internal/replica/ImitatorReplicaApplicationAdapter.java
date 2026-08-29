package com.github.gamekinger1st.imitationcoreapi.internal.replica;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.application.TemporaryStateDefinition;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationContext;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationReversionContext;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorReplicaPolicy;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorSkillCopyExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorSkillCopySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.replica.ReplicaEntityTags;
import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateStatus;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillOperationResult;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillUpdateRequest;
import com.github.gamekinger1st.imitationcoreapi.api.skill.TemporarySkillOwnership;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateOperationResult;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateSnapshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ImitatorReplicaApplicationAdapter implements TransformationApplicationAdapter {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "imitator_replica");
    private static final ResourceLocation PLAYER = ResourceLocation.withDefaultNamespace("player");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public boolean appliesTo(TransformationScope scope) {
        return scope == TransformationScope.REPLICA;
    }

    @Override
    public Optional<String> validate(TransformationApplicationContext context) {
        return replicaType(context.snapshot(), policy(context))
                .filter(type -> createsLivingEntity(type, context.owner().serverLevel()))
                .map(type -> Optional.<String>empty())
                .orElseGet(() -> Optional.of("The selected form cannot produce a living replica"));
    }

    @Override
    public List<TemporaryStateDefinition> prepare(TransformationApplicationContext context) {
        return List.of();
    }

    @Override
    public void apply(TransformationApplicationContext context, List<TemporaryStateReference> temporaryState) {
        ImitatorReplicaPolicy policy = policy(context);
        ServerLevel level = context.owner().serverLevel();
        EntityType<?> type = replicaType(context.snapshot(), policy).orElseThrow(() -> new IllegalStateException("Replica entity type is unavailable"));
        Entity entity = type.create(level);
        if (!(entity instanceof LivingEntity replica)) {
            throw new IllegalStateException("Replica entity type did not create a living entity");
        }
        if (policy.copyEntityNbt()) {
            replica.load(context.snapshot().entityData());
        }
        positionReplica(context, replica, policy);
        applyVisualState(replica, context.snapshot(), context.session().gameplayScale(), policy);
        applyTensuraState(replica, context.snapshot(), context.session().gameplayScale(), policy);
        long expires = context.gameTime() + policy.lifetimeTicks();
        ReplicaEntityTags.mark(replica, context.owner().getUUID(), context.session().sessionId(), expires, policy.suppressDrops(), policy.suppressExperience());
        ReplicaEntityTags.setVisualEquipment(replica, visualEquipment(context.snapshot().visualData()));
        replica.addTag("imitationcoreapi_replica");
        if (replica instanceof Mob mob) {
            if (policy.persistentMob()) {
                mob.setPersistenceRequired();
            }
            if (policy.suppressDrops()) {
                suppressEquipmentDrops(mob);
            }
            if (policy.targetOwner()) {
                mob.setTarget(context.owner());
            }
        }
        CompoundTag payload = payload(replica, level, policy, expires);
        TransformationService transformations = ImitationCoreServices.forServer(context.server());
        UUID referenceId = UUID.randomUUID();
        TemporaryStateReference reference = new TemporaryStateReference(referenceId, context.session().sessionId(), id(), TemporaryStateKinds.REPLICA_ENTITY, payload, TemporaryStateStatus.PREPARED);
        SessionTransitionResult added = transformations.addTemporaryState(context.session().sessionId(), reference, context.gameTime());
        if (!added.accepted()) {
            replica.discard();
            throw new IllegalStateException(added.message());
        }
        if (!level.addFreshEntity(replica)) {
            replica.discard();
            throw new IllegalStateException("Replica entity could not be added to the level");
        }
        try {
            applyRecordedSkills(replica, context.snapshot(), policy, context.session().sessionId(), referenceId);
        } catch (RuntimeException | LinkageError exception) {
            replica.discard();
            throw exception;
        }
        com.github.gamekinger1st.imitationcoreapi.internal.network.ImitationCoreNetwork.syncReplicaVisuals(replica);
        SessionTransitionResult activated = transformations.updateTemporaryState(context.session().sessionId(), reference.referenceId(), TemporaryStateStatus.ACTIVE, context.gameTime());
        if (!activated.accepted()) {
            replica.discard();
            throw new IllegalStateException(activated.message());
        }
    }

    @Override
    public void revert(TransformationReversionContext context, List<TemporaryStateReference> temporaryState) {
        for (TemporaryStateReference reference : temporaryState) {
            if (!reference.kind().equals(TemporaryStateKinds.REPLICA_ENTITY)) {
                continue;
            }
            findReplica(context.server(), reference.payload()).ifPresent(entity -> {
                if (ReplicaEntityTags.sessionId(entity).filter(context.session().sessionId()::equals).isPresent()) {
                    entity.discard();
                }
            });
        }
    }

    public static boolean isReplicaReference(TemporaryStateReference reference) {
        return reference.handlerId().equals(ID) && reference.kind().equals(TemporaryStateKinds.REPLICA_ENTITY);
    }

    public static Optional<Entity> findReplica(MinecraftServer server, CompoundTag payload) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(payload, "payload");
        if (!payload.hasUUID("entity")) {
            return Optional.empty();
        }
        UUID entityId = payload.getUUID("entity");
        if (payload.contains("level", Tag.TAG_STRING)) {
            ResourceLocation levelId = ResourceLocation.tryParse(payload.getString("level"));
            if (levelId != null) {
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, levelId));
                if (level != null) {
                    Entity entity = level.getEntity(entityId);
                    if (entity != null) {
                        return Optional.of(entity);
                    }
                }
            }
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    public static boolean shouldCleanup(MinecraftServer server, TemporaryStateReference reference, long gameTime, LivingEntity owner) {
        if (!isReplicaReference(reference)) {
            return false;
        }
        CompoundTag payload = reference.payload();
        Optional<Entity> entity = findReplica(server, payload);
        if (entity.isEmpty()) {
            return false;
        }
        if (entity.get().isRemoved() || !entity.get().isAlive()) {
            return true;
        }
        long expires = payload.getLong("expires_game_time");
        if (expires > 0L && gameTime >= expires) {
            return true;
        }
        double cleanupDistance = payload.contains("cleanup_distance") ? payload.getDouble("cleanup_distance") : ImitatorReplicaPolicy.DEFAULT.cleanupDistance();
        if (entity.get().level() != owner.level()) {
            return true;
        }
        return owner.distanceToSqr(entity.get()) > cleanupDistance * cleanupDistance;
    }

    private static Optional<EntityType<?>> replicaType(IdentitySnapshot snapshot, ImitatorReplicaPolicy policy) {
        if (snapshot.entityType().equals(PLAYER) && policy.fallbackPlayerForms()) {
            return Optional.of(EntityType.ARMOR_STAND);
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(snapshot.entityType())
                .filter(type -> type != EntityType.PLAYER || policy.fallbackPlayerForms())
                .map(type -> type == EntityType.PLAYER ? EntityType.ARMOR_STAND : type);
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

    private static ImitatorReplicaPolicy policy(TransformationApplicationContext context) {
        return ImitatorReplicaPolicy.fromTag(context.session().baseline().playerData());
    }

    private static void positionReplica(TransformationApplicationContext context, LivingEntity replica, ImitatorReplicaPolicy policy) {
        Vec3 look = context.owner().getLookAngle();
        Vec3 spawn = context.owner().position().add(look.x * policy.spawnDistance(), 0D, look.z * policy.spawnDistance());
        if (!moveToSafePosition(context.owner().serverLevel(), replica, spawn, context.owner().getYRot() + 180F)) {
            throw new IllegalStateException("Replica could not find a safe spawn position");
        }
        replica.fallDistance = 0F;
        replica.setDeltaMovement(Vec3.ZERO);
    }

    private static boolean moveToSafePosition(ServerLevel level, LivingEntity replica, Vec3 preferred, float yaw) {
        int[][] offsets = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        for (int[] offset : offsets) {
            double x = preferred.x + offset[0];
            double z = preferred.z + offset[1];
            for (int vertical : new int[]{0, 1, -1, 2}) {
                replica.moveTo(x, preferred.y + vertical, z, yaw, 0F);
                if (level.getWorldBorder().isWithinBounds(replica.getBoundingBox()) && level.noCollision(replica)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void applyVisualState(LivingEntity replica, IdentitySnapshot snapshot, double scale, ImitatorReplicaPolicy policy) {
        CompoundTag visualData = snapshot.visualData();
        if (!policy.namePrefix().isEmpty() || !snapshot.displayName().isBlank()) {
            replica.setCustomName(Component.literal(policy.namePrefix() + snapshot.displayName()));
            replica.setCustomNameVisible(true);
        }
        double boundedScale = Math.max(0D, Math.min(1D, scale));
        if (visualData.contains("max_health", Tag.TAG_FLOAT) && replica.getAttribute(Attributes.MAX_HEALTH) != null) {
            double maxHealth = Math.max(1D, visualData.getFloat("max_health") * boundedScale);
            replica.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        }
        if (visualData.contains("health", Tag.TAG_FLOAT)) {
            replica.setHealth(Math.max(1F, Math.min(replica.getMaxHealth(), (float) (visualData.getFloat("health") * boundedScale))));
        }
    }

    private static CompoundTag visualEquipment(CompoundTag visualData) {
        return visualData.contains("equipment", Tag.TAG_COMPOUND) ? visualData.getCompound("equipment").copy() : new CompoundTag();
    }

    private static void applyTensuraState(LivingEntity replica, IdentitySnapshot snapshot, double scale, ImitatorReplicaPolicy policy) {
        if (!policy.copyTensuraState()) {
            return;
        }
        Optional<TensuraStateSnapshot> state = TensuraStateExtensions.find(snapshot.extensions());
        if (state.isEmpty()) {
            return;
        }
        TensuraStateOperationResult result = ImitationApi.tensuraStates().restoreScaled(replica, state.get(), Math.max(0D, Math.min(1D, scale)));
        if (!result.successful()) {
            throw new IllegalStateException(result.detail());
        }
    }

    private static void applyRecordedSkills(LivingEntity replica, IdentitySnapshot snapshot, ImitatorReplicaPolicy policy, UUID sessionId, UUID referenceId) {
        if (!policy.copyRecordedSkills()) {
            return;
        }
        Optional<ImitatorSkillCopySnapshot> captured = ImitatorSkillCopyExtensions.find(snapshot.extensions());
        if (captured.isEmpty()) {
            return;
        }
        int lifetimeSeconds = Math.max(1, (policy.lifetimeTicks() + 19) / 20);
        TemporarySkillOwnership ownership = new TemporarySkillOwnership(sessionId, referenceId);
        for (var skill : captured.get().skills()) {
            SkillOperationResult granted = ImitationApi.skillBridges().grantTemporary(replica, captured.get().bridgeId(), skill.skillId(), lifetimeSeconds, ownership);
            if (!granted.successful()) {
                throw new IllegalStateException("Replica could not receive copied skill " + skill.skillId() + ": " + granted.detail());
            }
            SkillOperationResult updated = ImitationApi.skillBridges().update(
                    replica,
                    captured.get().bridgeId(),
                    new SkillUpdateRequest(skill.skillId(), Optional.of(skill.mastery()), Optional.empty(), Optional.empty(), Optional.of(lifetimeSeconds))
            );
            if (!updated.successful()) {
                throw new IllegalStateException("Replica copied skill state could not be restored for " + skill.skillId() + ": " + updated.detail());
            }
        }
    }

    private static void suppressEquipmentDrops(Mob mob) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0F);
        }
    }

    private static CompoundTag payload(LivingEntity replica, ServerLevel level, ImitatorReplicaPolicy policy, long expires) {
        CompoundTag payload = new CompoundTag();
        payload.putUUID("entity", replica.getUUID());
        payload.putString("level", level.dimension().location().toString());
        payload.putLong("expires_game_time", expires);
        payload.putDouble("cleanup_distance", policy.cleanupDistance());
        payload.putBoolean("suppress_drops", policy.suppressDrops());
        payload.putBoolean("suppress_experience", policy.suppressExperience());
        return payload;
    }
}
