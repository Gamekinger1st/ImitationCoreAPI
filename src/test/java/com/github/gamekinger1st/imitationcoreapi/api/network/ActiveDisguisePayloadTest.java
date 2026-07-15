package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityLevel;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.PlayerDisguiseProfile;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActiveDisguisePayloadTest {
    @Test
    void createsAnImmutableBoundedClientState() {
        CompoundTag entityData = new CompoundTag();
        entityData.putString("name", "Zombie");
        PlayerDisguiseProfile profile = new PlayerDisguiseProfile(
                UUID.randomUUID(),
                "CopiedPlayer",
                Optional.of("texture-value"),
                Optional.of("texture-signature")
        );
        DisguiseAppraisalSnapshot appraisal = new DisguiseAppraisalSnapshot(18F, 20F, 4, Optional.of(new TensuraVitals(120D, 80D, 40D, 16D)));
        ActiveDisguisePayload payload = new ActiveDisguisePayload(
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                TransformationScope.SURFACE,
                ResourceLocation.withDefaultNamespace("zombie"),
                "Zombie",
                entityData,
                new CompoundTag(),
                CompatibilityLevel.VISUAL,
                0L,
                Optional.of(profile),
                Optional.of(appraisal)
        );
        entityData.putString("name", "Changed");

        assertEquals("Zombie", payload.entityData().getString("name"));
        assertEquals("Zombie", payload.toState().entityData().getString("name"));
        assertEquals(TransformationScope.SURFACE, payload.toState().scope());
        assertEquals(Optional.of(profile), payload.toState().playerProfile());
        assertEquals(Optional.of(appraisal), payload.toState().appraisal());
    }

    @Test
    void rejectsOversizedVisualData() {
        CompoundTag data = new CompoundTag();
        data.putString("data", "x".repeat(ActiveDisguisePayload.MAX_ENTITY_DATA_BYTES + 1));

        assertThrows(IllegalArgumentException.class, () -> new ActiveDisguisePayload(
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ResourceLocation.withDefaultNamespace("zombie"),
                "Zombie",
                data,
                new CompoundTag(),
                CompatibilityLevel.VISUAL,
                0L
        ));
    }
}
