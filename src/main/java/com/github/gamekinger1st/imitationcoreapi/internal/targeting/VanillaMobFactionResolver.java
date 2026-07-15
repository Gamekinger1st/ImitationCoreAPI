package com.github.gamekinger1st.imitationcoreapi.internal.targeting;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.targeting.MobFactionResolver;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public final class VanillaMobFactionResolver implements MobFactionResolver {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "vanilla_mob_factions");
    private static final ResourceLocation ILLAGER = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "illager");
    private static final ResourceLocation PIGLIN = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "piglin");
    private static final ResourceLocation SKELETON = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "skeleton");
    private static final ResourceLocation ZOMBIE = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "zombie");
    private static final ResourceLocation GUARDIAN = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "guardian");
    private static final Map<ResourceLocation, ResourceLocation> FACTIONS = Map.ofEntries(
            Map.entry(vanilla("evoker"), ILLAGER),
            Map.entry(vanilla("illusioner"), ILLAGER),
            Map.entry(vanilla("pillager"), ILLAGER),
            Map.entry(vanilla("ravager"), ILLAGER),
            Map.entry(vanilla("vex"), ILLAGER),
            Map.entry(vanilla("vindicator"), ILLAGER),
            Map.entry(vanilla("piglin"), PIGLIN),
            Map.entry(vanilla("piglin_brute"), PIGLIN),
            Map.entry(vanilla("bogged"), SKELETON),
            Map.entry(vanilla("skeleton"), SKELETON),
            Map.entry(vanilla("stray"), SKELETON),
            Map.entry(vanilla("wither_skeleton"), SKELETON),
            Map.entry(vanilla("drowned"), ZOMBIE),
            Map.entry(vanilla("husk"), ZOMBIE),
            Map.entry(vanilla("zombie"), ZOMBIE),
            Map.entry(vanilla("zombie_villager"), ZOMBIE),
            Map.entry(vanilla("elder_guardian"), GUARDIAN),
            Map.entry(vanilla("guardian"), GUARDIAN)
    );

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Optional<ResourceLocation> resolve(ResourceLocation entityType) {
        return Optional.ofNullable(FACTIONS.get(entityType));
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
