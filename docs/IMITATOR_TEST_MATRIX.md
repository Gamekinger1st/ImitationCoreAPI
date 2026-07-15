# Imitator test matrix

## Solo baseline

- Start a dedicated server and join with one modded client.
- Verify `/troimitator inspect <player>` shows no active session before testing.
- Record a passive mob in range.
- Commit the pending record to slot 0.
- Select slot 0.
- Transform with Perfect Form disabled.
- Confirm visual disguise appears.
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
- Confirm the real authenticated account is still used for permissions and logs.
- Send normal chat and persona chat.
- Revert and confirm client cache clears.

## Perfect Form baseline

- Record a compatible Tensura target.
- Enable Perfect Form.
- Transform using the selected form.
- Confirm EP, magicule, aura, spiritual health, race, abilities, player state, spirit state, and attributes are scaled by precision and owner-to-target EP policy.
- Confirm denied Perfect Form attempts explain the rejection reason.
- Revert.
- Confirm the owner baseline is restored exactly or cleanup is left in an inspectable recovery state.

## Mastered skill-copy baseline

- Test with non-mastered Imitator and confirm no skills are copied.
- Test mastered Imitator with default policy and confirm common/extra/intrinsic/resistance skills can copy while unique, ultimate, and unknown skills are denied unless the integration policy allows them.
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

- Transform with temporary copied skills/items/equipment when the policy allows it.
- Drop temporary items.
- Put temporary items in inventory, armor, offhand, containers, and nearby player inventories.
- Craft, smelt, trade, die, disconnect, change dimension, and restart with temporary items present.
- Confirm ownership tags include owner UUID, session UUID, and reference UUID.
- Confirm cleanup removes only Core-owned temporary state and does not delete legitimate replacement items or blocks.
