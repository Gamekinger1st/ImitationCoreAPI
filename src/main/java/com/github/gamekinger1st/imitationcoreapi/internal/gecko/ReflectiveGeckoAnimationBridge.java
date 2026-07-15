package com.github.gamekinger1st.imitationcoreapi.internal.gecko;

import com.github.gamekinger1st.imitationcoreapi.api.gecko.GeckoAnimationBridge;
import com.github.gamekinger1st.imitationcoreapi.api.gecko.GeckoAnimationSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.gecko.GeckoControllerSnapshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ReflectiveGeckoAnimationBridge implements GeckoAnimationBridge {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "geckolib_reflective");
    private static final String GEO_ENTITY_CLASS = "software.bernie.geckolib.animatable.GeoEntity";

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean supports(Entity entity) {
        return geoEntityClass(entity).map(type -> type.isInstance(entity)).orElse(false);
    }

    @Override
    public Optional<GeckoAnimationSnapshot> capture(Entity entity) {
        try {
            if (!supports(entity)) {
                return Optional.empty();
            }
            Map<?, ?> controllers = animationControllers(entity).orElse(Map.of());
            List<GeckoControllerSnapshot> snapshots = controllers.entrySet().stream()
                    .map(entry -> controllerSnapshot(entry.getKey(), entry.getValue()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
            ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return type == null ? Optional.empty() : Optional.of(new GeckoAnimationSnapshot(entity.getUUID(), type, entity.tickCount, List.of(), snapshots));
        } catch (RuntimeException | LinkageError exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean mirror(Entity imitation, Entity subject) {
        try {
            if (!supports(imitation) || !supports(subject)) {
                return false;
            }
            Map<?, ?> sourceControllers = animationControllers(subject).orElse(Map.of());
            Map<?, ?> imitationControllers = animationControllers(imitation).orElse(Map.of());
            boolean mirrored = false;
            for (Map.Entry<?, ?> sourceEntry : sourceControllers.entrySet()) {
                String controllerName = controllerName(sourceEntry.getKey(), sourceEntry.getValue()).orElse(null);
                if (controllerName == null) {
                    continue;
                }
                Object imitationController = controllerByName(imitationControllers, controllerName).orElse(null);
                if (imitationController == null) {
                    continue;
                }
                mirrored |= mirrorRawAnimation(imitationController, sourceEntry.getValue());
                mirrored |= mirrorTriggeredAnimation(imitation, controllerName, sourceEntry.getValue());
            }
            return mirrored;
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    @Override
    public boolean trigger(Entity entity, String controllerName, String animationName) {
        return invoke(entity, "triggerAnim", controllerName, animationName);
    }

    @Override
    public boolean stop(Entity entity, String controllerName, String animationName) {
        return invoke(entity, "stopTriggeredAnim", controllerName, animationName);
    }

    private boolean invoke(Entity entity, String methodName, String controllerName, String animationName) {
        try {
            if (!supports(entity)) {
                return false;
            }
            findMethod(entity.getClass(), methodName, 2).invoke(entity, controllerName, animationName);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    private Optional<Class<?>> geoEntityClass(Entity entity) {
        try {
            return Optional.of(Class.forName(GEO_ENTITY_CLASS, false, entity.getClass().getClassLoader()));
        } catch (ClassNotFoundException | LinkageError exception) {
            return Optional.empty();
        }
    }

    private Optional<Map<?, ?>> animationControllers(Entity entity) {
        try {
            Object cache = findMethod(entity.getClass(), "getAnimatableInstanceCache", 0).invoke(entity);
            Method managerMethod = java.util.Arrays.stream(cache.getClass().getMethods()).filter(method -> method.getName().equals("getManagerForId") && method.getParameterCount() == 1).findFirst().orElseThrow();
            Object manager = managerMethod.invoke(cache, entity.getId());
            if (manager == null) {
                return Optional.empty();
            }
            Object controllers = findMethod(manager.getClass(), "getAnimationControllers", 0).invoke(manager);
            return controllers instanceof Map<?, ?> map ? Optional.of(map) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return Optional.empty();
        }
    }

    private Optional<GeckoControllerSnapshot> controllerSnapshot(Object mapKey, Object controller) {
        String name = controllerName(mapKey, controller).orElse(null);
        if (name == null) {
            return Optional.empty();
        }
        String state = stringValue(invokeNoArg(controller, "getAnimationState").or(() -> readField(controller, "animationState")).orElse(null)).orElse("UNKNOWN");
        double speed = doubleValue(invokeNoArg(controller, "getAnimationSpeed").orElse(null)).orElse(1D);
        double transition = doubleValue(readField(controller, "transitionLength").orElse(null)).orElse(0D);
        boolean playingTriggered = booleanValue(invokeNoArg(controller, "isPlayingTriggeredAnimation").orElse(null)).orElse(false);
        List<String> animationNames = animationNames(controller);
        List<String> triggerableNames = triggerableNames(controller);
        String triggeredName = invokeNoArg(controller, "getTriggeredAnimation")
                .or(() -> readField(controller, "triggeredAnimation"))
                .flatMap(rawAnimation -> triggerableNameFor(controller, rawAnimation))
                .orElse("");
        return Optional.of(new GeckoControllerSnapshot(name, state, animationNames, triggerableNames, speed, transition, triggeredName, playingTriggered));
    }

    private Optional<String> controllerName(Object mapKey, Object controller) {
        return stringValue(invokeNoArg(controller, "getName").or(() -> readField(controller, "name")).orElse(mapKey));
    }

    private Optional<Object> controllerByName(Map<?, ?> controllers, String name) {
        Object direct = controllers.get(name);
        if (direct != null) {
            return Optional.of(direct);
        }
        return controllers.entrySet().stream()
                .filter(entry -> controllerName(entry.getKey(), entry.getValue()).map(name::equals).orElse(false))
                .map(entry -> (Object) entry.getValue())
                .findFirst();
    }

    private boolean mirrorRawAnimation(Object imitationController, Object sourceController) {
        Object rawAnimation = invokeNoArg(sourceController, "getCurrentRawAnimation").or(() -> readField(sourceController, "currentRawAnimation")).orElse(null);
        if (rawAnimation == null || rawAnimationNames(rawAnimation).isEmpty()) {
            return false;
        }
        return invokeOneArg(imitationController, "setAnimation", rawAnimation);
    }

    private boolean mirrorTriggeredAnimation(Entity imitation, String controllerName, Object sourceController) {
        boolean playingTriggered = booleanValue(invokeNoArg(sourceController, "isPlayingTriggeredAnimation").orElse(null)).orElse(false);
        if (!playingTriggered) {
            return false;
        }
        Object triggeredAnimation = invokeNoArg(sourceController, "getTriggeredAnimation").or(() -> readField(sourceController, "triggeredAnimation")).orElse(null);
        if (triggeredAnimation == null) {
            return false;
        }
        return triggerableNameFor(sourceController, triggeredAnimation).map(name -> trigger(imitation, controllerName, name)).orElse(false);
    }

    private List<String> animationNames(Object controller) {
        Set<String> names = new LinkedHashSet<>();
        invokeNoArg(controller, "getCurrentRawAnimation").or(() -> readField(controller, "currentRawAnimation")).ifPresent(raw -> names.addAll(rawAnimationNames(raw)));
        invokeNoArg(controller, "getTriggeredAnimation").or(() -> readField(controller, "triggeredAnimation")).ifPresent(raw -> names.addAll(rawAnimationNames(raw)));
        invokeNoArg(controller, "getCurrentAnimation").or(() -> readField(controller, "currentAnimation")).flatMap(this::queuedAnimationName).ifPresent(names::add);
        return new ArrayList<>(names);
    }

    private List<String> rawAnimationNames(Object rawAnimation) {
        Object stages = invokeNoArg(rawAnimation, "getAnimationStages").orElse(null);
        if (!(stages instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Object stage : iterable) {
            stringValue(invokeNoArg(stage, "animationName").or(() -> readField(stage, "animationName")).orElse(null))
                    .filter(name -> !name.equals("internal.wait"))
                    .ifPresent(names::add);
        }
        return names.stream().distinct().toList();
    }

    private Optional<String> queuedAnimationName(Object queuedAnimation) {
        Object animation = invokeNoArg(queuedAnimation, "animation").orElse(null);
        return stringValue(invokeNoArg(animation, "name").or(() -> readField(animation, "name")).orElse(null));
    }

    private List<String> triggerableNames(Object controller) {
        Object triggerable = readField(controller, "triggerableAnimations").orElse(null);
        if (!(triggerable instanceof Map<?, ?> map)) {
            return List.of();
        }
        return map.keySet().stream().map(String::valueOf).filter(name -> !name.isBlank()).distinct().toList();
    }

    private Optional<String> triggerableNameFor(Object controller, Object rawAnimation) {
        Object triggerable = readField(controller, "triggerableAnimations").orElse(null);
        if (!(triggerable instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        return map.entrySet().stream()
                .filter(entry -> entry.getValue() == rawAnimation || Objects.equals(entry.getValue(), rawAnimation))
                .map(Map.Entry::getKey)
                .map(String::valueOf)
                .filter(name -> !name.isBlank())
                .findFirst();
    }

    private Optional<Object> invokeNoArg(Object target, String name) {
        if (target == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(findMethod(target.getClass(), name, 0).invoke(target));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return Optional.empty();
        }
    }

    private boolean invokeOneArg(Object target, String name, Object argument) {
        if (target == null || argument == null) {
            return false;
        }
        try {
            findMethod(target.getClass(), name, 1).invoke(target, argument);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private Optional<Object> readField(Object target, String name) {
        if (target == null) {
            return Optional.empty();
        }
        Class<?> type = target.getClass();
        while (type != null && type != Object.class) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return Optional.ofNullable(field.get(target));
            } catch (NoSuchFieldException exception) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<String> stringValue(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        String text = String.valueOf(value).strip();
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }

    private Optional<Double> doubleValue(Object value) {
        if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
            return Optional.of(number.doubleValue());
        }
        return Optional.empty();
    }

    private Optional<Boolean> booleanValue(Object value) {
        return value instanceof Boolean bool ? Optional.of(bool) : Optional.empty();
    }

    private Method findMethod(Class<?> type, String name, int parameters) throws NoSuchMethodException {
        return java.util.Arrays.stream(type.getMethods()).filter(method -> method.getName().equals(name) && method.getParameterCount() == parameters).findFirst().orElseThrow(NoSuchMethodException::new);
    }
}
