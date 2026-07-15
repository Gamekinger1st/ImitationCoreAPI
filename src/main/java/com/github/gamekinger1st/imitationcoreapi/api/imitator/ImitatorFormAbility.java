package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillClassification;
import net.minecraft.resources.ResourceLocation;

public interface ImitatorFormAbility {
    ResourceLocation id();

    int priority();

    boolean supports(IdentitySnapshot snapshot);

    default SkillClassification classification(IdentitySnapshot snapshot) {
        return SkillClassification.STANDARD;
    }

    default boolean hasActiveAbility(IdentitySnapshot snapshot) {
        return false;
    }

    default ImitatorActionResult activate(ImitatorFormAbilityContext context) {
        return ImitatorActionResult.rejected("The copied form has no active form ability");
    }

    default boolean hasTickAbility(IdentitySnapshot snapshot) {
        return false;
    }

    default void tick(ImitatorFormAbilityContext context) {
    }
}
