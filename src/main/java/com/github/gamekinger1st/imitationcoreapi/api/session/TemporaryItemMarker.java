package com.github.gamekinger1st.imitationcoreapi.api.session;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TemporaryItemMarker {
    public static final String ROOT_KEY = ImitationCoreApi.MOD_ID + "_temporary";
    public static final String OWNER_KEY = "owner";
    public static final String SESSION_KEY = "session";
    public static final String REFERENCE_KEY = "reference";
    public static final String KIND_KEY = "kind";

    private TemporaryItemMarker() {
    }

    public static ItemStack mark(ItemStack stack, UUID ownerId, UUID sessionId, UUID referenceId, ResourceLocation kind) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(kind, "kind");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Cannot mark an empty item stack");
        }
        CompoundTag root = customData(stack);
        CompoundTag marker = new CompoundTag();
        marker.putUUID(OWNER_KEY, ownerId);
        marker.putUUID(SESSION_KEY, sessionId);
        marker.putUUID(REFERENCE_KEY, referenceId);
        marker.putString(KIND_KEY, kind.toString());
        root.put(ROOT_KEY, marker);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }

    public static Optional<Ownership> ownership(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        CompoundTag root = customData(stack);
        if (!root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag marker = root.getCompound(ROOT_KEY);
        if (!marker.hasUUID(OWNER_KEY) || !marker.hasUUID(SESSION_KEY) || !marker.hasUUID(REFERENCE_KEY) || !marker.contains(KIND_KEY, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        ResourceLocation kind = ResourceLocation.tryParse(marker.getString(KIND_KEY));
        return kind == null
                ? Optional.empty()
                : Optional.of(new Ownership(marker.getUUID(OWNER_KEY), marker.getUUID(SESSION_KEY), marker.getUUID(REFERENCE_KEY), kind));
    }

    public static boolean ownedBy(ItemStack stack, UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        return ownership(stack).map(ownership -> ownership.ownerId().equals(ownerId)).orElse(false);
    }

    public static boolean ownedBySession(ItemStack stack, UUID ownerId, UUID sessionId) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(sessionId, "sessionId");
        return ownership(stack)
                .map(ownership -> ownership.ownerId().equals(ownerId) && ownership.sessionId().equals(sessionId))
                .orElse(false);
    }

    public static ItemStack clear(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (!stack.isEmpty()) {
            CompoundTag root = customData(stack);
            root.remove(ROOT_KEY);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        return stack;
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public record Ownership(UUID ownerId, UUID sessionId, UUID referenceId, ResourceLocation kind) {
        public Ownership {
            Objects.requireNonNull(ownerId, "ownerId");
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(referenceId, "referenceId");
            Objects.requireNonNull(kind, "kind");
        }
    }
}
