package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.api.race.RaceStatKeys;
import com.github.gamekinger1st.imitationcoreapi.api.race.RaceFunctionKeys;
import com.github.gamekinger1st.imitationcoreapi.api.race.RaceFunctionResult;
import com.github.gamekinger1st.imitationcoreapi.internal.race.ReflectiveRaceRuntimeHooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.Coerce;
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

    @Inject(method = "resetExistenceData", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$resetExistenceData(LivingEntity entity, org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if (ReflectiveRaceRuntimeHooks.function((Object)this, RaceFunctionKeys.RESET_EXISTENCE_DATA, entity, new CompoundTag()).handled()) {
            callback.cancel();
        }
    }

    @Inject(method = "getEvolutionProgress", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getEvolutionProgress(@Coerce Object instance, LivingEntity entity, @Coerce Object evolution, CallbackInfoReturnable<Float> callback) {
        RaceFunctionResult result = ReflectiveRaceRuntimeHooks.function((Object)this, RaceFunctionKeys.EVOLUTION_PROGRESS, entity, new CompoundTag());
        if (result.handled() && result.doubleValue().isPresent()) {
            callback.setReturnValue((float)result.doubleValue().getAsDouble());
        }
    }

    @Inject(method = "triggerEvolutionRewards", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$triggerEvolutionRewards(@Coerce Object instance, LivingEntity entity, org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if (ReflectiveRaceRuntimeHooks.function((Object)this, RaceFunctionKeys.TRIGGER_EVOLUTION_REWARDS, entity, new CompoundTag()).handled()) {
            callback.cancel();
        }
    }

    @Inject(method = "getEvolutionRequirements", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getEvolutionRequirements(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<Object> callback) {
        RaceFunctionResult result = ReflectiveRaceRuntimeHooks.function((Object)this, RaceFunctionKeys.EVOLUTION_REQUIREMENTS, entity, new CompoundTag());
        result.rawValue().ifPresent(callback::setReturnValue);
    }

    @Inject(method = "getAwakeningEvolution", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getAwakeningEvolution(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<Object> callback) {
        raceResult(RaceFunctionKeys.AWAKENING_EVOLUTION, entity).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "getHarvestFestivalEvolution", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getHarvestFestivalEvolution(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<Object> callback) {
        raceResult(RaceFunctionKeys.HARVEST_FESTIVAL_EVOLUTION, entity).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "getIntrinsicLearnable", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getIntrinsicLearnable(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<java.util.List<Object>> callback) {
        RaceFunctionResult result = ReflectiveRaceRuntimeHooks.function((Object)this, RaceFunctionKeys.INTRINSIC_LEARNABLE, entity, new CompoundTag());
        if (result.handled()) {
            callback.setReturnValue(ReflectiveRaceRuntimeHooks.skills(result.resourceList()));
        }
    }

    @Inject(method = "gainIntrinsicLearnable", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$gainIntrinsicLearnable(@Coerce Object instance, LivingEntity entity, org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if (ReflectiveRaceRuntimeHooks.function((Object)this, RaceFunctionKeys.GAIN_INTRINSIC_LEARNABLE, entity, new CompoundTag()).handled()) {
            callback.cancel();
        }
    }

    @Inject(method = "getAlignment", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getAlignment(CallbackInfoReturnable<Object> callback) {
        RaceFunctionResult result = ReflectiveRaceRuntimeHooks.function((Object)this, RaceFunctionKeys.ALIGNMENT, null, new CompoundTag());
        if (result.handled()) {
            result.resourceValue().flatMap(id -> ReflectiveRaceRuntimeHooks.enumValue("io.github.manasmods.tensura.storage.Alignment", id)).ifPresent(callback::setReturnValue);
        }
    }

    @Inject(method = "hasGuaranteeElemental", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$hasGuaranteeElemental(CallbackInfoReturnable<Boolean> callback) {
        RaceFunctionResult result = ReflectiveRaceRuntimeHooks.function((Object)this, RaceFunctionKeys.GUARANTEE_ELEMENTAL, null, new CompoundTag());
        if (result.handled() && result.booleanValue().isPresent()) {
            callback.setReturnValue(result.booleanValue().get());
        }
    }

    @Inject(method = "getElementalSpiritsChance", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getElementalSpiritsChance(@Coerce Object element, @Coerce Object level, CallbackInfoReturnable<Double> callback) {
        CompoundTag data = new CompoundTag();
        data.putString("element", String.valueOf(element));
        data.putString("level", String.valueOf(level));
        RaceFunctionResult result = ReflectiveRaceRuntimeHooks.function((Object)this, RaceFunctionKeys.ELEMENTAL_SPIRIT_CHANCE, null, data);
        if (result.handled() && result.doubleValue().isPresent()) {
            callback.setReturnValue(result.doubleValue().getAsDouble());
        }
    }

    private java.util.Optional<Object> raceResult(net.minecraft.resources.ResourceLocation key, LivingEntity entity) {
        RaceFunctionResult result = ReflectiveRaceRuntimeHooks.function((Object)this, key, entity, new CompoundTag());
        if (!result.handled() || result.resourceValue().isEmpty()) {
            return java.util.Optional.empty();
        }
        java.util.List<Object> values = ReflectiveRaceRuntimeHooks.races(java.util.List.of(result.resourceValue().get()));
        return values.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(values.getFirst());
    }
}
