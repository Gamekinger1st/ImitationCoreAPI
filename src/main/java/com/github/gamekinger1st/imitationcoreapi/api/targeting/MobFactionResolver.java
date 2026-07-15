package com.github.gamekinger1st.imitationcoreapi.api.targeting;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface MobFactionResolver {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    Optional<ResourceLocation> resolve(ResourceLocation entityType);
}
