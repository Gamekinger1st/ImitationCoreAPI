package com.github.gamekinger1st.imitationcoreapi.internal.replica;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.application.TemporaryStateDefinition;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationContext;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationReversionContext;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorReplicaPolicy;
import com.github.gamekinger1st.imitationcoreapi.api.replica.ReplicaEntityTags;
import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateStatus;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        TemporaryStateReference reference = new TemporaryStateReference(UUID.randomUUID(), context.session().sessionId(), id(), TemporaryStateKinds.REPLICA_ENTITY, payload, TemporaryStateStatus.PREPARED);
        SessionTransitionResult added = transformations.addTemporaryState(context.session().sessionId(), reference, context.gameTime());
        if (!added.accepted()) {
            replica.discard();
            throw new IllegalStateException(added.message());
        }
        if (!level.addFreshEntity(replica)) {
            replica.discard();
            throw new IllegalStateException("Replica entity could not be added to the level");
        }
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
        if (entity.isEmpty() || entity.get().isRemoved() || !entity.get().isAlive()) {
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
            return Optional.of(EntityType.ZOMBIE);
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(snapshot.entityType())
                .filter(type -> type != EntityType.PLAYER || policy.fallbackPlayerForms())
                .map(type -> type == EntityType.PLAYER ? EntityType.ZOMBIE : type);
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
        replica.moveTo(spawn.x, context.owner().getY(), spawn.z, context.owner().getYRot() + 180F, 0F);
        replica.fallDistance = 0F;
        replica.setDeltaMovement(Vec3.ZERO);
    }

    private static void applyVisualState(LivingEntity replica, IdentitySnapshot snapshot, double scale, ImitatorReplicaPolicy policy) {
        CompoundTag visualData = snapshot.visualData();
        if (!policy.namePrefix().isEmpty() || !snapshot.displayName().isBlank()) {
            replica.setCustomName(Component.literal(policy.namePrefix() + snapshot.displayName()));
            replica.setCustomNameVisible(true);
        }
        if (visualData.contains("equipment", Tag.TAG_COMPOUND)) {
            CompoundTag equipment = visualData.getCompound("equipment");
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (equipment.contains(slot.getName(), Tag.TAG_COMPOUND)) {
                    itemStack(equipment.getCompound(slot.getName())).ifPresent(stack -> replica.setItemSlot(slot, stack));
                }
            }
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

    private static void suppressEquipmentDrops(Mob mob) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0F);
        }
    }

    private static Optional<ItemStack> itemStack(CompoundTag tag) {
        if (!tag.contains("item", Tag.TAG_STRING)) {
            return Optional.empty();
        }
        ResourceLocation itemId = ResourceLocation.tryParse(tag.getString("item"));
        if (itemId == null) {
            return Optional.empty();
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            return Optional.empty();
        }
        ItemStack stack = new ItemStack(item, Math.max(1, Math.min(99, tag.getInt("count"))));
        if (stack.isDamageableItem()) {
            stack.setDamageValue(Math.max(0, tag.getInt("damage")));
        }
        return Optional.of(stack);
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
