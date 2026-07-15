package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.internal.client.ChatRestrictionBypassUserApiService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftChatRestrictions {
    @Inject(method = {"m_193585_", "createUserApiService"}, at = @At("RETURN"), cancellable = true, remap = false)
    private void imitationcoreapi$wrapUserApiService(YggdrasilAuthenticationService authenticationService, GameConfig gameConfig, CallbackInfoReturnable<UserApiService> callback) {
        UserApiService service = callback.getReturnValue();
        if (service != null && !(service instanceof ChatRestrictionBypassUserApiService)) {
            callback.setReturnValue(new ChatRestrictionBypassUserApiService(service));
        }
    }

    @Inject(method = {"m_294837_", "isNameBanned"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void imitationcoreapi$allowCurrentName(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}
