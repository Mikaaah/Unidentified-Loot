# Unidentified Loot — TNK Edition

Release 1 candidate: **1.0.0-tnk-neoforge-1.21.1**, Minecraft **1.21.1**, NeoForge **21.1.262**, Java **21**.

This edition uses the real TNK Progression API for source levels, milestone ceilings, rarity caps and mob levels. It retains the Beta 1.0 / 0.19.15 Identifier, seven-slot identification and salvage, previews, collection, custom artwork, boss editor, dimension rewards and optional Create Mechanical Identifier/Ponder integration.

The equipment and boss integration checks pass in focused dependency batches. **The complete Beta 1.0 pack is not certified:** Knaves' Needs optional-integration startup and a separate dimension registry-freeze failure block that test. See [the release audit](docs/TNK-RELEASE-1-AUDIT.md) for the evidence and exact limits.

| Component | Supported / checked |
| --- | --- |
| Minecraft / loader | 1.21.1 / NeoForge 21.1.262 |
| Java | 21 |
| TNK Progression | Required; 1.2.0-private.1 and 1.3.0-private.1 checked |
| Core integrations | Apotheosis 8.9.0, Apothic Attributes 2.11.0, Placebo 9.9.4, Patchouli 1.21.1-93-NEOFORGE |
| Create | Optional; 6.0.10 with bundled Ponder 1.0.82 checked |
| Liquid XP automation | Optional; checked against the Beta 1.0 Create Enchantment Industry runtime |
| Other loaders / Minecraft versions | Not supported by this build |

Replace the old `tnkloot` JAR on **both client and server**; install only one copy. Keep the Progression companion as a separate mod. Existing sealed drops retain their rarity and tier. The release migrates the shipped Beta preset once and writes configuration backups. Custom additions are retained by the release migration; existing compatibility import rules still apply. The optional full-pack configuration supplied with the audit is for the stock Beta pack, not a wholesale replacement for a customized server configuration.

The pack item catalogue's fully disabled equipment and infinite-food removals also apply to automatic imports, reward pools and previews. `/tnkloot reload` refreshes this policy.

Use `/tnkloot audit` as an operator to export the actual runtime item groups, source-level ranges, item modifiers, registered attributes, affix definitions and boss mappings to `config/tnkloot/reports`.

Build with Java 21:

```sh
python3 tools/prepare_progression.py
./gradlew build -PtnkProgressionJar=/absolute/path/to/build-deps/tnkprogression-1.2.0-private.1.jar
python3 verification/verify_release.py
```

Pinned, checksum-verified build inputs download automatically. APIs and companion mods are not embedded in the release JAR. `-sources.jar` is for development, not installation. The historical `progression-bootstrap` project is not the release companion.

For disposable-world server checks, use `-PtnkReleaseSmoke`, `-PtnkBetaSmoke`, `-PtnkAdvancementSmoke` or `-PtnkMachineSmoke` with the actual companion/runtime mods. These checks stop the server. The machine check additionally needs the actual Create and Liquid XP provider JARs in `-PextraRuntimeDirectory=/absolute/path/to/jars`.
