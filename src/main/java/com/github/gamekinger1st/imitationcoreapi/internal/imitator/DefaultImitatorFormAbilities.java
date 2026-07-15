package com.github.gamekinger1st.imitationcoreapi.internal.imitator;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorActionResult;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormAbility;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormAbilityContext;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DefaultImitatorFormAbilities {
    private static final ResourceLocation SKELETON = vanilla("skeleton");
    private static final ResourceLocation STRAY = vanilla("stray");
    private static final ResourceLocation WITHER_SKELETON = vanilla("wither_skeleton");
    private static final ResourceLocation BOGGED = vanilla("bogged");
    private static final ResourceLocation SPIDER = vanilla("spider");
    private static final ResourceLocation CAVE_SPIDER = vanilla("cave_spider");
    private static final ResourceLocation ENDERMAN = vanilla("enderman");
    private static final ResourceLocation ZOMBIE = vanilla("zombie");
    private static final ResourceLocation ZOMBIE_VILLAGER = vanilla("zombie_villager");
    private static final ResourceLocation DROWNED = vanilla("drowned");
    private static final ResourceLocation ZOMBIFIED_PIGLIN = vanilla("zombified_piglin");
    private static final ResourceLocation PHANTOM = vanilla("phantom");
    private static final ResourceLocation BLADE_TIGER = ResourceLocation.fromNamespaceAndPath("tensura", "blade_tiger");

    private DefaultImitatorFormAbilities() {
    }

    public static List<ImitatorFormAbility> create() {
        return List.of(
                active(id("skeleton_archer"), 100, Set.of(SKELETON, STRAY, WITHER_SKELETON, BOGGED), DefaultImitatorFormAbilities::shootArrow),
                active(id("enderman_teleport"), 100, Set.of(ENDERMAN), DefaultImitatorFormAbilities::randomTeleport),
                active(id("blade_tiger_voice_cannon"), 100, Set.of(BLADE_TIGER), DefaultImitatorFormAbilities::voiceCannon),
                ticking(id("spider_climb"), 10, Set.of(SPIDER, CAVE_SPIDER), DefaultImitatorFormAbilities::climbLikeSpider),
                ticking(id("sun_sensitive_undead"), 0, Set.of(SKELETON, STRAY, BOGGED, ZOMBIE, ZOMBIE_VILLAGER, DROWNED, ZOMBIFIED_PIGLIN, PHANTOM), DefaultImitatorFormAbilities::burnLikeUndead)
        );
    }

    private static ImitatorFormAbility active(ResourceLocation id, int priority, Set<ResourceLocation> entityTypes, ActiveHandler activeHandler) {
        return new BuiltInAbility(id, priority, entityTypes, activeHandler, null);
    }

    private static ImitatorFormAbility ticking(ResourceLocation id, int priority, Set<ResourceLocation> entityTypes, TickHandler tickHandler) {
        return new BuiltInAbility(id, priority, entityTypes, null, tickHandler);
    }

    private static ImitatorActionResult shootArrow(ImitatorFormAbilityContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 6.0F);
        arrow.setBaseDamage(Math.max(2.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5D));
        level.addFreshEntity(arrow);
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SKELETON_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return ImitatorActionResult.accepted("Copied skeleton shot an arrow");
    }

    private static ImitatorActionResult randomTeleport(ImitatorFormAbilityContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        for (int attempt = 0; attempt < 64; attempt++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5D) * 64.0D;
            double y = Mth.clamp(player.getY() + (player.getRandom().nextInt(64) - 32), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 1);
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5D) * 64.0D;
            if (player.randomTeleport(x, y, z, true)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                return ImitatorActionResult.accepted("Copied enderman teleported");
            }
        }
        return ImitatorActionResult.rejected("Copied enderman could not find a safe teleport location");
    }

    private static ImitatorActionResult voiceCannon(ImitatorFormAbilityContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 source = player.getEyePosition().add(direction.scale(2.0D));
        double damage = Math.max(4.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5D);
        Set<LivingEntity> hit = new HashSet<>();
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5F, 1.0F);
        for (int distance = 1; distance <= 20; distance++) {
            Vec3 particlePos = source.add(direction.scale(distance));
            level.sendParticles(ParticleTypes.SONIC_BOOM, particlePos.x(), particlePos.y(), particlePos.z(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            AABB area = new AABB(particlePos, particlePos).inflate(2.0D);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, living -> living != player && living.isAlive())) {
                if (!hit.add(target) || !player.hasLineOfSight(target)) {
                    continue;
                }
                target.hurt(player.damageSources().sonicBoom(player), (float) damage);
                double resistance = Math.max(0.0D, 1.0D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                Vec3 push = direction.scale(0.35D * resistance);
                target.push(push.x(), push.y(), push.z());
            }
        }
        return ImitatorActionResult.accepted("Copied blade tiger used Voice Cannon");
    }

    private static void climbLikeSpider(ImitatorFormAbilityContext context) {
        ServerPlayer player = context.player();
        if (!player.horizontalCollision) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x(), Math.max(0.2D, movement.y()), movement.z());
        player.resetFallDistance();
    }

    private static void burnLikeUndead(ImitatorFormAbilityContext context) {
        ServerPlayer player = context.player();
        if (player.fireImmune()
                || !player.level().isDay()
                || player.isInWaterRainOrBubble()
                || !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return;
        }
        BlockPos pos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        if (player.level().canSeeSkyFromBelowWater(pos)) {
            player.igniteForSeconds(8.0F);
        }
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, path);
    }

    @FunctionalInterface
    private interface ActiveHandler {
        ImitatorActionResult activate(ImitatorFormAbilityContext context);
    }

    @FunctionalInterface
    private interface TickHandler {
        void tick(ImitatorFormAbilityContext context);
    }

    private record BuiltInAbility(ResourceLocation id, int priority, Set<ResourceLocation> entityTypes, ActiveHandler activeHandler, TickHandler tickHandler) implements ImitatorFormAbility {
        private BuiltInAbility {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(entityTypes, "entityTypes");
            entityTypes = Set.copyOf(entityTypes);
        }

        @Override
        public boolean supports(IdentitySnapshot snapshot) {
            return entityTypes.contains(snapshot.entityType());
        }

        @Override
        public boolean hasActiveAbility(IdentitySnapshot snapshot) {
            return activeHandler != null && supports(snapshot);
        }

        @Override
        public ImitatorActionResult activate(ImitatorFormAbilityContext context) {
            return activeHandler == null ? ImitatorFormAbility.super.activate(context) : activeHandler.activate(context);
        }

        @Override
        public boolean hasTickAbility(IdentitySnapshot snapshot) {
            return tickHandler != null && supports(snapshot);
        }

        @Override
        public void tick(ImitatorFormAbilityContext context) {
            if (tickHandler != null) {
                tickHandler.tick(context);
            }
        }
    }
}
