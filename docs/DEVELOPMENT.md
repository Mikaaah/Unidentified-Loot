# Contributor reference

Use Java 21 and the pinned NeoForge 21.1.262 build. The [root README](../README.md) contains the supported companion and build commands; [the release audit](TNK-RELEASE-1-AUDIT.md) records the tested dependency batches and remaining pack blockers.

`tools/prepare_build_dependencies.py` fetches checksum-verified Beta 1.0 compile/runtime inputs. `tools/prepare_ponder.py` fetches Create 6.0.10 and extracts its bundled Ponder 1.0.82 API. None of these dependency classes are bundled. CI downloads the real TNK Progression 1.2.0-private.1 companion rather than building the obsolete historical bootstrap project.

The build produces `build/libs/tnkloot-1.0.0-tnk-neoforge-1.21.1.jar` and a separate sources JAR. Use `verification/verify_release.py` to check all 88 original Beta artwork PNGs, the model dependency graph, mixins, optional affix data, licences and dependency exclusion.

Runtime smoke checks are opt-in and require disposable worlds. Inspect their positive `TNKLOOT_*_OK` markers: a successful Gradle task alone does not prove that Minecraft or an integration test succeeded. The real-render Ponder harness is enabled only with `-PponderVisualCheck` and is excluded from the release JAR.
