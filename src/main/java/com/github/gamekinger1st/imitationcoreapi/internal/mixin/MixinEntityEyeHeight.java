package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseQueries;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntityEyeHeight {
    @Inject(method = "getEyeHeight()F", at = @At("RETURN"), cancellable = true, remap = false)
    private void imitationcoreapi$copyCameraEyeHeight(CallbackInfoReturnable<Float> callback) {
        Entity entity = (Entity)(Object)this;
        if (Minecraft.getInstance().player != entity) {
            return;
        }
        ClientDisguiseQueries.eyeHeight(entity)
                .ifPresent(height -> callback.setReturnValue(Mth.clamp((float)height, 0.15F, 12F)));
    }
}
