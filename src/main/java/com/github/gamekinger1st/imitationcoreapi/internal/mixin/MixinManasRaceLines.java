package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.api.race.RaceLineKeys;
import com.github.gamekinger1st.imitationcoreapi.internal.race.ReflectiveRaceRuntimeHooks;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.github.manasmods.manascore.race.api.ManasRace", remap = false)
public abstract class MixinManasRaceLines {
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getRaceName(CallbackInfoReturnable<MutableComponent> callback) {
        ReflectiveRaceRuntimeHooks.line((Object)this, RaceLineKeys.NAME).ifPresent(line -> callback.setReturnValue(line.copy()));
    }

    @Inject(method = "getRaceDescription", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getRaceDescription(CallbackInfoReturnable<MutableComponent> callback) {
        ReflectiveRaceRuntimeHooks.line((Object)this, RaceLineKeys.DESCRIPTION).ifPresent(line -> callback.setReturnValue(line.copy()));
    }
}
