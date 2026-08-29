package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseQueries;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LocalPlayer.class, remap = false)
public abstract class MixinLocalPlayerAutoJump {
    @Inject(method = "isAutoJumpEnabled()Z", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$overrideAutoJump(CallbackInfoReturnable<Boolean> callback) {
        LocalPlayer player = (LocalPlayer)(Object)this;
        ClientDisguiseQueries.state(player)
                .flatMap(state -> state.transformationModifiers().autoJumpOverride().forcedValue())
                .ifPresent(callback::setReturnValue);
    }
}
