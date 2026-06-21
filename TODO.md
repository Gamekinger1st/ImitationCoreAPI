# Imitation Core API roadmap

This checklist separates reusable API work from the TensuraOverhaul implementation of Imitator. A checked item must include automated coverage where practical and a multiplayer validation note where it changes synchronized state.

## Core architecture

- [ ] Define a stable public API package and semantic-versioning policy.
- [ ] Define immutable, versioned `IdentitySnapshot` data contracts.
- [ ] Define `TransformationSession` contracts for the full transformation lifecycle.
- [ ] Define a baseline-state contract that captures every value changed by a transformation.
- [ ] Implement persistent session and snapshot storage with schema migrations.
- [ ] Implement a server-authoritative transformation state machine: capture, validate, channel, apply, active, revert, and failed cleanup.
- [ ] Add lifecycle recovery for death, clone, logout, disconnect, dimension transfer, skill removal, world unload, and server shutdown.
- [ ] Add explicit cleanup ownership/session identifiers for all temporary state.
- [ ] Define an adapter registry for snapshot capture, rendering, animation, gameplay state, and compatibility providers.
- [ ] Define compatibility levels: Full, Visual, Fallback, and Unsupported.
- [ ] Provide diagnostics explaining why a target was accepted, degraded, or rejected.
- [ ] Provide public events for snapshot capture, transformation validation, application, reversion, and cleanup.
- [ ] Provide configuration for server policy and operational safety only.

## Snapshot capture and identity data

- [ ] Capture and sanitize entity type and entity NBT.
- [ ] Capture display name, custom name, and relevant visual metadata.
- [ ] Capture attributes, health, equipment, and other safe visual state.
- [ ] Capture player profile properties, skin/cape information, and a safe offline fallback.
- [ ] Capture camera/eye-height and supported hitbox presentation data.
- [ ] Capture animation-relevant pose and controller state through adapters.
- [ ] Establish snapshot size limits, NBT allowlists, and network serialization limits.
- [ ] Add snapshot migration tests and malformed-data rejection tests.

## Networking and client presentation

- [ ] Register dedicated versioned C2S and S2C payloads.
- [ ] Validate every client request server-side against session ownership and current state.
- [ ] Sync active disguise state to trackers, late joiners, and newly tracked entities.
- [ ] Clear stale disguise state when entities leave the client level or a client disconnects.
- [ ] Implement client-side disguise state storage and render-cache invalidation.
- [ ] Render fake client entities in place of real players while preserving the real server-side player.
- [ ] Synchronize position, rotation, pose, equipment, swinging, damage, swimming, crouching, sprinting, and movement animation.
- [ ] Implement a safe default renderer for incomplete adapters.
- [ ] Add per-entity adapter rendering for unusual models and render paths.
- [ ] Implement first-person camera-height support.
- [ ] Implement configurable name-tag and tab-list presentation with safe fallbacks.
- [ ] Add appraisal/HUD masking and alternate appraisal rendering hooks.

## Persona chat replacement

- [ ] Design a server-authoritative persona-chat payload and message format.
- [ ] Validate that the sender has an active, authorized persona before broadcasting.
- [ ] Preserve the real sender UUID in server logs and moderation/audit data.
- [ ] Render an imitated sender identity for clients that have Imitation Core API installed.
- [ ] Send an explicit unsigned/system-compatible fallback to vanilla clients.
- [ ] Never forge or replace Mojang signed-player messages.
- [ ] Support player personas, entity personas, and future NPC/mod-provided personas.
- [ ] Support persona display name, profile/skin styling, optional chat formatting, and localization.
- [ ] Define chat permission, mute, moderation, rate-limit, and anti-spam integration points.
- [ ] Test mixed-mod clients, vanilla-client fallback, moderation logging, and reconnect behavior.

## GeckoLib compatibility module

- [ ] Create an optional client-only GeckoLib compatibility module with no hard core dependency.
- [ ] Detect loaded GeckoLib-backed entity types.
- [ ] Capture public, serializable GeckoLib visual state through generic adapters.
- [ ] Create fake render entities and bind GeckoLib models/controllers for active disguises.
- [ ] Map movement, idle, walk, sprint, swim, crouch, attack, hurt, death, head rotation, and body rotation into animation state.
- [ ] Support held-item and equipment-driven controller inputs where exposed.
- [ ] Define a public registration API for mod-specific GeckoLib adapters.
- [ ] Add adapters for controller variables, custom NBT, multipart entities, and bespoke renderers.
- [ ] Fall back to stable model rendering when a controller cannot be safely reproduced.
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

- [ ] Create an optional ManasCore/Tensura bridge with no hard core dependency.
- [ ] Snapshot and restore Tensura skill storage safely.
- [ ] Track, grant, and remove temporary ManasCore/Tensura skills.
- [ ] Snapshot and restore skill modes, presets, and slot layouts.
- [ ] Snapshot and restore Tensura race, EP, MP, aura, spiritual health, and relevant attributes.
- [ ] Provide constrained reproduction-scale calculations.
- [ ] Provide standard MP charging and mastery helpers compatible with Tensura lifecycle rules.
- [ ] Add capability-safe hooks for login, death, clone, dimension transfer, and logout.
- [ ] Expose public extension points for other Tensura addons.
- [ ] Test against the supported Tensura and ManasCore versions.

## TensuraOverhaul Imitator integration

- [ ] Register Imitator as a Tensura unique skill in TensuraOverhaul.
- [ ] Add Imitator to Overhaul's reincarnation configuration.
- [ ] Implement record, transform, and replica skill modes.
- [ ] Implement target validation, form slots, selection UI, and persistent form library.
- [ ] Implement recording precision, precision refinement, perfect-form thresholds, and mastery progression.
- [ ] Implement Surface Imitation as a complete appearance-only transformation.
- [ ] Implement Mirror Sync for temporary stats, race, skills, inventory, equipment, and team state.
- [ ] Apply EP-versus-target and precision-based power limits.
- [ ] Implement safe player-profile/name presentation when the copied player is online or offline.
- [ ] Implement mob faction/targeting behavior for copied mob forms.
- [ ] Implement mastered recording skill-copy rules.
- [ ] Implement mastery-only training replicas and borrowed nearby-player form memory.
- [ ] Integrate appraisal masking, camera support, persona chat, and GeckoLib adapters.
- [ ] Add administrator commands for clearing form slots and forcing safe reversion.
- [ ] Create solo and multiplayer test matrices covering all transformation and cleanup states.

## Release and compatibility quality

- [ ] Publish API documentation and integration examples.
- [ ] Document binary/network compatibility guarantees.
- [ ] Maintain a tested-mod compatibility matrix and adapter support levels.
- [ ] Add crash-safe logging for failed captures, rendering adapters, and cleanup.
- [ ] Build dedicated-server and client smoke-test profiles.
- [ ] Test mixed versions, missing optional integrations, and vanilla-client fallbacks.
- [ ] Establish a release checklist for core, compatibility modules, and TensuraOverhaul.
