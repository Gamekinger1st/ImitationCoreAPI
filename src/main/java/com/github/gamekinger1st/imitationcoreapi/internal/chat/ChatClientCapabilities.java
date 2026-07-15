package com.github.gamekinger1st.imitationcoreapi.internal.chat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatClientCapabilities {
    private static final Map<UUID, Integer> VERSIONS = new ConcurrentHashMap<>();

    private ChatClientCapabilities() {
    }

    public static void accept(UUID playerId, int protocolVersion) {
        VERSIONS.put(playerId, protocolVersion);
    }

    public static boolean supports(UUID playerId, int protocolVersion) {
        return VERSIONS.getOrDefault(playerId, 0) >= protocolVersion;
    }

    public static void clear(UUID playerId) {
        VERSIONS.remove(playerId);
    }
}
