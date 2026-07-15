package com.github.gamekinger1st.imitationcoreapi.api.application;

import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransformationApplicationRegistryTest {
    @Test
    void appliesHighestPriorityFirstAndRevertsLowestPriorityFirst() {
        TransformationApplicationRegistry registry = new TransformationApplicationRegistry();
        registry.register(new TestAdapter("low", -5));
        registry.register(new TestAdapter("high", 5));
        registry.register(new TestAdapter("middle", 0));

        assertEquals(List.of("high", "middle", "low"), registry.applyOrder().stream().map(adapter -> adapter.id().getPath()).toList());
        assertEquals(List.of("low", "middle", "high"), registry.revertOrder().stream().map(adapter -> adapter.id().getPath()).toList());
    }

    @Test
    void excludesGameplayAdaptersFromSurfaceImitation() {
        TransformationApplicationRegistry registry = new TransformationApplicationRegistry();
        registry.register(new TestAdapter("gameplay", 0));
        registry.register(new SurfaceAdapter());

        assertEquals(List.of("surface"), registry.applyOrder(TransformationScope.SURFACE).stream().map(adapter -> adapter.id().getPath()).toList());
        assertEquals(List.of("gameplay"), registry.applyOrder(TransformationScope.GAMEPLAY).stream().map(adapter -> adapter.id().getPath()).toList());
    }

    private record TestAdapter(String path, int priority) implements TransformationApplicationAdapter {
        @Override
        public ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath("test", path);
        }

        @Override
        public Optional<String> validate(TransformationApplicationContext context) {
            return Optional.empty();
        }

        @Override
        public List<TemporaryStateDefinition> prepare(TransformationApplicationContext context) {
            return List.of();
        }

        @Override
        public void apply(TransformationApplicationContext context, List<TemporaryStateReference> temporaryState) {
        }

        @Override
        public void revert(TransformationReversionContext context, List<TemporaryStateReference> temporaryState) {
        }
    }

    private static final class SurfaceAdapter implements TransformationApplicationAdapter {
        @Override
        public ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath("test", "surface");
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public boolean appliesTo(TransformationScope scope) {
            return scope == TransformationScope.SURFACE;
        }

        @Override
        public Optional<String> validate(TransformationApplicationContext context) {
            return Optional.empty();
        }

        @Override
        public List<TemporaryStateDefinition> prepare(TransformationApplicationContext context) {
            return List.of();
        }

        @Override
        public void apply(TransformationApplicationContext context, List<TemporaryStateReference> temporaryState) {
        }

        @Override
        public void revert(TransformationReversionContext context, List<TemporaryStateReference> temporaryState) {
        }
    }
}
