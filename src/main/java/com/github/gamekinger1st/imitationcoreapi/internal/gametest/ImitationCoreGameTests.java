package com.github.gamekinger1st.imitationcoreapi.internal.gametest;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorTargetEligibility;
import com.github.gamekinger1st.imitationcoreapi.api.replica.ReplicaEntityTags;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotCaptureService;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotLimits;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.UUID;

@GameTestHolder(ImitationCoreApi.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ImitationCoreGameTests {
    private ImitationCoreGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void livingEntitySnapshotsCapturePhysicalState(GameTestHelper helper) {
        Chicken chicken = helper.spawn(EntityType.CHICKEN, 1, 2, 1);
        var result = new SnapshotCaptureService(ImitationApi.adapters(), SnapshotLimits.DEFAULT)
                .capture(chicken, Optional.empty(), helper.getLevel().getGameTime());
        helper.assertTrue(result.snapshot().entityType().equals(ResourceLocation.withDefaultNamespace("chicken")), "Snapshot did not preserve the chicken entity type");
        helper.assertTrue(result.snapshot().visualData().getFloat("bb_width") > 0F, "Snapshot did not preserve physical width");
        helper.assertTrue(result.snapshot().visualData().getFloat("bb_height") > 0F, "Snapshot did not preserve physical height");
        helper.assertTrue(result.snapshot().visualData().contains("attributes"), "Snapshot did not preserve living attributes");
        var movement = chicken.getAttribute(Attributes.MOVEMENT_SPEED);
        movement.addPermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "gametest_speed"), 0.125D, AttributeModifier.Operation.ADD_VALUE));
        var effectiveResult = new SnapshotCaptureService(ImitationApi.adapters(), SnapshotLimits.DEFAULT)
                .capture(chicken, Optional.empty(), helper.getLevel().getGameTime());
        var values = effectiveResult.snapshot().visualData().getCompound("attributes").getList("values", net.minecraft.nbt.Tag.TAG_COMPOUND);
        boolean foundEffectiveSpeed = false;
        for (int index = 0; index < values.size(); index++) {
            var attribute = values.getCompound(index);
            if (attribute.getString("id").equals("minecraft:generic.movement_speed")) {
                foundEffectiveSpeed = attribute.contains("value") && attribute.getDouble("value") > attribute.getDouble("base");
            }
        }
        helper.assertTrue(foundEffectiveSpeed, "Snapshot did not preserve effective movement speed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void imitatorEligibilityRejectsBossesAndNonMobs(GameTestHelper helper) {
        Chicken chicken = helper.spawn(EntityType.CHICKEN, 1, 2, 1);
        ArmorStand armorStand = helper.spawn(EntityType.ARMOR_STAND, 2, 2, 1);
        WitherBoss wither = EntityType.WITHER.create(helper.getLevel());
        helper.assertTrue(ImitatorTargetEligibility.rejection(chicken).isEmpty(), "Ordinary mobs should be copyable");
        helper.assertTrue(ImitatorTargetEligibility.rejection(armorStand).isPresent(), "Non-mob living entities should not be copyable");
        helper.assertTrue(wither != null && ImitatorTargetEligibility.rejection(wither).isPresent(), "Bosses should not be copyable");
        if (wither != null) {
            wither.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void replicaMarkersRetainOwnershipAndSafetyPolicy(GameTestHelper helper) {
        ArmorStand replica = helper.spawn(EntityType.ARMOR_STAND, 1, 2, 1);
        UUID ownerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        long expires = helper.getLevel().getGameTime() + 200L;
        ReplicaEntityTags.mark(replica, ownerId, sessionId, expires, true, true);
        helper.assertTrue(ReplicaEntityTags.isReplica(replica), "Replica marker was not retained");
        helper.assertTrue(ReplicaEntityTags.ownerId(replica).orElseThrow().equals(ownerId), "Replica owner was not retained");
        helper.assertTrue(ReplicaEntityTags.sessionId(replica).orElseThrow().equals(sessionId), "Replica session was not retained");
        helper.assertTrue(ReplicaEntityTags.expiresGameTime(replica) == expires, "Replica expiry was not retained");
        helper.assertTrue(ReplicaEntityTags.suppressDrops(replica), "Replica drop suppression was not retained");
        helper.assertTrue(ReplicaEntityTags.suppressExperience(replica), "Replica experience suppression was not retained");
        helper.succeed();
    }
}
