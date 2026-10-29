# One Piece Arsenal

A companion mod for the One Piece Mod. Build your own swords, guns and armor from parts, and the materials you choose decide how strong they are, in the style of Tinkers' Construct. Everything is pirate or Navy themed: players are pirates, and Navy gear has to be taken from the Marines.

Requires the One Piece Mod (Fabric, Minecraft 26.3, Java 25).

## What's in so far

- **Katana** built from three parts: a **Katana Blade**, a **Tsuba** and a **Hilt**.
- **Six more hand weapons**, each from a head, a binding and a handle: the pirate **Cutlass** (fast; +2 damage on charged hits while falling, for boarding), the Navy **Marine Sabre** (put together at a Marine Armory Bench, like the Jitte; sneak to parry 1 in 3 hits from the front), the pirate **Boarding Axe** (heavy, breaks shields, chops wood), the Navy **Jitte** (light, charged hits drain Devil Fruit users whatever it's made of), the **Kanabo** (slow, huge knockback) and the **Spear** (1.5 blocks more reach). Cutlasses and sabres are swords, so whetstones and sword styles work on them.
- **Three guns**, each from a barrel, a firing part and a stock: the **Flintlock** pistol, the **Musket** (long barrel: harder, further, steadier, slower) and the **Hand Cannon** (cannon bore and fuse; fires the One Piece Mod's Cannonballs). They're One Piece Mod guns, so they take that mod's ammo, Scopes, Long Barrels, Hair Triggers, Dials, Quick Draw and Sniper's Eye.
- **Fifteen materials** in five tiers, from Driftwood and Ship Iron up to Wapometal, Adam Wood and Vegapunk Compound. Their numbers and traits are in `src/main/resources/arsenal/materials.json`; edit that file to rebalance or add one. Sea King Hide and Island Whale Leather only make armor. Black Blade Steel can't be crafted (it's for awakened meito).
- **Where the new materials come from:** Cannon Bronze is copper melted with a cannonball (or iron), Treasure Gold is gold, Wano Tamahagane, Wapometal, Kairoseki and Adam Wood are the One Piece Mod's ingots and logs, Sea King Hide is Sea King Scale. Sea Kings drop Sea King Bone, Island Whales drop Island Whale Leather, Pacifistas drop Pacifista Alloy, and Seraphim (and sometimes Vice Admirals) drop Vegapunk Compound.
- **Traits:** Seastone, Draining, Rusty, Regulation, Light and Riveted, plus Recoil (knockback), Heavy (+toughness), Serrated (crits bleed), Predator (+2 vs sea creatures), Gilded (Berries from kills), Greed (more bounty from Marines), Keen (15% ignores armor), Laser-etched (scorch), Plated (deflects shots), Waterproof (swim faster), Endurance (slower hunger), Edible-forged (self-repair), Unbreakable Grain (double durability), Legendary Hull (knockback resistance), Frontier Science (burn or freeze) and Ultra-alloy (fireproof). Buoyant, Issued, Folded, Reforged, Inherit and Black Blade are planned.
- **Armor:** a **Hat**, **Coat**, **Sash** and **Boots**, each built from a shell part (Hat Crown, Coat Plates, Sash Weave or Boot Soles, which sets the defense) and a **Lining** (any material, which adds its trait). Kairoseki plates drain Devil Fruit users who hit you.
- **Marine disguise:** wear a full set of Arsenal armor whose shells are all Navy materials and Marines stop hunting you. Marine Standard Steel is a recruit's uniform (hides a bounty up to 10M; Captains and CP agents see through it), Kairoseki an officer's (up to 100M; Vice Admirals, CP0, Pacifistas and Seraphim see through it). Hit a Marine and your cover is blown for a minute, even if you log out. Hunters sent after you give up after a minute. Named Marines like Garp are never fooled.
- **Suspicion:** Marines who stand close and watch you get suspicious, faster in groups, when they're senior, when you carry a pirate-made blade or gun, or when your bounty is near the uniform's limit. Stay too long and one calls you out: your cover is blown and they attack. Walk away and they forget.
- **Pirates see a Marine:** enemy pirates who spot you in a full Navy uniform drop whatever they're fighting and come for you, blown cover or not. Emperor crews still follow their Emperor's mood.
- **Navy-issue weapons:** some Marines carry Arsenal katanas (Kairoseki blades for senior officers) or flintlocks instead of their usual sword or pistol, and drop them now and then.
- **Marine Standard Steel** can't be crafted: beaten Marines drop it (officers more often), and Marine base and bunker chests hold some.
- **Shipwright's Anvil:** right-click it with an empty hand while carrying a full set of parts to forge the weapon.
- **Meito (graded blades):** katanas gain blade levels from kills (+0.1 damage a level), and their grade caps the level: 5 ungraded, 10 Skillful, 15 Great, 20 Supreme. A One Piece Mod whetstone only raises an Arsenal katana's grade once it is at that cap, has a strong enough blade (tier 2+ for Skillful, tier 3+ after) and has done its deed: 25 armed enemies for Skillful, a named boss for Great, an Emperor, Admiral or other tier 5 boss for Supreme. The katana's tooltip shows its progress.
- **Worthy wielders:** any graded sword, the main mod's famous blades like Wado Ichimonji and Yoru included, refuses a player without Haki level 1 / 3 / 5 (any type) or a 30M / 300M / 1B bounty for Skillful / Great / Supreme. Holding one anyway gives Weakness, the blade gains no levels, and a whetstone won't raise a blade past what you could wield.
- Arsenal katanas are in the `minecraft:swords` tag, so the One Piece Mod's whetstones (sword grades), Dials and Haki work on them. Traits work on flintlocks too: a Kairoseki barrel drains Devil Fruit users it shoots.

Pirate and shared materials' parts are made at a crafting table from 1 to 8 of a material (Navy materials' parts come from the captured Navy stations instead), each part in its own shape (the recipe book shows them all). Armor shells have a leather pad in the middle so they don't clash with vanilla armor. Model and recipe JSON is generated by `tools/gen_resources.py` (run it after changing `materials.json`).

## Art

Every katana, flintlock, part and armor piece of the first four materials looks like what it's made of, Tinkers' Construct style: a Kairoseki blade on a Ship Iron hilt shows both. Armor looks like clothes when worn, cut by its shell material: Driftwood is a castaway's straw hat, red vest, shorts and sandals; Ship Iron a pirate captain's tricorn, crimson coat and buckled boots; Marine Standard Steel a Marine recruit's cap and sailor uniform; Kairoseki a Navy officer's peaked cap and "Justice" coat over a dark suit. So a full Navy set looks like the disguise it is.

The textures are drawn by `tools/gen_textures.py` (needs Python 3 and Pillow): each shape is drawn once and coloured per material. To give a new material its look, add its colours to `MATERIALS` and an outfit to `OUTFITS` there, and run both (`tools/gen_resources.py` reads the materials from `materials.json`; it gives a material art once it's listed in `TEXTURED` there):

```
python3 tools/gen_textures.py src/main/resources
python3 tools/gen_resources.py src/main/resources
```

The newer materials, parts and weapons (cutlass to hand cannon) still use placeholder art: new materials look like Ship Iron until they are drawn.

## Building

The One Piece Mod isn't on a maven, so build against its jar:

1. In the One Piece Mod, run `./gradlew build`.
2. Copy `build/libs/onepiece-0.1.0.jar` into this repo's `libs/` folder (change `onepiece_jar` in `gradle.properties` if the name differs).
3. Run `./gradlew build` here. The jar lands in `build/libs/`; put it in your `mods/` folder next to the One Piece Mod.
4. `./gradlew runGameTest` runs the in-world tests in `src/gametest` (the Marine disguise, suspicion gear and Navy-issue weapons). They live in their own test mod and never ship in the jar.

## Design

The full plan (materials, meito grades, Haki and Devil Fruit interactions, the Marine disguise, balance) is in the One Piece Arsenal design doc.
