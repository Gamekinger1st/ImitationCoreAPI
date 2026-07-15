# Imitation Core API roadmap

This checklist separates reusable API work from the TensuraOverhaul implementation of Imitator. A checked item must include automated coverage where practical and a multiplayer validation note where it changes synchronized state.

## Imitator V2 rewrite target

The rewrite target is not exact parity with the old Imitator. Preserve the fantasy and the working player-facing flows, but rebuild the internals around stable Core-owned contracts. Imitator's main idea is copying what a target looks like well enough to fool the world around the player. A recorded form is a reusable entity blueprint plus policy-approved traits, not a frozen copy of the target's transient live state.

- [ ] Keep TensuraOverhaul's `ImitatorSkill` as a thin skill host only: registration, icon/name/description, mode labels, constants, mastery, cooldowns, MP spending, and calls into Core.
- [ ] Move every reusable handler to Imitation Core: recording, form slots, selected form, pending record commits, menus, networking, transformations, replicas, factions, abilities, copied skills, appraisal masking, camera, animation, chat persona, diagnostics, and cleanup.
- [ ] Replace transient raw-state copying with explicit form components: identity, renderer profile, animation catalog, physical attributes, movement profile, faction profile, ability profile, Tensura profile, skill-copy profile, and replica profile.
- [ ] Treat `Record` as blueprint capture: sanitize brain/AI target state, active attack state, one-shot animations, motion, position, owner data, passengers, leash state, temporary combat state, and any state that should not become permanent form memory.
- [ ] Treat `Transform` as an active runtime session driven by the player: current movement, crouch, swim, sprint, swing, attack, hurt, death prevention, input abilities, cooldowns, faction profile, camera, appraisal, and renderer state come from the transformed player plus the form's static capabilities.
- [ ] Rename the player-facing Mirror Sync mode to `Perfect Form` while keeping the lower-level mirror-sync application function available for this skill and other addon integrations.
- [ ] Treat `Perfect Form` as an optional gameplay layer over Transform: copy policy-approved Tensura race, EP/MP/aura/spiritual health, attributes, skills, equipment/inventory/team data, and race abilities with a transactional baseline/revert ledger.
- [x] Make ordinary Transform always block the owner's original active skills and abilities while transformed; the player acts through the copied form, not their normal kit.
- [x] If the imitator's current/max EP is greater than the copied target's recorded max EP, grant access to all recorded target skills and form abilities for the active session.
- [x] If the imitator's current/max EP is not greater than the copied target's recorded max EP, grant access only to recorded target skills that are not Ultimate and not Intrinsic, while still blocking the owner's original active skills and abilities.
- [x] Apply copied form abilities through the same EP rule: stronger imitators get the complete ability profile, weaker imitators get only ability categories that are explicitly safe for the copied form policy.
- [ ] Treat `Replica` as a separate temporary-entity session: use the form blueprint to spawn and manage a living copy without sharing the player's transform state machine.
- [x] Rebuild form abilities through a public registry so copied forms can define `R` actions such as climbing, teleporting, ranged attacks, voice cannon, poison, flight-like movement, or mod-provided behavior without hardcoding Imitator-specific cases in TensuraOverhaul.
- [x] Rebuild faction behavior through entity-type/faction resolvers and targeting hooks, not individual mob exceptions; transformed players should be evaluated as their active form faction even when Perfect Form is disabled.
- [x] Rebuild GeckoLib support as animation-capability mapping: record available controllers/triggers, ignore stale recorded playback, and drive walk, sprint, crouch, swim, attack, hurt, death, head/body rotation, and custom triggers from live player state.
- [x] Rebuild copied-skill behavior as an EP-based skill-host policy: stronger imitators receive all known target skills, weaker or equal imitators receive every copied skill except Ultimate and Intrinsic, unknown/denied skills remain policy-gated, and the owner's original skills are suppressed while transformed.
- [x] Track owner-and-form progression deltas while transformed: EP/stat gains earned through the active form must persist to the real owner, update the active form profile by the same amount, update live transformed stats, and make appraisal/analysis masking report the updated copied-form values.
- [x] Add per-skill transform duration policy to the public skill definition API: addon-defined minutes, `0` means unlimited, positive values create a persisted auto-revert deadline handled by the normal safe reversion path.
- [x] Make Imitator-like skill authoring a first-class public contract: any Tensura addon depending on Imitation Core should be able to define its own Imitator-style skill by supplying definitions, costs, cooldowns, progression policy, form limits, copied-skill policy, duration policy, and optional Perfect Form/Replica policies without copying Troverhaul internals.
- [x] Rebuild death handling so lethal damage to a transformed player first reverts the active form and restores the player's pre-transform baseline instead of killing the owner unless the revert itself cannot be completed safely.
- [x] Add diagnostics for every degraded or rejected path: unsupported renderer, missing GeckoLib bridge, missing Tensura bridge, invalid snapshot, unsafe Perfect Form, no selected form, replica unsupported, faction resolver missing, temporary cleanup failed, or client protocol mismatch.
- [x] Add tests around the V2 contracts before depending on gameplay testing: blueprint sanitization, session transitions, Perfect Form apply/revert, faction profile matching, copied skills, form ability registry, animation trigger mapping, death-revert, and malformed stored data.

## Core architecture

- [x] Define a stable public API package and semantic-versioning policy.
- [x] Define immutable, versioned `IdentitySnapshot` data contracts.
- [x] Define `TransformationSession` contracts for the full transformation lifecycle.
- [x] Define a baseline-state contract that captures every value changed by a transformation.
- [x] Implement persistent session and snapshot storage with schema migrations.
- [x] Implement a server-authoritative transformation state machine: capture, validate, channel, apply, active, revert, and failed cleanup.
- [ ] Add lifecycle recovery for death, clone, logout, disconnect, dimension transfer, skill removal, world unload, and server shutdown.
- [x] Add explicit cleanup ownership/session identifiers for all temporary state.
- [x] Define an adapter registry for snapshot capture, rendering, animation, gameplay state, and compatibility providers.
- [x] Define compatibility levels: Full, Visual, Fallback, and Unsupported.
- [x] Provide diagnostics explaining why a target was accepted, degraded, or rejected.
- [x] Provide public events for snapshot capture, transformation validation, application, reversion, and cleanup.
- [ ] Provide configuration for server policy and operational safety only.
- [x] Add server controls for matching-mob target suppression, reconciliation interval, and reconciliation range.

## Snapshot capture and identity data

- [x] Capture and sanitize entity type and entity NBT.
- [x] Capture display name, custom name, and relevant visual metadata.
- [ ] Capture attributes, health, equipment, and other safe visual state.
- [x] Capture player profile UUID, account name, signed texture property metadata, and an offline-safe fallback without server-side profile lookup.
- [x] Capture camera/eye-height and supported hitbox presentation data.
- [x] Capture animation-relevant pose and controller state through adapters.
- [ ] Establish snapshot size limits, NBT allowlists, and network serialization limits.
- [ ] Add snapshot migration tests and malformed-data rejection tests.

## Networking and client presentation

- [ ] Register dedicated versioned C2S and S2C payloads.
- [ ] Validate every client request server-side against session ownership and current state.
- [ ] Sync active disguise state to trackers, late joiners, and newly tracked entities.
- [ ] Clear stale disguise state when entities leave the client level or a client disconnects.
- [ ] Implement client-side disguise state storage and render-cache invalidation.
- [x] Render fake client entities in place of real players while preserving the real server-side player.
- [x] Synchronize captured player profile identity and signed texture properties to fake-player presentation without mutating another player's GameProfile.
- [ ] Synchronize position, rotation, pose, equipment, swinging, damage, swimming, crouching, sprinting, and movement animation.
- [x] Implement a safe default renderer for incomplete adapters.
- [x] Add per-entity adapter rendering for unusual models and render paths.
- [x] Implement first-person camera-height support.
- [ ] Implement configurable name-tag and tab-list presentation with safe fallbacks.
- [ ] Add appraisal/HUD masking and alternate appraisal rendering hooks.

## Persona chat replacement

- [x] Design a server-authoritative persona-chat payload and message format.
- [x] Validate that the sender has an active, authorized persona before broadcasting.
- [x] Preserve the real sender UUID in server logs and moderation/audit data.
- [x] Render an imitated sender identity for clients that have Imitation Core API installed.
- [x] Send an explicit unsigned/system-compatible fallback to vanilla clients.
- [x] Never forge or replace Mojang signed-player messages.
- [ ] Support player personas, entity personas, and future NPC/mod-provided personas.
- [ ] Support persona display name, profile/skin styling, optional chat formatting, and localization.
- [ ] Define chat permission, mute, moderation, rate-limit, and anti-spam integration points.
- [ ] Test mixed-mod clients, vanilla-client fallback, moderation logging, and reconnect behavior.

## Complete chat overhaul

The current persona payload is only the first transport layer. Imitation Core must become a complete, optional chat replacement for clients and servers that use it. The server remains authoritative for verification, moderation, delivery, and audit. A persona is presentation data only: the real authenticated sender is retained for every permission, mute, report, rate-limit, and log decision. The implementation must never forge a Mojang-signed player message.

### Server chat authority and delivery

- [x] Define versioned server chat envelopes for global, local, party, direct, system, action-bar, and mod-defined channels, with strict packet-size and list-size budgets.
- [x] Route incoming player chat through one server-authoritative pipeline after vanilla signature verification and before client presentation.
- [x] Resolve active Imitator/persona presentation only after the real sender, permissions, channel, and target audience are known.
- [x] Require Record and Transform execution to originate from a verified server-side skill host; retain C2S packets only for bounded form-library selection and record commit actions.
- [ ] Support configurable server routing, channel membership, delivery audience, formatting policy, and vanilla-compatible fallback per channel. The `chat.default_channel` server setting currently accepts `global` or `local` for new players.
- [x] Implement a safe fallback for unmodded or mismatched clients that preserves the real sender and cannot impersonate a signed player.
- [x] Provide explicit integration hooks for mutes, bans, chat reports, profanity filters, anti-spam, rate limits, social/ignore lists, server logs, and audit plugins.
- [x] Record a structured audit entry for each delivered message: real sender, rendered persona, channel, recipients, timestamp, moderation decision, and message identifier.
- [x] Support server-owned system, announcement, death, advancement, command-feedback, and external-mod messages without treating them as player speech.
- [x] Add a server-only Discord bridge: outgoing webhook delivery for Core-delivered chat and bot-token polling for Discord-to-Minecraft messages, with loop prevention, bounded input, and local/direct relay opt-ins.
- [ ] Add Discord Gateway support as an alternative to polling, including reconnect/backoff handling and explicit message-content-intent diagnostics.
- [ ] Add configurable Discord attachment, reply, embed, reaction, thread, role, and command policy instead of treating all inbound content as plain chat.
- [ ] Handle reconnect, disconnect, dimension transfer, permission change, muted state change, and active-persona removal without stale sender presentation.

### Client chat replacement and accessibility

- [ ] Replace the core client chat screen and HUD presentation when the server advertises the chat protocol; retain vanilla chat when it does not.
- [ ] Render channel tabs, history, unread state, scrollback, timestamps, sender/persona identities, hover data, click actions, text filtering, and search from bounded client-side state.
- [ ] Render player, entity, NPC, and future mod-provided personas with clear visual distinction between a rendered persona and a real account identity where server policy requires it.
- [ ] Support configurable compact/expanded layouts, chat scale, opacity, colors, timestamps, sender labels, notification policy, and keyboard navigation.
- [ ] Support narration, screen-reader-friendly text, high-contrast settings, safe color contrast, copy/select behavior, and vanilla-equivalent chat accessibility settings.
- [ ] Preserve command entry, command suggestions, error feedback, direct-message shortcuts, links, hover text, click actions, and client-side filtering without bypassing server authority. Basic `/imitchat` global, local, and direct server commands are available pending replacement-screen integration.
- [ ] Bound history, attachment/hover payloads, avatar/profile cache size, reassembly work, and rendering work; clear transient state on logout, server change, and protocol downgrade.

### Chat protocol safety and verification

- [x] Keep signed vanilla player messages separate from persona-rendered envelopes; never claim that a persona message was cryptographically signed by the rendered identity.
- [x] Negotiate protocol and feature support during login so mixed core versions and vanilla clients receive only supported payloads.
- [x] Validate every C2S chat action against the actual connection player, channel access, rate limit, mute state, message bounds, and active server session.
- [ ] Add regression and multiplayer tests for signed chat, persona chat, mixed clients, moderation integrations, reconnects, channel routing, malformed payloads, and audit logging.

## GeckoLib compatibility module

- [ ] Create an optional client-only GeckoLib compatibility module with no hard core dependency.
- [x] Detect loaded GeckoLib-backed entity types.
- [x] Capture public, serializable GeckoLib visual state through generic adapters.
- [ ] Create fake render entities and bind GeckoLib models/controllers for active disguises.
- [ ] Map movement, idle, walk, sprint, swim, crouch, attack, hurt, death, head rotation, and body rotation into animation state.
- [ ] Support held-item and equipment-driven controller inputs where exposed.
- [x] Define a public registration API for mod-specific GeckoLib adapters.
- [ ] Add adapters for controller variables, custom NBT, multipart entities, and bespoke renderers.
- [x] Fall back to stable model rendering when a controller cannot be safely reproduced.
- [ ] Test representative GeckoLib entities from multiple mods and document support level per adapter.

## Temporary-state and anti-duplication infrastructure

- [ ] Tag temporary copied item stacks with owner UUID and transformation session ID.
- [ ] Tag and track temporary placed blocks.
- [ ] Track temporary skills, stats, effects, race state, profile state, scoreboard tags, teams, and inventories.
- [ ] Prevent or safely handle temporary-item tosses, drops, deaths, containers, crafting, smelting, trading, and item-handler inventories.
- [ ] Implement cleanup scans for owner inventories, nearby players, loaded containers, world entities, and supported modded item handlers.
- [ ] Implement deferred cleanup for unloaded or unavailable holders.
- [ ] Restore baselines atomically before the session is considered reverted.
- [ ] Add regression tests for duplication, disconnect, crash recovery, and multiplayer handoff cases.

## ManasCore and Tensura compatibility module

- [x] Create an optional ManasCore/Tensura bridge with no hard core dependency.
- [x] Add merge-safe skill snapshot restoration and ownership-tagged temporary skill grant/revoke operations through the Core temporary-state ledger.
- [x] Add optional snapshots for Manas race, Tensura existence/EP/magicule/aura/spiritual-health, ability presets, player state, spirit state, and attribute data through public storage save/load APIs.
- [x] Add a standard magicule charging operation and an EP/precision Perfect Form policy helper for addon use.
- [x] Snapshot and restore Tensura race, EP, MP, aura, spiritual health, abilities, player state, spirit state, and relevant attributes for a policy-approved Perfect Form session.
- [ ] Snapshot and restore Tensura skill storage safely.
- [x] Track, grant, configure, and remove ownership-tagged temporary ManasCore/Tensura skills.
- [ ] Snapshot and restore skill modes, presets, and slot layouts.
- [ ] Expose standalone Tensura race, EP, MP, aura, spiritual-health, and attribute restore operations outside the guarded Perfect Form transaction.
- [x] Provide constrained reproduction-scale calculations from recorded precision and live imitator-versus-target EP ratio.
- [ ] Provide standard MP charging and mastery helpers compatible with Tensura lifecycle rules.
- [ ] Add capability-safe hooks for login, death, clone, dimension transfer, and logout.
- [ ] Expose public extension points for other Tensura addons.
- [ ] Test against the supported Tensura and ManasCore versions.

## TensuraOverhaul Imitator integration

- [x] Register Imitator as a Tensura unique skill in TensuraOverhaul.
- [x] Add Imitator to Overhaul's reincarnation configuration.
- [x] Implement Core-backed Record and Transform modes with server-authoritative target validation, record staging, slot commit, automatic form selection, and reversion entry points.
- [x] Implement Replica mode.
- [x] Implement target validation, form slots, a client selection UI, and persistent form library.
- [x] Implement recording precision, precision refinement, perfect-form thresholds, and mastery progression.
- [x] Expose a reusable progression policy for recording context, transform and replica refinement, Perfect Form eligibility, perfect-form resource costs, reproduction scaling, and mastery rewards.
- [x] Implement Surface Imitation as an appearance-only transformation with persisted session scope, tracker synchronization, fake-client rendering, and a hard block on gameplay application adapters.
- [x] Implement policy-gated Tensura Perfect Form for captured race, existence, abilities, player, spirit, and attribute state with a durable apply/revert ledger entry.
- [ ] Implement Perfect Form for temporary stats, race, skills, inventory, equipment, and team state.
- [x] Apply live Tensura EP-versus-target limits through the optional bridge before starting a mirrored transformation, and cap copied vitals and attributes at the lower of form precision and owner-to-target EP ratio.
- [x] Implement safe copied-player profile and display-name presentation that remains available after the copied player logs out.
- [x] Implement configurable matching-mob faction target suppression for copied mob forms.
- [x] Implement EP-based copied-skill category selection with bounded, policy-gated temporary grants.
- [x] Implement transformed-owner original skill suppression so copied skills replace the owner's usable kit instead of stacking on top of it.
- [ ] Implement mastery-only training replicas and borrowed nearby-player form memory.
- [x] Integrate optional Tensura appraisal masking that presents copied visual-state data until a high analysis level bypasses it.
- [x] Integrate copied local-player camera eye height through a client-only core hook.
- [x] Integrate persona chat.
- [ ] Finish GeckoLib adapter support with active-animation state capture and live rendering validation.
- [x] Add administrator commands for clearing form slots and forcing safe reversion.
- [x] Create solo and multiplayer test matrices covering all transformation and cleanup states.

## Troverhaul Imitator migration and parity inventory

This is the complete behavior inventory extracted from TensuraOverhaul's Imitator implementation before its current source is removed for a rewrite. Imitator remains a TensuraOverhaul skill; the core supplies its reusable foundation. This inventory is intentionally more specific than the integration checklist above: preserve every supported player-facing behavior, but rebuild it on the versioned, server-authoritative core contracts in this roadmap. Do not copy the old mutable active-state, raw-NBT, global-broadcast, or owner-only temporary-item designs.

### Core ownership boundary

- [ ] Move every reusable Imitator handler into Imitation Core: form persistence, capture, slot selection, validation, session transition, baseline restoration, temporary-state ledger, replica lifecycle, target/faction policy, network protocol, menu protocol, client disguise rendering, camera, appraisal, persona chat, GeckoLib animation, commands, diagnostics, and recovery.
- [ ] Make Imitation Core's handler services the only owner of Imitator saved data, session identifiers, packet types, cleanup queues, and client cache state.
- [x] Keep TensuraOverhaul limited to registering the unique skill, declaring its configurable Tensura-facing costs/cooldowns/mastery defaults, wiring input/UI entry points to the core handlers, and supplying a thin optional ManasCore/Tensura adapter where the core cannot express a version-specific API directly.
- [x] Do not permit TensuraOverhaul to duplicate Imitator persistence, packet handlers, cleanup handlers, fake-render handlers, chat handlers, temporary-item handlers, or transformation state machines.
- [ ] Give every remaining legacy handler an explicit core destination and remove the Troverhaul copy only after the core handler has equivalent automated and multiplayer coverage.

### Skill definition, acquisition, and mode control

- [ ] Register the Imitator unique skill through the ManasCore/Tensura bridge, including its icon, display name, description, learning cost, EP mastery requirement, and maximum mastery.
- [x] Preserve the three skill modes: Record, Transform, and Replica.
- [ ] Preserve mode cycling, per-mode names, key handling, toggle semantics, hold/channel behavior, cooldowns, and MP costs.
- [ ] Preserve the distinction between ordinary and mastered Imitator behavior.
- [ ] Preserve Imitator's selection-menu flow when a record or transform requires a slot choice.
- [ ] Validate every target and requested operation before consuming MP, starting cooldown, mutating session state, or opening a commit path.
- [ ] Route MP payment through the bridge's standard payer-policy helper; do not bypass Family, Sentient Being, or future payment-routing rules.
- [ ] Make all skill constants and policy-dependent limits configurable through the integration module rather than hard-coding them into core.
- [x] Expose a reusable Imitator-like skill builder/factory for Tensura addons so non-Troverhaul mods can intentionally create their own version from Core policies instead of duplicating handlers.
- [x] Include per-skill transform duration in that definition; `0` is unlimited and positive minute values auto-revert through the Core session lifecycle.

### Form library, slots, and progression

- [x] Persist Core-owned indexed form slots, selected-form state, pending-record state, and bounded seen-form history with versioned serialization.
- [x] Expose slot replacement, clearing, selection, pending-record expiry, seen-form deduplication, and bounded retention through Core APIs.
- [x] Remove form-library ownership from the Troverhaul integration contract.
- [ ] Preserve persistent per-player form slots and their stable slot indexes.
- [ ] Preserve pending-record state until the player commits, cancels, times out, disconnects, or is safely recovered after restart.
- [x] Preserve transform-slot selection and refinement of the selected form.
- [ ] Preserve the seen-form library used by mastered training/replica behavior.
- [ ] Preserve deduplication rules for equivalent seen forms and recorded forms.
- [x] Preserve per-form precision, refinement gains, perfect-form thresholds, and precision-driven mastery progression.
- [x] Preserve per-player Perfect Form toggle state and make its policy/availability explicit.
- [ ] Version and migrate every stored form, slot, pending action, preference, and session record.
- [ ] Bound form count, NBT size, texture size, profile metadata, and retained seen-form history with visible diagnostics.

### Capture contract and recorded identity data

- [ ] Capture the target entity type, sanitized entity NBT, visual metadata, custom name, pose-relevant data, and stable display identity.
- [x] Capture player profile UUID, account name, and signed texture property metadata without blocking server gameplay threads.
- [x] Capture a render-safe player disguise payload with a copied profile and display name that remains usable when the source player is offline.
- [ ] Verify model variant, skin, cape, and texture-resource availability in a live client without blocking server gameplay threads.
- [ ] Apply an allowlist, byte limit, timeout policy, cache policy, and failure fallback for any remote profile/texture data.
- [ ] Capture a render-safe player disguise payload separately from gameplay-affecting state.
- [ ] Capture appraisal/HUD data needed to present the copied identity.
- [x] Capture a bounded, sanitized list of non-temporary source skill IDs and mastery values for transformed-form copy rules.
- [ ] Capture Tensura skill storage, skill modes, presets, slots, race data, EP, MP, aura, spiritual health, relevant attributes, and bridge-specific state through explicit adapters.
- [ ] Capture held items, armor, equipment, inventory, team/scoreboard state, effects, and other copied gameplay state only when the selected transformation policy allows it.
- [ ] Capture a complete baseline before applying a session, including original profile presentation, inventory, skills, race, stats, effects, attributes, team state, and temporary-state ledger.
- [ ] Sanitize untrusted NBT, reject unsupported entities/components, and record an explicit compatibility level and rejection/degradation reason.
- [ ] Never treat raw old NBT as a durable cross-version contract; give each bridge-owned snapshot section a schema version and migration path.

### Transform, Surface Imitation, and Perfect Form behavior

- [x] Establish a Core-owned transactional apply/revert adapter pipeline that persists prepared state before mutation and does not complete reversion while any state is unresolved.
- [x] Preserve Surface Imitation as an appearance-only transformation that does not accidentally grant copied gameplay state.
- [x] Preserve policy-gated Tensura Perfect Form with captured target state, captured owner baseline, live EP/precision scaling for copied vitals and attributes, and exact baseline restoration.
- [ ] Preserve Transform as a form-slot based transformation with explicit channel, validation, apply, active, revert, and failed-cleanup transitions.
- [ ] Preserve Perfect Form as an opt-in, policy-gated transformation that can temporarily mirror stats, race, skills, inventory, equipment, and team state.
- [ ] Preserve live EP-versus-target and precision-based power limits for copied gameplay state through the optional Tensura bridge.
- [x] Preserve transformed-form skill-copy rules with explicit EP-based eligibility, source-bridge matching, bounded mastery, and session-temporary cleanup.
- [x] Preserve owner-and-form EP/stat progression while transformed: legitimate progression applies to the real owner and the active form profile, survives revert, updates active form stats, and is reflected by appraisal masking.
- [ ] Preserve mastery-only training replicas and nearby-player borrowed-form memory, with explicit consent/permission and expiry policy where required.
- [ ] Preserve the inventory-empty or equivalent safety gate where a mirror transformation requires it.
- [ ] Preserve hidden/visible state handling used while a copied form is active.
- [ ] Preserve forced safe reversion when the skill is removed, toggled off, invalidated, or an unrecoverable session fault occurs.
- [ ] Make apply and revert transactional: persist `APPLYING` or `REVERTING`, perform reversible steps, verify cleanup, then commit the terminal session state.
- [ ] Never clear the active session record before baseline restoration and temporary-state reconciliation are confirmed or durably queued.

### Player identity, tab list, name tags, and player availability

- [x] Preserve copied player display names, custom/offline presentation, and visible name-tag behavior through the fake-player renderer.
- [ ] Preserve online-target tab-list behavior, including hiding/showing the original entry when a copied identity must be represented safely.
- [ ] Preserve join/leave updates when the player being impersonated changes online status.
- [x] Preserve client-visible copied profile and skin behavior without mutating another player's authoritative GameProfile.
- [ ] Replace private-field reflection for tab-list display names with public APIs or a packet-only fallback, and report a Visual/Fallback compatibility level when unavailable.
- [ ] Preserve appraisal masking and alternate appraisal presentation for active player and mob disguises.
- [ ] Keep persona-chat presentation separate from signed player chat; use only the roadmap's audited unsigned/system fallback for unsupported clients.

### Replica behavior and copied-mob interactions

- [x] Preserve Replica mode as a distinct temporary entity/session feature rather than conflating it with player transformation.
- [x] Preserve replica drop suppression, experience suppression, lifetime/ownership cleanup, and all target/death cleanup paths.
- [x] Preserve copied-mob faction target cancellation while a matching disguise is active through target interception and periodic reconciliation.
- [x] Expose deterministic public mob-faction resolver registration so addons can group their own related entity types.
- [ ] Support configurable mob-target redirection rather than only safe target cancellation.
- [ ] Preserve interaction policy for copied forms: item use, block use, entity interaction, and actions that must be denied while temporary state is active.
- [ ] Define ownership, chunk-loading, despawn, dimension, death, and server-stop behavior for every replica.

### Temporary items, blocks, inventories, and anti-duplication parity

- [x] Define standard durable ledger kinds for borrowed skills, items, stats, effects, inventories, blocks, profiles, and teams, with adapter ownership on every newly created reference.
- [ ] Tag every temporary copied item with both owner UUID and immutable transformation session ID.
- [ ] Record item origin, creation reason, stack identity/contents, holder class, and terminal cleanup status in a durable ledger.
- [ ] Preserve temporary-item handling for player inventories, armor, offhand, dropped item entities, nearby players, loaded containers, block entities, and NeoForge item handlers.
- [ ] Preserve protections and cleanup for item tosses, drops, deaths, crafting, smelting, trading, right-click item use, block use, and entity interaction.
- [ ] Preserve marked-container tracking while replacing the old unbounded owner purge queue with retryable, session-scoped deferred work that can become terminal.
- [ ] Track temporary placed blocks with world/dimension, position, original state, placed state, session ID, and a non-destructive ownership proof before removal.
- [ ] Preserve single-block and multi-block placement tracking and cleanup.
- [ ] Do not delete a legitimate replacement block merely because it has the same block type as the former temporary block.
- [ ] Preserve periodic reconciliation while making scan radius, loaded-chunk behavior, deferred work, and performance budgets configurable and observable.
- [ ] Define how unloaded chunks, unavailable inventories, modded storage, and unsupported holders are quarantined, retried, reported, and eventually resolved.
- [ ] Add explicit anti-duplication handling for disconnect, crash, restart, clone, cross-dimension transfer, container handoff, and multiplayer item transfer.

### Lifecycle, cleanup, and recovery

- [x] Route startup, login, logout, clone/death, dimension change, shutdown, interrupted apply, and failed reversion through the Core application/reversion pipeline.
- [ ] Preserve cleanup on clone/respawn, logout, death, dimension change, skill removal, toggle-off, forced reversion, and target invalidation.
- [ ] Add missing cleanup/recovery for server stopping, world unload, chunk unload where applicable, interrupted apply, interrupted revert, and failed bridge operations.
- [ ] Preserve player tick handling required for active transformation validity, costs, timing, and reconciliation.
- [ ] Preserve tracker synchronization for a player who begins tracking a disguised player.
- [ ] Add a full late-join snapshot path so a joining player receives every currently active disguise relevant to their view.
- [ ] Preserve client cache invalidation on entity leave and client logout.
- [ ] Persist recovery intent before performing destructive cleanup; emit structured diagnostics for retries and terminal failures.

### Network protocol and menus

- [x] Establish Core-owned record staging, slot commit, form selection, transform-start, and reversion request handlers bound to the actual server connection player.
- [x] Add dedicated bounded C2S action payloads and S2C form-library/feedback payloads with server-side target, slot, owner, revision, and session-state validation.
- [ ] Preserve server-authoritative open-slot, select-slot, commit-record, begin-transform, and revert operations.
- [ ] Preserve separate record-selection and transform-selection menu modes.
- [ ] Preserve client slot UI, keybind entry points, selected-slot feedback, cancel/revert controls, and invalid-selection feedback.
- [ ] Replace the old multiplexed envelope with dedicated, versioned C2S and S2C payload types.
- [ ] Bind all C2S actions to `ctx.player()`, active session ownership, expected state, slot bounds, and server-side permission checks.
- [ ] Send disguise state only to entity trackers and self; do not broadcast every update to every online player.
- [ ] Define aggregate packet budgets for NBT, skin, cape, profile, appraisal, and lists; use bounded codecs and graceful visual fallback instead of disconnect-worthy oversized packets.
- [ ] Preserve synchronization of entity type, disguise NBT, player/mob distinction, appraisal data, skin/cape bytes, model variant, and camera height.
- [ ] Support a compatibility fallback for clients without the core or an optional bridge, without leaking unsafe or malformed payloads.

### Client rendering, animation, camera, and HUD parity

- [x] Add tracker-targeted active-disguise and clear payloads, late-join/start-tracking synchronization, and a bounded client disguise cache that clears on entity leave and logout.
- [x] Add public presentation, camera-height, name-tag, appraisal, and Gecko-backed renderer-selection hooks over synchronized disguise state.
- [x] Provide a client-only cached fake-entity factory for renderer integrations while retaining the real player as the server authority.
- [ ] Preserve fake player rendering, including skin model parts, model variant, skin, cape, profile data, pose, equipment, and held-item presentation.
- [x] Preserve fake entity rendering for supported disguises and cancel the original player renderer when a surface disguise is active.
- [ ] Preserve copied entity position, yaw, pitch, body/head rotation, crouch/swim/sprint state, walk animation, limb motion, hurt/death state, and living-entity extras.
- [ ] Preserve disguised name-tag suppression/replacement behavior.
- [ ] Preserve first-person/third-person camera-height adjustments and safe restoration when the disguise clears.
- [ ] Preserve appraisal/HUD rendering data, scale, opacity, positioning, and fallback when an appraisal adapter is absent.
- [ ] Preserve generic living-entity, player, Tensura Lizard, and Gecko-backed animation adapter behavior as separate adapter implementations.
- [ ] Replace hard-coded namespace/entity adapter selection with a public registry, deterministic priority, support-level reporting, and mod-specific extension points.
- [ ] Keep GeckoLib optional and client-only; support serializable public controller data where available and stable visual fallback where it is not.
- [ ] Release dynamically registered skin/cape textures from `TextureManager` when the disguise cache is cleared or replaced.
- [ ] Bound client disguise caches and clear them on logout, world change, entity leave, resource reload where needed, and failed rendering.

### Commands, diagnostics, and operational controls

- [x] Preserve administrator commands to clear one form slot, clear all form slots, and force a safe reversion.
- [ ] Add session inspection, recovery/retry, quarantine, and cleanup-status commands gated by explicit permissions.
- [ ] Emit structured logs/events for capture rejection, compatibility downgrade, packet refusal, bridge failure, cleanup retry, cleanup completion, and unrecoverable residue.
- [ ] Provide player-facing messages that explain rejected targets, unavailable forms, insufficient MP, unsafe Perfect Form state, and degraded visual compatibility without exposing sensitive profile data.
- [ ] Document every retained Imitator behavior, every intentional behavior change, and every unsupported or fallback path before the Troverhaul implementation is removed.

### Parity and migration test matrix

- [ ] Add regression coverage for every Record, Transform, Replica, Surface Imitation, and Perfect Form path.
- [ ] Test ordinary and mastered Imitator behavior, each slot state, precision threshold, refinement flow, and seen-form path.
- [ ] Test player targets online and offline, mobs, unsupported entities, malformed NBT, oversized profile data, missing textures, and unavailable optional mods.
- [ ] Test client rendering, camera, appraisal, name tags, tab list, packet order, late join, start tracking, stop tracking, entity leave, and client logout.
- [ ] Test temporary items and blocks through inventories, armor, offhand, drops, players, containers, block entities, item handlers, crafting, smelting, trading, and chunk boundaries.
- [ ] Test reversion during death, clone, logout, disconnect, dimension transfer, server restart, server shutdown, crash recovery, skill removal, and failed apply/revert steps.
- [ ] Test mixed clients, vanilla fallback, optional GeckoLib absent/present, ManasCore/Tensura version compatibility, and multiplayer transfer/duplication attempts.
- [ ] Maintain an explicit migration mapping from this inventory to the rewritten Troverhaul skill and require passing replacement smoke coverage before release.

## Release and compatibility quality

- [x] Keep public API packages independent of internal implementation packages through injected service, chat transport, and synchronization contracts.
- [ ] Publish API documentation and integration examples.
- [ ] Document binary/network compatibility guarantees.
- [ ] Maintain a tested-mod compatibility matrix and adapter support levels.
- [ ] Add crash-safe logging for failed captures, rendering adapters, and cleanup.
- [ ] Build dedicated-server and client smoke-test profiles.
- [ ] Test mixed versions, missing optional integrations, and vanilla-client fallbacks.
- [ ] Establish a release checklist for core, compatibility modules, and TensuraOverhaul.
