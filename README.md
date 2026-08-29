# Imitation Core API

Imitation Core API is reusable Minecraft 1.21.1 NeoForge infrastructure for mods that copy entity identity, appearance, physical traits, skills, abilities, animations, appraisal data, and chat personas.

Version 0.5.0 is a beta. The API, persistence format, and network protocol are versioned; standalone and dependency-present dedicated-server startup pass, while live client and multiplayer behavior still require beta testing.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.150 or newer in the 21.1 line
- Java 21

The core mod loads without GeckoLib, ManasCore, or Tensura.

## Included systems

- Server-authoritative identity snapshots with size limits, sanitization, schema migration, and malformed-entry recovery
- Persistent form slots, record staging, selection, transformation sessions, automatic duration expiry, recovery, and exact baseline reversion
- Separate Surface, gameplay Transform, and opt-in Perfect Form operations with copied player skins, mob rendering, camera height, physical stats and dimensions, equipment presentation, and appraisal masking
- Live animation mapping for movement, sprinting, crouching, swimming, attacks, hurt, death, and mod-defined triggers
- Owner-skill suppression, policy-based copied skills, copied form abilities with a configurable `R` keybind, cooldowns, and owner/form progression
- Mob-faction targeting hooks with retaliation preservation and public faction resolvers
- Temporary replicas with safe spawning, ownership, expiry, drop/experience suppression, unloaded-entity recovery, and client-only copied equipment visuals
- Public helpers for skill classification, skill state, race stats, race text, and race lifecycle/function replacement
- Global, local, direct, persona, system, and addon-defined chat channels with replacement tabs, moderation, audit, rate-limit, and vanilla-client fallback hooks
- Optional two-way Minecraft/Discord relay

## Discord chat relay

Start a server once to generate `config/imitationcoreapi-discord.properties`.

```properties
webhook_url=
bot_token=
channel_id=
poll_interval_seconds=3
relay_local_messages=false
relay_direct_messages=false
relay_system_messages=true
```

- `webhook_url` sends Minecraft chat to Discord.
- `bot_token` and `channel_id` are both required to receive Discord messages in Minecraft.
- A webhook cannot receive Discord messages by itself.
- The bot needs permission to view the channel and read message history.
- Keep the configuration private. The file is ignored by Git.
- `IMITATIONCOREAPI_DISCORD_WEBHOOK_URL`, `IMITATIONCOREAPI_DISCORD_BOT_TOKEN`, and `IMITATIONCOREAPI_DISCORD_CHANNEL_ID` override credentials from the file.
- Operators can run `/imitchat discord` to inspect the bridge or `/imitchat discord reload` after changing its configuration.

## Addon authoring

Create an Imitator-style definition without copying Core internals:

```java
ImitatorSkillDefinition definition = ImitatorSkillDefinition.builder(
        ResourceLocation.fromNamespaceAndPath("example", "mimic"),
        "Mimic",
        "Records and assumes another identity"
).transformDurationMinutes(5)
 .forceAutoJump(true)
 .skillCopyPolicy(ImitatorSkillCopyPolicy.builder()
         .maximumCopiedSkills(32)
         .allowUniqueSkills(true)
         .build())
 .build();
```

Register custom form actions, faction families, snapshot adapters, Gecko animation adapters, race edits, and chat channels through `ImitationApi`. Public API classes live under `com.github.gamekinger1st.imitationcoreapi.api`; packages under `internal` are not supported addon dependencies.

More examples are in [docs/API_EXAMPLES.md](docs/API_EXAMPLES.md).

## Validation

```powershell
.\gradlew.bat clean check build
```

Automated tests validate pure state, policy, serialization, limits, recovery, chat, Discord parsing, and API contracts. They do not replace in-world client rendering, AI, mixin, optional-mod, or multiplayer tests. See [docs/IMITATOR_TEST_MATRIX.md](docs/IMITATOR_TEST_MATRIX.md) and [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md).

## Optional GeckoLib compatibility

When GeckoLib 4.8 or newer is loaded, Core discovers Gecko-backed entity controllers and exposes public adapters for animation lookup and live disguise playback. Unsupported controllers fall back to the normal entity renderer without making GeckoLib a required dependency.

## Optional ManasCore and Tensura compatibility

When ManasCore 4.x and Tensura 2.x are loaded, Core adds reflective bridges for skill storage, temporary copied skills, exact skill restoration, race editing, EP/MP/aura/spiritual-health state, appraisal masking, Perfect Form policies, and Tensura entity targeting. These integrations remain absent safely when the mods are not installed.
