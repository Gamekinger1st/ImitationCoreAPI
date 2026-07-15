package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.Optional;

public record RaceFunctionContext(
        ResourceLocation bridgeId,
        ResourceLocation raceId,
        ResourceLocation functionId,
        Optional<LivingEntity> entity,
        CompoundTag data
) {
    public RaceFunctionContext(ResourceLocation bridgeId, ResourceLocation raceId, ResourceLocation functionId) {
        this(bridgeId, raceId, functionId, Optional.empty(), new CompoundTag());
    }

    public RaceFunctionContext {
        Objects.requireNonNull(bridgeId, "bridgeId");
        Objects.requireNonNull(raceId, "raceId");
        Objects.requireNonNull(functionId, "functionId");
        Objects.requireNonNull(entity, "entity");
        data = Objects.requireNonNull(data, "data").copy();
    }
}
