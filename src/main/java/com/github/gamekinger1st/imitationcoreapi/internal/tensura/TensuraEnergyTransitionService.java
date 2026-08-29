package com.github.gamekinger1st.imitationcoreapi.internal.tensura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.OptionalDouble;

public final class TensuraEnergyTransitionService {
    public static final String ACTIVE_FORM_TAG = "ImitationCoreAPI.ActiveForm";
    private static final String TENSURA_STORAGES = "io.github.manasmods.tensura.storage.TensuraStorages";
    private static final String ENERGY_HELPER = "io.github.manasmods.tensura.util.EnergyHelper";
    private static final String TRANSITION_ACTIVE = "tensura_energy_transition";
    private static final String MAGICULE_PENDING = "tensura_magicule_transition";
    private static final String AURA_PENDING = "tensura_aura_transition";
    private static final String TARGET_MAGICULE_BASE = "tensura_target_magicule_base";
    private static final String TARGET_MAGICULE_MAX = "tensura_target_magicule_max";
    private static final String TARGET_AURA_BASE = "tensura_target_aura_base";
    private static final String TARGET_AURA_MAX = "tensura_target_aura_max";
    private static final String BASELINE_MAGICULE = "tensura_baseline_magicule";
    private static final String BASELINE_AURA = "tensura_baseline_aura";
    private static final String MAX_MAGICULE_ATTRIBUTE = "tensura:max_magicule";
    private static final String MAX_AURA_ATTRIBUTE = "tensura:max_aura";
    private static final double DEFAULT_STEP = 5.0D;
    private static final double EPSILON = 0.000001D;

    private TensuraEnergyTransitionService() {
    }

    public static void captureBaseline(ServerPlayer player, CompoundTag payload) {
        try {
            Object existence = existence(player);
            payload.putDouble(BASELINE_MAGICULE, nonNegative(number(existence, "getMagicule")));
            payload.putDouble(BASELINE_AURA, nonNegative(number(existence, "getAura")));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    public static void restoreBaseline(ServerPlayer player, CompoundTag payload) {
        if (!payload.contains(BASELINE_MAGICULE, Tag.TAG_DOUBLE) && !payload.contains(BASELINE_AURA, Tag.TAG_DOUBLE)) {
            return;
        }
        try {
            Object existence = existence(player);
            if (payload.contains(BASELINE_MAGICULE, Tag.TAG_DOUBLE)) {
                double maximum = energyNumber(player, "getMaxMagicule");
                setNumber(existence, "setMagicule", Math.min(nonNegative(payload.getDouble(BASELINE_MAGICULE)), maximum));
            }
            if (payload.contains(BASELINE_AURA, Tag.TAG_DOUBLE)) {
                double maximum = energyNumber(player, "getMaxAura");
                setNumber(existence, "setAura", Math.min(nonNegative(payload.getDouble(BASELINE_AURA)), maximum));
            }
            markDirty(existence);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    public static void begin(ServerPlayer player, CompoundTag marker, ListTag attributes, double scale) {
        clearTransition(marker);
        double boundedScale = Math.max(0.0D, Math.min(1.0D, scale));
        OptionalDouble magiculeBase = attributeBase(attributes, MAX_MAGICULE_ATTRIBUTE, boundedScale);
        OptionalDouble auraBase = attributeBase(attributes, MAX_AURA_ATTRIBUTE, boundedScale);
        if (magiculeBase.isEmpty() && auraBase.isEmpty()) {
            return;
        }
        try {
            Object existence = existence(player);
            boolean magiculePending = magiculeBase.isPresent() && beginEnergy(
                    player,
                    existence,
                    marker,
                    magiculeBase.getAsDouble(),
                    "getMagicule",
                    "setMagicule",
                    "getMaxMagicule",
                    "setMaxMagicule",
                    MAGICULE_PENDING,
                    TARGET_MAGICULE_BASE,
                    TARGET_MAGICULE_MAX
            );
            boolean auraPending = auraBase.isPresent() && beginEnergy(
                    player,
                    existence,
                    marker,
                    auraBase.getAsDouble(),
                    "getAura",
                    "setAura",
                    "getMaxAura",
                    "setMaxAura",
                    AURA_PENDING,
                    TARGET_AURA_BASE,
                    TARGET_AURA_MAX
            );
            marker.putBoolean(TRANSITION_ACTIVE, magiculePending || auraPending);
            markDirty(existence);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            clearTransition(marker);
        }
    }

    public static void tick(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(ACTIVE_FORM_TAG, Tag.TAG_COMPOUND)
                || player.serverLevel().getServer().getTickCount() % 10 != 0) {
            return;
        }
        CompoundTag marker = persistentData.getCompound(ACTIVE_FORM_TAG);
        if (!marker.getBoolean(TRANSITION_ACTIVE)) {
            return;
        }
        try {
            Object existence = existence(player);
            double step = transitionStep(player);
            boolean magiculePending = marker.getBoolean(MAGICULE_PENDING) && advanceEnergy(
                    player,
                    existence,
                    marker,
                    step,
                    "getMagicule",
                    "setMagicule",
                    "getMaxMagicule",
                    "setMaxMagicule",
                    MAGICULE_PENDING,
                    TARGET_MAGICULE_BASE,
                    TARGET_MAGICULE_MAX
            );
            boolean auraPending = marker.getBoolean(AURA_PENDING) && advanceEnergy(
                    player,
                    existence,
                    marker,
                    step,
                    "getAura",
                    "setAura",
                    "getMaxAura",
                    "setMaxAura",
                    AURA_PENDING,
                    TARGET_AURA_BASE,
                    TARGET_AURA_MAX
            );
            marker.putBoolean(TRANSITION_ACTIVE, magiculePending || auraPending);
            persistentData.put(ACTIVE_FORM_TAG, marker);
            markDirty(existence);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            clearTransition(marker);
            persistentData.put(ACTIVE_FORM_TAG, marker);
        }
    }

    static double nextDownwardValue(double current, double target, double step) {
        if (!Double.isFinite(current) || !Double.isFinite(target) || !Double.isFinite(step) || step <= 0.0D) {
            throw new IllegalArgumentException("Energy transition values must be finite and the step must be positive");
        }
        return current <= target ? current : Math.max(target, current - step);
    }

    private static boolean beginEnergy(
            ServerPlayer player,
            Object existence,
            CompoundTag marker,
            double targetBase,
            String currentGetter,
            String currentSetter,
            String maximumGetter,
            String maximumSetter,
            String pendingKey,
            String targetBaseKey,
            String targetMaximumKey
    ) throws ReflectiveOperationException {
        double targetMaximum = nonNegative(energyNumber(player, maximumGetter));
        double current = nonNegative(number(existence, currentGetter));
        if (current <= targetMaximum + EPSILON) {
            return false;
        }
        marker.putBoolean(pendingKey, true);
        marker.putDouble(targetBaseKey, nonNegative(targetBase));
        marker.putDouble(targetMaximumKey, targetMaximum);
        double safeMaximum = setEffectiveMaximum(player, maximumSetter, maximumGetter, current);
        setNumber(existence, currentSetter, safeMaximum);
        if (safeMaximum <= targetMaximum + EPSILON) {
            setMaxEnergy(player, maximumSetter, nonNegative(targetBase));
            marker.remove(pendingKey);
            return false;
        }
        return true;
    }

    private static boolean advanceEnergy(
            ServerPlayer player,
            Object existence,
            CompoundTag marker,
            double step,
            String currentGetter,
            String currentSetter,
            String maximumGetter,
            String maximumSetter,
            String pendingKey,
            String targetBaseKey,
            String targetMaximumKey
    ) throws ReflectiveOperationException {
        double targetMaximum = nonNegative(marker.getDouble(targetMaximumKey));
        double current = nonNegative(number(existence, currentGetter));
        double next = nextDownwardValue(current, targetMaximum, step);
        if (next <= targetMaximum + EPSILON) {
            setMaxEnergy(player, maximumSetter, nonNegative(marker.getDouble(targetBaseKey)));
            double restoredMaximum = nonNegative(energyNumber(player, maximumGetter));
            setNumber(existence, currentSetter, Math.min(current, restoredMaximum));
            marker.remove(pendingKey);
            marker.remove(targetBaseKey);
            marker.remove(targetMaximumKey);
            return false;
        }
        double synchronizedMaximum = setEffectiveMaximum(player, maximumSetter, maximumGetter, next);
        setNumber(existence, currentSetter, synchronizedMaximum);
        return synchronizedMaximum > targetMaximum + EPSILON;
    }

    private static double setEffectiveMaximum(
            LivingEntity entity,
            String maximumSetter,
            String maximumGetter,
            double desiredMaximum
    ) throws ReflectiveOperationException {
        double candidateBase = nonNegative(desiredMaximum);
        double actualMaximum = 0.0D;
        for (int attempt = 0; attempt < 8; attempt++) {
            setMaxEnergy(entity, maximumSetter, candidateBase);
            actualMaximum = nonNegative(energyNumber(entity, maximumGetter));
            double difference = desiredMaximum - actualMaximum;
            if (Math.abs(difference) <= EPSILON * Math.max(1.0D, desiredMaximum)) {
                return actualMaximum;
            }
            candidateBase = nonNegative(candidateBase + difference);
        }
        return actualMaximum;
    }

    private static OptionalDouble attributeBase(ListTag attributes, String id, double scale) {
        for (int index = 0; index < attributes.size(); index++) {
            CompoundTag attribute = attributes.getCompound(index);
            if (id.equals(attribute.getString("id")) && attribute.contains("base", Tag.TAG_DOUBLE)) {
                double value = attribute.getDouble("base");
                if (Double.isFinite(value) && value >= 0.0D) {
                    return OptionalDouble.of(value * scale);
                }
            }
        }
        return OptionalDouble.empty();
    }

    private static double transitionStep(LivingEntity entity) {
        try {
            Class<?> helper = Class.forName(ENERGY_HELPER, false, entity.getClass().getClassLoader());
            Field configField = helper.getField("CONFIG");
            Object config = configField.get(null);
            Field stepField = config.getClass().getField("exceedMaxLost");
            Object value = stepField.get(config);
            if (value instanceof Number number && Double.isFinite(number.doubleValue()) && number.doubleValue() > 0.0D) {
                return number.doubleValue();
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return DEFAULT_STEP;
    }

    private static Object existence(LivingEntity entity) throws ReflectiveOperationException {
        Class<?> storages = Class.forName(TENSURA_STORAGES, false, entity.getClass().getClassLoader());
        Object storage = method(storages, "getExistenceFrom", 1).invoke(null, entity);
        if (storage == null) {
            throw new IllegalStateException("The Tensura existence storage is unavailable");
        }
        return storage;
    }

    private static double number(Object target, String methodName) throws ReflectiveOperationException {
        Object value = method(target.getClass(), methodName, 0).invoke(target);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            throw new IllegalStateException(methodName + " did not return a finite number");
        }
        return number.doubleValue();
    }

    private static double energyNumber(LivingEntity entity, String methodName) throws ReflectiveOperationException {
        Class<?> helper = Class.forName(ENERGY_HELPER, false, entity.getClass().getClassLoader());
        Object value = method(helper, methodName, 1).invoke(null, entity);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            throw new IllegalStateException(methodName + " did not return a finite number");
        }
        return number.doubleValue();
    }

    private static void setMaxEnergy(LivingEntity entity, String methodName, double amount) throws ReflectiveOperationException {
        Class<?> helper = Class.forName(ENERGY_HELPER, false, entity.getClass().getClassLoader());
        method(helper, methodName, 2).invoke(null, entity, nonNegative(amount));
    }

    private static void setNumber(Object target, String methodName, double amount) throws ReflectiveOperationException {
        method(target.getClass(), methodName, 1).invoke(target, nonNegative(amount));
    }

    private static void markDirty(Object storage) throws ReflectiveOperationException {
        method(storage.getClass(), "markDirty", 0).invoke(storage);
    }

    private static Method method(Class<?> type, String name, int parameterCount) throws NoSuchMethodException {
        return java.util.Arrays.stream(type.getMethods())
                .filter(candidate -> candidate.getName().equals(name) && candidate.getParameterCount() == parameterCount)
                .findFirst()
                .orElseThrow(NoSuchMethodException::new);
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static void clearTransition(CompoundTag marker) {
        marker.remove(TRANSITION_ACTIVE);
        marker.remove(MAGICULE_PENDING);
        marker.remove(AURA_PENDING);
        marker.remove(TARGET_MAGICULE_BASE);
        marker.remove(TARGET_MAGICULE_MAX);
        marker.remove(TARGET_AURA_BASE);
        marker.remove(TARGET_AURA_MAX);
    }
}
