package com.github.gamekinger1st.imitationcoreapi.internal.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaChatDecision;
import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaChatProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ImitatorPersonaChatProvider implements PersonaChatProvider {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "imitator_persona");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public PersonaChatDecision resolve(ServerPlayer sender, String rawText) {
        return ImitationApi.imitatorIntegrations().activePersona(sender).map(PersonaChatDecision::replace).orElseGet(PersonaChatDecision::passthrough);
    }
}
