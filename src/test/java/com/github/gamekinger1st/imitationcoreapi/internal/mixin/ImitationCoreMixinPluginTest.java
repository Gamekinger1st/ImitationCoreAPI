package com.github.gamekinger1st.imitationcoreapi.internal.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ImitationCoreMixinPluginTest {
    @Test
    void rewritesSubordinateTargetTypeLookupsToTheActiveFormHook() {
        ClassNode target = new ClassNode();
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC,
                "shouldTarget",
                "(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Z",
                null,
                null
        );
        MethodInsnNode typeLookup = new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/LivingEntity",
                "getType",
                "()Lnet/minecraft/world/entity/EntityType;",
                false
        );
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(typeLookup);
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        target.methods.add(method);

        new ImitationCoreMixinPlugin().preApply(
                "io.github.manasmods.tensura.entity.template.subclass.ISubordinate",
                target,
                "com.github.gamekinger1st.imitationcoreapi.internal.mixin.MixinTensuraSubordinateTargeting",
                null
        );

        assertEquals(Opcodes.INVOKESTATIC, typeLookup.getOpcode());
        assertEquals("com/github/gamekinger1st/imitationcoreapi/internal/targeting/ActiveFormTargetingHooks", typeLookup.owner);
        assertEquals("effectiveType", typeLookup.name);
        assertEquals(
                "(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/entity/EntityType;",
                typeLookup.desc
        );
        assertFalse(typeLookup.itf);
    }
}
