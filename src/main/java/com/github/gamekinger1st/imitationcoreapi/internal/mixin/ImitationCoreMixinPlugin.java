package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class ImitationCoreMixinPlugin implements IMixinConfigPlugin {
    private static final String TENSURA_OVERLAY = "io.github.manasmods.tensura.handler.client.OverlayHandler";
    private static final String TENSURA_BEHAVIOUR_HELPER = "io.github.manasmods.tensura.entity.ai.behaviour.TensuraBehaviourHelper";
    private static final String TENSURA_SUBORDINATE = "io.github.manasmods.tensura.entity.template.subclass.ISubordinate";
    private static final String SUBORDINATE_MIXIN = "com.github.gamekinger1st.imitationcoreapi.internal.mixin.MixinTensuraSubordinateTargeting";
    private static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    private static final String ENTITY_TYPE = "net/minecraft/world/entity/EntityType";
    private static final String TARGETING_HOOKS = "com/github/gamekinger1st/imitationcoreapi/internal/targeting/ActiveFormTargetingHooks";
    private static final Set<String> OPTIONAL_TARGETS = Set.of(
            TENSURA_OVERLAY,
            TENSURA_BEHAVIOUR_HELPER,
            TENSURA_SUBORDINATE,
            "io.github.manasmods.manascore.skill.api.ManasSkillInstance",
            "io.github.manasmods.manascore.race.api.ManasRace",
            "io.github.manasmods.tensura.race.TensuraRace"
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !OPTIONAL_TARGETS.contains(targetClassName) || classPresent(targetClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (SUBORDINATE_MIXIN.equals(mixinClassName)) {
            replaceSubordinateTypeLookups(targetClass);
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean classPresent(String className) {
        String resourceName = className.replace('.', '/') + ".class";
        return ImitationCoreMixinPlugin.class.getClassLoader().getResource(resourceName) != null;
    }

    private static void replaceSubordinateTypeLookups(ClassNode targetClass) {
        for (MethodNode method : targetClass.methods) {
            if (!method.name.equals("shouldTarget") && !method.name.equals("shouldStopTarget")) {
                continue;
            }
            for (var instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode invocation
                        && invocation.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && invocation.owner.equals(LIVING_ENTITY)
                        && invocation.name.equals("getType")
                        && invocation.desc.equals("()L" + ENTITY_TYPE + ";")) {
                    invocation.setOpcode(Opcodes.INVOKESTATIC);
                    invocation.owner = TARGETING_HOOKS;
                    invocation.name = "effectiveType";
                    invocation.desc = "(L" + LIVING_ENTITY + ";)L" + ENTITY_TYPE + ";";
                    invocation.itf = false;
                }
            }
        }
    }
}
