package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record DisguiseAnimationIntent(
        boolean moving,
        boolean sprinting,
        boolean crouching,
        boolean swimming,
        boolean fallFlying,
        boolean usingItem,
        boolean attacking,
        boolean hurt,
        boolean dying,
        boolean onGround,
        float walkSpeed,
        float attackAnimation,
        float previousAttackAnimation,
        int swingTime,
        int hurtTime,
        int deathTime,
        float yRot,
        float yRotO,
        float xRot,
        float xRotO,
        float yBodyRot,
        float yBodyRotO,
        float yHeadRot,
        float yHeadRotO,
        List<String> customTriggers
) {
    public DisguiseAnimationIntent {
        if (!Float.isFinite(walkSpeed)) {
            walkSpeed = 0F;
        }
        walkSpeed = Mth.clamp(walkSpeed, 0F, 1F);
        if (!Float.isFinite(attackAnimation)) {
            attackAnimation = 0F;
        }
        if (!Float.isFinite(previousAttackAnimation)) {
            previousAttackAnimation = 0F;
        }
        attackAnimation = Mth.clamp(attackAnimation, 0F, 1F);
        previousAttackAnimation = Mth.clamp(previousAttackAnimation, 0F, 1F);
        swingTime = Math.max(0, swingTime);
        hurtTime = Math.max(0, hurtTime);
        deathTime = Math.max(0, deathTime);
        yRot = finiteOrZero(yRot);
        yRotO = finiteOrZero(yRotO);
        xRot = finiteOrZero(xRot);
        xRotO = finiteOrZero(xRotO);
        yBodyRot = finiteOrZero(yBodyRot);
        yBodyRotO = finiteOrZero(yBodyRotO);
        yHeadRot = finiteOrZero(yHeadRot);
        yHeadRotO = finiteOrZero(yHeadRotO);
        customTriggers = normalize(customTriggers);
    }

    public static DisguiseAnimationIntent from(Entity subject, float partialTick) {
        Objects.requireNonNull(subject, "subject");
        if (!Float.isFinite(partialTick)) {
            partialTick = 1F;
        }
        partialTick = Mth.clamp(partialTick, 0F, 1F);
        double horizontalMotion = subject.getDeltaMovement().horizontalDistance();
        boolean moving = horizontalMotion > 1.0E-4D;
        float walkSpeed = (float) Math.min(1D, horizontalMotion * 4D);
        boolean attacking = false;
        boolean hurt = false;
        boolean dying = !subject.isAlive();
        boolean fallFlying = false;
        boolean usingItem = false;
        float attackAnimation = 0F;
        float previousAttackAnimation = 0F;
        int swingTime = 0;
        int hurtTime = 0;
        int deathTime = 0;
        float yBodyRot = subject.getYRot();
        float yBodyRotO = subject.yRotO;
        float yHeadRot = subject.getYHeadRot();
        float yHeadRotO = subject.getYHeadRot();
        if (subject instanceof LivingEntity living) {
            walkSpeed = living.walkAnimation.speed(partialTick);
            moving = moving || walkSpeed > 0.01F;
            attacking = living.swinging || living.attackAnim > 0.01F;
            hurt = living.hurtTime > 0;
            dying = dying || living.deathTime > 0 || !living.isAlive();
            fallFlying = living.isFallFlying();
            usingItem = living.isUsingItem();
            attackAnimation = living.attackAnim;
            previousAttackAnimation = living.oAttackAnim;
            swingTime = living.swingTime;
            hurtTime = living.hurtTime;
            deathTime = living.deathTime;
            yBodyRot = living.yBodyRot;
            yBodyRotO = living.yBodyRotO;
            yHeadRot = living.yHeadRot;
            yHeadRotO = living.yHeadRotO;
        }
        return new DisguiseAnimationIntent(
                moving,
                subject.isSprinting(),
                subject.isShiftKeyDown() || subject.isCrouching(),
                subject.isVisuallySwimming() || subject.isSwimming(),
                fallFlying,
                usingItem,
                attacking,
                hurt,
                dying,
                subject.onGround(),
                walkSpeed,
                attackAnimation,
                previousAttackAnimation,
                swingTime,
                hurtTime,
                deathTime,
                subject.getYRot(),
                subject.yRotO,
                subject.getXRot(),
                subject.xRotO,
                yBodyRot,
                yBodyRotO,
                yHeadRot,
                yHeadRotO,
                List.of()
        );
    }

    public List<List<String>> triggerKeywordGroups() {
        List<List<String>> groups = new ArrayList<>();
        for (String trigger : customTriggers) {
            groups.add(List.of(trigger));
        }
        if (dying) {
            groups.add(List.of("death", "dead", "die", "faint"));
        }
        if (hurt) {
            groups.add(List.of("hurt", "damage", "hit", "flinch"));
        }
        if (attacking) {
            groups.add(List.of("attack", "bite", "claw", "tail", "roar", "shoot", "cannon", "slam", "spit", "leap"));
        }
        if (usingItem) {
            groups.add(List.of("use", "eat", "drink", "cast"));
        }
        if (swimming) {
            groups.add(List.of("swim", "water"));
        }
        if (crouching) {
            groups.add(List.of("crouch", "sneak", "crawl", "sit"));
        }
        if (fallFlying) {
            groups.add(List.of("fly", "glide", "fall_fly"));
        }
        if (sprinting) {
            groups.add(List.of("sprint", "run", "dash"));
        }
        if (moving) {
            groups.add(List.of("walk", "move", "locomotion"));
        } else {
            groups.add(List.of("idle", "stand"));
        }
        return List.copyOf(groups);
    }

    public DisguiseAnimationIntent withCustomTriggers(List<String> customTriggers) {
        return new DisguiseAnimationIntent(
                moving,
                sprinting,
                crouching,
                swimming,
                fallFlying,
                usingItem,
                attacking,
                hurt,
                dying,
                onGround,
                walkSpeed,
                attackAnimation,
                previousAttackAnimation,
                swingTime,
                hurtTime,
                deathTime,
                yRot,
                yRotO,
                xRot,
                xRotO,
                yBodyRot,
                yBodyRotO,
                yHeadRot,
                yHeadRotO,
                customTriggers
        );
    }

    public DisguiseAnimationIntent withAdditionalCustomTriggers(List<String> additionalTriggers) {
        List<String> merged = new ArrayList<>(customTriggers);
        if (additionalTriggers != null) {
            merged.addAll(additionalTriggers);
        }
        return withCustomTriggers(merged);
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0F;
    }

    private static List<String> normalize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(java.util.Locale.ROOT))
                .distinct()
                .toList();
    }
}
