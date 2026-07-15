package com.github.gamekinger1st.imitationcoreapi.internal.targeting;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.targeting.MobFactionResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TagBackedMobFactionResolver implements MobFactionResolver {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "tag_backed_mob_factions");
    private static final List<TagFaction> DEFAULTS = List.of(
            tagFaction("tensura", "daemons", "tensura_daemon"),
            tagFaction("tensura", "slimes", "tensura_slime"),
            tagFaction("tensura", "hostile_monster", "tensura_hostile_monster"),
            tagFaction("tensura", "neutral_monster", "tensura_neutral_monster"),
            tagFaction("tensura", "animal_prey", "tensura_animal_prey")
    );

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return -100;
    }

    @Override
    public Optional<ResourceLocation> resolve(ResourceLocation entityType) {
        Objects.requireNonNull(entityType, "entityType");
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entityType)
                .flatMap(type -> DEFAULTS.stream()
                        .filter(faction -> type.is(faction.tag()))
                        .map(TagFaction::faction)
                        .findFirst());
    }

    private static TagFaction tagFaction(String namespace, String tag, String faction) {
        return new TagFaction(
                TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(namespace, tag)),
                ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, faction)
        );
    }

    private record TagFaction(TagKey<EntityType<?>> tag, ResourceLocation faction) {
    }
}
