package com.github.gamekinger1st.imitationcoreapi.internal.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorActionResult;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorHandlerService;
import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationLifecycleReason;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateStatus;
import com.github.gamekinger1st.imitationcoreapi.internal.network.ImitationCoreNetwork;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Optional;

public final class ImitatorAdminCommands {
    private static final String TROVERHAUL_MOD_ID = "troverhaul";
    private static boolean registered;

    private ImitatorAdminCommands() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(ImitatorAdminCommands::onRegisterCommands);
        registered = true;
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!ModList.get().isLoaded(TROVERHAUL_MOD_ID)) {
            return;
        }
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(root("troimitator"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ImitatorAdminCommands::inspect)))
                .then(Commands.literal("revert")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ImitatorAdminCommands::forceRevert)))
                .then(Commands.literal("forms")
                        .then(Commands.literal("clear")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0, 255))
                                                .executes(ImitatorAdminCommands::clearSlot))))
                        .then(Commands.literal("clearall")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ImitatorAdminCommands::clearAll))))
                .then(Commands.literal("recover")
                        .then(Commands.literal("all")
                                .executes(ImitatorAdminCommands::recoverAll)));
    }

    private static int inspect(CommandContext<CommandSourceStack> context) {
        ServerPlayer target = target(context);
        ImitatorHandlerService handlers = ImitationCoreServices.imitatorHandlers(target);
        List<TransformationSession> sessions = handlers.sessionsFor(target);
        Optional<TransformationSession> active = handlers.activeSessionFor(target);
        if (sessions.isEmpty()) {
            return success(context, target.getGameProfile().getName() + " has no Imitator sessions");
        }
        TransformationSession latest = sessions.getFirst();
        long outstanding = latest.temporaryState().stream().filter(reference -> reference.status().requiresReconciliation()).count();
        long quarantined = latest.temporaryState().stream().filter(reference -> reference.status() == TemporaryStateStatus.QUARANTINED).count();
        String message = target.getGameProfile().getName()
                + " sessions=" + sessions.size()
                + ", active=" + active.isPresent()
                + ", latest=" + latest.sessionId()
                + ", state=" + latest.state()
                + ", scope=" + latest.scope()
                + ", revision=" + latest.revision()
                + ", outstanding=" + outstanding
                + ", quarantined=" + quarantined;
        return success(context, message);
    }

    private static int clearSlot(CommandContext<CommandSourceStack> context) {
        ServerPlayer target = target(context);
        int slot = IntegerArgumentType.getInteger(context, "slot");
        ImitatorActionResult result = ImitationCoreServices.imitatorHandlers(target).clearForm(target, slot);
        ImitationCoreNetwork.syncFormLibrary(target);
        return report(context, result.accepted(), result.message());
    }

    private static int clearAll(CommandContext<CommandSourceStack> context) {
        ServerPlayer target = target(context);
        ImitatorActionResult result = ImitationCoreServices.imitatorHandlers(target).clearAllForms(target);
        ImitationCoreNetwork.syncFormLibrary(target);
        return report(context, result.accepted(), result.message());
    }

    private static int forceRevert(CommandContext<CommandSourceStack> context) {
        ServerPlayer target = target(context);
        ImitatorHandlerService handlers = ImitationCoreServices.imitatorHandlers(target);
        Optional<TransformationSession> session = handlers.activeSessionFor(target).or(() -> handlers.sessionFor(target));
        if (session.isEmpty()) {
            return fail(context, "The target has no active or recoverable Imitator session");
        }
        SessionTransitionResult result = handlers.requestReversion(target, session.get().sessionId(), TransformationLifecycleReason.FORCE_REVERT);
        result.session().ifPresent(updated -> ImitationCoreNetwork.syncToTrackingAndSelf(target, updated));
        return report(context, result.accepted(), result.accepted() ? "Reversion requested for " + target.getGameProfile().getName() : result.message());
    }

    private static int recoverAll(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        List<SessionTransitionResult> results = ImitationCoreServices.applications(server).recoverInterruptedSessions(context.getSource().getLevel().getGameTime());
        int accepted = 0;
        for (SessionTransitionResult result : results) {
            if (result.accepted()) {
                accepted++;
            }
            result.session().ifPresent(session -> {
                ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerId());
                if (owner != null) {
                    ImitationCoreNetwork.syncToTrackingAndSelf(owner, session);
                }
            });
        }
        return success(context, "Recovery requested for " + accepted + " of " + results.size() + " interrupted sessions");
    }

    private static ServerPlayer target(CommandContext<CommandSourceStack> context) {
        try {
            return EntityArgument.getPlayer(context, "target");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static int report(CommandContext<CommandSourceStack> context, boolean accepted, String message) {
        return accepted ? success(context, message) : fail(context, message);
    }

    private static int success(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendFailure(Component.literal(message));
        return 0;
    }
}
