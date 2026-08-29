package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannels;
import com.github.gamekinger1st.imitationcoreapi.api.network.SelectChatChannelPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.SendCoreChatPayload;
import com.github.gamekinger1st.imitationcoreapi.internal.client.ChatDraftStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = ChatScreen.class, remap = false)
public abstract class MixinChatScreenDraft {
    @Shadow
    protected EditBox input;

    @Shadow
    private String initial;

    @Unique
    private boolean imitationcoreapi$draftRestored;

    @Unique
    private boolean imitationcoreapi$submitted;

    @Shadow
    public abstract String normalizeChatMessage(String message);

    @Inject(method = "init()V", at = @At("TAIL"))
    private void imitationcoreapi$restoreDraft(CallbackInfo callback) {
        if (imitationcoreapi$draftRestored) {
            return;
        }
        imitationcoreapi$draftRestored = true;
        if (ImitationApi.clientChatProtocol().replacementEnabled()) {
            ImitationApi.clientChatStore().markRead(ImitationApi.clientChatProtocol().activeChannel());
        }
        String restored = ChatDraftStore.restore(initial);
        if (initial.isEmpty() && input.getValue().isEmpty()) {
            input.setValue(restored);
        }
    }

    @Inject(method = "handleChatInput(Ljava/lang/String;Z)V", at = @At("HEAD"))
    private void imitationcoreapi$clearSubmittedDraft(String message, boolean addToRecentChat, CallbackInfo callback) {
        imitationcoreapi$submitted = true;
        ChatDraftStore.clear();
    }

    @Inject(method = "handleChatInput(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$sendCoreChat(String message, boolean addToRecentChat, CallbackInfo callback) {
        if (!ImitationApi.clientChatProtocol().replacementEnabled()) {
            return;
        }
        String normalized = normalizeChatMessage(message);
        if (normalized.isEmpty() || normalized.startsWith("/")) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (addToRecentChat) {
            minecraft.gui.getChat().addRecentChat(normalized);
        }
        imitationcoreapi$submitted = true;
        ChatDraftStore.clear();
        PacketDistributor.sendToServer(new SendCoreChatPayload(ImitationApi.clientChatProtocol().activeChannel(), normalized, Optional.empty()));
        callback.cancel();
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void imitationcoreapi$renderChannelTabs(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (!ImitationApi.clientChatProtocol().replacementEnabled()) {
            return;
        }
        int y = Minecraft.getInstance().getWindow().getGuiScaledHeight() - 29;
        imitationcoreapi$renderTab(graphics, ChatChannels.GLOBAL, Component.translatable("chat.imitationcoreapi.channel.global"), 4, y);
        imitationcoreapi$renderTab(graphics, ChatChannels.LOCAL, Component.translatable("chat.imitationcoreapi.channel.local"), 76, y);
    }

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$selectChannel(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button != 0 || !ImitationApi.clientChatProtocol().replacementEnabled()) {
            return;
        }
        int y = Minecraft.getInstance().getWindow().getGuiScaledHeight() - 29;
        ResourceLocation selected = null;
        if (mouseY >= y && mouseY <= y + 13 && mouseX >= 4 && mouseX <= 72) {
            selected = ChatChannels.GLOBAL;
        } else if (mouseY >= y && mouseY <= y + 13 && mouseX >= 76 && mouseX <= 144) {
            selected = ChatChannels.LOCAL;
        }
        if (selected == null) {
            return;
        }
        ImitationApi.clientChatStore().markRead(selected);
        PacketDistributor.sendToServer(new SelectChatChannelPayload(selected));
        callback.setReturnValue(true);
    }

    @Unique
    private void imitationcoreapi$renderTab(GuiGraphics graphics, ResourceLocation channel, Component label, int x, int y) {
        boolean active = ImitationApi.clientChatProtocol().activeChannel().equals(channel);
        int background = active ? 0xCC3A6EA5 : 0xAA111111;
        graphics.fill(x, y, x + 68, y + 13, background);
        int unread = ImitationApi.clientChatStore().unread(channel);
        Component text = unread > 0 ? label.copy().append(" (" + unread + ")") : label;
        graphics.drawString(Minecraft.getInstance().font, text, x + 3, y + 3, active ? 0xFFFFFF : 0xC0C0C0, false);
    }

    @Inject(method = "removed()V", at = @At("HEAD"))
    private void imitationcoreapi$saveUnsubmittedDraft(CallbackInfo callback) {
        if (imitationcoreapi$submitted) {
            initial = "";
            ChatDraftStore.clear();
            return;
        }
        initial = input == null ? initial : input.getValue();
        ChatDraftStore.save(initial);
    }
}
