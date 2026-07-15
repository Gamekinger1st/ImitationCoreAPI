package com.github.gamekinger1st.imitationcoreapi.internal.chat;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaChatDecision;
import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaChatDisposition;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelRequest;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatMessageSource;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatService;
import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
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
        PersonaChatDecision decision = ImitationApi.personaChats().resolve(event.getPlayer(), event.getRawText());
        event.setCanceled(true);
        if (decision.disposition() == PersonaChatDisposition.BLOCK) {
            decision.reason().ifPresent(reason -> event.getPlayer().sendSystemMessage(Component.literal(reason)));
            return;
        }
        ChatService chats = ImitationCoreServices.chats(event.getPlayer());
        chats.route(new ChatChannelRequest(event.getPlayer(), chats.activeChannel(event.getPlayer()), event.getRawText(), java.util.Optional.empty(), ChatMessageSource.VANILLA_SIGNED_PLAYER))
                .envelope()
                .ifPresent(envelope -> ImitationCoreApi.LOGGER.info("Core chat sender={} persona={} channel={} message={}", event.getPlayer().getUUID(), envelope.persona().map(persona -> persona.personaId().toString()).orElse("none"), envelope.channelId(), event.getRawText()));
    }
}
