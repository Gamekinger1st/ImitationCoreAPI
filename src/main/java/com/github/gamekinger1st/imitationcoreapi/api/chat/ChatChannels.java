package com.github.gamekinger1st.imitationcoreapi.api.chat;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.resources.ResourceLocation;

public final class ChatChannels {
    public static final ResourceLocation GLOBAL = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "global");
    public static final ResourceLocation LOCAL = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "local");
    public static final ResourceLocation PARTY = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "party");
    public static final ResourceLocation DIRECT = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "direct");
    public static final ResourceLocation SYSTEM = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "system");
    public static final ResourceLocation ACTION_BAR = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "action_bar");

    private ChatChannels() {
    }
}
