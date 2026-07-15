package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public record PlayerDisguiseProfile(UUID playerId, String accountName, Optional<String> texturesValue, Optional<String> texturesSignature) {
    public static final int MAX_ACCOUNT_NAME_LENGTH = 16;
    public static final int MAX_TEXTURE_VALUE_LENGTH = 32_768;
    public static final int MAX_TEXTURE_SIGNATURE_LENGTH = 2_048;
    private static final Pattern ACCOUNT_NAME = Pattern.compile("[A-Za-z0-9_]{1," + MAX_ACCOUNT_NAME_LENGTH + "}");

    public PlayerDisguiseProfile {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(accountName, "accountName");
        Objects.requireNonNull(texturesValue, "texturesValue");
        Objects.requireNonNull(texturesSignature, "texturesSignature");
        accountName = accountName.strip();
        if (!ACCOUNT_NAME.matcher(accountName).matches()) {
            throw new IllegalArgumentException("accountName must be a valid Minecraft profile name");
        }
        texturesValue = texturesValue.map(String::strip).filter(value -> !value.isEmpty());
        texturesSignature = texturesSignature.map(String::strip).filter(signature -> !signature.isEmpty());
        if (texturesValue.isEmpty() != texturesSignature.isEmpty()) {
            throw new IllegalArgumentException("Texture properties must contain both a value and a signature");
        }
        if (texturesValue.map(String::length).orElse(0) > MAX_TEXTURE_VALUE_LENGTH) {
            throw new IllegalArgumentException("Texture property value exceeds the configured limit");
        }
        if (texturesSignature.map(String::length).orElse(0) > MAX_TEXTURE_SIGNATURE_LENGTH) {
            throw new IllegalArgumentException("Texture property signature exceeds the configured limit");
        }
    }

    public static Optional<PlayerDisguiseProfile> from(GameProfile profile) {
        Objects.requireNonNull(profile, "profile");
        UUID playerId = profile.getId();
        String accountName = profile.getName();
        if (playerId == null || accountName == null || !ACCOUNT_NAME.matcher(accountName).matches()) {
            return Optional.empty();
        }
        Optional<Property> textures = profile.getProperties().get("textures").stream().filter(Property::hasSignature).findFirst();
        try {
            return Optional.of(new PlayerDisguiseProfile(
                    playerId,
                    accountName,
                    textures.map(Property::value),
                    textures.map(Property::signature)
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public GameProfile toGameProfile() {
        GameProfile profile = new GameProfile(playerId, accountName);
        texturesValue.ifPresent(value -> profile.getProperties().put("textures", new Property("textures", value, texturesSignature.orElseThrow())));
        return profile;
    }
}
