package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.internal.targeting.ActiveFormTargetingHooks;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Pseudo
@Mixin(targets = "io.github.manasmods.tensura.entity.ai.behaviour.TensuraBehaviourHelper", remap = false)
public abstract class MixinTensuraAnimalPreyTargeting {
    @Inject(method = "getAnimalPreyPredicate", at = @At("RETURN"), cancellable = true, remap = false)
    private static void imitationcoreapi$wrapAnimalPreyPredicate(LivingEntity subordinate, CallbackInfoReturnable<Predicate<LivingEntity>> callback) {
        Predicate<LivingEntity> original = callback.getReturnValue();
        callback.setReturnValue(target -> ActiveFormTargetingHooks.animalPreyDecision(subordinate, target).orElseGet(() -> original.test(target)));
    }
}
