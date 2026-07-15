package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record RaceEditContext(ResourceLocation bridgeId, ResourceLocation raceId) {
    public RaceEditContext {
        Objects.requireNonNull(bridgeId, "bridgeId");
        Objects.requireNonNull(raceId, "raceId");
    }
}
