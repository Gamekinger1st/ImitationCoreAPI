package com.github.gamekinger1st.imitationcoreapi.api.application;

import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public interface TransformationApplicationAdapter {
    ResourceLocation id();

    int priority();

    default boolean appliesTo(TransformationScope scope) {
        return scope.appliesGameplayState();
    }

    Optional<String> validate(TransformationApplicationContext context);

    List<TemporaryStateDefinition> prepare(TransformationApplicationContext context);

    void apply(TransformationApplicationContext context, List<TemporaryStateReference> temporaryState);

    void revert(TransformationReversionContext context, List<TemporaryStateReference> temporaryState);
}
