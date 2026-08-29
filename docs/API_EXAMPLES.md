# API examples

## Define an imitation skill

```java
ImitatorSkillDefinition definition = ImitatorSkillDefinition.builder(
        ResourceLocation.fromNamespaceAndPath("example", "echo_form"),
        "Echo Form",
        "Records a target and assumes its form"
).transformDurationMinutes(10)
 .forceAutoJump(true)
 .formLibraryLimits(new ImitatorFormLibraryLimits(8, 64, 12_000L))
 .skillCopyPolicy(ImitatorSkillCopyPolicy.builder()
         .maximumCopiedSkills(32)
         .allowUniqueSkills(true)
         .allowUltimateSkills(true)
         .build())
 .build();
```

Use `ImitatorSkillController` from the host skill to stage recordings, open selection, transform, revert, and activate the selected form ability. Costs, cooldowns, mastery, and the skill icon remain owned by the addon skill.

Use `.autoJumpOverride(ImitatorAutoJumpOverride.INHERIT)`, `FORCE_ENABLED`, or `FORCE_DISABLED` when the transformed player should inherit or override their client Auto-Jump setting. `.forceAutoJump(true)` is the convenience form for forced NPC-style obstacle jumping.

## Start Transform or Perfect Form directly

```java
SessionTransitionResult transform = ImitationApi.imitatorHandlers()
        .beginTransform(player, snapshotId);

SessionTransitionResult perfectForm = ImitationApi.imitatorHandlers()
        .beginPerfectForm(player, snapshotId, 1D);
```

`beginTransform` copies the form's appearance, physical attributes, factions, abilities, animations, and permitted skills without replacing optional Tensura race or storage state. When copied physical attributes lower a Tensura energy limit, current magicules or aura and its maximum descend together at Tensura's configured excess-energy rate. `beginPerfectForm` explicitly enables the configured exact Tensura state-copy path; the scale is a skill-definition value, where `1D` requests the recorded values exactly.

## Register a form ability

```java
ImitationApi.imitatorFormAbilities().register(new ImitatorFormAbility() {
    public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath("example", "burrow"); }
    public int priority() { return 100; }
    public boolean supports(IdentitySnapshot snapshot) { return snapshot.entityType().equals(MY_ENTITY); }
    public boolean hasActiveAbility(IdentitySnapshot snapshot) { return true; }
    public ImitatorActionResult activate(ImitatorFormAbilityContext context) {
        return ImitatorActionResult.accepted("Burrow activated");
    }
});
```

## Register a faction family

```java
ImitationApi.mobFactions().register(new MobFactionResolver() {
    public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath("example", "colony"); }
    public int priority() { return 200; }
    public Optional<ResourceLocation> resolve(ResourceLocation entityType) {
        return COLONY_TYPES.contains(entityType) ? Optional.of(COLONY_FACTION) : Optional.empty();
    }
});
```

## Edit a race without mutating Core

```java
RaceEditProfile profile = RaceEditProfile.builder(TARGET_RACE)
        .stat(RaceStatKeys.MAX_HEALTH, 60D)
        .line(RaceLineKeys.DESCRIPTION, Component.literal("Custom description"))
        .build();

ImitationApi.raceEdits().override(
        ResourceLocation.fromNamespaceAndPath("example", "race_edits"),
        profile
);

ImitationApi.raceEdits().registerFunction(new RaceFunctionHandler() {
    public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath("example", "race_functions"); }
    public RaceFunctionResult handle(RaceFunctionContext context) {
        if (context.raceId().equals(TARGET_RACE) && context.functionId().equals(RaceFunctionKeys.ON_TICK)) {
            return RaceFunctionResult.handledResult();
        }
        return RaceFunctionResult.pass();
    }
});
```

Use `RaceFunctionResult.rawValue` only when an optional-mod method returns its own type, such as a Tensura evolution-requirement map. The addon that supplies that value is responsible for using the exact supported optional-mod type.

## Override copied-skill classification safely

```java
SkillClassificationRegistration registration = ImitationApi.skillClassifications()
        .override(SKILL_ID, SkillClassification.INTRINSIC);
```

Closing the returned registration removes the override. This lookup-level API is preferred over globally mutating a registered skill object.
