# Compatibility

## Tested build targets

| Component | Supported range | Build target |
|---|---:|---:|
| Minecraft | 1.21.1 | 1.21.1 |
| Java | 21+ | 21 |
| NeoForge | 21.1.150 to below 21.2 | 21.1.150 |
| GeckoLib | optional 4.8+ | 4.8.4 startup passed |
| ManasCore | optional 4.x | 4.0.0.2 startup passed |
| Tensura | optional 2.x | 2.0.1.0 startup passed |

## Compatibility levels

- `FULL`: capture, gameplay application, and presentation adapters accepted the form.
- `VISUAL`: presentation is supported but gameplay state is intentionally not copied.
- `FALLBACK`: the original renderer or a reduced presentation is retained safely.
- `UNSUPPORTED`: recording or transformation is rejected with a diagnostic.

Optional integrations are isolated from core class loading. A missing optional mod disables only its bridge. Compatibility mixins are allowed to degrade; Minecraft-owned core mixins are required so a broken core hook fails visibly during startup.

Network payloads are optional and versioned. Clients without the matching Core protocol receive vanilla-compatible system chat and do not receive Core-only screens or disguise presentation.

## Discord

Outbound relay requires an HTTPS Discord webhook. Inbound relay requires a bot token and channel ID because webhooks cannot receive channel messages. Credentials may instead be supplied with `IMITATIONCOREAPI_DISCORD_WEBHOOK_URL`, `IMITATIONCOREAPI_DISCORD_BOT_TOKEN`, and `IMITATIONCOREAPI_DISCORD_CHANNEL_ID`; environment values override the properties file. Operators can apply changes with `/imitchat discord reload` without restarting the server. Polling includes attachment and reply context and observes Discord retry responses, but a live bot and webhook test is still required.
