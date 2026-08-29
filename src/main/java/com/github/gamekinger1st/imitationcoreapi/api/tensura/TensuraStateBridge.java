package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public interface TensuraStateBridge {
    ResourceLocation id();

    int priority();

    boolean isAvailable();

    Optional<TensuraStateSnapshot> capture(LivingEntity entity);

    default Optional<TensuraVitals> captureVitals(LivingEntity entity) {
        return capture(entity).map(TensuraStateSnapshot::vitals);
    }

    TensuraStateOperationResult restore(LivingEntity entity, TensuraStateSnapshot snapshot);

    default TensuraStateOperationResult restoreScaled(LivingEntity entity, TensuraStateSnapshot snapshot, double scale) {
        if (!Double.isFinite(scale) || scale < 0D || scale > 1D) {
            return TensuraStateOperationResult.failure("Tensura state scale must be between zero and one");
        }
        return scale == 1D
                ? restore(entity, snapshot)
                : TensuraStateOperationResult.failure("The loaded Tensura state bridge cannot safely scale copied state");
    }

    TensuraStateOperationResult chargeMagicule(LivingEntity entity, double amount);

    default TensuraStateOperationResult addVitalsDelta(LivingEntity entity, TensuraVitals delta) {
        return TensuraStateOperationResult.failure("The loaded Tensura state bridge cannot safely apply vitals progression");
    }
}
