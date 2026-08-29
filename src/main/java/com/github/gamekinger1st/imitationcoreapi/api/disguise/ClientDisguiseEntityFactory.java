package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ClientDisguiseEntityFactory {
    private static final int MAX_CACHED_ENTITIES = 256;
    private final Map<UUID, CachedEntity> entities = new LinkedHashMap<>();

    public synchronized Optional<Entity> createOrUpdate(ClientLevel level, Entity subject, ClientDisguiseState state) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(state, "state");
        CachedEntity cached = entities.get(state.sessionId());
        boolean reusable = cached != null
                && cached.snapshotId().equals(state.snapshotId())
                && cached.playerProfile().equals(state.playerProfile());
        Entity entity = reusable ? cached.entity() : create(level, state).orElse(null);
        if (entity == null) {
            return Optional.empty();
        }
        entity.setId(disguiseEntityId(state.sessionId()));
        if ((cached == null || cached.entity() != entity || cached.revision() != state.revision()) && !(entity instanceof RemotePlayer)) {
            try {
                entity.load(state.entityData());
            } catch (RuntimeException exception) {
                entities.remove(state.sessionId());
                return Optional.empty();
            }
        }
        entity.moveTo(subject.getX(), subject.getY(), subject.getZ(), subject.getYRot(), subject.getXRot());
        entity.tickCount = subject.tickCount;
        entity.setDeltaMovement(subject.getDeltaMovement());
        entity.setPose(subject.getPose());
        applyVisualData(entity, state.visualData());
        entities.put(state.sessionId(), new CachedEntity(entity, state.ownerId(), state.snapshotId(), state.revision(), state.playerProfile()));
        while (entities.size() > MAX_CACHED_ENTITIES) {
            entities.remove(entities.keySet().iterator().next());
        }
        return Optional.of(entity);
    }

    public synchronized void clear(UUID sessionId) {
        entities.remove(Objects.requireNonNull(sessionId, "sessionId"));
    }

    public synchronized void clearByOwner(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        entities.entrySet().removeIf(entry -> entry.getValue().ownerId().equals(ownerId));
    }

    public synchronized Set<UUID> sessionIdsForOwner(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        return entities.entrySet().stream()
                .filter(entry -> entry.getValue().ownerId().equals(ownerId))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public synchronized void clearAll() {
        entities.clear();
    }

    private Optional<Entity> create(ClientLevel level, ClientDisguiseState state) {
        if (state.entityType().equals(net.minecraft.resources.ResourceLocation.withDefaultNamespace("player"))) {
            GameProfile profile = state.playerProfile()
                    .map(PlayerDisguiseProfile::toGameProfile)
                    .orElseGet(() -> new GameProfile(state.snapshotId(), "Imitator"));
            return Optional.of(new ProfiledRemotePlayer(level, profile));
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(state.entityType());
        return type == null ? Optional.empty() : Optional.ofNullable(type.create(level));
    }

    private static int disguiseEntityId(UUID sessionId) {
        return -((sessionId.hashCode() & 0x3FFFFFFF) + 1);
    }

    private void applyVisualData(Entity entity, CompoundTag visualData) {
        if (visualData.getBoolean("has_custom_name") && visualData.contains("custom_name", Tag.TAG_STRING)) {
            entity.setCustomName(Component.literal(visualData.getString("custom_name")));
            entity.setCustomNameVisible(visualData.getBoolean("custom_name_visible"));
        }
        if (entity instanceof LivingEntity living && visualData.contains("equipment", Tag.TAG_COMPOUND)) {
            CompoundTag equipment = visualData.getCompound("equipment");
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!equipment.contains(slot.getName(), Tag.TAG_COMPOUND)) {
                    continue;
                }
                itemStack(equipment.getCompound(slot.getName())).ifPresent(stack -> living.setItemSlot(slot, stack));
            }
        }
    }

    private Optional<ItemStack> itemStack(CompoundTag tag) {
        if (tag.contains("stack", Tag.TAG_COMPOUND)) {
            ItemStack parsed = ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), tag.getCompound("stack"));
            if (!parsed.isEmpty()) {
                return Optional.of(parsed);
            }
        }
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
        if (tag.getBoolean("enchanted")) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return Optional.of(stack);
    }

    private record CachedEntity(Entity entity, UUID ownerId, UUID snapshotId, long revision, Optional<PlayerDisguiseProfile> playerProfile) {
    }

    private static final class ProfiledRemotePlayer extends RemotePlayer {
        private final PlayerSkin fallbackSkin;
        private final CompletableFuture<PlayerSkin> loadedSkin;

        private ProfiledRemotePlayer(ClientLevel level, GameProfile profile) {
            super(level, profile);
            fallbackSkin = DefaultPlayerSkin.get(profile);
            loadedSkin = profile.getProperties().containsKey("textures")
                    ? Minecraft.getInstance().getSkinManager().getOrLoad(profile)
                    : CompletableFuture.completedFuture(fallbackSkin);
        }

        @Override
        public PlayerSkin getSkin() {
            return loadedSkin.getNow(fallbackSkin);
        }
    }
}
