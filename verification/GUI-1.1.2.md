# Unidentified Loot TNK 1.1.2 — GUI restoration

The unrequested 1.1.1 redesign is reverted. The preserved 1.1.0 source, original palette, frame shapes, title plates, controls, balance panel and complete native framed inventory/hotbar are restored.

The limited correction uses brown 18×18 work wells from the supplied CleanLook chest texture, aligned to the existing 16×16 item origins, and reduces the generated white perimeter from three pixels to one without changing its shape. The supplied palette and button artwork are not recoloured. The inventory/hotbar are copied as complete native artwork, not rebuilt as individual cells.

Validation: Java 21 Gradle build and all 19 existing tests pass. Eight Minecraft menus were rendered at GUI scales 2 and 3 with Progression 1.1.0 and Create 6.0.10. All 136 equipment wells match the native brown source crop exactly. Each of the four inventory-bearing menus preserves 16,589 opaque source pixels in its complete inventory frame. All three native button textures are byte-for-byte identical to the supplied pack.

Compared with the original 1.1.0 JAR: all 185 class paths remain; 183 classes are byte-for-byte identical. MarketTheme.class describes the 18-pixel native slot texture. IdentifierRenderer.java is unchanged; its recompiled class differs only in compiler-generated bridge methods. All gameplay classes and all 529 protected loot data, equipment art, model, font and Ponder resources remain byte-for-byte identical. The release excludes the client verification harness.

Target: Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21, client and server. FML version: 1.1.2. Required companion: TNK Progression 1.1.0+. Earlier complete-pack audit limitations remain. This GUI correction does not recertify the complete pack or repeat Ponder playback.
