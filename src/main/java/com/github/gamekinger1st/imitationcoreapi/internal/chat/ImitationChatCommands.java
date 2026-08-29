package com.github.gamekinger1st.imitationcoreapi.internal.chat;

import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelRequest;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelSelectionResult;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannels;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatMessageSource;
import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import com.github.gamekinger1st.imitationcoreapi.internal.discord.DiscordChatBridge;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.github.gamekinger1st.imitationcoreapi.internal.network.ImitationCoreNetwork;

import java.util.Optional;
import java.util.UUID;

public final class ImitationChatCommands {
    private static boolean registered;

    private ImitationChatCommands() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(ImitationChatCommands::onRegisterCommands);
        registered = true;
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(root("imitchat"));
        dispatcher.register(root("imitationchat"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
                .then(Commands.literal("global")
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .executes(context -> select(context, ChatChannels.GLOBAL))
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(context -> send(context, ChatChannels.GLOBAL, Optional.empty()))))
                .then(Commands.literal("local")
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .executes(context -> select(context, ChatChannels.LOCAL))
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(context -> send(context, ChatChannels.LOCAL, Optional.empty()))))
                .then(Commands.literal("direct")
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(context -> send(context, ChatChannels.DIRECT, directTarget(context))))))
                .then(Commands.literal("discord")
                        .requires(source -> source.hasPermission(2))
                        .executes(ImitationChatCommands::discordStatus)
                        .then(Commands.literal("reload").executes(ImitationChatCommands::reloadDiscord)));
    }

    private static int discordStatus(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(DiscordChatBridge.status(context.getSource().getServer())), false);
        return 1;
    }

    private static int reloadDiscord(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(DiscordChatBridge.reload(context.getSource().getServer())), true);
        return 1;
    }

    private static int send(CommandContext<CommandSourceStack> context, ResourceLocation channelId, Optional<UUID> targetPlayerId) {
        ServerPlayer sender = context.getSource().getPlayer();
        if (sender == null) {
            return 0;
        }
        try {
            com.github.gamekinger1st.imitationcoreapi.api.chat.ChatDeliveryResult result = ImitationCoreServices.chats(sender)
                    .route(new ChatChannelRequest(sender, channelId, StringArgumentType.getString(context, "message"), targetPlayerId, ChatMessageSource.CORE_UNSIGNED_PLAYER));
            return result.accepted() ? 1 : fail(context, result.reason().orElse("The chat message was rejected"));
        } catch (IllegalArgumentException exception) {
            return fail(context, exception.getMessage());
        }
    }

    private static int select(CommandContext<CommandSourceStack> context, ResourceLocation channelId) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        ChatChannelSelectionResult result = ImitationCoreServices.chats(player).selectChannel(player, channelId);
        if (!result.accepted()) {
            return fail(context, result.message());
        }
        context.getSource().sendSuccess(() -> Component.literal(result.message()), false);
        ImitationCoreNetwork.advertiseChat(player);
        return 1;
    }

    private static Optional<UUID> directTarget(CommandContext<CommandSourceStack> context) {
        try {
            return Optional.of(EntityArgument.getPlayer(context, "target").getUUID());
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private static int fail(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendFailure(Component.literal(message));
        return 0;
    }
}
