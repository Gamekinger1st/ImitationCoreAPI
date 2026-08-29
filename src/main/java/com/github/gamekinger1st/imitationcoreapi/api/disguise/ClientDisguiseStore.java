package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientDisguiseStore implements ClientDisguiseStateListener {
    public static final int MAX_ACTIVE_DISGUISES = 2_048;
    private final Map<Integer, ClientDisguiseState> states = new ConcurrentHashMap<>();

    public Optional<ClientDisguiseState> get(int entityId) {
        return Optional.ofNullable(states.get(entityId));
    }

    public Collection<ClientDisguiseState> active() {
        return java.util.List.copyOf(states.values());
    }

    public void clearEntity(int entityId) {
        states.remove(entityId);
    }

    public void clearAll() {
        states.clear();
    }

    @Override
    public void onDisguiseActivated(ClientDisguiseState state) {
        states.compute(state.entityId(), (id, current) -> current == null
                || !current.ownerId().equals(state.ownerId())
                || !current.sessionId().equals(state.sessionId())
                || state.revision() >= current.revision() ? state : current);
        while (states.size() > MAX_ACTIVE_DISGUISES) {
            Integer oldest = states.keySet().stream().min(Integer::compareTo).orElse(null);
            if (oldest == null) {
                break;
            }
            states.remove(oldest);
        }
    }

    @Override
    public void onDisguiseCleared(int entityId, UUID ownerId) {
        states.computeIfPresent(entityId, (id, current) -> current.ownerId().equals(ownerId) ? null : current);
    }
}
