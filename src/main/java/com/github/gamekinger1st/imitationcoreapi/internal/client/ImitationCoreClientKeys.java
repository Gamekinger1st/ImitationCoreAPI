package com.github.gamekinger1st.imitationcoreapi.internal.client;

import com.github.gamekinger1st.imitationcoreapi.api.network.ActivateFormAbilityPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ImitationCoreClientKeys {
    private static final KeyMapping ACTIVATE_FORM_ABILITY = new KeyMapping(
            "key.imitationcoreapi.activate_form_ability",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.imitationcoreapi"
    );

    private ImitationCoreClientKeys() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ImitationCoreClientKeys::registerMappings);
        NeoForge.EVENT_BUS.addListener(ImitationCoreClientKeys::onClientTick);
    }

    private static void registerMappings(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE_FORM_ABILITY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            while (ACTIVATE_FORM_ABILITY.consumeClick()) {
            }
            return;
        }
        while (ACTIVATE_FORM_ABILITY.consumeClick()) {
            PacketDistributor.sendToServer(ActivateFormAbilityPayload.INSTANCE);
        }
    }
}
