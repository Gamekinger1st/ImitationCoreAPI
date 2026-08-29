package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ImitatorMenuContinuationRegistry {
    public static final long MAX_WAIT_TICKS = 2_400L;
    public static final int MAX_PENDING_PLAYERS = 1_024;

    private final Map<UUID, PendingContinuation> pending = new LinkedHashMap<>();

    public synchronized void stage(ServerPlayer player, ImitatorMenuRequest request, ImitatorSkillMode mode, ImitatorMenuContinuation continuation) {
        Objects.requireNonNull(player, "player");
        stage(player.getUUID(), request, mode, player.level().getGameTime(), continuation);
    }

    public synchronized Optional<String> resume(ServerPlayer player, ImitatorSkillMode mode, int selectedSlot) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(mode, "mode");
        PendingContinuation continuation = pending.remove(player.getUUID());
        if (continuation == null) {
            return Optional.empty();
        }
        if (continuation.expiresAt() < player.level().getGameTime()) {
            return Optional.of("The form-selection action expired; activate Imitator again");
        }
        if (continuation.mode() != mode) {
            return Optional.of("The Imitator mode changed before the form was selected");
        }
        try {
            continuation.continuation().resume(player, selectedSlot);
            return Optional.empty();
        } catch (RuntimeException | LinkageError exception) {
            ImitationCoreApi.LOGGER.error("An Imitator form-selection continuation failed", exception);
            String message = exception.getMessage();
            return Optional.of(message == null || message.isBlank() ? "The selected Imitator action could not continue" : message);
        }
    }

    public synchronized void clear(UUID playerId) {
        pending.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    public synchronized void clearAll() {
        pending.clear();
    }

    synchronized void stage(UUID playerId, ImitatorMenuRequest request, ImitatorSkillMode mode, long gameTime, ImitatorMenuContinuation continuation) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(continuation, "continuation");
        if (request != ImitatorMenuRequest.SELECT_TRANSFORM_FORM) {
            throw new IllegalArgumentException("Only form-selection menus can continue an Imitator action");
        }
        if (mode != ImitatorSkillMode.TRANSFORM && mode != ImitatorSkillMode.REPLICA) {
            throw new IllegalArgumentException("Only Transform and Replica can continue after form selection");
        }
        prune(gameTime);
        if (!pending.containsKey(playerId) && pending.size() >= MAX_PENDING_PLAYERS) {
            UUID oldest = pending.keySet().iterator().next();
            pending.remove(oldest);
        }
        pending.put(playerId, new PendingContinuation(mode, expiresAt(gameTime), continuation));
    }

    synchronized Optional<PendingContinuation> take(UUID playerId, ImitatorSkillMode mode, long gameTime) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(mode, "mode");
        PendingContinuation continuation = pending.remove(playerId);
        if (continuation == null || continuation.expiresAt() < gameTime || continuation.mode() != mode) {
            return Optional.empty();
        }
        return Optional.of(continuation);
    }

    synchronized int pendingCount(long gameTime) {
        prune(gameTime);
        return pending.size();
    }

    private void prune(long gameTime) {
        pending.values().removeIf(continuation -> continuation.expiresAt() < gameTime);
    }

    private static long expiresAt(long gameTime) {
        return gameTime > Long.MAX_VALUE - MAX_WAIT_TICKS ? Long.MAX_VALUE : gameTime + MAX_WAIT_TICKS;
    }

    record PendingContinuation(ImitatorSkillMode mode, long expiresAt, ImitatorMenuContinuation continuation) {
    }
}
