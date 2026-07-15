package com.github.gamekinger1st.imitationcoreapi.api.targeting;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

public record MobFactionResolution(ResourceLocation entityType, ResourceLocation factionId, Optional<ResourceLocation> resolverId) {
    public MobFactionResolution {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(factionId, "factionId");
        Objects.requireNonNull(resolverId, "resolverId");
    }

    public boolean usedFallback() {
        return resolverId.isEmpty();
    }
}
