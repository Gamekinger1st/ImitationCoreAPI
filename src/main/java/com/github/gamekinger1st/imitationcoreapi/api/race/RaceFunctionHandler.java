package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.resources.ResourceLocation;

public interface RaceFunctionHandler {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    RaceFunctionResult handle(RaceFunctionContext context);
}
