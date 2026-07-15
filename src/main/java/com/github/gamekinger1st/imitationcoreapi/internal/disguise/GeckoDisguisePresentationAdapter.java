package com.github.gamekinger1st.imitationcoreapi.internal.disguise;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseState;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguisePresentation;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguisePresentationAdapter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.OptionalDouble;

public final class GeckoDisguisePresentationAdapter implements DisguisePresentationAdapter {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "geckolib_disguise_presentation");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean supports(Entity subject, ClientDisguiseState state) {
        CompoundTag visualData = state.visualData();
        return visualData.contains("gecko_controller_states") || visualData.contains("gecko_controllers");
    }

    @Override
    public DisguisePresentation presentation(Entity subject, ClientDisguiseState state) {
        CompoundTag visualData = state.visualData();
        OptionalDouble eyeHeight = visualData.contains("eye_height")
                ? OptionalDouble.of(visualData.getFloat("eye_height"))
                : OptionalDouble.empty();
        return new DisguisePresentation(DisguisePresentation.RenderMode.GECKO, true, eyeHeight);
    }
}
