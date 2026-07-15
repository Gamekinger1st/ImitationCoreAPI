package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface RaceEditProvider {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    Optional<RaceEditProfile> edit(RaceEditContext context);
}
