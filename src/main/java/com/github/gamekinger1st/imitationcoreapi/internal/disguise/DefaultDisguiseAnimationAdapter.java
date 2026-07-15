package com.github.gamekinger1st.imitationcoreapi.internal.disguise;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseState;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAnimationAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAnimationIntent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultDisguiseAnimationAdapter implements DisguiseAnimationAdapter {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "default_disguise_animation");
    private static final int MAX_TRACKED_SESSIONS = 512;
    private final Map<UUID, Integer> lastWalkAnimationTick = new ConcurrentHashMap<>();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean supports(Entity imitation, Entity subject, ClientDisguiseState state) {
        return true;
    }

    @Override
    public void synchronize(Entity imitation, Entity subject, ClientDisguiseState state, float partialTick) {
        synchronize(imitation, subject, state, partialTick, DisguiseAnimationIntent.from(subject, partialTick));
    }

    @Override
    public void synchronize(Entity imitation, Entity subject, ClientDisguiseState state, float partialTick, DisguiseAnimationIntent intent) {
        if (lastWalkAnimationTick.size() > MAX_TRACKED_SESSIONS) {
            lastWalkAnimationTick.clear();
        }
        imitation.noPhysics = true;
        imitation.setNoGravity(true);
        imitation.tickCount = subject.tickCount;
        imitation.xo = subject.xo;
        imitation.yo = subject.yo;
        imitation.zo = subject.zo;
        imitation.xOld = subject.xOld;
        imitation.yOld = subject.yOld;
        imitation.zOld = subject.zOld;
        imitation.yRotO = intent.yRotO();
        imitation.xRotO = intent.xRotO();
        imitation.moveTo(subject.getX(), subject.getY(), subject.getZ(), intent.yRot(), intent.xRot());
        imitation.setYRot(intent.yRot());
        imitation.setXRot(intent.xRot());
        imitation.setYHeadRot(intent.yHeadRot());
        imitation.setYBodyRot(intent.yBodyRot());
        imitation.setDeltaMovement(subject.getDeltaMovement());
        imitation.setOnGround(intent.onGround());
        imitation.setPose(subject.getPose());
        imitation.setSprinting(intent.sprinting());
        imitation.setShiftKeyDown(intent.crouching());
        imitation.setSwimming(intent.swimming() || subject.getPose() == Pose.SWIMMING);
        if (imitation instanceof LivingEntity living) {
            living.yBodyRot = intent.yBodyRot();
            living.yBodyRotO = intent.yBodyRotO();
            living.yHeadRot = intent.yHeadRot();
            living.yHeadRotO = intent.yHeadRotO();
            living.swinging = intent.attacking();
            if (subject instanceof LivingEntity sourceLiving) {
                living.swingingArm = sourceLiving.swingingArm;
            }
            living.swingTime = intent.swingTime();
            living.hurtTime = intent.hurtTime();
            living.deathTime = intent.deathTime();
            living.oAttackAnim = intent.previousAttackAnimation();
            living.attackAnim = intent.attackAnimation();
            living.walkAnimation.setSpeed(intent.walkSpeed());
            updateWalkAnimationOncePerTick(state.sessionId(), subject.tickCount, living, intent.walkSpeed());
        }
        if (imitation instanceof Player imitationPlayer && subject instanceof Player subjectPlayer) {
            imitationPlayer.oBob = subjectPlayer.oBob;
            imitationPlayer.bob = subjectPlayer.bob;
        }
    }

    private void updateWalkAnimationOncePerTick(UUID sessionId, int tick, LivingEntity living, float walkSpeed) {
        Integer previous = lastWalkAnimationTick.put(sessionId, tick);
        if (previous == null || previous != tick) {
            living.walkAnimation.update(walkSpeed, 1.0F);
        }
    }
}
