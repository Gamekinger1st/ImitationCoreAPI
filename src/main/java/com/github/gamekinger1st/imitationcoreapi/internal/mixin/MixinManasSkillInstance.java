package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.internal.skill.ReflectiveOwnerSkillSuppressionHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.github.manasmods.manascore.skill.api.ManasSkillInstance", remap = false)
public abstract class MixinManasSkillInstance {
    @Inject(method = "canInteractSkill(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$canInteractSkill(LivingEntity user, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(user, (Object)this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "canBeToggled(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$canBeToggled(LivingEntity entity, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "canTick(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$canTick(LivingEntity entity, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "canScroll(Lnet/minecraft/world/entity/LivingEntity;I)Z", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$canScroll(LivingEntity entity, int mode, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "onTick(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onTick(LivingEntity entity, CallbackInfo callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.cancel();
        }
    }

    @Inject(method = "onPressed(Lnet/minecraft/world/entity/LivingEntity;II)V", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onPressed(LivingEntity entity, int keyNumber, int mode, CallbackInfo callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.cancel();
        }
    }

    @Inject(method = "onHeld(Lnet/minecraft/world/entity/LivingEntity;II)Z", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onHeld(LivingEntity entity, int heldTicks, int mode, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "onRelease(Lnet/minecraft/world/entity/LivingEntity;III)V", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onRelease(LivingEntity entity, int heldTicks, int keyNumber, int mode, CallbackInfo callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.cancel();
        }
    }

    @Inject(method = "onScroll(Lnet/minecraft/world/entity/LivingEntity;DI)V", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onScroll(LivingEntity entity, double delta, int mode, CallbackInfo callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.cancel();
        }
    }

    @Inject(method = "onEffectAdded", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onEffectAdded(LivingEntity entity, Entity source, @Coerce Object effect, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "onBeingTargeted", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onBeingTargeted(@Coerce Object target, LivingEntity attacker, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUseChangeable(target, (Object)this)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "onBeingDamaged", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onBeingDamaged(LivingEntity entity, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "onDamageEntity", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onDamageEntity(LivingEntity entity, LivingEntity target, DamageSource source, @Coerce Object amount, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "onTouchEntity", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onTouchEntity(LivingEntity entity, LivingEntity target, DamageSource source, @Coerce Object amount, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "onTakenDamage", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onTakenDamage(LivingEntity entity, DamageSource source, @Coerce Object amount, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "onProjectileHit", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onProjectileHit(LivingEntity entity, EntityHitResult hit, Projectile projectile, @Coerce Object deflection, @Coerce Object result, CallbackInfo callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.cancel();
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onDeath(LivingEntity entity, DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "onRespawn", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onRespawn(ServerPlayer player, boolean conqueredEnd, CallbackInfo callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(player, (Object)this)) {
            callback.cancel();
        }
    }
}
