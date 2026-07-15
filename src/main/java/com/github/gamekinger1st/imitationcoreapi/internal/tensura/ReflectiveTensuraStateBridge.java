package com.github.gamekinger1st.imitationcoreapi.internal.tensura;

import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateBridge;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateOperationResult;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateSections;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ReflectiveTensuraStateBridge implements TensuraStateBridge {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "tensura_state_reflective");
    private static final String TENSURA_STORAGES = "io.github.manasmods.tensura.storage.TensuraStorages";
    private static final String ENERGY_HELPER = "io.github.manasmods.tensura.util.EnergyHelper";
    private static final String RACE_API = "io.github.manasmods.manascore.race.api.RaceAPI";

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean isAvailable() {
        try {
            ClassLoader loader = getClass().getClassLoader();
            Class.forName(TENSURA_STORAGES, false, loader);
            Class.forName(RACE_API, false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }

    @Override
    public Optional<TensuraStateSnapshot> capture(LivingEntity entity) {
        try {
            Object existence = tensuraStorage(entity, "getExistenceFrom");
            Map<ResourceLocation, CompoundTag> sections = new LinkedHashMap<>();
            sections.put(TensuraStateSections.RACE, save(raceStorage(entity)));
            sections.put(TensuraStateSections.EXISTENCE, save(existence));
            sections.put(TensuraStateSections.ABILITIES, save(tensuraStorage(entity, "getAbilityFrom")));
            saveOptional(tensuraStorage(entity, "getPlayerDataFrom")).ifPresent(data -> sections.put(TensuraStateSections.PLAYER, data));
            sections.put(TensuraStateSections.SPIRIT, save(tensuraStorage(entity, "getSpiritFrom")));
            captureAttributesSafely(entity).ifPresent(attributes -> sections.put(TensuraStateSections.ATTRIBUTES, attributes));
            TensuraVitals vitals = captureVitals(entity, existence);
            return Optional.of(new TensuraStateSnapshot(ID, TensuraStateSnapshot.CURRENT_SCHEMA_VERSION, vitals, sections));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public TensuraStateOperationResult restore(LivingEntity entity, TensuraStateSnapshot snapshot) {
        if (!ID.equals(snapshot.bridgeId())) {
            return TensuraStateOperationResult.failure("The Tensura state snapshot belongs to another bridge");
        }
        try {
            Map<ResourceLocation, CompoundTag> sections = snapshot.sections();
            restore(raceStorage(entity), sections.get(TensuraStateSections.RACE));
            restore(tensuraStorage(entity, "getExistenceFrom"), sections.get(TensuraStateSections.EXISTENCE));
            restore(tensuraStorage(entity, "getAbilityFrom"), sections.get(TensuraStateSections.ABILITIES));
            restore(tensuraStorage(entity, "getPlayerDataFrom"), sections.get(TensuraStateSections.PLAYER));
            restore(tensuraStorage(entity, "getSpiritFrom"), sections.get(TensuraStateSections.SPIRIT));
            restoreAttributes(entity, sections.get(TensuraStateSections.ATTRIBUTES));
            return TensuraStateOperationResult.success();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return TensuraStateOperationResult.failure(exception.getClass().getSimpleName());
        }
    }

    @Override
    public TensuraStateOperationResult restoreScaled(LivingEntity entity, TensuraStateSnapshot snapshot, double scale) {
        if (!Double.isFinite(scale) || scale < 0D || scale > 1D) {
            return TensuraStateOperationResult.failure("Tensura state scale must be between zero and one");
        }
        TensuraStateOperationResult restored = restore(entity, scaled(snapshot, scale));
        if (!restored.successful()) {
            return restored;
        }
        try {
            Object existence = tensuraStorage(entity, "getExistenceFrom");
            TensuraVitals vitals = scaledVitals(snapshot.vitals(), scale);
            double magicule = vitals.magicule();
            double aura = vitals.aura();
            setMaxEnergy(entity, "setMaxMagicule", magicule);
            setMaxEnergy(entity, "setMaxAura", aura);
            method(existence.getClass(), "setEP", 1).invoke(existence, magicule + aura);
            method(existence.getClass(), "setMagicule", 1).invoke(existence, magicule);
            method(existence.getClass(), "setAura", 1).invoke(existence, aura);
            method(existence.getClass(), "setSpiritualHealth", 1).invoke(existence, vitals.spiritualHealth());
            method(existence.getClass(), "markDirty", 0).invoke(existence);
            return TensuraStateOperationResult.success();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return TensuraStateOperationResult.failure(exception.getClass().getSimpleName());
        }
    }

    @Override
    public TensuraStateOperationResult chargeMagicule(LivingEntity entity, double amount) {
        if (!Double.isFinite(amount) || amount < 0D) {
            return TensuraStateOperationResult.failure("Magicule cost must be finite and non-negative");
        }
        try {
            Object existence = tensuraStorage(entity, "getExistenceFrom");
            double magicule = number(existence, "getMagicule");
            if (magicule < amount) {
                return TensuraStateOperationResult.failure("Insufficient magicule");
            }
            method(existence.getClass(), "setMagicule", 1).invoke(existence, magicule - amount);
            method(existence.getClass(), "markDirty", 0).invoke(existence);
            return TensuraStateOperationResult.success();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return TensuraStateOperationResult.failure(exception.getClass().getSimpleName());
        }
    }

    @Override
    public TensuraStateOperationResult addVitalsDelta(LivingEntity entity, TensuraVitals delta) {
        if (delta.isZero()) {
            return TensuraStateOperationResult.success();
        }
        try {
            Object existence = tensuraStorage(entity, "getExistenceFrom");
            double previousMaxMagicule = energyNumber(entity, "getMaxMagicule", number(existence, "getMagicule"));
            double previousMaxAura = energyNumber(entity, "getMaxAura", number(existence, "getAura"));
            double maxEp = energyNumber(entity, "getMaxEP", previousMaxMagicule + previousMaxAura) + delta.ep();
            double maxMagicule = previousMaxMagicule + delta.magicule();
            double maxAura = previousMaxAura + delta.aura();
            double magicule = number(existence, "getMagicule") + delta.magicule();
            double aura = number(existence, "getAura") + delta.aura();
            double spiritualHealth = number(existence, "getSpiritualHealth") + delta.spiritualHealth();
            setMaxEnergy(entity, "setMaxMagicule", maxMagicule);
            setMaxEnergy(entity, "setMaxAura", maxAura);
            method(existence.getClass(), "setEP", 1).invoke(existence, Math.max(maxEp, maxMagicule + maxAura));
            method(existence.getClass(), "setMagicule", 1).invoke(existence, magicule);
            method(existence.getClass(), "setAura", 1).invoke(existence, aura);
            method(existence.getClass(), "setSpiritualHealth", 1).invoke(existence, spiritualHealth);
            method(existence.getClass(), "markDirty", 0).invoke(existence);
            return TensuraStateOperationResult.success();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return TensuraStateOperationResult.failure(exception.getClass().getSimpleName());
        }
    }

    private Object tensuraStorage(LivingEntity entity, String methodName) throws ReflectiveOperationException {
        Class<?> storages = Class.forName(TENSURA_STORAGES, false, entity.getClass().getClassLoader());
        return method(storages, methodName, 1).invoke(null, entity);
    }

    private Object raceStorage(LivingEntity entity) throws ReflectiveOperationException {
        Class<?> raceApi = Class.forName(RACE_API, false, entity.getClass().getClassLoader());
        return method(raceApi, "getRaceFrom", 1).invoke(null, entity);
    }

    private CompoundTag save(Object storage) throws ReflectiveOperationException {
        if (storage == null) {
            throw new IllegalStateException("The requested Tensura storage is unavailable");
        }
        CompoundTag tag = new CompoundTag();
        method(storage.getClass(), "save", 1).invoke(storage, tag);
        return tag;
    }

    private Optional<CompoundTag> saveOptional(Object storage) throws ReflectiveOperationException {
        return storage == null ? Optional.empty() : Optional.of(save(storage));
    }

    private void restore(Object storage, CompoundTag data) throws ReflectiveOperationException {
        if (data == null) {
            return;
        }
        method(storage.getClass(), "load", 1).invoke(storage, data.copy());
        method(storage.getClass(), "markDirty", 0).invoke(storage);
    }

    private Optional<CompoundTag> captureAttributes(LivingEntity entity) throws ReflectiveOperationException {
        Object values = method(entity.getAttributes().getClass(), "save", 0).invoke(entity.getAttributes());
        if (!(values instanceof ListTag attributes)) {
            return Optional.empty();
        }
        CompoundTag tag = new CompoundTag();
        tag.put("values", attributes.copy());
        return Optional.of(tag);
    }

    private Optional<CompoundTag> captureAttributesSafely(LivingEntity entity) {
        try {
            return captureAttributes(entity);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private TensuraVitals captureVitals(LivingEntity entity, Object existence) throws ReflectiveOperationException {
        double currentMagicule = number(existence, "getMagicule");
        double currentAura = number(existence, "getAura");
        double maxMagicule = energyNumber(entity, "getMaxMagicule", currentMagicule);
        double maxAura = energyNumber(entity, "getMaxAura", currentAura);
        double maxEp = energyNumber(entity, "getMaxEP", maxMagicule + maxAura);
        return new TensuraVitals(maxEp, maxMagicule, maxAura, number(existence, "getSpiritualHealth"));
    }

    private void restoreAttributes(LivingEntity entity, CompoundTag data) throws ReflectiveOperationException {
        if (data == null || !data.contains("values", Tag.TAG_LIST)) {
            return;
        }
        clearAttributeModifiers(entity);
        ListTag attributes = data.getList("values", Tag.TAG_COMPOUND);
        method(entity.getAttributes().getClass(), "load", 1).invoke(entity.getAttributes(), attributes);
    }

    private TensuraStateSnapshot scaled(TensuraStateSnapshot snapshot, double scale) {
        if (scale == 1D) {
            return snapshot;
        }
        Map<ResourceLocation, CompoundTag> sections = new LinkedHashMap<>(snapshot.sections());
        CompoundTag attributes = sections.get(TensuraStateSections.ATTRIBUTES);
        if (attributes != null && attributes.contains("values", Tag.TAG_LIST)) {
            CompoundTag scaled = attributes.copy();
            ListTag values = scaled.getList("values", Tag.TAG_COMPOUND);
            for (int index = 0; index < values.size(); index++) {
                CompoundTag attribute = values.getCompound(index);
                attribute.putDouble("base", attribute.getDouble("base") * scale);
                if (attribute.contains("modifiers", Tag.TAG_LIST)) {
                    ListTag modifiers = attribute.getList("modifiers", Tag.TAG_COMPOUND);
                    for (int modifierIndex = 0; modifierIndex < modifiers.size(); modifierIndex++) {
                        CompoundTag modifier = modifiers.getCompound(modifierIndex);
                        modifier.putDouble("amount", modifier.getDouble("amount") * scale);
                    }
                }
            }
            sections.put(TensuraStateSections.ATTRIBUTES, scaled);
        }
        return new TensuraStateSnapshot(snapshot.bridgeId(), snapshot.schemaVersion(), scaledVitals(snapshot.vitals(), scale), sections);
    }

    private void clearAttributeModifiers(LivingEntity entity) {
        ListTag attributes = entity.getAttributes().save();
        for (int index = 0; index < attributes.size(); index++) {
            CompoundTag attribute = attributes.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(attribute.getString("id"));
            if (id == null) {
                continue;
            }
            BuiltInRegistries.ATTRIBUTE.getHolder(id)
                    .map(entity::getAttribute)
                    .ifPresent(instance -> removePermanentModifiers(instance, attribute));
        }
    }

    private void removePermanentModifiers(AttributeInstance instance, CompoundTag attribute) {
        if (!attribute.contains("modifiers", Tag.TAG_LIST)) {
            return;
        }
        ListTag modifiers = attribute.getList("modifiers", Tag.TAG_COMPOUND);
        for (int index = 0; index < modifiers.size(); index++) {
            ResourceLocation id = ResourceLocation.tryParse(modifiers.getCompound(index).getString("id"));
            if (id != null) {
                instance.removeModifier(id);
            }
        }
    }

    private double number(Object target, String methodName) throws ReflectiveOperationException {
        Object value = method(target.getClass(), methodName, 0).invoke(target);
        if (!(value instanceof Number number)) {
            throw new IllegalStateException(methodName + " did not return a number");
        }
        return number.doubleValue();
    }

    private double energyNumber(LivingEntity entity, String methodName, double fallback) {
        try {
            Class<?> helper = Class.forName(ENERGY_HELPER, false, entity.getClass().getClassLoader());
            Object value = method(helper, methodName, 1).invoke(null, entity);
            return value instanceof Number number ? number.doubleValue() : fallback;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return fallback;
        }
    }

    private void setMaxEnergy(LivingEntity entity, String methodName, double amount) throws ReflectiveOperationException {
        Class<?> helper = Class.forName(ENERGY_HELPER, false, entity.getClass().getClassLoader());
        method(helper, methodName, 2).invoke(null, entity, amount);
    }

    private TensuraVitals scaledVitals(TensuraVitals vitals, double scale) {
        return new TensuraVitals(vitals.ep() * scale, vitals.magicule() * scale, vitals.aura() * scale, vitals.spiritualHealth() * scale);
    }

    private Method method(Class<?> type, String name, int parameterCount) throws NoSuchMethodException {
        return java.util.Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(name) && method.getParameterCount() == parameterCount)
                .findFirst()
                .orElseThrow(NoSuchMethodException::new);
    }
}
