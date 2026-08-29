package com.github.gamekinger1st.imitationcoreapi.internal.targeting;

import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import com.github.gamekinger1st.imitationcoreapi.api.targeting.MobImitationTargetingService;
import com.github.gamekinger1st.imitationcoreapi.internal.config.ImitationCoreConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class ActiveFormTargetingHooks {
    private static final String ACTIVE_FORM_TAG = "ImitationCoreAPI.ActiveForm";

    private ActiveFormTargetingHooks() {
    }

    public static EntityType<?> effectiveType(LivingEntity entity) {
        Optional<ResourceLocation> id = effectiveTypeId(entity);
        if (id.isEmpty()) {
            return entity.getType();
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id.get()).orElse(entity.getType());
    }

    public static Optional<ResourceLocation> effectiveTypeId(LivingEntity entity) {
        if (!ImitationCoreConfig.mobTargetingEnabled()
                || !(entity instanceof ServerPlayer)
                || !entity.getPersistentData().contains(ACTIVE_FORM_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag marker = entity.getPersistentData().getCompound(ACTIVE_FORM_TAG);
        if (!marker.getBoolean("changes_owner_presentation")) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(marker.getString("entity_type")));
    }

    public static boolean shouldSuppressTarget(Mob aggressor, LivingEntity target) {
        if (!ImitationCoreConfig.mobTargetingEnabled() || !(target instanceof ServerPlayer player)) {
            return false;
        }
        return new MobImitationTargetingService(ImitationCoreServices.forServer(player.serverLevel().getServer())).shouldSuppress(aggressor, player);
    }

    public static Optional<Boolean> animalPreyDecision(LivingEntity subordinate, LivingEntity target) {
        Optional<ResourceLocation> formType = effectiveTypeId(target);
        if (formType.isEmpty()) {
            return Optional.empty();
        }
        EntityType<?> effectiveType = BuiltInRegistries.ENTITY_TYPE.getOptional(formType.get()).orElse(target.getType());
        net.minecraft.tags.TagKey<EntityType<?>> animalPrey = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath("tensura", "animal_prey")
        );
        return Optional.of(effectiveType != subordinate.getType() && effectiveType.is(animalPrey));
    }
}
