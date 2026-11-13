# TNK3 equipment decisions — 1.1.3

These rules apply to generated TNK/Apotheosis equipment pools. Native crafting, recipes, item use and owned-item reforging remain available. Excluded native items retain their own components and are not converted into unrelated unidentified equipment. The rules add no mod dependencies.

| Equipment | TNK generated-loot policy |
| --- | --- |
| Vinery, Brewery, HerbalBrews clothing | Excluded; native crafting/use retained. |
| Copper equipment and copper spear from Vanilla Backport | Chainmail band: tiers 1–2. |
| Other Backport spears | Follow their material: wooden 0; stone 0–1; iron 2–3; diamond 3–5; netherite 5–7. Golden equipment stays excluded. |
| Deeper and Darker resonarium/warden armour | Above netherite: tiers 6–7; Otherside origin. Staff, soul elytra and other unapproved equipment stay native. |
| The Beyond brittle tools/sword | Tiers 4–6, End origin. |
| The Beyond anchor leggings/pathfinder boots | Tiers 5–7, End origin. |
| Enderscape drift leggings | Tiers 5–7, End origin; retain native components. |
| Enderscape rubble shields | Tiers 3–5, End origin. |
| The Beyond Ring Remembrance | Native ordinary item; excluded from wearable-equipment rewards. Its registry constructor uses a plain Item, not a wearable ring implementation. |
| Hybrid Aquatic equipment | Native crafting/use only; no generated TNK rewards. Removed the diving-equipment entries from the aquatic example floor. |
| Jaden's Nether Expansion and Wildernature equipment | Excluded from TNK generated pools; native handling retained. |
| IE steel/Faraday armour | Above iron: tiers 3–4. |
| IE ordinary steel tools/sword | Iron band: tiers 2–3. Powered, modular and ranged devices stay native. |
| AE2 certus/nether-quartz/fluix tools and swords | Iron band: tiers 2–3. Charged staff and other utility equipment stay native. |
| Supplementaries slingshot/quiver | Excluded; native use and stored contents retained. |
| Archaion Echo Mace | Exclusive Last of Deepslate boss reward, tier 6. Keep the native Echo Charges and smithing-template drops. Echo's Grace stays native/crafted. |

The pack-pinned Vanilla Backport registers this equipment in the **minecraft** namespace. The rules use the actual registry IDs, such as `minecraft:copper_helmet` and `minecraft:iron_spear`.

End equipment is restricted to End equipment pools; Deeper and Darker armour is restricted to Otherside equipment pools. This changes equipment origins, not access to dimensions. Global equipment remains enabled by default within those dimensions.

The migration backs up `tiers.json`, `dimensions.json` and changed boss definitions using `before-1.1.3.bak` names. It preserves weights and enabled flags while applying the approved tier changes once. Later manual tier edits remain intact. Dimension migration retains custom chances, prefixes, namespaces and global-equipment settings, adds the new End/Nether namespaces and Otherside route, and removes BetterEnd/BetterNether namespace references when those mods are absent.

The equipment-choice migration also saves `tiers.json.before-1.1.3-pack-rules.bak`, so a backup from the earlier 1.1.3 jewellery cleanup does not replace the current pre-migration configuration.

Server dependency note: the tested Enderscape 1.0.9 JAR initializes YACL classes on a dedicated server even though its metadata labels YACL as client-only. Keep YACL installed on the server too. The verification runtime uses [YACL 3.7.1 for NeoForge 1.21.1](https://modrinth.com/mod/yacl/version/zwRKksBU). This is an Enderscape setup requirement; Loot does not add a hard YACL dependency.

Already earned exact dungeon rewards and saved payout bundles retain their stored result. These eligibility rules control future generation; they do not delete inventory items or remove recipes.
