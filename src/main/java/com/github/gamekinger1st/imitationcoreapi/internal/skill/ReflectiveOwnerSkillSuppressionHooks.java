package com.github.gamekinger1st.imitationcoreapi.internal.skill;

import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import com.github.gamekinger1st.imitationcoreapi.api.skill.OwnerSkillUseDecision;
import com.github.gamekinger1st.imitationcoreapi.api.skill.TemporarySkillOwnership;
import com.github.gamekinger1st.imitationcoreapi.api.skill.TemporarySkillService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

public final class ReflectiveOwnerSkillSuppressionHooks {
    private ReflectiveOwnerSkillSuppressionHooks() {
    }

    public static boolean canUse(LivingEntity entity, Object skillInstance) {
        if (!(entity instanceof ServerPlayer player) || skillInstance == null) {
            return true;
        }
        Optional<ResourceLocation> skillId = skillId(skillInstance);
        if (skillId.isEmpty()) {
            return !isSuppressing(player);
        }
        try {
            OwnerSkillUseDecision decision = ImitationCoreServices.ownerSkillSuppressions(player).evaluate(player, skillId.get(), temporaryOwnership(skillInstance));
            return decision.allowed();
        } catch (RuntimeException | LinkageError exception) {
            return !isSuppressing(player);
        }
    }

    public static boolean canUseChangeable(Object changeable, Object skillInstance) {
        if (changeable == null) {
            return true;
        }
        try {
            Object value = method(changeable.getClass(), "get", 0).invoke(changeable);
            return !(value instanceof LivingEntity entity) || canUse(entity, skillInstance);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return true;
        }
    }

    private static Optional<ResourceLocation> skillId(Object skillInstance) {
        try {
            Object value = method(skillInstance.getClass(), "getSkillId", 0).invoke(skillInstance);
            return value instanceof ResourceLocation resourceLocation ? Optional.of(resourceLocation) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return Optional.empty();
        }
    }

    private static Optional<TemporarySkillOwnership> temporaryOwnership(Object skillInstance) {
        try {
            Object tag = method(skillInstance.getClass(), "getTag", 0).invoke(skillInstance);
            if (!(tag instanceof CompoundTag data)
                    || !data.hasUUID(TemporarySkillService.SESSION_KEY)
                    || !data.hasUUID(TemporarySkillService.REFERENCE_KEY)) {
                return Optional.empty();
            }
            UUID sessionId = data.getUUID(TemporarySkillService.SESSION_KEY);
            UUID referenceId = data.getUUID(TemporarySkillService.REFERENCE_KEY);
            return Optional.of(new TemporarySkillOwnership(sessionId, referenceId));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return Optional.empty();
        }
    }

    private static Method method(Class<?> type, String name, int parameters) throws NoSuchMethodException {
        return java.util.Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(name) && method.getParameterCount() == parameters)
                .findFirst()
                .orElseThrow(NoSuchMethodException::new);
    }

    private static boolean isSuppressing(ServerPlayer player) {
        try {
            return ImitationCoreServices.ownerSkillSuppressions(player).isSuppressing(player);
        } catch (RuntimeException | LinkageError exception) {
            return true;
        }
    }
}
