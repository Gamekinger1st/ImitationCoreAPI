package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerBossEvent;
import net.neoforged.neoforge.common.Tags;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public final class ImitatorTargetEligibility {
    public static final TagKey<EntityType<?>> NON_COPYABLE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "non_copyable"));
    private static final TagKey<EntityType<?>> TENSURA_BOSSES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("tensura", "boss_for_hero"));

    private ImitatorTargetEligibility() {
    }

    public static Optional<String> rejection(Entity target) {
        if (!(target instanceof Player) && !(target instanceof Mob)) {
            return Optional.of("Only players and mobs can be copied");
        }
        return isBoss(target) ? Optional.of("Boss forms cannot be copied") : Optional.empty();
    }

    public static Optional<String> rejection(IdentitySnapshot snapshot, ServerLevel level) {
        if (snapshot.entityType().equals(ResourceLocation.withDefaultNamespace("player"))) {
            return Optional.empty();
        }
        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(snapshot.entityType()).orElse(null);
        if (type == null) {
            return Optional.of("The recorded entity type is no longer registered");
        }
        Entity entity;
        try {
            entity = type.create(level);
        } catch (RuntimeException | LinkageError exception) {
            return Optional.of("The recorded entity type cannot be reconstructed");
        }
        if (!(entity instanceof Mob)) {
            if (entity != null) {
                entity.discard();
            }
            return Optional.of("Only players and mobs can be copied");
        }
        try {
            entity.load(snapshot.entityData());
            return isBoss(entity) ? Optional.of("Boss forms cannot be copied") : Optional.empty();
        } catch (RuntimeException | LinkageError exception) {
            return Optional.of("The recorded mob could not be validated");
        } finally {
            entity.discard();
        }
    }

    public static boolean isBoss(Entity target) {
        EntityType<?> type = target.getType();
        if (type.is(NON_COPYABLE) || type.is(Tags.EntityTypes.BOSSES) || type.is(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED) || type.is(TENSURA_BOSSES)) {
            return true;
        }
        Optional<Boolean> declaredBossState = declaredBossState(target);
        if (declaredBossState.isPresent()) {
            return declaredBossState.get();
        }
        for (Class<?> current = target.getClass(); current != null && Entity.class.isAssignableFrom(current); current = current.getSuperclass()) {
            if (current.getSimpleName().toLowerCase(Locale.ROOT).contains("boss")) {
                return true;
            }
            for (Class<?> implemented : current.getInterfaces()) {
                if (implemented.getSimpleName().toLowerCase(Locale.ROOT).contains("boss")) {
                    return true;
                }
            }
            for (Field field : current.getDeclaredFields()) {
                if (ServerBossEvent.class.isAssignableFrom(field.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Optional<Boolean> declaredBossState(Entity target) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals("isBoss") || method.getParameterCount() != 0 || method.getReturnType() != boolean.class) {
                continue;
            }
            try {
                return Optional.of((boolean) method.invoke(target));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
