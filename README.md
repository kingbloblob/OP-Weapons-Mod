# One Piece Arsenal

A companion mod for the One Piece Mod. Build your own swords, guns and armor from parts, and the materials you choose decide how strong they are, in the style of Tinkers' Construct. Everything is pirate or Navy themed: players are pirates, and Navy gear has to be taken from the Marines.

Requires the One Piece Mod (Fabric, Minecraft 26.3, Java 25).

## What's in so far

- **Katana** built from three parts: a **Katana Blade**, a **Tsuba** and a **Hilt**.
- **Four materials:** Driftwood, Ship Iron, Marine Standard Steel and Kairoseki (the One Piece Mod's seastone ingot).
- **Traits:** Seastone (drains Devil Fruit users on hit), Draining (fruit users who hold it feel weak), Rusty (can poison in the rain), Regulation (+1 damage against pirate NPCs), Light and Riveted. Buoyant and Issued are planned.
- **Shipwright's Anvil:** right-click it with an empty hand while carrying a blade, tsuba and hilt to forge the katana.
- Arsenal katanas are in the `minecraft:swords` tag, so the One Piece Mod's whetstones (sword grades), Dials and Haki work on them.

Parts are made at a crafting table: 3 of a material in a column for a blade, 4 in a ring for a tsuba, and 1 over leather for a hilt. Item art is placeholder vanilla textures for now.

## Building

The One Piece Mod isn't on a maven, so build against its jar:

1. In the One Piece Mod, run `./gradlew build`.
2. Copy `build/libs/onepiece-0.1.0.jar` into this repo's `libs/` folder (change `onepiece_jar` in `gradle.properties` if the name differs).
3. Run `./gradlew build` here. The jar lands in `build/libs/`; put it in your `mods/` folder next to the One Piece Mod.

## Design

The full plan (materials, meito grades, Haki and Devil Fruit interactions, the Marine disguise, balance) is in the One Piece Arsenal design doc.
