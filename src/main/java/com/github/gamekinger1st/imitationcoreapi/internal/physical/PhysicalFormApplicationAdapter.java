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
    private static final String ACTIVE_FORM_TAG = "ImitationCoreAPI.ActiveForm";

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
        payload.put("attributes", owner.getAttributes().save());
        payload.putFloat("health", owner.getHealth());
        payload.putFloat("max_health", owner.getMaxHealth());
        payload.putFloat("absorption", owner.getAbsorptionAmount());
        payload.putInt("air", owner.getAirSupply());
        payload.putInt("fire", owner.getRemainingFireTicks());
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
        if (attributes.isEmpty()) {
            return;
        }
        float baselineHealth = Math.max(1.0F, baseline.payload().getFloat("health"));
        float baselineMaxHealth = Math.max(1.0F, baseline.payload().getFloat("max_health"));
        double healthRatio = Math.max(0.0D, Math.min(1.0D, baselineHealth / baselineMaxHealth));
        restoreAttributes(owner, attributes);
        float transformedHealth = (float) Math.max(1.0D, Math.min(owner.getMaxHealth(), owner.getMaxHealth() * healthRatio));
        owner.setHealth(transformedHealth);
    }

    @Override
    public void revert(TransformationReversionContext context, List<TemporaryStateReference> temporaryState) {
        Optional<ServerPlayer> owner = context.owner();
        if (owner.isEmpty()) {
            return;
        }
        Optional<TemporaryStateReference> baseline = physicalReference(temporaryState);
        if (baseline.isEmpty()) {
            return;
        }
        CompoundTag payload = baseline.get().payload();
        if (payload.contains("attributes", Tag.TAG_LIST)) {
            restoreAttributes(owner.get(), payload.getList("attributes", Tag.TAG_COMPOUND));
        }
        float maxHealth = Math.max(1.0F, owner.get().getMaxHealth());
        owner.get().setHealth(Math.max(1.0F, Math.min(maxHealth, payload.getFloat("health"))));
        owner.get().setAbsorptionAmount(Math.max(0.0F, payload.getFloat("absorption")));
        owner.get().setAirSupply(payload.getInt("air"));
        owner.get().setRemainingFireTicks(payload.getInt("fire"));
        restoreActiveFormMarker(owner.get(), payload);
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

    private static void restoreAttributes(LivingEntity entity, ListTag attributes) {
        clearAttributeModifiers(entity);
        entity.getAttributes().load(attributes);
        applyBaseAttributeValues(entity, attributes);
    }

    private static void clearAttributeModifiers(LivingEntity entity) {
        ListTag attributes = entity.getAttributes().save();
        for (int index = 0; index < attributes.size(); index++) {
            ResourceLocation id = ResourceLocation.tryParse(attributes.getCompound(index).getString("id"));
            if (id == null) {
                continue;
            }
            BuiltInRegistries.ATTRIBUTE.getHolder(id).ifPresent(attribute -> {
                AttributeInstance instance = entity.getAttribute(attribute);
                if (instance != null) {
                    instance.removeModifiers();
                }
            });
        }
    }

    private static void applyBaseAttributeValues(LivingEntity entity, ListTag attributes) {
        for (int index = 0; index < attributes.size(); index++) {
            CompoundTag attribute = attributes.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(attribute.getString("id"));
            if (id == null || !attribute.contains("base", Tag.TAG_DOUBLE)) {
                continue;
            }
            BuiltInRegistries.ATTRIBUTE.getHolder(id).ifPresent(holder -> {
                AttributeInstance instance = entity.getAttribute(holder);
                if (instance != null) {
                    instance.setBaseValue(attribute.getDouble("base"));
                }
            });
        }
    }

    private static void writeActiveFormMarker(ServerPlayer owner, TransformationApplicationContext context) {
        CompoundTag marker = new CompoundTag();
        marker.putString("entity_type", context.snapshot().entityType().toString());
        marker.putString("scope", context.session().scope().name());
        marker.putBoolean("changes_owner_presentation", context.session().scope().changesOwnerPresentation());
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
