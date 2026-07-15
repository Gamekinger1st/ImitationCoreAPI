package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "io.github.manasmods.tensura.entity.template.subclass.ISubordinate", remap = false)
public interface MixinTensuraSubordinateTargeting {
}
