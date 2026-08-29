package com.github.gamekinger1st.imitationcoreapi.internal.physical;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.application.TemporaryStateDefinition;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationContext;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationReversionContext;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorHandlerService;
import com.github.gamekinger1st.imitationcoreapi.internal.tensura.TensuraEnergyTransitionService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.List;
import java.util.Optional;

public final class PhysicalFormApplicationAdapter implements TransformationApplicationAdapter {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "physical_form");
    private static final String ACTIVE_FORM_TAG = TensuraEnergyTransitionService.ACTIVE_FORM_TAG;
    private static final ResourceLocation PLAYER_ENTITY_TYPE = ResourceLocation.withDefaultNamespace("player");
    private static final ResourceLocation MAX_HEALTH = ResourceLocation.withDefaultNamespace("generic.max_health");
    private static final ResourceLocation MOVEMENT_SPEED = ResourceLocation.withDefaultNamespace("generic.movement_speed");
    private static final ResourceLocation JUMP_STRENGTH = ResourceLocation.withDefaultNamespace("generic.jump_strength");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return 150;
    }

    @Override
    public boolean appliesTo(TransformationScope scope) {
        return scope.changesOwnerPresentation();
    }

    @Override
    public Optional<String> validate(TransformationApplicationContext context) {
        return Optional.empty();
    }

    @Override
    public List<TemporaryStateDefinition> prepare(TransformationApplicationContext context) {
        ServerPlayer owner = context.owner();
        CompoundTag payload = new CompoundTag();
        if (context.session().scope().appliesGameplayState()) {
            payload.put("attributes", owner.getAttributes().save());
            payload.putFloat("health", owner.getHealth());
            payload.putFloat("max_health", owner.getMaxHealth());
            payload.putFloat("absorption", owner.getAbsorptionAmount());
            payload.putInt("air", owner.getAirSupply());
            payload.putInt("fire", owner.getRemainingFireTicks());
            TensuraEnergyTransitionService.captureBaseline(owner, payload);
        }
        CompoundTag persistentData = owner.getPersistentData();
        payload.putBoolean("had_active_form_marker", persistentData.contains(ACTIVE_FORM_TAG, Tag.TAG_COMPOUND));
        if (persistentData.contains(ACTIVE_FORM_TAG, Tag.TAG_COMPOUND)) {
            payload.put("active_form_marker", persistentData.getCompound(ACTIVE_FORM_TAG).copy());
        }
        return List.of(new TemporaryStateDefinition(TemporaryStateKinds.PHYSICAL_FORM, payload));
    }

    @Override
    public void apply(TransformationApplicationContext context, List<TemporaryStateReference> temporaryState) {
        if (temporaryState.stream().noneMatch(reference -> reference.kind().equals(TemporaryStateKinds.PHYSICAL_FORM))) {
            return;
        }
        CompoundTag visualData = context.snapshot().visualData();
        ListTag attributes = attributeValues(visualData);
        ServerPlayer owner = context.owner();
        TemporaryStateReference baseline = physicalReference(temporaryState).orElseThrow();
        writeActiveFormMarker(owner, context);
        owner.refreshDimensions();
        if (!context.session().scope().appliesGameplayState() || attributes.isEmpty()) {
            return;
        }
        float recordedHealth = Math.max(0.0F, visualData.getFloat("health"));
        float recordedMaxHealth = Math.max(1.0F, visualData.getFloat("max_health"));
        double healthRatio = Math.max(0.0D, Math.min(1.0D, recordedHealth / recordedMaxHealth));
        boolean sourceIsPlayer = context.snapshot().entityType().equals(PLAYER_ENTITY_TYPE);
        applyCopiedAttributes(owner, attributes, context.session().gameplayScale(), sourceIsPlayer);
        if (!context.session().baseline().playerData().getBoolean(ImitatorHandlerService.PERFECT_FORM_BASELINE_KEY)) {
            CompoundTag marker = owner.getPersistentData().getCompound(ACTIVE_FORM_TAG);
            TensuraEnergyTransitionService.begin(owner, marker, attributes, context.session().gameplayScale());
            owner.getPersistentData().put(ACTIVE_FORM_TAG, marker);
        }
        float transformedHealth = (float) Math.max(1.0D, Math.min(owner.getMaxHealth(), owner.getMaxHealth() * healthRatio));
        owner.setHealth(transformedHealth);
    }

    @Override
    public void revert(TransformationReversionContext context, List<TemporaryStateReference> temporaryState) {
        Optional<TemporaryStateReference> baseline = physicalReference(temporaryState);
        if (baseline.isEmpty()) {
            return;
        }
        ServerPlayer owner = context.owner().orElseThrow(() -> new IllegalStateException("Physical form reversion requires the owner to be online"));
        CompoundTag payload = baseline.get().payload();
        if (context.session().scope().appliesGameplayState() && payload.contains("attributes", Tag.TAG_LIST)) {
            restoreBaselineAttributes(owner, payload.getList("attributes", Tag.TAG_COMPOUND));
            TensuraEnergyTransitionService.restoreBaseline(owner, payload);
            float maxHealth = Math.max(1.0F, owner.getMaxHealth());
            owner.setHealth(Math.max(1.0F, Math.min(maxHealth, payload.getFloat("health"))));
            owner.setAbsorptionAmount(Math.max(0.0F, payload.getFloat("absorption")));
            owner.setAirSupply(payload.getInt("air"));
            owner.setRemainingFireTicks(payload.getInt("fire"));
        }
        restoreActiveFormMarker(owner, payload);
        owner.refreshDimensions();
    }

    private static Optional<TemporaryStateReference> physicalReference(List<TemporaryStateReference> temporaryState) {
        return temporaryState.stream()
                .filter(reference -> reference.kind().equals(TemporaryStateKinds.PHYSICAL_FORM))
                .findFirst();
    }

    private static ListTag attributeValues(CompoundTag visualData) {
        if (!visualData.contains("attributes", Tag.TAG_COMPOUND)) {
            return new ListTag();
        }
        CompoundTag attributes = visualData.getCompound("attributes");
        if (!attributes.contains("values", Tag.TAG_LIST)) {
            return new ListTag();
        }
        return attributes.getList("values", Tag.TAG_COMPOUND).copy();
    }

    public static void reapplyMobLocomotion(ServerPlayer owner, CompoundTag visualData, double scale) {
        ListTag attributes = attributeValues(visualData);
        double boundedScale = Math.max(0.0D, Math.min(1.0D, scale));
        for (int index = 0; index < attributes.size(); index++) {
            CompoundTag attribute = attributes.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(attribute.getString("id"));
            if (id == null || !attribute.contains("base", Tag.TAG_DOUBLE) || !locomotionAttribute(id)) {
                continue;
            }
            BuiltInRegistries.ATTRIBUTE.getHolder(id).map(owner::getAttribute)
                    .ifPresent(instance -> instance.setBaseValue(adaptedAttributeBase(id, copiedAttributeValue(attribute) * boundedScale, false)));
        }
    }

    public static void reapplyCopiedHealth(ServerPlayer owner, CompoundTag visualData, double scale) {
        if (!visualData.contains("health", Tag.TAG_FLOAT) || !visualData.contains("max_health", Tag.TAG_FLOAT)) {
            return;
        }
        double boundedScale = Math.max(0.0D, Math.min(1.0D, scale));
        float targetMax = (float)Math.max(1.0D, visualData.getFloat("max_health") * boundedScale);
        BuiltInRegistries.ATTRIBUTE.getHolder(MAX_HEALTH)
                .map(owner::getAttribute)
                .ifPresent(instance -> instance.setBaseValue(targetMax));
        double healthRatio = Math.max(0.0D, Math.min(1.0D, visualData.getFloat("health") / Math.max(1.0F, visualData.getFloat("max_health"))));
        owner.setHealth((float)Math.max(1.0D, Math.min(owner.getMaxHealth(), owner.getMaxHealth() * healthRatio)));
    }

    static double adaptedAttributeBase(ResourceLocation id, double value, boolean sourceIsPlayer) {
        if (sourceIsPlayer) {
            return value;
        }
        if (id.equals(MOVEMENT_SPEED)) {
            return value / 0.2D * 0.1D;
        }
        if (id.equals(JUMP_STRENGTH)) {
            return Math.max(value / 0.7D * 0.42D, 0.42D);
        }
        return value;
    }

    private static void applyCopiedAttributes(LivingEntity entity, ListTag attributes, double scale, boolean sourceIsPlayer) {
        double boundedScale = Math.max(0.0D, Math.min(1.0D, scale));
        boolean sprinting = entity.isSprinting();
        entity.setSprinting(false);
        for (int index = 0; index < attributes.size(); index++) {
            CompoundTag attribute = attributes.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(attribute.getString("id"));
            if (id == null || !attribute.contains("base", Tag.TAG_DOUBLE)) {
                continue;
            }
            BuiltInRegistries.ATTRIBUTE.getHolder(id).ifPresent(holder -> {
                AttributeInstance instance = entity.getAttribute(holder);
                if (instance != null) {
                    instance.removeModifiers();
                    instance.setBaseValue(adaptedAttributeBase(id, copiedAttributeValue(attribute) * boundedScale, sourceIsPlayer));
                }
            });
        }
        entity.setSprinting(false);
        if (sprinting) {
            entity.setSprinting(true);
        }
    }

    private static void restoreBaselineAttributes(LivingEntity entity, ListTag attributes) {
        boolean sprinting = entity.isSprinting();
        entity.setSprinting(false);
        entity.getAttributes().load(attributes);
        entity.setSprinting(false);
        if (sprinting) {
            entity.setSprinting(true);
        }
    }

    private static boolean locomotionAttribute(ResourceLocation id) {
        return id.equals(MOVEMENT_SPEED) || id.equals(JUMP_STRENGTH);
    }

    static double copiedAttributeValue(CompoundTag attribute) {
        return attribute.contains("value", Tag.TAG_DOUBLE) ? attribute.getDouble("value") : attribute.getDouble("base");
    }

    private static void writeActiveFormMarker(ServerPlayer owner, TransformationApplicationContext context) {
        CompoundTag marker = new CompoundTag();
        marker.putString("entity_type", context.snapshot().entityType().toString());
        marker.putString("scope", context.session().scope().name());
        marker.putBoolean("changes_owner_presentation", context.session().scope().changesOwnerPresentation());
        if (context.session().scope().appliesGameplayState()) {
            CompoundTag visualData = context.snapshot().visualData();
            if (visualData.contains("bb_width", Tag.TAG_FLOAT) && visualData.contains("bb_height", Tag.TAG_FLOAT)) {
                marker.putFloat("bb_width", visualData.getFloat("bb_width"));
                marker.putFloat("bb_height", visualData.getFloat("bb_height"));
                marker.putBoolean("physical_dimensions", true);
            }
        }
        if (context.session().scope() == TransformationScope.SURFACE) {
            marker.putBoolean("magicule_source_override", true);
            double sourceMagicules = TensuraStateExtensions.find(context.snapshot().extensions())
                    .map(state -> state.vitals().magicule())
                    .orElse(0.0D);
            marker.putDouble("source_magicules", Math.max(0.0D, sourceMagicules));
        }
        owner.getPersistentData().put(ACTIVE_FORM_TAG, marker);
    }

    private static void restoreActiveFormMarker(ServerPlayer owner, CompoundTag payload) {
        if (payload.getBoolean("had_active_form_marker") && payload.contains("active_form_marker", Tag.TAG_COMPOUND)) {
            owner.getPersistentData().put(ACTIVE_FORM_TAG, payload.getCompound("active_form_marker"));
        } else {
            owner.getPersistentData().remove(ACTIVE_FORM_TAG);
        }
    }
}
