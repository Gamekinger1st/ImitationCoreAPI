# Imitation Core API roadmap

The broader goal is a reusable NeoForge API for identity imitation: other mods can record an entity blueprint, present and play as that identity, and opt into GeckoLib or ManasCore/Tensura behavior without making those mods mandatory.

## 0.5.0 implemented foundation

### Recording and form libraries

- [x] Capture bounded, sanitized mob and player identity snapshots without copying transient position, target, motion, attack, passenger, leash, or inventory state.
- [x] Capture entity type, display identity, dimensions, eye height, physical attributes, visual equipment, player profile textures, appraisal data, skills, Gecko controller metadata, and optional Tensura state.
- [x] Store versioned form libraries with bounded slots, deterministic migration, malformed-data rejection, future-version quarantine, pruning, selection, pending records, and continuation after selection.
- [x] Keep copied equipment visual-only; transformations and replicas create no real copied item stacks.

### Transform

- [x] Run capture, validation, apply, active, revert, and failed-cleanup through server-authoritative sessions.
- [x] Apply recorded health ratio, physical attribute bases, movement speed, jump strength, gravity, scale, interaction range, and dimensions without deleting unrelated attribute modifiers.
- [x] Restore the owner's exact recorded health, absorption, air, fire time, attribute bases, dimensions, and optional compatibility state on revert.
- [x] Keep Surface Imitation available as a separate visual-only operation.
- [x] Force safe reversion on lethal form damage, logout, clone/death, dimension transfer, skill removal, duration expiry, server stop, and interrupted-session recovery.

### Perfect Form

- [x] Retain the lower-level mirror-sync operation under the player-facing name Perfect Form.
- [x] Make Perfect Form an explicit opt-in instead of inferring it from ordinary gameplay transformation.
- [x] Separate physical-form scaling from Perfect Form scaling so disabling Perfect Form cannot zero attributes or Tensura energy.
- [x] Capture and transactionally restore supported race, EP, MP, aura, spiritual health, abilities, spirit state, existence state, and attributes.
- [x] Set copied current EP components to the copied form's applied limits and restore the owner's previous values exactly.
- [x] Let addon skill definitions choose exact copying, target-EP limits, precision requirements, and optional scaling in code rather than server configuration.

### Skills and owner suppression

- [x] Capture bounded non-temporary skill IDs and mastery through the optional skill bridge.
- [x] Let stronger imitators use all policy-approved recorded skills and abilities; let weaker imitators exclude Ultimate and Intrinsic entries by default.
- [x] Let mastered, addon-defined policies opt into Unique or Ultimate copying.
- [x] Suppress the owner's original active skills while transformed and fail closed if transformed skill ownership cannot be proven.
- [x] Back up existing copies, grant session-owned skills transactionally, restore existing skill state exactly, and remove skills that did not exist before the session.
- [x] Keep controller skills usable so the host skill can still revert or change modes.

### Form abilities

- [x] Expose a public prioritized registry for active and ticking form abilities with policy categories and per-player cooldowns.
- [x] Provide built-in spider climbing, skeleton arrow, enderman teleport, undead sunlight, and blade-tiger voice-cannon behavior.
- [x] Register a configurable client keybind, default `R`, and validate activation against the active server session.
- [x] Allow addons to register entity-specific or snapshot-predicate abilities without editing Core.

### Factions and targeting

- [x] Resolve factions through public providers, vanilla family mappings, exact-type fallback, and Tensura entity-type tags.
- [x] Apply copied faction behavior during ordinary Transform as well as Perfect Form.
- [x] Intercept normal mob targets, brain attack-target memories, Tensura prey checks, and Tensura subordinate type comparisons.
- [x] Preserve natural predator behavior and retaliation after the disguised player attacks a mob.
- [x] Periodically reconcile already-acquired targets with configurable server bounds.

### Form stats and progression

- [x] Track legitimate owner progression while transformed.
- [x] Apply the same EP and supported stat deltas to the stored form and current transformed presentation.
- [x] Persist updated form values so appraisal masking reflects later progression.
- [x] Support addon-defined transform time limits in minutes, where `0` is unlimited.
- [x] Move current magicules and aura together with lower copied limits so gradual form transitions cannot trigger Magicule Poison or Insanity.

### Rendering, animations, camera, and appraisal

- [x] Synchronize disguises to self, tracking players, late joiners, and newly tracking players with bounded versioned payloads.
- [x] Maintain bounded client disguise and replica-visual caches and clear them on entity leave or disconnect.
- [x] Render cached fake player or living entities while retaining the real server-side player.
- [x] Synchronize position, rotation, body/head rotation, pose, crouch, sprint, swim, use, swing, attack, hurt, death, and once-per-tick walk state.
- [x] Map live animation intent to captured Gecko trigger names, retain one-shots for their controller playback, and use a crash-safe renderer fallback.
- [x] Apply first-person eye height and gameplay dimensions only in scopes that permit them.
- [x] Render localized appraisal values from the active, progression-updated form while preserving a safe fallback.

### Replicas

- [x] Keep replicas in separate temporary sessions with safe collision-checked spawning.
- [x] Support living mob forms and a safe armor-stand presentation fallback for recorded players.
- [x] Copy permitted NBT, physical state, optional Tensura state, captured temporary skills and mastery, and visual-only equipment.
- [x] Suppress drops and experience, default away from targeting the owner, and enforce lifetime, distance, death, dimension, and server cleanup.
- [x] Persist ownership/session/expiry tags so orphaned replicas are discarded when their chunks load.

### Chat replacement and Discord

- [x] Route global, configurable-range local, direct, system, and addon-defined messages through a server-authoritative moderation, audit, rate-limit, persona, and delivery pipeline.
- [x] Preserve the real account UUID for authority and audit, never forge a Mojang signature, and provide a vanilla-compatible system-message fallback.
- [x] Remember a player's global/local default and let servers configure the initial channel.
- [x] Negotiate an optional client replacement protocol with Global/Local tabs, unread counts, command preservation, bounded history, and unsigned Core delivery.
- [x] Preserve unfinished chat drafts when the screen closes without submission and clear them after a submitted message or command.
- [x] Relay Core chat plus join, leave, death, and Core system messages to Discord through a webhook.
- [x] Relay Discord messages, replies, and attachment links into global Minecraft chat using bounded bot-token polling, loop prevention, and rate-limit backoff.
- [x] Support environment-provided Discord secrets and live status/reload commands without printing credentials.

### Public customization APIs

- [x] Let addons define complete Imitator-like skills with their own costs, cooldowns, slots, duration, progression, Perfect Form, skill-copy, form-ability, and replica policies.
- [x] Expose prioritized registration for snapshot, gameplay, renderer, animation, faction, chat, Gecko, skill, Tensura, race-line, race-stat, and race-function adapters.
- [x] Support lookup-scoped skill classification overrides without requiring unsafe permanent mutation of shared skill singletons.
- [x] Support race stat, line, text, resource, and function edits with typed replacement results and reflective optional-mod isolation.

### Safety, packaging, and verification

- [x] Split required core mixins from degradable optional compatibility mixins.
- [x] Catch runtime and linkage failures at optional integration boundaries while leaving unrelated Core features available.
- [x] Package both mixin configs, a deterministic refmap, metadata, translations, and no runtime logs or Discord credentials.
- [x] Ignore generated run worlds, logs, caches, and local credentials in Git.
- [x] Verify public API sources do not import internal implementation packages.
- [x] Cover serialization, limits, migration, targeting, policies, cleanup, chat, Discord parsing, Gecko timing, and service behavior with unit tests.
- [x] Cover real entity snapshot capture and replica ownership tags with NeoForge GameTests.
- [x] Start successfully as a standalone dedicated-server mod and with GeckoLib 4.8.4, ManasCore 4.0.0.2, and Tensura 2.0.1.0 present.

## Reported beta defects fixed in code

The items below have automated or headless coverage where possible. The client-visible results remain part of the live beta validation section.

### Recording and eligible targets

- [x] Make bosses non-copiable.
- [x] Reject transformation into non-mob entities.

### Perfect Form

- [x] Change the transformed player's health correctly when Perfect Form copies another player.
- [x] Make the copied form's Unique and Ultimate skills available during Perfect Form.

### Player identity and names

- [x] Remove purple transformed-player names so a copied player is visually indistinguishable from the real player.
- [x] Keep the owner's player name unchanged when transforming into a non-player entity.
- [x] Keep chat names unchanged by transformations while preserving normal analysis fooling for copied entities.

### Analysis fooling

- [x] Render the complete analysis background by calling the main mod's analysis overlay instead of drawing only copied text.
- [x] Update fooled analysis values from the transformed entity's live status so displayed health falls when the transformed player is damaged.

### Form abilities

- [x] Add an appropriate copied-form ability for Creepers.

### Movement

- [x] Fix copied walk speed; it now captures the effective attribute value and uses Minecraft 1.21's generic attribute IDs.
- [x] Fix Auto-Jump still occurring during transformations when it should not.

## Required live beta validation

- [ ] Verify mob and player rendering, copied skins, first-person camera, all vanilla animations, and representative GeckoLib attack/one-shot animations in a client.
- [ ] Verify ordinary Transform copies movement, jump, health, dimensions, energy limits, abilities, skills, factions, appraisal, and form progression without applying Perfect Form race or storage state.
- [ ] Verify high magicules and aura descend with their copied maximums and restore on reversion without Magicule Poison, Insanity, or death.
- [ ] Verify Perfect Form applies exact configured Tensura state and restores the owner without magicule poisoning or death.
- [ ] Verify giant-ant, barghest, leech-lizard, skeleton, retaliation, and natural-prey targeting with several Tensura entity families.
- [ ] Verify `R` abilities and their cooldown feedback for every built-in form.
- [ ] Verify replica AI uses its native behavior and captured passive/AI-consumed skills, then cleans up across death, distance, expiry, restart, and chunk unload/reload.
- [ ] Verify two-client tracking, late join, dimension change, reconnect, death reversion, and mixed modded/vanilla chat fallback.
- [ ] Verify replacement chat tabs, commands, drafts, global/local preference, moderation rejection, and reconnect behavior.
- [ ] Verify Discord webhook delivery and inbound bot polling with live credentials, attachments, replies, 429 backoff, reload, and loop prevention.

## Later expansion

- [ ] Add a richer replacement chat HUD with searchable history, timestamps, party tabs, direct-message UI, accessibility controls, and addon-defined channel tabs.
- [ ] Add Discord Gateway support for servers that prefer it over bounded REST polling.
- [ ] Add public replica behavior adapters for skills whose active AI cannot be inferred from the copied entity's native goals.
- [ ] Add more mod-specific form-ability, renderer, multipart, and Gecko controller adapters as incompatibilities are reported.
