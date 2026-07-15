package com.github.gamekinger1st.imitationcoreapi.api.targeting;

import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationRepository;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobImitationTargetingServiceTest {
    @Test
    void suppressesTargetsOnlyWhenTheCopiedFormSharesTheAggressorFaction() {
        MobFactionRegistry factions = new MobFactionRegistry();
        ResourceLocation skeleton = ResourceLocation.withDefaultNamespace("skeleton");
        ResourceLocation stray = ResourceLocation.withDefaultNamespace("stray");
        ResourceLocation zombie = ResourceLocation.withDefaultNamespace("zombie");
        ResourceLocation undeadArcher = ResourceLocation.fromNamespaceAndPath("test", "undead_archer");
        ResourceLocation zombieHorde = ResourceLocation.fromNamespaceAndPath("test", "zombie_horde");
        factions.register(new Resolver(ResourceLocation.fromNamespaceAndPath("test", "skeletons"), 10, undeadArcher, List.of(skeleton, stray)));
        factions.register(new Resolver(ResourceLocation.fromNamespaceAndPath("test", "zombies"), 10, zombieHorde, List.of(zombie)));
        MobImitationTargetingService service = service(factions);

        assertTrue(service.shouldSuppressResolvedTarget(skeleton, stray, false));
        assertFalse(service.shouldSuppressResolvedTarget(zombie, skeleton, false));
        assertFalse(service.shouldSuppressResolvedTarget(skeleton, stray, true));
    }

    @Test
    void unresolvedFactionFallbackOnlySuppressesIdenticalEntityTypes() {
        MobImitationTargetingService service = service(new MobFactionRegistry());
        ResourceLocation giantAnt = ResourceLocation.fromNamespaceAndPath("tensura", "giant_ant");
        ResourceLocation barghest = ResourceLocation.fromNamespaceAndPath("tensura", "barghest");

        assertTrue(service.shouldSuppressResolvedTarget(giantAnt, giantAnt, false));
        assertFalse(service.shouldSuppressResolvedTarget(barghest, giantAnt, false));
    }

    private static MobImitationTargetingService service(MobFactionRegistry factions) {
        return new MobImitationTargetingService(new TransformationService(new EmptyRepository()), factions);
    }

    private record Resolver(ResourceLocation id, int priority, ResourceLocation faction, List<ResourceLocation> entityTypes) implements MobFactionResolver {
        @Override
        public Optional<ResourceLocation> resolve(ResourceLocation entityType) {
            return entityTypes.contains(entityType) ? Optional.of(faction) : Optional.empty();
        }
    }

    private static final class EmptyRepository implements TransformationRepository {
        @Override
        public Optional<IdentitySnapshot> snapshot(UUID snapshotId) {
            return Optional.empty();
        }

        @Override
        public void saveSnapshot(IdentitySnapshot snapshot) {
        }

        @Override
        public Optional<TransformationSession> session(UUID sessionId) {
            return Optional.empty();
        }

        @Override
        public Collection<TransformationSession> sessionsForOwner(UUID ownerId) {
            return List.of();
        }

        @Override
        public Collection<TransformationSession> sessions() {
            return List.of();
        }

        @Override
        public void saveSession(TransformationSession session) {
        }
    }
}
