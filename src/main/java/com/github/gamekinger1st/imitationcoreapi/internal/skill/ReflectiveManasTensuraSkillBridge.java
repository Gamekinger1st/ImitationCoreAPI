package com.github.gamekinger1st.imitationcoreapi.internal.skill;

import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillBridge;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillClassification;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillOperationResult;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillState;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillUpdateRequest;
import com.github.gamekinger1st.imitationcoreapi.api.skill.TemporarySkillOwnership;
import com.github.gamekinger1st.imitationcoreapi.api.skill.TemporarySkillService;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class ReflectiveManasTensuraSkillBridge implements SkillBridge {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "manas_tensura_reflective");
    private static final String SKILL_API_CLASS = "io.github.manasmods.manascore.skill.api.SkillAPI";

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
            Class.forName(SKILL_API_CLASS, false, getClass().getClassLoader());
            Class.forName("io.github.manasmods.tensura.ability.TensuraSkill", false, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }

    @Override
    public SkillClassification classify(ResourceLocation skillId) {
        try {
            Object skill = registeredSkill(skillId, getClass().getClassLoader());
            if (skill == null) {
                return SkillClassification.UNKNOWN;
            }
            Object type = method(skill.getClass(), "getType", 0).invoke(skill);
            if (!(type instanceof Enum<?> skillType)) {
                return SkillClassification.UNKNOWN;
            }
            return switch (skillType.name()) {
                case "RESISTANCE" -> SkillClassification.RESISTANCE;
                case "INTRINSIC" -> SkillClassification.INTRINSIC;
                case "COMMON" -> SkillClassification.COMMON;
                case "EXTRA" -> SkillClassification.EXTRA;
                case "UNIQUE" -> SkillClassification.UNIQUE;
                case "ULTIMATE" -> SkillClassification.ULTIMATE;
                default -> SkillClassification.STANDARD;
            };
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return SkillClassification.UNKNOWN;
        }
    }

    @Override
    public SkillOperationResult alterClassification(ResourceLocation skillId, SkillClassification classification) {
        try {
            String typeName = tensuraTypeName(classification);
            ClassLoader classLoader = getClass().getClassLoader();
            Object skill = registeredSkill(skillId, classLoader);
            if (skill == null) {
                return SkillOperationResult.failure("The requested skill is not registered");
            }
            Class<?> skillClass = Class.forName("io.github.manasmods.tensura.ability.skill.Skill", false, classLoader);
            if (!skillClass.isInstance(skill)) {
                return SkillOperationResult.failure("The requested skill does not expose a Tensura skill type");
            }
            Class<?> typeClass = Class.forName("io.github.manasmods.tensura.ability.skill.Skill$SkillType", false, classLoader);
            Object type = enumValue(typeClass, typeName);
            field(skillClass, "type").set(skill, type);
            clearSkillCaches(skill, skillClass);
            return SkillOperationResult.success();
        } catch (IllegalArgumentException exception) {
            return SkillOperationResult.failure(exception.getMessage());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return SkillOperationResult.failure(exception.getClass().getSimpleName());
        }
    }

    @Override
    public Optional<SkillSnapshot> capture(LivingEntity entity) {
        try {
            Object storage = skillStorage(entity);
            if (storage == null) {
                return Optional.empty();
            }
            Object learned = method(storage.getClass(), "getLearnedSkills", 0).invoke(storage);
            if (!(learned instanceof Collection<?> skills)) {
                return Optional.empty();
            }
            List<SkillState> states = skills.stream().filter(java.util.Objects::nonNull).map(this::state).flatMap(Optional::stream).toList();
            return Optional.of(new SkillSnapshot(ID, 1, states));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public SkillOperationResult restore(LivingEntity entity, SkillSnapshot snapshot) {
        if (!ID.equals(snapshot.bridgeId())) {
            return SkillOperationResult.failure("The skill snapshot belongs to another bridge");
        }
        try {
            Object storage = skillStorage(entity);
            for (SkillState state : snapshot.skills()) {
                Object optional = method(storage.getClass(), "getSkill", 1).invoke(storage, state.skillId());
                if (optional instanceof Optional<?> existing && existing.isPresent()) {
                    Object instance = existing.get();
                    method(instance.getClass(), "deserialize", 1).invoke(instance, state.serializedData());
                    method(storage.getClass(), "updateSkill", 2).invoke(storage, instance, true);
                    continue;
                }
                Class<?> instanceClass = Class.forName("io.github.manasmods.manascore.skill.api.ManasSkillInstance", false, entity.getClass().getClassLoader());
                Object instance = instanceClass.getMethod("fromNBT", CompoundTag.class).invoke(null, state.serializedData());
                learn(storage, instance);
            }
            return SkillOperationResult.success();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return SkillOperationResult.failure(exception.getClass().getSimpleName());
        }
    }

    @Override
    public SkillOperationResult grantTemporary(LivingEntity entity, ResourceLocation skillId, int removeTime, TemporarySkillOwnership ownership) {
        if (removeTime < 0) {
            return SkillOperationResult.failure("Temporary skill remove time cannot be negative");
        }
        try {
            Object storage = skillStorage(entity);
            Object existing = method(storage.getClass(), "getSkill", 1).invoke(storage, skillId);
            if (existing instanceof Optional<?> skill && skill.isPresent()) {
                return SkillOperationResult.failure("The requested skill is already learned");
            }
            Object skill = registeredSkill(skillId, entity.getClass().getClassLoader());
            if (skill == null) {
                return SkillOperationResult.failure("The requested skill is not registered");
            }
            Object instance = method(skill.getClass(), "createDefaultInstance", 0).invoke(skill);
            invoke(instance, "setRemoveTime", removeTime);
            Object tag = method(instance.getClass(), "getOrCreateTag", 0).invoke(instance);
            if (!(tag instanceof CompoundTag data)) {
                return SkillOperationResult.failure("The temporary skill cannot store ownership data");
            }
            data.putUUID(TemporarySkillService.SESSION_KEY, ownership.sessionId());
            data.putUUID(TemporarySkillService.REFERENCE_KEY, ownership.referenceId());
            return learn(storage, instance) ? SkillOperationResult.success() : SkillOperationResult.failure("The skill storage rejected the temporary skill");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return SkillOperationResult.failure(exception.getClass().getSimpleName());
        }
    }

    @Override
    public SkillOperationResult revokeTemporary(LivingEntity entity, ResourceLocation skillId, TemporarySkillOwnership ownership) {
        try {
            Object storage = skillStorage(entity);
            Object optional = method(storage.getClass(), "getSkill", 1).invoke(storage, skillId);
            if (!(optional instanceof Optional<?> skillOptional) || skillOptional.isEmpty()) {
                return SkillOperationResult.failure("The temporary skill is no longer learned");
            }
            Object instance = skillOptional.get();
            Object tag = method(instance.getClass(), "getTag", 0).invoke(instance);
            if (!(tag instanceof CompoundTag data)
                    || !data.hasUUID(TemporarySkillService.SESSION_KEY)
                    || !data.hasUUID(TemporarySkillService.REFERENCE_KEY)
                    || !ownership.sessionId().equals(data.getUUID(TemporarySkillService.SESSION_KEY))
                    || !ownership.referenceId().equals(data.getUUID(TemporarySkillService.REFERENCE_KEY))) {
                return SkillOperationResult.failure("The learned skill is not owned by this temporary session");
            }
            method(storage.getClass(), "forgetSkill", 2).invoke(storage, skillId, Component.literal("Imitation Core cleanup"));
            Object remaining = method(storage.getClass(), "getSkill", 1).invoke(storage, skillId);
            return remaining instanceof Optional<?> after && after.isEmpty()
                    ? SkillOperationResult.success()
                    : SkillOperationResult.failure("The skill storage did not remove the temporary skill");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return SkillOperationResult.failure(exception.getClass().getSimpleName());
        }
    }

    @Override
    public SkillOperationResult update(LivingEntity entity, SkillUpdateRequest request) {
        try {
            Object storage = skillStorage(entity);
            Object optional = method(storage.getClass(), "getSkill", 1).invoke(storage, request.skillId());
            if (!(optional instanceof Optional<?> skillOptional) || skillOptional.isEmpty()) {
                return SkillOperationResult.failure("The requested skill is not learned");
            }
            Object skill = skillOptional.get();
            request.mastery().ifPresent(value -> invoke(skill, "setMastery", value));
            request.toggled().ifPresent(value -> invoke(skill, "setToggled", value));
            request.cooldowns().ifPresent(value -> invoke(skill, "setCoolDownList", value));
            request.temporaryRemoveTime().ifPresent(value -> invoke(skill, "setRemoveTime", value));
            method(storage.getClass(), "updateSkill", 2).invoke(storage, skill, true);
            return SkillOperationResult.success();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return SkillOperationResult.failure(exception.getClass().getSimpleName());
        }
    }

    private Object skillStorage(LivingEntity entity) throws ReflectiveOperationException {
        Class<?> skillApi = Class.forName(SKILL_API_CLASS, false, entity.getClass().getClassLoader());
        return method(skillApi, "getSkillsFrom", 1).invoke(null, entity);
    }

    private Object registeredSkill(ResourceLocation skillId, ClassLoader classLoader) throws ReflectiveOperationException {
        Class<?> skillApi = Class.forName(SKILL_API_CLASS, false, classLoader);
        Object registry = method(skillApi, "getSkillRegistry", 0).invoke(null);
        Object skill = method(registry.getClass(), "get", 1).invoke(registry, skillId);
        return skill instanceof Optional<?> optionalSkill ? optionalSkill.orElse(null) : skill;
    }

    private boolean learn(Object storage, Object instance) throws ReflectiveOperationException {
        Method learn = java.util.Arrays.stream(storage.getClass().getMethods())
                .filter(method -> method.getName().equals("learnSkill") && method.getParameterCount() == 2 && method.getParameterTypes()[0].isAssignableFrom(instance.getClass()))
                .findFirst()
                .orElseThrow(NoSuchMethodException::new);
        Object learned = learn.invoke(storage, instance, Component.literal("Imitation Core temporary skill"));
        return learned instanceof Boolean success && success;
    }

    private Optional<SkillState> state(Object skill) {
        try {
            Object id = method(skill.getClass(), "getSkillId", 0).invoke(skill);
            Object data = method(skill.getClass(), "toNBT", 0).invoke(skill);
            Object cooldowns = method(skill.getClass(), "getCooldownList", 0).invoke(skill);
            if (!(id instanceof ResourceLocation skillId) || !(data instanceof CompoundTag tag) || !(cooldowns instanceof List<?> list)) {
                return Optional.empty();
            }
            List<Integer> values = list.stream().filter(Integer.class::isInstance).map(Integer.class::cast).toList();
            double mastery = ((Number)method(skill.getClass(), "getMastery", 0).invoke(skill)).doubleValue();
            boolean toggled = (Boolean)method(skill.getClass(), "isToggled", 0).invoke(skill);
            boolean temporary = (Boolean)method(skill.getClass(), "isTemporarySkill", 0).invoke(skill);
            return Optional.of(new SkillState(skillId, tag, mastery, toggled, values, temporary));
        } catch (ReflectiveOperationException | ClassCastException | LinkageError exception) {
            return Optional.empty();
        }
    }

    private Method method(Class<?> type, String name, int parameters) throws NoSuchMethodException {
        return java.util.Arrays.stream(type.getMethods()).filter(method -> method.getName().equals(name) && method.getParameterCount() == parameters).findFirst().orElseThrow(NoSuchMethodException::new);
    }

    private Field field(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException exception) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private void clearSkillCaches(Object skill, Class<?> skillClass) throws IllegalAccessException {
        setIfPresent(skill, skillClass, "cachedMaxMastery", Integer.MIN_VALUE);
        setIfPresent(skill, skillClass, "cachedAcquiringMpCost", Double.NaN);
    }

    private void setIfPresent(Object target, Class<?> type, String name, Object value) throws IllegalAccessException {
        try {
            field(type, name).set(target, value);
        } catch (NoSuchFieldException exception) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object enumValue(Class<?> typeClass, String name) {
        return Enum.valueOf((Class) typeClass.asSubclass(Enum.class), name);
    }

    private String tensuraTypeName(SkillClassification classification) {
        return switch (classification) {
            case STANDARD, COMMON -> "COMMON";
            case RESISTANCE -> "RESISTANCE";
            case INTRINSIC -> "INTRINSIC";
            case EXTRA -> "EXTRA";
            case UNIQUE -> "UNIQUE";
            case ULTIMATE -> "ULTIMATE";
            case UNKNOWN -> throw new IllegalArgumentException("Unknown is not a writable Tensura skill type");
        };
    }

    private void invoke(Object target, String name, Object value) {
        try {
            method(target.getClass(), name, 1).invoke(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
