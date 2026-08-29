# Imitator test matrix

## Solo baseline

- Start a dedicated server and join with one modded client.
- Verify `/troimitator inspect <player>` shows no active session before testing.
- Record a passive mob in range.
- Commit the pending record to slot 0.
- Select slot 0.
- Confirm selecting the form immediately continues the pending Transform action.
- Transform with Perfect Form disabled again after reverting.
- Confirm visual disguise appears.
- Confirm recorded health, movement speed, jump strength, gravity, scale, dimensions, and energy limits apply without copying Perfect Form race or storage state.
- Compare walking and sprinting before and after copying a clearly faster and clearly slower mob; confirm mob locomotion is converted to player scale and sprint remains faster than walking.
- Try to record a Wither or another boss and confirm the recording is rejected.
- Try to record an Armor Stand, boat, projectile, or dropped item and confirm the recording is rejected.
- Turn client Auto-Jump off, transform with a forced-enabled definition, and confirm one-block obstacles trigger auto-jump; revert and confirm the original off setting applies again.
- Test a forced-disabled definition from an initially enabled setting and confirm auto-jump stays disabled only during the transformation; replicas should continue using their native mob navigation and jump control.
- Transform from high magicules or aura into a much weaker form and confirm current and maximum values descend together without Magicule Poison, Insanity, or death.
- Confirm first-person camera height matches the disguise closely enough to play.
- Confirm appraisal masking shows copied visual data below the configured bypass level.
- Revert through the skill path.
- Confirm the original player render, camera height, name, and active session state are restored.
- Run `/troimitator inspect <player>` and confirm no outstanding temporary state remains.

## Player form baseline

- Record an online player.
- Commit the form to a new slot.
- Have the source player disconnect.
- Transform into the recorded player form.
- Confirm the copied skin/profile presentation still renders.
- Confirm the copied player's ordinary name color and presentation match the source player without a purple custom-name label.
- Confirm the real authenticated account is still used for permissions and logs.
- Send normal chat and confirm the transformed player's authenticated chat name does not change.
- Take damage and confirm Perfect Form uses the copied player's effective maximum health and updates current health normally.
- Revert and confirm client cache clears.

## Perfect Form baseline

- Record a compatible Tensura target.
- Enable Perfect Form.
- Transform using the selected form.
- Confirm EP, magicule, aura, spiritual health, race, abilities, player state, spirit state, and attributes follow the skill definition's exact/limited/scaled policy.
- Confirm Unique and Ultimate skills enabled by the skill definition are usable during Perfect Form.
- Confirm denied Perfect Form attempts explain the rejection reason.
- Revert.
- Confirm the owner's pre-transform magicules and aura are restored exactly or cleanup is left in an inspectable recovery state.

## Mastered skill-copy baseline

- Test a stronger imitator and confirm every category enabled by the skill definition can copy.
- Test a weaker or equal imitator and confirm Ultimate and Intrinsic skills are denied while other enabled categories can copy.
- Test mastery-gated Unique or Ultimate copying with a skill definition that explicitly enables it.
- Confirm copied skills are tagged as temporary.
- Revert.
- Confirm temporary copied skills are removed.

## Mob faction targeting

- Record a mob.
- Transform into that mob with matching-mob targeting suppression enabled.
- Spawn matching mobs nearby.
- Confirm they do not target the disguised player.
- Disable Perfect Form and repeat to confirm faction behavior does not depend on Perfect Form.
- Disable targeting suppression in server config and confirm normal targeting returns.

## Client rendering and animation

- Test a vanilla living entity with armor and held items.
- Confirm equipment appears on the fake entity.
- Confirm position, yaw, pitch, head rotation, body rotation, crouch, sprint, swim, and movement animation follow the real player.
- Test a GeckoLib entity with GeckoLib installed.
- Confirm the Gecko fallback does not crash when controller state cannot be reproduced.
- Confirm entity leave, logout, and server switch clear the client disguise cache.
- Press the copied-form ability key, default `R`, and confirm the server activates only an ability permitted by the current form policy.
- Copy a Creeper, press `R`, and confirm it explodes without killing the owner through its own blast and then safely reverts.

## Analysis masking

- Analyze a transformed player below the configured bypass level and confirm Tensura's complete analysis panel background is present.
- Damage the transformed player and confirm the masked health value changes immediately while the copied identity stays in place.
- Analyze at or above the configured bypass level and confirm Tensura can reveal the real player normally.

## Chat replacement

- Open chat after protocol negotiation and confirm Global and Local tabs, active selection, unread counts, normal message submission, and command submission work.
- Type without submitting, close chat, and confirm the draft returns; submit a message or command and confirm the input is empty next time.

## Lifecycle recovery

- Transform, then die.
- Transform, then disconnect.
- Transform, then change dimension.
- Transform, then stop the server.
- Transform, then force revert with `/troimitator revert <player>`.
- For each case, inspect session state after recovery and confirm no temporary state is active unless it is explicitly quarantined.

## Multiplayer synchronization

- Use one server and at least two modded clients.
- Client A transforms while Client B is watching.
- Confirm Client B sees the disguise.
- Have Client C join late and enter tracking range.
- Confirm Client C receives the active disguise.
- Have Client B leave tracking range and return.
- Confirm stale client state is cleared and restored correctly.
- Revert Client A and confirm all tracking clients clear the disguise.

## Mixed-client fallback

- Join with one modded client and one vanilla or mismatched client.
- Confirm modded clients receive Core disguise/chat payloads.
- Confirm vanilla or mismatched clients receive safe fallback chat and are not sent unsafe disguise payload assumptions.

## Anti-duplication

- Transform and create replicas from forms that visually carry weapons or armor.
- Confirm no real copied item stacks are added to the player, replica, drops, inventories, containers, or nearby players.
- Confirm held items and armor remain visual-only on fake client presentation.
- Confirm cleanup removes only Core-owned non-item temporary state and does not delete legitimate items or blocks.
