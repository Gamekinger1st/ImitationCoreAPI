package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import net.minecraft.resources.ResourceLocation;

public final class TensuraStateSections {
    public static final ResourceLocation RACE = id("race");
    public static final ResourceLocation EXISTENCE = id("existence");
    public static final ResourceLocation ABILITIES = id("abilities");
    public static final ResourceLocation PLAYER = id("player");
    public static final ResourceLocation SPIRIT = id("spirit");
    public static final ResourceLocation ATTRIBUTES = id("attributes");

    private TensuraStateSections() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("tensura", path);
    }
}
