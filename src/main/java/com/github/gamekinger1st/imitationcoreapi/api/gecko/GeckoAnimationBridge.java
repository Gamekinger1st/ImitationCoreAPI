package com.github.gamekinger1st.imitationcoreapi.api.gecko;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public interface GeckoAnimationBridge {
    ResourceLocation id();

    int priority();

    boolean supports(Entity entity);

    Optional<GeckoAnimationSnapshot> capture(Entity entity);

    default boolean mirror(Entity imitation, Entity subject) {
        return false;
    }

    boolean trigger(Entity entity, String controllerName, String animationName);

    boolean stop(Entity entity, String controllerName, String animationName);
}
