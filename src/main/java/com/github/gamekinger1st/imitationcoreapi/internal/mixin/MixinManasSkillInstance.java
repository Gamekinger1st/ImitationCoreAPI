package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.internal.skill.ReflectiveOwnerSkillSuppressionHooks;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.github.manasmods.manascore.skill.api.ManasSkillInstance", remap = false)
public abstract class MixinManasSkillInstance {
    @Inject(method = "canInteractSkill", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$canInteractSkill(LivingEntity user, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(user, (Object)this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "canActivateSkill", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$canActivateSkill(LivingEntity user, int mode, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(user, (Object)this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "canBeToggled", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$canBeToggled(LivingEntity entity, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "canScroll", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$canScroll(LivingEntity entity, int mode, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "onPressed", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onPressed(LivingEntity entity, int keyNumber, int mode, CallbackInfo callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.cancel();
        }
    }

    @Inject(method = "onHeld", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onHeld(LivingEntity entity, int heldTicks, int mode, CallbackInfoReturnable<Boolean> callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "onRelease", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onRelease(LivingEntity entity, int heldTicks, int keyNumber, int mode, CallbackInfo callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onScroll(LivingEntity entity, double delta, int mode, CallbackInfo callback) {
        if (!ReflectiveOwnerSkillSuppressionHooks.canUse(entity, (Object)this)) {
            callback.cancel();
        }
    }
}
