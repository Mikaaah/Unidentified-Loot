# Third-party data adapters

The TNK rarity adapters in `data/tnkloot/affixes/compat` adapt affix JSON definitions and the corresponding English names from these exact Beta 1.0 dependencies:

- Apotheosis x Iron's Spellbooks Compat (mod ID `irons_apothic`), by MuonR — MIT + CC BY 4.0, as declared in the original JAR. Source project: https://www.curseforge.com/minecraft/mc-mods/apotheosis-x-irons-spellbooks-compat
- Apothic Compats (mod ID `apothic_compats`), by ianm1647 — MIT, as declared in the original JAR. Source project: https://www.curseforge.com/minecraft/mc-mods/apothic-compats. The source JAR and its licence are recorded by `tools/build_tnk_compat_affixes.py` from the pinned Beta 1.0 inputs.

Changes: namespaced copies restricted to TNK rarities, six-rarity value mappings, explicit optional-mod conditions, and remapped exclusivity links. Native third-party affixes and their original rarity distributions remain supplied by those mods. No third-party code or artwork is bundled by these adapters. See the individual dependency JARs for the full original copyright and licence notices; these are copied into `third-party-licenses` when generating the release adapters.
