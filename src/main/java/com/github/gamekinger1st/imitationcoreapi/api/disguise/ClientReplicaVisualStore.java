package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientReplicaVisualStore {
    public static final int MAX_REPLICAS = 2_048;
    private final Map<Integer, CompoundTag> equipment = new ConcurrentHashMap<>();

    public void put(int entityId, CompoundTag visualEquipment) {
        if (entityId < 0) {
            throw new IllegalArgumentException("entityId cannot be negative");
        }
        equipment.put(entityId, visualEquipment.copy());
        while (equipment.size() > MAX_REPLICAS) {
            Integer oldest = equipment.keySet().stream().min(Integer::compareTo).orElse(null);
            if (oldest == null) {
                break;
            }
            equipment.remove(oldest);
        }
    }

    public Optional<CompoundTag> get(int entityId) {
        return Optional.ofNullable(equipment.get(entityId)).map(CompoundTag::copy);
    }

    public void clearEntity(int entityId) {
        equipment.remove(entityId);
    }

    public void clearAll() {
        equipment.clear();
    }
}
