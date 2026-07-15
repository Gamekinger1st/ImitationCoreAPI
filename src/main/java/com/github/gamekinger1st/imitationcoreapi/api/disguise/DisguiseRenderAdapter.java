package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import net.minecraft.resources.ResourceLocation;

public interface DisguiseRenderAdapter {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    boolean supports(DisguiseRenderContext context);

    boolean render(DisguiseRenderContext context);
}
