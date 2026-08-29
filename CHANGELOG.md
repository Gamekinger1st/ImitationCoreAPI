# Changelog

## 0.5.0-beta

- Reworked transformation application and reversion as durable transactions.
- Restored exact owner vitals, attributes, skills, and temporary state after copied forms.
- Added EP-aware copied-skill and copied-ability policy with mastered Unique support.
- Added form-stat progression for owner and recorded form data.
- Added safe death reversion, duration expiry, skill-removal reversion, and replica recovery.
- Hardened snapshot, payload, form-library, persistence, and request limits.
- Added copied player profiles, full visual item data, appraisal state, physical dimensions, and improved animation synchronization.
- Separated ordinary physical form stats from explicit Perfect Form state to prevent zeroed attributes or energy when Perfect Form is disabled.
- Synchronized downward magicule and aura transitions with their copied maximums to prevent Magicule Poison, Insanity, and form-transition deaths.
- Converted NPC movement and jump attributes to Minecraft's player scale, isolated copied physical stats from owner modifiers, and preserved normal sprint behavior.
- Added per-definition Auto-Jump transformation modifiers that inherit, force-enable, or force-disable effective client Auto-Jump without changing the player's saved option.
- Added a server-authoritative copied-form ability keybind, defaulting to `R`.
- Added public race-function editing and optional ManasCore/Tensura bridges.
- Added configurable chat routing, rate limits, privacy-safe logging, draft preservation, and optional mixed-client networking.
- Added negotiated Global/Local replacement tabs, unread state, and Core chat submission while preserving vanilla commands.
- Expanded Discord parsing, replies, attachments, and rate-limit handling.
- Split required core mixins from degradable optional compatibility mixins.
- Added generated refmap packaging, optional dependency metadata, compatibility documentation, and release checks.
- Verified standalone and GeckoLib/ManasCore/Tensura-present dedicated-server startup.
