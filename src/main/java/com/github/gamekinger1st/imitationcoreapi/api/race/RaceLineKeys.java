package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class RaceLineKeys {
    public static final ResourceLocation NAME = key("name");
    public static final ResourceLocation DESCRIPTION = key("description");
    public static final ResourceLocation DISPLAY_NAME = key("display_name");

    private RaceLineKeys() {
    }

    public static List<ResourceLocation> builtin() {
        return List.of(NAME, DESCRIPTION, DISPLAY_NAME);
    }

    private static ResourceLocation key(String path) {
        return ResourceLocation.fromNamespaceAndPath("imitationcoreapi", path);
    }
}
