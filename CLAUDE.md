# Personality Construct Chip (NeoForge 1.21.1, JDK 21)

Design decisions and the build order live in `docs/DESIGN.md`. Read it before you change anything, and treat it as decided unless the user says otherwise.

- Mod id `constructchip`, base package `io.github.constructchip`.
- `chip/`: data component records (immutable; replace them, never mutate) and `ChipStacks` helpers. `registry/`: DeferredRegisters. `command/`: `/construct`.
- All Vista code goes in one compat package behind `ModList.get().isLoaded("vista")`.
- Never commit or ship third-party jars (`libs/`, `reference/` are gitignored).
- Build: `./gradlew build`. Dev client: `./gradlew runClient`. This needs `maven.neoforged.net` to be reachable.
