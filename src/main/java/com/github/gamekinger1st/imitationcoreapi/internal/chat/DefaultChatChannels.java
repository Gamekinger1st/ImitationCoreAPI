package com.github.gamekinger1st.imitationcoreapi.internal.chat;

import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelKind;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelProvider;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelRequest;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannels;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatDelivery;
import com.github.gamekinger1st.imitationcoreapi.internal.config.ImitationCoreConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class DefaultChatChannels {
    private DefaultChatChannels() {
    }

    public static List<ChatChannelProvider> create() {
        return List.of(new Global(), new Local(), new Direct());
    }

    private static final class Global implements ChatChannelProvider {
        @Override
        public ResourceLocation id() {
            return ChatChannels.GLOBAL;
        }

        @Override
        public ChatChannelKind kind() {
            return ChatChannelKind.GLOBAL;
        }

        @Override
        public Optional<ChatDelivery> route(ChatChannelRequest request) {
            return Optional.of(new ChatDelivery(kind(), request.sender().server.getPlayerList().getPlayers()));
        }
    }

    private static final class Local implements ChatChannelProvider {
        @Override
        public ResourceLocation id() {
            return ChatChannels.LOCAL;
        }

        @Override
        public ChatChannelKind kind() {
            return ChatChannelKind.LOCAL;
        }

        @Override
        public Optional<ChatDelivery> route(ChatChannelRequest request) {
            double range = ImitationCoreConfig.localChatRange();
            double rangeSquared = range * range;
            return Optional.of(new ChatDelivery(kind(), request.sender().server.getPlayerList().getPlayers().stream()
                    .filter(player -> player.level() == request.sender().level())
                    .filter(player -> player.distanceToSqr(request.sender()) <= rangeSquared)
                    .toList()));
        }
    }

    private static final class Direct implements ChatChannelProvider {
        @Override
        public ResourceLocation id() {
            return ChatChannels.DIRECT;
        }

        @Override
        public ChatChannelKind kind() {
            return ChatChannelKind.DIRECT;
        }

        @Override
        public Optional<ChatDelivery> route(ChatChannelRequest request) {
            return request.targetPlayerId().flatMap(targetId -> Optional.ofNullable(request.sender().server.getPlayerList().getPlayer(targetId)))
                    .map(target -> target.getUUID().equals(request.sender().getUUID())
                            ? new ChatDelivery(kind(), List.of(request.sender()))
                            : new ChatDelivery(kind(), List.of(request.sender(), target)));
        }
    }
}
