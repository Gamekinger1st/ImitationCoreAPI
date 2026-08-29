package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.internal.client.ChatRestrictionBypassUserApiService;
import com.github.gamekinger1st.imitationcoreapi.internal.config.ImitationCoreConfig;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Minecraft.class, remap = false)
public abstract class MixinMinecraftChatRestrictions {
    @Inject(method = "createUserApiService", at = @At("RETURN"), cancellable = true)
    private void imitationcoreapi$wrapUserApiService(YggdrasilAuthenticationService authenticationService, GameConfig gameConfig, CallbackInfoReturnable<UserApiService> callback) {
        UserApiService service = callback.getReturnValue();
        if (ImitationCoreConfig.bypassChatRestrictions() && service != null && !(service instanceof ChatRestrictionBypassUserApiService)) {
            callback.setReturnValue(new ChatRestrictionBypassUserApiService(service));
        }
    }

    @Inject(method = "isNameBanned", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$allowCurrentName(CallbackInfoReturnable<Boolean> callback) {
        if (ImitationCoreConfig.bypassChatRestrictions()) {
            callback.setReturnValue(false);
        }
    }
}
