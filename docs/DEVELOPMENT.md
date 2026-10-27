# Contributor reference

## Development

Use Java 21:

```sh
./gradlew build
```

Windows: `gradlew.bat build`. Output: `build/libs/tnkloot-0.6.1-alpha.jar`. The `-sources.jar` is for development, not installation. Tests cover config validation/atomic reloads, category pools, identification prices, weighted selection and progression persistence. GitHub Actions runs the build, unit tests and a dedicated-server integration test that generates every configured tier/category, verifies seven-item payment and safe close, and checks the NPC lifecycle.

See [CONTRIBUTING.md](CONTRIBUTING.md) and [CHANGELOG.md](CHANGELOG.md). All rights reserved, matching the original project metadata.

See [Apotheosis presets](https://github.com/Mikaaah/Unidentified-Loot/wiki/Apotheosis-Presets) for the affix ladder and datapack customization. Existing `tiers.json` files are preserved; new worlds receive the improved material pools. Apply the [example](examples/tiers.example.json) manually if you want those pool changes in an existing pack.
