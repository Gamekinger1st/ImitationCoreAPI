package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceEditRegistryTest {
    @Test
    void mergesRaceProfilesWithHigherPriorityWinningPerKey() {
        RaceEditRegistry registry = new RaceEditRegistry();
        ResourceLocation bridge = ResourceLocation.fromNamespaceAndPath("test", "bridge");
        ResourceLocation race = ResourceLocation.fromNamespaceAndPath("test", "race");
        CompoundTag data = new CompoundTag();
        data.putString("value", "stored");

        registry.register(new Provider(ResourceLocation.fromNamespaceAndPath("test", "low"), 0, RaceEditProfile.builder(race)
                .stat(RaceStatKeys.MAX_HEALTH, 20D)
                .line(RaceLineKeys.NAME, Component.literal("Low"))
                .data(ResourceLocation.fromNamespaceAndPath("test", "payload"), data)
                .build()));
        registry.register(new Provider(ResourceLocation.fromNamespaceAndPath("test", "high"), 10, RaceEditProfile.builder(race)
                .stat(RaceStatKeys.MAX_HEALTH, 40D)
                .line(RaceLineKeys.DESCRIPTION, Component.literal("High description"))
                .build()));

        RaceEditProfile profile = registry.profile(bridge, race);

        assertEquals(Optional.of(40D), profile.stat(RaceStatKeys.MAX_HEALTH));
        assertEquals("Low", profile.line(RaceLineKeys.NAME).orElseThrow().getString());
        assertEquals("High description", profile.line(RaceLineKeys.DESCRIPTION).orElseThrow().getString());
        assertEquals("stored", profile.data(ResourceLocation.fromNamespaceAndPath("test", "payload")).orElseThrow().getString("value"));
    }

    @Test
    void appliesDirectOverridesAfterProvidersWithBridgeSpecificOverrideLast() {
        RaceEditRegistry registry = new RaceEditRegistry();
        ResourceLocation bridge = ResourceLocation.fromNamespaceAndPath("test", "bridge");
        ResourceLocation otherBridge = ResourceLocation.fromNamespaceAndPath("test", "other_bridge");
        ResourceLocation race = ResourceLocation.fromNamespaceAndPath("test", "race");

        registry.register(new Provider(ResourceLocation.fromNamespaceAndPath("test", "provider"), 0, RaceEditProfile.builder(race)
                .stat(RaceStatKeys.MAX_AURA, 10D)
                .build()));
        registry.override(RaceEditProfile.builder(race)
                .stat(RaceStatKeys.MAX_AURA, 20D)
                .build());
        registry.override(bridge, RaceEditProfile.builder(race)
                .stat(RaceStatKeys.MAX_AURA, 30D)
                .build());

        assertEquals(Optional.of(30D), registry.profile(bridge, race).stat(RaceStatKeys.MAX_AURA));
        assertEquals(Optional.of(20D), registry.profile(otherBridge, race).stat(RaceStatKeys.MAX_AURA));
    }

    @Test
    void unregistersDirectOverridesWithoutRemovingReplacement() {
        RaceEditRegistry registry = new RaceEditRegistry();
        ResourceLocation bridge = ResourceLocation.fromNamespaceAndPath("test", "bridge");
        ResourceLocation race = ResourceLocation.fromNamespaceAndPath("test", "race");
        RaceEditRegistration first = registry.override(RaceEditProfile.builder(race).stat(RaceStatKeys.MAX_AURA, 10D).build());
        registry.override(RaceEditProfile.builder(race).stat(RaceStatKeys.MAX_AURA, 20D).build());

        assertFalse(first.unregister());
        assertEquals(Optional.of(20D), registry.profile(bridge, race).stat(RaceStatKeys.MAX_AURA));
    }

    @Test
    void usesHighestPriorityHandledFunctionResult() {
        RaceEditRegistry registry = new RaceEditRegistry();
        ResourceLocation bridge = ResourceLocation.fromNamespaceAndPath("test", "bridge");
        ResourceLocation race = ResourceLocation.fromNamespaceAndPath("test", "race");

        registry.registerFunction(new FunctionProvider(ResourceLocation.fromNamespaceAndPath("test", "low"), 0, RaceFunctionResult.booleanValue(false)));
        registry.registerFunction(new FunctionProvider(ResourceLocation.fromNamespaceAndPath("test", "high"), 10, RaceFunctionResult.booleanValue(true)));

        RaceFunctionResult result = registry.handleFunction(new RaceFunctionContext(bridge, race, RaceFunctionKeys.CAN_TICK));

        assertTrue(result.handled());
        assertEquals(Optional.of(true), result.booleanValue());
    }

    @Test
    void returnsPassWhenProvidersCannotHandleFunction() {
        RaceEditRegistry registry = new RaceEditRegistry();
        ResourceLocation bridge = ResourceLocation.fromNamespaceAndPath("test", "bridge");
        ResourceLocation race = ResourceLocation.fromNamespaceAndPath("test", "race");

        registry.registerFunction(new ThrowingFunctionProvider(ResourceLocation.fromNamespaceAndPath("test", "throwing")));

        RaceFunctionResult result = registry.handleFunction(new RaceFunctionContext(bridge, race, RaceFunctionKeys.CAN_TICK));

        assertFalse(result.handled());
    }

    private record Provider(ResourceLocation id, int priority, RaceEditProfile profile) implements RaceEditProvider {
        @Override
        public Optional<RaceEditProfile> edit(RaceEditContext context) {
            return Optional.of(profile);
        }
    }

    private record FunctionProvider(ResourceLocation id, int priority, RaceFunctionResult result) implements RaceFunctionHandler {
        @Override
        public RaceFunctionResult handle(RaceFunctionContext context) {
            return result;
        }
    }

    private record ThrowingFunctionProvider(ResourceLocation id) implements RaceFunctionHandler {
        @Override
        public RaceFunctionResult handle(RaceFunctionContext context) {
            throw new IllegalStateException();
        }
    }
}
