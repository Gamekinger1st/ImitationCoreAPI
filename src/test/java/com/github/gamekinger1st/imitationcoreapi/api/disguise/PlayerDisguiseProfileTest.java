package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotExtension;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotSerialization;
import com.mojang.authlib.properties.Property;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerDisguiseProfileTest {
    @Test
    void persistsTheRecordedProfileAndSignedTextureProperty() {
        PlayerDisguiseProfile profile = new PlayerDisguiseProfile(
                UUID.randomUUID(),
                "CopiedPlayer",
                Optional.of("texture-value"),
                Optional.of("texture-signature")
        );
        SnapshotExtension extension = PlayerDisguiseProfileExtensions.create(profile);

        assertEquals(Optional.of(profile), PlayerDisguiseProfileExtensions.find(List.of(extension)));
        Property texture = profile.toGameProfile().getProperties().get("textures").iterator().next();
        assertEquals("texture-value", texture.value());
        assertEquals("texture-signature", texture.signature());
    }

    @Test
    void retainsTheProfileWhenTheIdentitySnapshotIsSavedAndLoaded() {
        PlayerDisguiseProfile profile = new PlayerDisguiseProfile(
                UUID.randomUUID(),
                "CopiedPlayer",
                Optional.of("texture-value"),
                Optional.of("texture-signature")
        );
        IdentitySnapshot snapshot = IdentitySnapshot.builder(ResourceLocation.withDefaultNamespace("player"), 20L)
                .displayName("Copied Player")
                .extension(PlayerDisguiseProfileExtensions.create(profile))
                .build();

        IdentitySnapshot loaded = SnapshotSerialization.identityFromTag(SnapshotSerialization.identityToTag(snapshot));

        assertEquals(Optional.of(profile), PlayerDisguiseProfileExtensions.find(loaded.extensions()));
    }

    @Test
    void rejectsInvalidOrOversizedProfileData() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerDisguiseProfile(
                UUID.randomUUID(),
                "CopiedPlayer",
                Optional.of("texture-value"),
                Optional.empty()
        ));
        assertThrows(IllegalArgumentException.class, () -> new PlayerDisguiseProfile(
                UUID.randomUUID(),
                "CopiedPlayer",
                Optional.empty(),
                Optional.of("signature")
        ));
        assertThrows(IllegalArgumentException.class, () -> new PlayerDisguiseProfile(
                UUID.randomUUID(),
                "CopiedPlayer",
                Optional.of("x".repeat(PlayerDisguiseProfile.MAX_TEXTURE_VALUE_LENGTH + 1)),
                Optional.empty()
        ));
    }
}
