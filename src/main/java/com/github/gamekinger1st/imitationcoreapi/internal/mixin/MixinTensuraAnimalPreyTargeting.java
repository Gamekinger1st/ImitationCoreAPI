package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.internal.targeting.ActiveFormTargetingHooks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.github.manasmods.tensura.entity.ai.behaviour.TensuraBehaviourHelper", remap = false)
public abstract class MixinTensuraAnimalPreyTargeting {
    @Redirect(method = "lambda$getAnimalPreyPredicate$15", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getType()Lnet/minecraft/world/entity/EntityType;"))
    private static EntityType<?> imitationcoreapi$effectiveTargetType(LivingEntity entity) {
        return ActiveFormTargetingHooks.effectiveType(entity);
    }
}
