package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

import com.github.gamekinger1st.imitationcoreapi.api.adapter.AdapterKind;
import com.github.gamekinger1st.imitationcoreapi.api.adapter.ImitationAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.adapter.ImitationAdapterRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.event.TransformationEvent;
import com.github.gamekinger1st.imitationcoreapi.api.event.TransformationEventType;
import com.github.gamekinger1st.imitationcoreapi.api.gecko.GeckoAnimationSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.gecko.GeckoControllerSnapshot;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class SnapshotCaptureService {
    private final ImitationAdapterRegistry adapters;
    private final SnapshotLimits limits;

    public SnapshotCaptureService(ImitationAdapterRegistry adapters, SnapshotLimits limits) {
        this.adapters = Objects.requireNonNull(adapters, "adapters");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public SnapshotCaptureResult capture(Entity subject, Optional<UUID> requesterId, long capturedGameTime) {
        SnapshotCaptureContext context = new SnapshotCaptureContext(subject, requesterId, capturedGameTime, limits);
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(subject.getType());
        if (entityType == null) {
            throw new SnapshotCaptureException("The target entity type is not registered");
        }
        CompoundTag entityData = SnapshotNbtSanitizer.sanitizeEntityData(subject.saveWithoutId(new CompoundTag()));
        CompoundTag visualData = new CompoundTag();
        visualData.putString("pose", subject.getPose().name());
        visualData.putFloat("y_rot", subject.getYRot());
        visualData.putFloat("x_rot", subject.getXRot());
        visualData.putFloat("eye_height", subject.getEyeHeight());
        visualData.putFloat("bb_width", subject.getBbWidth());
        visualData.putFloat("bb_height", subject.getBbHeight());
        visualData.putBoolean("has_custom_name", subject.hasCustomName());
        visualData.putBoolean("custom_name_visible", subject.isCustomNameVisible());
        visualData.putBoolean("glowing", subject.isCurrentlyGlowing());
        visualData.putBoolean("silent", subject.isSilent());
        visualData.putBoolean("crouching", subject.isCrouching());
        visualData.putBoolean("sprinting", subject.isSprinting());
        visualData.putBoolean("swimming", subject.isSwimming());
        if (subject.hasCustomName() && subject.getCustomName() != null) {
            visualData.putString("custom_name", subject.getCustomName().getString());
        }
        if (subject instanceof LivingEntity living) {
            visualData.putFloat("health", living.getHealth());
            visualData.putFloat("max_health", living.getMaxHealth());
            visualData.put("attributes", attributeData(living));
            visualData.putFloat("walk_animation_speed", living.walkAnimation.speed(1.0F));
            visualData.putBoolean("using_item", living.isUsingItem());
            visualData.putBoolean("fall_flying", living.isFallFlying());
            visualData.putString("main_arm", living.getMainArm().name());
            visualData.put("equipment", equipmentData(living));
        }
        ImitationApi.geckoAnimations().capture(subject).ifPresent(snapshot -> {
            visualData.put("gecko_controllers", geckoControllerNames(snapshot));
            visualData.put("gecko_controller_states", geckoControllerStates(snapshot));
            visualData.putInt("gecko_controller_count", snapshot.controllers().size());
        });
        IdentitySnapshot.Builder builder = IdentitySnapshot.builder(entityType, capturedGameTime)
                .displayName(subject.getDisplayName().getString())
                .entityData(entityData)
                .visualData(SnapshotNbtSanitizer.sanitizeVisualData(visualData));
        CompatibilityAssessment assessment = CompatibilityAssessment.full();
        for (ImitationAdapter adapter : adapters.adapters(AdapterKind.SNAPSHOT)) {
            if (!(adapter instanceof SnapshotCaptureAdapter captureAdapter)) {
                continue;
            }
            CompatibilityAssessment adapterAssessment = Objects.requireNonNull(captureAdapter.assess(builder.build(limits)), "adapter assessment");
            assessment = assessment.combine(adapterAssessment);
            if (!assessment.level().isUsable()) {
                throw new SnapshotCaptureException("Snapshot capture is unsupported: " + String.join("; ", assessment.reasons()));
            }
            try {
                captureAdapter.capture(context, builder);
            } catch (RuntimeException exception) {
                throw new SnapshotCaptureException("Snapshot capture adapter failed: " + captureAdapter.id(), exception);
            }
        }
        IdentitySnapshot snapshot = builder.build(limits);
        if (adapters.adapters(AdapterKind.GAMEPLAY).isEmpty()) {
            assessment = assessment.combine(CompatibilityAssessment.visual("No gameplay adapter has been applied"));
        } else {
            assessment = assessment.combine(adapters.assess(snapshot, java.util.List.of(AdapterKind.GAMEPLAY)));
        }
        ImitationApi.events().post(new TransformationEvent(TransformationEventType.SNAPSHOT_CAPTURED, requesterId, Optional.of(snapshot), Optional.empty(), Optional.empty()));
        return new SnapshotCaptureResult(snapshot, assessment);
    }

    private static CompoundTag equipmentData(LivingEntity living) {
        CompoundTag equipment = new CompoundTag();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = living.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId == null) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("item", itemId.toString());
            tag.putInt("count", stack.getCount());
            tag.putInt("damage", stack.getDamageValue());
            tag.putBoolean("enchanted", stack.isEnchanted());
            equipment.put(slot.getName(), tag);
        }
        return equipment;
    }

    private static CompoundTag attributeData(LivingEntity living) {
        CompoundTag attributes = new CompoundTag();
        primePhysicalAttributes(living);
        ListTag values = living.getAttributes().save();
        attributes.put("values", values.copy());
        return attributes;
    }

    private static void primePhysicalAttributes(LivingEntity living) {
        for (Holder<Attribute> attribute : physicalAttributes()) {
            living.getAttribute(attribute);
        }
    }

    private static java.util.List<Holder<Attribute>> physicalAttributes() {
        return java.util.List.of(
                Attributes.MAX_HEALTH,
                Attributes.ARMOR,
                Attributes.ARMOR_TOUGHNESS,
                Attributes.ATTACK_DAMAGE,
                Attributes.ATTACK_KNOCKBACK,
                Attributes.ATTACK_SPEED,
                Attributes.BLOCK_BREAK_SPEED,
                Attributes.BLOCK_INTERACTION_RANGE,
                Attributes.ENTITY_INTERACTION_RANGE,
                Attributes.FALL_DAMAGE_MULTIPLIER,
                Attributes.FLYING_SPEED,
                Attributes.FOLLOW_RANGE,
                Attributes.GRAVITY,
                Attributes.JUMP_STRENGTH,
                Attributes.KNOCKBACK_RESISTANCE,
                Attributes.MOVEMENT_SPEED,
                Attributes.OXYGEN_BONUS,
                Attributes.SAFE_FALL_DISTANCE,
                Attributes.SCALE,
                Attributes.SNEAKING_SPEED,
                Attributes.STEP_HEIGHT,
                Attributes.SUBMERGED_MINING_SPEED,
                Attributes.SWEEPING_DAMAGE_RATIO,
                Attributes.WATER_MOVEMENT_EFFICIENCY
        );
    }

    private static CompoundTag geckoControllerNames(GeckoAnimationSnapshot snapshot) {
        CompoundTag controllers = new CompoundTag();
        for (int index = 0; index < snapshot.controllerNames().size(); index++) {
            controllers.putString(Integer.toString(index), snapshot.controllerNames().get(index));
        }
        return controllers;
    }

    private static CompoundTag geckoControllerStates(GeckoAnimationSnapshot snapshot) {
        CompoundTag states = new CompoundTag();
        for (int index = 0; index < snapshot.controllers().size(); index++) {
            GeckoControllerSnapshot controller = snapshot.controllers().get(index);
            CompoundTag tag = new CompoundTag();
            tag.putString("name", controller.controllerName());
            tag.putString("state", controller.animationState());
            tag.putDouble("speed", controller.animationSpeed());
            tag.putDouble("transition", controller.transitionLength());
            tag.put("animations", stringList(controller.animationNames()));
            tag.put("triggerable_animations", stringList(controller.triggerableAnimationNames()));
            states.put(Integer.toString(index), tag);
        }
        return states;
    }

    private static CompoundTag stringList(java.util.List<String> values) {
        CompoundTag tag = new CompoundTag();
        for (int index = 0; index < values.size(); index++) {
            tag.putString(Integer.toString(index), values.get(index));
        }
        return tag;
    }
}
