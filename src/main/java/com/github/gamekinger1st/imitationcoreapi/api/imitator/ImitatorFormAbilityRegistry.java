package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ImitatorFormAbilityRegistry {
    private final Map<ResourceLocation, ImitatorFormAbility> abilities = new LinkedHashMap<>();
    private final Map<java.util.UUID, Map<ResourceLocation, Long>> cooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    public synchronized ImitatorFormAbilityRegistration register(ImitatorFormAbility ability) {
        Objects.requireNonNull(ability, "ability");
        ResourceLocation id = Objects.requireNonNull(ability.id(), "ability.id");
        if (abilities.putIfAbsent(id, ability) != null) {
            throw new IllegalArgumentException("An Imitator form ability is already registered for " + id);
        }
        return new RegisteredAbility(this, id, ability);
    }

    public synchronized Optional<ImitatorFormAbility> get(ResourceLocation id) {
        return Optional.ofNullable(abilities.get(Objects.requireNonNull(id, "id")));
    }

    public List<ImitatorFormAbility> activeAbilities(IdentitySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return orderedAbilities().stream()
                .filter(ability -> supports(ability, snapshot))
                .filter(ability -> hasActiveAbility(ability, snapshot))
                .toList();
    }

    public List<ImitatorFormAbility> activeAbilities(IdentitySnapshot snapshot, ImitatorSkillCopyPolicy policy, ImitatorSkillCopyAccess access) {
        return activeAbilities(snapshot, policy, access, false);
    }

    public List<ImitatorFormAbility> activeAbilities(IdentitySnapshot snapshot, ImitatorSkillCopyPolicy policy, ImitatorSkillCopyAccess access, boolean mastered) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(access, "access");
        return activeAbilities(snapshot).stream()
                .filter(ability -> policy.allows(ability, snapshot, access, mastered))
                .toList();
    }

    public List<ImitatorFormAbility> tickingAbilities(IdentitySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return orderedAbilities().stream()
                .filter(ability -> supports(ability, snapshot))
                .filter(ability -> hasTickAbility(ability, snapshot))
                .toList();
    }

    public List<ImitatorFormAbility> tickingAbilities(IdentitySnapshot snapshot, ImitatorSkillCopyPolicy policy, ImitatorSkillCopyAccess access) {
        return tickingAbilities(snapshot, policy, access, false);
    }

    public List<ImitatorFormAbility> tickingAbilities(IdentitySnapshot snapshot, ImitatorSkillCopyPolicy policy, ImitatorSkillCopyAccess access, boolean mastered) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(access, "access");
        return tickingAbilities(snapshot).stream()
                .filter(ability -> policy.allows(ability, snapshot, access, mastered))
                .toList();
    }

    public ImitatorActionResult activate(ServerPlayer player, IdentitySnapshot snapshot) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshot, "snapshot");
        ImitatorFormAbilityContext context = new ImitatorFormAbilityContext(player, snapshot);
        for (ImitatorFormAbility ability : activeAbilities(snapshot)) {
            try {
                return activateWithCooldown(ability, context);
            } catch (RuntimeException | LinkageError exception) {
                return ImitatorActionResult.rejected("Copied form ability failed: " + ability.id());
            }
        }
        return ImitatorActionResult.rejected("The copied form has no active form ability");
    }

    public ImitatorActionResult activate(ServerPlayer player, IdentitySnapshot snapshot, ImitatorSkillCopyPolicy policy, ImitatorSkillCopyAccess access) {
        return activate(player, snapshot, policy, access, false);
    }

    public ImitatorActionResult activate(ServerPlayer player, IdentitySnapshot snapshot, ImitatorSkillCopyPolicy policy, ImitatorSkillCopyAccess access, boolean mastered) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(access, "access");
        ImitatorFormAbilityContext context = new ImitatorFormAbilityContext(player, snapshot);
        for (ImitatorFormAbility ability : activeAbilities(snapshot, policy, access, mastered)) {
            try {
                return activateWithCooldown(ability, context);
            } catch (RuntimeException | LinkageError exception) {
                return ImitatorActionResult.rejected("Copied form ability failed: " + ability.id());
            }
        }
        return ImitatorActionResult.rejected("The copied form has no active form ability");
    }

    public void tick(ServerPlayer player, IdentitySnapshot snapshot) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshot, "snapshot");
        ImitatorFormAbilityContext context = new ImitatorFormAbilityContext(player, snapshot);
        for (ImitatorFormAbility ability : tickingAbilities(snapshot)) {
            try {
                ability.tick(context);
            } catch (RuntimeException | LinkageError exception) {
            }
        }
    }

    public void tick(ServerPlayer player, IdentitySnapshot snapshot, ImitatorSkillCopyPolicy policy, ImitatorSkillCopyAccess access) {
        tick(player, snapshot, policy, access, false);
    }

    public void tick(ServerPlayer player, IdentitySnapshot snapshot, ImitatorSkillCopyPolicy policy, ImitatorSkillCopyAccess access, boolean mastered) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(access, "access");
        ImitatorFormAbilityContext context = new ImitatorFormAbilityContext(player, snapshot);
        for (ImitatorFormAbility ability : tickingAbilities(snapshot, policy, access, mastered)) {
            try {
                ability.tick(context);
            } catch (RuntimeException | LinkageError exception) {
            }
        }
    }

    public synchronized List<ImitatorFormAbility> abilities() {
        return orderedAbilities();
    }

    public void clearPlayer(java.util.UUID playerId) {
        cooldowns.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    private ImitatorActionResult activateWithCooldown(ImitatorFormAbility ability, ImitatorFormAbilityContext context) {
        int cooldown = Math.max(0, ability.cooldownTicks(context.snapshot()));
        long now = context.player().level().getGameTime();
        Map<ResourceLocation, Long> playerCooldowns = cooldowns.computeIfAbsent(context.player().getUUID(), ignored -> new java.util.concurrent.ConcurrentHashMap<>());
        long readyAt = playerCooldowns.getOrDefault(ability.id(), 0L);
        if (now < readyAt) {
            return ImitatorActionResult.rejected("Copied form ability is on cooldown for " + (readyAt - now) + " ticks");
        }
        ImitatorActionResult result = Objects.requireNonNull(ability.activate(context), "form ability activation result");
        if (result.accepted() && cooldown > 0) {
            playerCooldowns.put(ability.id(), Math.addExact(now, cooldown));
        }
        return result;
    }

    private boolean supports(ImitatorFormAbility ability, IdentitySnapshot snapshot) {
        try {
            return ability.supports(snapshot);
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private boolean hasActiveAbility(ImitatorFormAbility ability, IdentitySnapshot snapshot) {
        try {
            return ability.hasActiveAbility(snapshot);
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private boolean hasTickAbility(ImitatorFormAbility ability, IdentitySnapshot snapshot) {
        try {
            return ability.hasTickAbility(snapshot);
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private synchronized List<ImitatorFormAbility> orderedAbilities() {
        return abilities.values().stream()
                .sorted(Comparator.comparingInt(ImitatorFormAbility::priority).reversed().thenComparing(ability -> ability.id().toString()))
                .toList();
    }

    private synchronized boolean unregister(ResourceLocation id, ImitatorFormAbility ability) {
        return abilities.remove(id, ability);
    }

    private record RegisteredAbility(ImitatorFormAbilityRegistry registry, ResourceLocation id, ImitatorFormAbility ability) implements ImitatorFormAbilityRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, ability);
        }
    }
}
