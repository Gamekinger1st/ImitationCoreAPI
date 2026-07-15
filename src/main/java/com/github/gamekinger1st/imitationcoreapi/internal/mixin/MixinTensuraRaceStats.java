package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.api.race.RaceStatKeys;
import com.github.gamekinger1st.imitationcoreapi.internal.race.ReflectiveRaceRuntimeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.github.manasmods.tensura.race.TensuraRace", remap = false)
public abstract class MixinTensuraRaceStats {
    @Inject(method = "getMinBaseAura", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getMinBaseAura(CallbackInfoReturnable<Double> callback) {
        ReflectiveRaceRuntimeHooks.stat((Object)this, RaceStatKeys.MIN_AURA).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "getMaxBaseAura", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getMaxBaseAura(CallbackInfoReturnable<Double> callback) {
        ReflectiveRaceRuntimeHooks.stat((Object)this, RaceStatKeys.MAX_AURA).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "getMinBaseMagicule", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getMinBaseMagicule(CallbackInfoReturnable<Double> callback) {
        ReflectiveRaceRuntimeHooks.stat((Object)this, RaceStatKeys.MIN_MAGICULE).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "getMaxBaseMagicule", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getMaxBaseMagicule(CallbackInfoReturnable<Double> callback) {
        ReflectiveRaceRuntimeHooks.stat((Object)this, RaceStatKeys.MAX_MAGICULE).ifPresent(callback::setReturnValue);
    }
}
