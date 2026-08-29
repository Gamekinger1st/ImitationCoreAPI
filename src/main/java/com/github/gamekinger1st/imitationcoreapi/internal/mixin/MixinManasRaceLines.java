package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import com.github.gamekinger1st.imitationcoreapi.api.race.RaceLineKeys;
import com.github.gamekinger1st.imitationcoreapi.api.race.RaceFunctionKeys;
import com.github.gamekinger1st.imitationcoreapi.api.race.RaceFunctionResult;
import com.github.gamekinger1st.imitationcoreapi.internal.race.ReflectiveRaceRuntimeHooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.github.manasmods.manascore.race.api.ManasRace", remap = false)
public abstract class MixinManasRaceLines {
    @Shadow(remap = false)
    protected java.util.Map<Object, Object> attributeModifiers;

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getRaceName(CallbackInfoReturnable<MutableComponent> callback) {
        ReflectiveRaceRuntimeHooks.line((Object)this, RaceLineKeys.DISPLAY_NAME)
                .or(() -> ReflectiveRaceRuntimeHooks.line((Object)this, RaceLineKeys.NAME))
                .ifPresent(line -> callback.setReturnValue(line.copy()));
    }

    @Inject(method = "getRaceDescription", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getRaceDescription(CallbackInfoReturnable<MutableComponent> callback) {
        ReflectiveRaceRuntimeHooks.line((Object)this, RaceLineKeys.DESCRIPTION).ifPresent(line -> callback.setReturnValue(line.copy()));
    }

    @Inject(method = "getRaceIcon", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getRaceIcon(CallbackInfoReturnable<net.minecraft.resources.ResourceLocation> callback) {
        RaceFunctionResult result = function(RaceFunctionKeys.RACE_ICON, null, new CompoundTag());
        result.resourceValue().ifPresent(callback::setReturnValue);
    }

    @Inject(method = "addAttributeModifiers", at = @At("HEAD"))
    private void imitationcoreapi$applyAttributeStats(@Coerce Object instance, LivingEntity entity, CallbackInfo callback) {
        ReflectiveRaceRuntimeHooks.applyAttributeStats((Object)this, attributeModifiers);
    }

    @Inject(method = "canActivateAbility", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$canActivateAbility(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<Boolean> callback) {
        booleanResult(RaceFunctionKeys.CAN_ACTIVATE_ABILITY, entity).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "getMaxHeldTime", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getMaxHeldTime(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<Integer> callback) {
        RaceFunctionResult result = function(RaceFunctionKeys.MAX_HELD_TIME, entity, new CompoundTag());
        if (result.handled() && result.intValue().isPresent()) {
            callback.setReturnValue(Math.max(0, result.intValue().getAsInt()));
        }
    }

    @Inject(method = "canTick", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$canTick(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<Boolean> callback) {
        booleanResult(RaceFunctionKeys.CAN_TICK, entity).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onTick(@Coerce Object instance, LivingEntity entity, CallbackInfo callback) {
        cancelHandled(RaceFunctionKeys.ON_TICK, entity, new CompoundTag(), callback);
    }

    @Inject(method = "onActivateAbility", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onActivateAbility(@Coerce Object instance, LivingEntity entity, CallbackInfo callback) {
        cancelHandled(RaceFunctionKeys.ON_ACTIVATE_ABILITY, entity, new CompoundTag(), callback);
    }

    @Inject(method = "onHeldAbility", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onHeldAbility(@Coerce Object instance, LivingEntity entity, int heldTicks, CallbackInfoReturnable<Boolean> callback) {
        CompoundTag data = new CompoundTag();
        data.putInt("held_ticks", heldTicks);
        RaceFunctionResult result = function(RaceFunctionKeys.ON_HELD_ABILITY, entity, data);
        if (result.handled() && result.booleanValue().isPresent()) {
            callback.setReturnValue(result.booleanValue().get());
        }
    }

    @Inject(method = "onReleaseAbility", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onReleaseAbility(@Coerce Object instance, LivingEntity entity, int heldTicks, CallbackInfo callback) {
        CompoundTag data = new CompoundTag();
        data.putInt("held_ticks", heldTicks);
        cancelHandled(RaceFunctionKeys.ON_RELEASE_ABILITY, entity, data, callback);
    }

    @Inject(method = "onRaceSet", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onRaceSet(@Coerce Object instance, LivingEntity entity, CallbackInfo callback) {
        cancelHandled(RaceFunctionKeys.ON_RACE_SET, entity, new CompoundTag(), callback);
    }

    @Inject(method = "onEffectAdded", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onEffectAdded(@Coerce Object instance, LivingEntity entity, Entity source, @Coerce Object effect, CallbackInfoReturnable<Boolean> callback) {
        booleanResult(RaceFunctionKeys.ON_EFFECT_ADDED, entity).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "onBeingTargeted", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onBeingTargeted(@Coerce Object instance, @Coerce Object target, LivingEntity owner, CallbackInfoReturnable<Boolean> callback) {
        booleanResult(RaceFunctionKeys.ON_BEING_TARGETED, owner).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "onAttackEntity", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onAttackEntity(@Coerce Object instance, LivingEntity owner, LivingEntity target, DamageSource source, @Coerce Object amount, CallbackInfoReturnable<Boolean> callback) {
        booleanResult(RaceFunctionKeys.ON_ATTACK_ENTITY, owner).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "onHurt", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onHurt(@Coerce Object instance, LivingEntity owner, DamageSource source, @Coerce Object amount, CallbackInfoReturnable<Boolean> callback) {
        booleanResult(RaceFunctionKeys.ON_HURT, owner).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onDeath(@Coerce Object instance, LivingEntity owner, DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        booleanResult(RaceFunctionKeys.ON_DEATH, owner).ifPresent(callback::setReturnValue);
    }

    @Inject(method = "onRespawn", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onRespawn(@Coerce Object instance, ServerPlayer owner, boolean conqueredEnd, CallbackInfo callback) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("conquered_end", conqueredEnd);
        cancelHandled(RaceFunctionKeys.ON_RESPAWN, owner, data, callback);
    }

    @Inject(method = "getRespawnDimension", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getRespawnDimension(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<Object> callback) {
        RaceFunctionResult result = function(RaceFunctionKeys.RESPAWN_DIMENSION, entity, new CompoundTag());
        result.rawValue().ifPresent(callback::setReturnValue);
    }

    @Inject(method = "getIntrinsicSkills", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getIntrinsicSkills(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<java.util.List<Object>> callback) {
        RaceFunctionResult result = function(RaceFunctionKeys.INTRINSIC_SKILLS, entity, new CompoundTag());
        if (result.handled()) {
            callback.setReturnValue(ReflectiveRaceRuntimeHooks.skills(result.resourceList()));
        }
    }

    @Inject(method = "isIntrinsicSkill", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$isIntrinsicSkill(@Coerce Object instance, LivingEntity entity, @Coerce Object skill, CallbackInfoReturnable<Boolean> callback) {
        CompoundTag data = new CompoundTag();
        data.putString("skill", String.valueOf(skill));
        RaceFunctionResult result = function(RaceFunctionKeys.IS_INTRINSIC_SKILL, entity, data);
        if (result.handled() && result.booleanValue().isPresent()) {
            callback.setReturnValue(result.booleanValue().get());
        }
    }

    @Inject(method = "learnIntrinsicSkills", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$learnIntrinsicSkills(@Coerce Object instance, LivingEntity entity, CallbackInfo callback) {
        cancelHandled(RaceFunctionKeys.LEARN_INTRINSIC_SKILLS, entity, new CompoundTag(), callback);
    }

    @Inject(method = "getNextEvolutions", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getNextEvolutions(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<java.util.List<Object>> callback) {
        RaceFunctionResult result = function(RaceFunctionKeys.NEXT_EVOLUTIONS, entity, new CompoundTag());
        if (result.handled()) {
            callback.setReturnValue(ReflectiveRaceRuntimeHooks.races(result.resourceList()));
        }
    }

    @Inject(method = "getPreviousEvolutions", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getPreviousEvolutions(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<java.util.List<Object>> callback) {
        RaceFunctionResult result = function(RaceFunctionKeys.PREVIOUS_EVOLUTIONS, entity, new CompoundTag());
        if (result.handled()) {
            callback.setReturnValue(ReflectiveRaceRuntimeHooks.races(result.resourceList()));
        }
    }

    @Inject(method = "getDefaultEvolution", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getDefaultEvolution(@Coerce Object instance, LivingEntity entity, CallbackInfoReturnable<Object> callback) {
        RaceFunctionResult result = function(RaceFunctionKeys.DEFAULT_EVOLUTION, entity, new CompoundTag());
        if (result.handled()) {
            callback.setReturnValue(result.resourceValue().map(java.util.List::of).map(ReflectiveRaceRuntimeHooks::races).filter(values -> !values.isEmpty()).map(java.util.List::getFirst).orElse(null));
        }
    }

    @Inject(method = "getEvolutionProgress", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getEvolutionProgress(@Coerce Object instance, LivingEntity entity, @Coerce Object evolution, CallbackInfoReturnable<Float> callback) {
        RaceFunctionResult result = function(RaceFunctionKeys.EVOLUTION_PROGRESS, entity, new CompoundTag());
        if (result.handled() && result.doubleValue().isPresent()) {
            callback.setReturnValue((float)result.doubleValue().getAsDouble());
        }
    }

    @Inject(method = "onRaceEvolution", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$onRaceEvolution(@Coerce Object instance, LivingEntity entity, @Coerce Object evolution, CallbackInfo callback) {
        cancelHandled(RaceFunctionKeys.ON_RACE_EVOLUTION, entity, new CompoundTag(), callback);
    }

    @Inject(method = "getDifficulty", at = @At("HEAD"), cancellable = true)
    private void imitationcoreapi$getDifficulty(CallbackInfoReturnable<Object> callback) {
        RaceFunctionResult result = function(RaceFunctionKeys.DIFFICULTY, null, new CompoundTag());
        result.rawValue().ifPresent(callback::setReturnValue);
    }

    private java.util.Optional<Boolean> booleanResult(net.minecraft.resources.ResourceLocation key, LivingEntity entity) {
        RaceFunctionResult result = function(key, entity, new CompoundTag());
        return result.handled() ? result.booleanValue() : java.util.Optional.empty();
    }

    private RaceFunctionResult function(net.minecraft.resources.ResourceLocation key, LivingEntity entity, CompoundTag data) {
        return ReflectiveRaceRuntimeHooks.function((Object)this, key, entity, data);
    }

    private void cancelHandled(net.minecraft.resources.ResourceLocation key, LivingEntity entity, CompoundTag data, CallbackInfo callback) {
        if (function(key, entity, data).handled()) {
            callback.cancel();
        }
    }
}
