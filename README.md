# Imitation Core API

Reusable Minecraft 1.21.1 NeoForge infrastructure for identity imitation, client disguise rendering, persona chat, and compatibility adapters.

## Baseline

- Java 21
- NeoForge 21.1.150

The core has no gameplay-mod dependency. GeckoLib and Tensura/ManasCore support will be implemented as optional compatibility modules.

## Migration boundary

TensuraOverhaul remains the home of the Imitator skill. It will consume this API for transformation sessions, snapshots, client presentation, temporary-state cleanup, chat personas, and Tensura-specific compatibility.
