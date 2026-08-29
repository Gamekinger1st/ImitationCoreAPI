package com.github.gamekinger1st.imitationcoreapi.internal.chat;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelRequest;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatDeliveryResult;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatMessageSource;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatService;
import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import com.github.gamekinger1st.imitationcoreapi.internal.config.ImitationCoreConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;

public final class PersonaChatServer {
    private static boolean registered;

    private PersonaChatServer() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(PersonaChatServer::onServerChat);
        registered = true;
    }

    private static void onServerChat(ServerChatEvent event) {
        ChatService chats = ImitationCoreServices.chats(event.getPlayer());
        ChatDeliveryResult result;
        try {
            result = chats.route(new ChatChannelRequest(event.getPlayer(), chats.activeChannel(event.getPlayer()), event.getRawText(), java.util.Optional.empty(), ChatMessageSource.VANILLA_SIGNED_PLAYER));
        } catch (RuntimeException | LinkageError exception) {
            ImitationCoreApi.LOGGER.error("Core chat routing failed for sender {}; preserving vanilla signed delivery", event.getPlayer().getUUID(), exception);
            return;
        }
        event.setCanceled(true);
        if (!result.accepted()) {
            result.reason().ifPresent(reason -> event.getPlayer().sendSystemMessage(Component.literal(reason).withStyle(ChatFormatting.RED)));
            return;
        }
        result.envelope().ifPresent(envelope -> {
            if (ImitationCoreConfig.logChatContent()) {
                ImitationCoreApi.LOGGER.info("Core chat sender={} persona={} channel={} message={}", event.getPlayer().getUUID(), envelope.persona().map(persona -> persona.personaId().toString()).orElse("none"), envelope.channelId(), event.getRawText());
            } else {
                ImitationCoreApi.LOGGER.info("Core chat sender={} persona={} channel={} messageId={}", event.getPlayer().getUUID(), envelope.persona().map(persona -> persona.personaId().toString()).orElse("none"), envelope.channelId(), envelope.messageId());
            }
        });
    }
}
