# Release checklist

- Run `clean check build` from a clean checkout.
- Confirm the JAR contains both mixin configurations, the generated refmap, metadata, translations, and no runtime logs or Discord credentials.
- Start a dedicated server with no optional mods and reach `Done`.
- Start with the supported GeckoLib version and verify one vanilla and two unrelated Gecko entities.
- Start with the supported ManasCore and Tensura versions and verify apply, death-revert, manual revert, restart recovery, and skill restoration.
- Test an unmodded client, a matching Core client, and a mismatched Core client against the same server.
- Test two players observing record, transform, faction behavior, equipment visuals, chat fallback, and late tracking.
- Test malformed/oversized snapshot and packet rejection without disconnecting unrelated clients.
- Test Discord outbound and inbound with real credentials supplied outside version control.
- Record exact dependency versions and remaining runtime limitations in the release notes.
