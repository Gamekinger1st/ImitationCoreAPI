package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotExtension;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PlayerDisguiseProfileExtensions {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "player_profile");
    public static final int SCHEMA_VERSION = 1;
    private static final String PLAYER_ID = "player_id";
    private static final String ACCOUNT_NAME = "account_name";
    private static final String TEXTURES_VALUE = "textures_value";
    private static final String TEXTURES_SIGNATURE = "textures_signature";

    private PlayerDisguiseProfileExtensions() {
    }

    public static SnapshotExtension create(PlayerDisguiseProfile profile) {
        Objects.requireNonNull(profile, "profile");
        CompoundTag tag = new CompoundTag();
        tag.putUUID(PLAYER_ID, profile.playerId());
        tag.putString(ACCOUNT_NAME, profile.accountName());
        profile.texturesValue().ifPresent(value -> tag.putString(TEXTURES_VALUE, value));
        profile.texturesSignature().ifPresent(signature -> tag.putString(TEXTURES_SIGNATURE, signature));
        return new SnapshotExtension(ID, SCHEMA_VERSION, tag);
    }

    public static Optional<PlayerDisguiseProfile> find(List<SnapshotExtension> extensions) {
        Objects.requireNonNull(extensions, "extensions");
        return extensions.stream()
                .filter(extension -> extension.adapterId().equals(ID))
                .filter(extension -> extension.schemaVersion() == SCHEMA_VERSION)
                .findFirst()
                .flatMap(extension -> fromTag(extension.payload()));
    }

    private static Optional<PlayerDisguiseProfile> fromTag(CompoundTag tag) {
        if (!tag.hasUUID(PLAYER_ID) || !tag.contains(ACCOUNT_NAME, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        Optional<String> textureValue = string(tag, TEXTURES_VALUE);
        Optional<String> textureSignature = string(tag, TEXTURES_SIGNATURE);
        try {
            return Optional.of(new PlayerDisguiseProfile(tag.getUUID(PLAYER_ID), tag.getString(ACCOUNT_NAME), textureValue, textureSignature));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static Optional<String> string(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_STRING) ? Optional.of(tag.getString(key)) : Optional.empty();
    }
}
