package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.internal.targeting.ActiveFormTargetingHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MixinMobTargeting {
    @Inject(method = {"m_6710_", "setTarget"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void imitationcoreapi$suppressInvalidActiveFormTarget(LivingEntity target, CallbackInfo callback) {
        if (target != null && ActiveFormTargetingHooks.shouldSuppressTarget((Mob)(Object)this, target)) {
            callback.cancel();
        }
    }
}
