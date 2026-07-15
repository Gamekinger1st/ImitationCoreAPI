package com.github.gamekinger1st.imitationcoreapi.internal.disguise;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseState;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguisePresentation;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguisePresentationAdapter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.OptionalDouble;

public final class DefaultDisguisePresentationAdapter implements DisguisePresentationAdapter {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "default_disguise_presentation");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean supports(Entity subject, ClientDisguiseState state) {
        return true;
    }

    @Override
    public DisguisePresentation presentation(Entity subject, ClientDisguiseState state) {
        OptionalDouble eyeHeight = state.visualData().contains("eye_height")
                ? OptionalDouble.of(state.visualData().getFloat("eye_height"))
                : OptionalDouble.empty();
        DisguisePresentation.RenderMode mode = state.entityType().equals(ResourceLocation.withDefaultNamespace("player"))
                ? DisguisePresentation.RenderMode.FAKE_PLAYER
                : DisguisePresentation.RenderMode.FAKE_ENTITY;
        return new DisguisePresentation(mode, true, eyeHeight);
    }
}
