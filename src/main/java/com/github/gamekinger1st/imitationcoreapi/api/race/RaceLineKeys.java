package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class RaceLineKeys {
    public static final ResourceLocation NAME = key("name");
    public static final ResourceLocation DESCRIPTION = key("description");
    public static final ResourceLocation DISPLAY_NAME = key("display_name");
    public static final ResourceLocation ALIGNMENT = key("alignment");
    public static final ResourceLocation EVOLUTION_REQUIREMENTS = key("evolution_requirements");
    public static final ResourceLocation EVOLUTION_PROGRESS = key("evolution_progress");
    public static final ResourceLocation INTRINSIC_LEARNABLE = key("intrinsic_learnable");

    private RaceLineKeys() {
    }

    public static List<ResourceLocation> builtin() {
        return List.of(NAME, DESCRIPTION, DISPLAY_NAME, ALIGNMENT, EVOLUTION_REQUIREMENTS, EVOLUTION_PROGRESS, INTRINSIC_LEARNABLE);
    }

    private static ResourceLocation key(String path) {
        return ResourceLocation.fromNamespaceAndPath("imitationcoreapi", path);
    }
}
