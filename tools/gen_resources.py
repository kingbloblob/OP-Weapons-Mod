# Writes the JSON files under src/main/resources (models, recipes, tags, lang).
# Run from the repo root: python3 tools/gen_resources.py src/main/resources
import json, os, sys

os.chdir(sys.argv[1])


def w(path, obj):
    os.makedirs(os.path.dirname(path) or '.', exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


w('fabric.mod.json', {
    "schemaVersion": 1, "id": "arsenal", "version": "${version}",
    "name": "One Piece Arsenal",
    "description": "Build your own weapons and armor from parts, Tinkers' Construct style, for the One Piece Mod.",
    "authors": ["ryan"], "environment": "*",
    "entrypoints": {"main": ["com.onepiecearsenal.ArsenalMod"], "client": ["com.onepiecearsenal.client.ArsenalClient"]},
    "mixins": ["arsenal.mixins.json"],
    "depends": {"fabricloader": ">=0.19.5", "minecraft": "~26.3", "java": ">=25", "fabric-api": "*", "onepiece": "*"}
})

A = 'assets/arsenal'
# The material list the mod reads (and the recipes below come from).
MATERIALS = json.load(open('arsenal/materials.json'))['materials']
# Weapon and gun items, with the One Piece-style names the code also uses (WeaponType, GunType).
WEAPONS = {"cutlass": "Cutlass", "marine_sabre": "Marine Sabre", "boarding_axe": "Boarding Axe",
           "jitte": "Jitte", "kanabo": "Kanabo", "spear": "Spear",
           "fishman_gloves": "Fishman Gloves", "impact_palm": "Impact Palm", "reject_palm": "Reject Palm"}
GUNS = {"flintlock": "Flintlock", "musket": "Musket", "hand_cannon": "Hand Cannon"}
lang = {
    "item.arsenal.driftwood": "Driftwood",
    "item.arsenal.marine_steel_ingot": "Marine Standard Steel Ingot",
    "item.arsenal.cannon_bronze_ingot": "Cannon Bronze Ingot",
    "item.arsenal.sea_king_bone": "Sea King Bone",
    "item.arsenal.island_whale_leather": "Island Whale Leather",
    "item.arsenal.pacifista_alloy": "Pacifista Alloy",
    "item.arsenal.vegapunk_compound": "Vegapunk Compound",
    "item.arsenal.shipwrights_manual": "Shipwright's Manual",
    "item.arsenal.katana_blade": "Katana Blade",
    "item.arsenal.tsuba": "Tsuba",
    "item.arsenal.hilt": "Hilt",
    "item.arsenal.katana": "Katana",
    "item.arsenal.barrel": "Barrel",
    "item.arsenal.firing_lock": "Firing Lock",
    "item.arsenal.stock": "Stock",
    "item.arsenal.long_barrel": "Long Barrel",
    "item.arsenal.cannon_bore": "Cannon Bore",
    "item.arsenal.fuse": "Fuse",
    "item.arsenal.gun_carriage": "Gun Carriage",
    "item.arsenal.cannon_shell": "Cannon Shell",
    "item.arsenal.musket_ball": "Musket Ball",
    "item.arsenal.flame_dial_round": "Flame Dial Round",
    "item.arsenal.impact_dial_round": "Impact Dial Round",
    "item.arsenal.breath_dial_round": "Breath Dial Round",
    "item.arsenal.flash_dial_round": "Flash Dial Round",
    "block.arsenal.ship_cannon": "Ship Cannon",
    "item.arsenal.cutlass_blade": "Cutlass Blade",
    "item.arsenal.sabre_blade": "Sabre Blade",
    "item.arsenal.axe_head": "Axe Head",
    "item.arsenal.jitte_rod": "Jitte Rod",
    "item.arsenal.club_head": "Kanabo Head",
    "item.arsenal.spearhead": "Spearhead",
    "item.arsenal.knuckles": "Knuckle Plates",
    "item.arsenal.impact_core": "Impact Dial Core",
    "item.arsenal.reject_core": "Reject Dial Core",
    "item.arsenal.hand_guard": "Hand Guard",
    "item.arsenal.binding": "Binding",
    "item.arsenal.studs": "Studs",
    "item.arsenal.grip": "Grip",
    "item.arsenal.haft": "Haft",
    "item.arsenal.crown": "Hat Crown",
    "item.arsenal.coat_plates": "Coat Plates",
    "item.arsenal.sash_weave": "Sash Weave",
    "item.arsenal.boot_soles": "Boot Soles",
    "item.arsenal.lining": "Lining",
    "item.arsenal.hat": "Hat",
    "item.arsenal.coat": "Coat",
    "item.arsenal.sash": "Sash",
    "item.arsenal.boots": "Boots",
    "item.arsenal.shield_face": "Shield Face",
    "item.arsenal.gauntlet_plates": "Gauntlet Plates",
    "item.arsenal.shield": "Shield",
    "item.arsenal.gauntlets": "Gauntlets",
    "block.arsenal.shipwrights_anvil": "Shipwright's Anvil",
    "block.arsenal.marine_armory_bench": "Marine Armory Bench",
    "block.arsenal.quartermasters_desk": "Quartermaster's Desk",
    "block.arsenal.marine_base_smeltery": "Marine Base Smeltery"
}
for name, title in {**WEAPONS, **GUNS}.items():
    lang[f"item.arsenal.{name}"] = title
# Options > Controls > Key Binds (client/ArsenalClient).
lang["key.category.arsenal.arsenal"] = "One Piece Arsenal"
lang["key.arsenal.weapon_skill"] = "Weapon Skill"
lang["key.arsenal.imbue_haki"] = "Imbue Haki"
w(f'{A}/lang/en_us.json', lang)
# Item art is drawn by tools/gen_textures.py, one texture per material for every part and weapon layer.
# The item model picks the texture from the material saved on the stack (the "arsenal:part_material" property,
# registered in client/ArsenalClient). Weapons stack one layer per part, so a Kairoseki blade on a Ship Iron
# hilt looks like one. gen_textures.py draws every material in materials.json (it stops if one has no colours).
TEXTURED = [m["id"] for m in MATERIALS]
WEAPON_TEXTURED = [m["id"] for m in MATERIALS if not m.get("armor_only")]
FALLBACK = "ship_iron"  # what a blank part (from the creative menu) looks like


def model(path, parent="minecraft:item/generated", suffix=""):
    """An item model for the texture item/<path>. suffix names a second model of the same texture (another pose)."""
    w(f'{A}/models/item/{path}{suffix}.json', {"parent": parent, "textures": {"layer0": f"arsenal:item/{path}"}})
    return {"type": "minecraft:model", "model": f"arsenal:item/{path}{suffix}"}


# A weapon coated in Armament Haki (haki/HakiImbue) turns its first layer black: the "arsenal:haki_coat" tint
# (client/HakiCoatTint) multiplies it down to near black while the coat is on, and leaves it as drawn otherwise.
HAKI_COAT = [{"type": "arsenal:haki_coat"}]


def coated(entry):
    return {**entry, "tints": HAKI_COAT}


def by_material(folder, index=None, parent="minecraft:item/generated", materials=None, haki=False, suffix=""):
    """Picks folder/<material> by the material of the part at index (or of the part itself).
    haki=True lets these models turn black while the weapon is coated in Haki. suffix names a second model of
    the same textures (another pose, like a raised shield)."""
    tint = coated if haki else (lambda entry: entry)
    select = {"type": "minecraft:select", "property": "arsenal:part_material"}
    if index is not None:
        select["index"] = index
    select["cases"] = [{"when": m, "model": tint(model(f"{folder}/{m}", parent, suffix))} for m in (materials or TEXTURED)]
    select["fallback"] = tint({"type": "minecraft:model", "model": f"arsenal:item/{folder}/{FALLBACK}{suffix}"})
    return select


for name in ["driftwood", "marine_steel_ingot", "cannon_bronze_ingot", "sea_king_bone", "island_whale_leather",
             "pacifista_alloy", "vegapunk_compound", "shipwrights_manual",
             "flame_dial_round", "impact_dial_round", "breath_dial_round", "flash_dial_round"]:
    w(f'{A}/items/{name}.json', {"model": model(name)})
for part in ["katana_blade", "tsuba", "hilt", "barrel", "firing_lock", "stock", "long_barrel", "cannon_bore", "fuse",
             "gun_carriage", "cannon_shell", "musket_ball", "cutlass_blade", "sabre_blade", "axe_head", "jitte_rod", "club_head", "spearhead",
             "knuckles", "impact_core", "reject_core", "hand_guard", "binding", "studs", "grip", "haft", "shield_face"]:
    w(f'{A}/items/{part}.json', {"model": by_material(f"part/{part}", materials=WEAPON_TEXTURED)})
for part in ["crown", "coat_plates", "sash_weave", "boot_soles", "lining", "gauntlet_plates"]:
    w(f'{A}/items/{part}.json', {"model": by_material(f"part/{part}")})
# Each weapon's layers, in the order its parts are saved (katana_parts, gun_parts or weapon_parts).
weapon_layers = {
    "katana": ["blade", "tsuba", "hilt"],
    "flintlock": ["barrel", "lock", "stock"], "musket": ["barrel", "lock", "stock"],
    "hand_cannon": ["bore", "fuse", "stock"],
    "ship_cannon": ["bore", "fuse", "carriage"],  # cannon_parts; placed, it looks like the One Piece Mod's Cannon
    "cutlass": ["blade", "guard", "grip"], "marine_sabre": ["blade", "guard", "grip"],
    "boarding_axe": ["head", "binding", "haft"], "jitte": ["rod", "binding", "grip"],
    "kanabo": ["head", "studs", "haft"], "spear": ["head", "binding", "haft"],
    "fishman_gloves": ["knuckles", "strap", "palm"],
    "impact_palm": ["dial", "strap", "palm"], "reject_palm": ["dial", "strap", "palm"],
}
# The famous Devil Fruit weapons (fruit/FamousFruitWeapon) are forged gear of these kinds with their own look,
# picked by the custom_model_data string FamousFruitWeapon.create sets.
famous = {"hand_cannon": ["lassoo"], "marine_sabre": ["funkfreed"], "cutlass": ["cerberus"], "kanabo": ["ragnir"]}
for weapon, layers in weapon_layers.items():
    forged = {"type": "minecraft:composite", "models": [
        by_material(f"{weapon}/{layer}", i, "minecraft:item/handheld", WEAPON_TEXTURED, haki=i == 0 and weapon != "ship_cannon")
        for i, layer in enumerate(layers)]}
    if weapon in famous:
        forged = {"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": 0,
                  "cases": [{"when": name, "model": coated(model(f"famous/{name}", "minecraft:item/handheld"))} for name in famous[weapon]],
                  "fallback": forged}
    w(f'{A}/items/{weapon}.json', {"model": forged})


def held(scale, tilt=55, first_tilt=25, lift=0.0, first_lift=None, flip=False):
    """Vanilla's handheld poses (item/handheld.json), scaled up so the famous weapons are their anime size in hand.
    tilt is the angle of the texture's diagonal (55 is a sword's); a texture drawn level needs 45 more. lift raises it so
    the grip stays in the hand once it's bigger, and first_lift (half of lift unless given) does the same in first person.
    flip turns it upside down, for a texture whose top would end up underneath once it's tilted past level."""
    third, first = scale
    if first_lift is None:
        first_lift = lift / 2
    up = -1 if flip else 1
    pose = {}
    for side, sign in (("right", 1), ("left", -1)):
        pose[f"thirdperson_{side}hand"] = {"rotation": [0, -90 * sign, tilt * sign],
                                           "translation": [0, 4 + lift, 0.5], "scale": [0.85 * third, 0.85 * third * up, 0.85 * third]}
        pose[f"firstperson_{side}hand"] = {"rotation": [0, -90 * sign, first_tilt * sign],
                                           "translation": [1.13, 3.2 + first_lift, 1.13], "scale": [0.68 * first, 0.68 * first * up, 0.68 * first]}
    return pose


# How big each famous weapon is in hand (third person, first person). Icons in the inventory stay normal size.
for name, display in {"ragnir": held((1.8, 1.4), lift=3.0, first_lift=0.0),  # a giant's hammer
                      "lassoo": held((1.4, 1.4), tilt=100, first_tilt=200, flip=True),  # a bazooka, held level
                      "funkfreed": held((1.15, 1.15), lift=0.5), "cerberus": held((1.15, 1.15), lift=0.5)}.items():
    w(f'{A}/models/item/famous/{name}.json', {"parent": "minecraft:item/handheld",
                                              "textures": {"layer0": f"arsenal:item/famous/{name}"}, "display": display})
# Shields (shield_parts: face, rim, strap; the strap shows as the boss in the middle). Held flat like a buckler, and
# brought up in front of you while it's raised (minecraft:using_item). Both poses are the two parent models below.
# Tuned in game. A flat item/generated sprite lies like a tray in third person and edge-on in first person at
# rotation 0, so: held = a buckler on the forearm, face outward; raised = face toward the camera, lower middle.
# Left-hand transforms are mirrored by the game, so both hands get the same numbers.
SHIELD_HELD_3RD = {"rotation": [0, 90, 90], "translation": [2, 4, 1.5], "scale": [0.85, 0.85, 0.85]}
SHIELD_HELD_1ST = {"rotation": [0, -90, 10], "translation": [-3, 4, -2], "scale": [0.8, 0.8, 0.8]}
SHIELD_POSE = {
    "thirdperson_righthand": SHIELD_HELD_3RD,
    "thirdperson_lefthand": SHIELD_HELD_3RD,
    "firstperson_righthand": SHIELD_HELD_1ST,
    "firstperson_lefthand": SHIELD_HELD_1ST,
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}
SHIELD_RAISED_1ST = {"rotation": [0, -90, 0], "translation": [22, 4, 5], "scale": [1.1, 1.1, 1.1]}
SHIELD_RAISED = {**SHIELD_POSE,
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [-3, 5, 3], "scale": [1.0, 1.0, 1.0]},
    "thirdperson_lefthand": {"rotation": [0, -90, 0], "translation": [-3, 5, 3], "scale": [1.0, 1.0, 1.0]},
    "firstperson_righthand": SHIELD_RAISED_1ST,
    "firstperson_lefthand": SHIELD_RAISED_1ST,
}
w(f'{A}/models/item/shield_held.json', {"parent": "minecraft:item/generated", "display": SHIELD_POSE})
w(f'{A}/models/item/shield_raised.json', {"parent": "minecraft:item/generated", "display": SHIELD_RAISED})
shield_layers = ["face", "rim", "boss"]
w(f'{A}/items/shield.json', {"model": {
    "type": "minecraft:condition", "property": "minecraft:using_item",
    "on_true": {"type": "minecraft:composite", "models": [
        by_material(f"shield/{layer}", i, "arsenal:item/shield_raised", WEAPON_TEXTURED, suffix="_raised") for i, layer in enumerate(shield_layers)]},
    "on_false": {"type": "minecraft:composite", "models": [
        by_material(f"shield/{layer}", i, "arsenal:item/shield_held", WEAPON_TEXTURED) for i, layer in enumerate(shield_layers)]}}})
# Gauntlets (gauntlet_parts: plates, lining). Any material makes plates, hides and leathers too.
w(f'{A}/items/gauntlets.json', {"model": {"type": "minecraft:composite", "models": [
    by_material(f"gauntlets/{layer}", i) for i, layer in enumerate(["plates", "lining"])]}})
for piece in ["hat", "coat", "sash", "boots"]:
    w(f'{A}/items/{piece}.json', {"model": by_material(f"armor/{piece}", 0)})
# The worn look of Arsenal armor, one per shell material (ArmorStats picks it when a piece is forged).
for mat in MATERIALS:
    look = mat["id"] if mat["id"] in TEXTURED else FALLBACK
    w(f'{A}/equipment/{mat["id"]}.json', {"layers": {"humanoid": [{"texture": f"arsenal:{look}"}],
                                                     "humanoid_leggings": [{"texture": f"arsenal:{look}"}]}})
# Pieces forged before the per-material looks existed.
w(f'{A}/equipment/pirate.json', {"layers": {"humanoid": [{"texture": f"arsenal:{FALLBACK}"}],
                                            "humanoid_leggings": [{"texture": f"arsenal:{FALLBACK}"}]}})
w(f'{A}/items/shipwrights_anvil.json', {"model": {"type": "minecraft:model", "model": "arsenal:block/shipwrights_anvil"}})
w(f'{A}/models/block/shipwrights_anvil.json', {"parent": "minecraft:block/anvil", "textures": {
    "particle": "minecraft:block/anvil", "body": "minecraft:block/anvil", "top": "minecraft:block/anvil_top"}})
variants = {}
for d, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
    variants[f"facing={d}"] = {"model": "arsenal:block/shipwrights_anvil", "y": y} if y else {"model": "arsenal:block/shipwrights_anvil"}
w(f'{A}/blockstates/shipwrights_anvil.json', {"variants": variants})
# Navy stations (captured from Marine bases). Drawn by tools/gen_textures.py; a captured one has a Jolly Roger
# on its front and top.
navy_stations = {
    # front (north), sides, top
    "marine_armory_bench": ("front", "side", "top"),
    "quartermasters_desk": ("front", "side", "top"),
    "marine_base_smeltery": ("front", "side", "top"),
}
for name, (front, side, top) in navy_stations.items():
    for tag in ["", "_captured"]:
        t = lambda face: f"arsenal:block/{name}_{face}{tag}"
        s = f"arsenal:block/{name}_{side}"  # the flag only goes on the front and top
        w(f'{A}/models/block/{name}{tag}.json', {"parent": "minecraft:block/cube", "textures": {
            "particle": t(side), "north": t(front), "south": s, "east": s, "west": s,
            "up": t(top), "down": "arsenal:block/navy_station_bottom"}})
    w(f'{A}/items/{name}.json', {"model": {"type": "minecraft:model", "model": f"arsenal:block/{name}"}})
    variants = {}
    for captured in ["false", "true"]:
        m = f"arsenal:block/{name}" + ("_captured" if captured == "true" else "")
        for d, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
            variants[f"captured={captured},facing={d}"] = {"model": m, "y": y} if y else {"model": m}
    w(f'{A}/blockstates/{name}.json', {"variants": variants})

# The Ship Cannon block wears the One Piece Mod's Cannon model, so it looks like the cannons it fires with.
variants = {}
for d, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
    variants[f"facing={d}"] = {"model": "onepiece:block/cannon", "y": y} if y else {"model": "onepiece:block/cannon"}
w(f'{A}/blockstates/ship_cannon.json', {"variants": variants})

D = 'data/arsenal'
w(f'{D}/loot_table/blocks/shipwrights_anvil.json', {"type": "minecraft:block", "pools": [{
    "rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
    "entries": [{"type": "minecraft:item", "name": "arsenal:shipwrights_anvil"}]}],
    "random_sequence": "arsenal:blocks/shipwrights_anvil"})
# A broken Ship Cannon keeps its parts (copied back from its block entity).
w(f'{D}/loot_table/blocks/ship_cannon.json', {"type": "minecraft:block", "pools": [{
    "rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
    "entries": [{"type": "minecraft:item", "name": "arsenal:ship_cannon", "modifier": {
        "type": "minecraft:copy_components", "source": "block_entity", "include": ["arsenal:cannon_parts"]}}]}],
    "random_sequence": "arsenal:blocks/ship_cannon"})
# The One Piece Mod fires every block in this tag (see its CannonAddons).
w('data/onepiece/tags/block/cannons.json', {"replace": False, "values": ["arsenal:ship_cannon"]})
for name in navy_stations:
    w(f'{D}/loot_table/blocks/{name}.json', {"type": "minecraft:block", "pools": [{
        "rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": f"arsenal:{name}"}]}],
        "random_sequence": f"arsenal:blocks/{name}"})
# Which of the One Piece Mod's Marine bases have which Navy station (see NavyStations).
w(f'{D}/tags/worldgen/structure/has_armory_bench.json', {"replace": False, "values": [
    "onepiece:marine_base", "onepiece:g5_base", "onepiece:navarone", "onepiece:marineford"]})
w(f'{D}/tags/worldgen/structure/has_quartermasters_desk.json', {"replace": False, "values": [
    "onepiece:marine_base", "onepiece:marine_bunker", "onepiece:marineford"]})
w(f'{D}/tags/worldgen/structure/has_base_smeltery.json', {"replace": False, "values": [
    "onepiece:g5_base", "onepiece:navarone", "onepiece:marineford"]})
# Cutlasses and sabres are swords too, so the main mod's sword styles, whetstones and Dials work on them.
w('data/minecraft/tags/item/swords.json', {"replace": False, "values": ["arsenal:katana", "arsenal:cutlass", "arsenal:marine_sabre"]})
w('data/minecraft/tags/item/axes.json', {"replace": False, "values": ["arsenal:boarding_axe"]})
# Sword enchantments (Sharpness, Looting, Fire Aspect...) on the weapons that aren't swords or axes.
OTHER_WEAPONS = ["arsenal:jitte", "arsenal:kanabo", "arsenal:spear", "arsenal:fishman_gloves", "arsenal:impact_palm", "arsenal:reject_palm"]
w('data/minecraft/tags/item/enchantable/sword.json', {"replace": False, "values": OTHER_WEAPONS})
w('data/minecraft/tags/item/enchantable/sharp_weapon.json', {"replace": False, "values": OTHER_WEAPONS})
# The One Piece Mod's fist styles (Fish-Man Karate, Ryusoken, Fist of Love) treat Fishman Gloves as an empty hand.
w('data/onepiece/tags/item/fist_weapons.json', {"replace": False, "values": ["arsenal:fishman_gloves"]})
# These take the One Piece Mod's Flame and Impact Dials (WeaponUpgrades.HOLDS_DIAL). The Dial palms are Dials already.
w('data/onepiece/tags/item/holds_dial.json', {"replace": False, "values": [
    "arsenal:jitte", "arsenal:kanabo", "arsenal:spear", "arsenal:fishman_gloves"]})
# The One Piece Mod's gun enchantments (Quick Draw, Sniper's Eye) and Unbreaking/Mending work on Arsenal guns.
w('data/onepiece/tags/item/guns.json', {"replace": False, "values": [f"arsenal:{g}" for g in GUNS]})
w('data/minecraft/tags/item/enchantable/durability.json', {"replace": False,
    "values": [f"arsenal:{g}" for g in GUNS] + OTHER_WEAPONS + ["arsenal:shield", "arsenal:gauntlets"]})
# Any part material repairs Arsenal armor at an anvil (the main mod's tags do the same per set).
w(f'{D}/tags/item/repairs_arsenal_armor.json', {"replace": False,
    "values": sorted({m["item"] for m in MATERIALS if m.get("craftable", True)})})
# What Predator hits harder.
w(f'{D}/tags/entity_type/sea_creatures.json', {"replace": False, "values": [
    "#minecraft:aquatic", {"id": "onepiece:sea_king", "required": False}, {"id": "onepiece:island_whale", "required": False}]})
w('data/minecraft/tags/item/enchantable/armor.json', {"replace": False, "values": ["arsenal:hat", "arsenal:coat", "arsenal:sash", "arsenal:boots"]})
w('data/minecraft/tags/item/head_armor.json', {"replace": False, "values": ["arsenal:hat"]})
w('data/minecraft/tags/item/chest_armor.json', {"replace": False, "values": ["arsenal:coat"]})
w('data/minecraft/tags/item/leg_armor.json', {"replace": False, "values": ["arsenal:sash"]})
w('data/minecraft/tags/item/foot_armor.json', {"replace": False, "values": ["arsenal:boots"]})
w('data/minecraft/tags/block/mineable/pickaxe.json', {"replace": False, "values": [
    "arsenal:shipwrights_anvil", "arsenal:marine_armory_bench", "arsenal:marine_base_smeltery", "arsenal:ship_cannon"]})
w('data/minecraft/tags/block/mineable/axe.json', {"replace": False, "values": ["arsenal:quartermasters_desk"]})

w(f'{D}/recipe/shipwrights_anvil.json', {"type": "minecraft:crafting_shaped", "category": "misc",
    "key": {"A": "minecraft:anvil", "P": "#minecraft:planks"}, "pattern": ["A", "P"],
    "result": {"id": "arsenal:shipwrights_anvil"}})
w(f'{D}/recipe/driftwood.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["minecraft:stick", "minecraft:stick", "minecraft:kelp"],
    "result": {"id": "arsenal:driftwood", "count": 2}})

# The guide book (GuideBook), for players who lost theirs.
w(f'{D}/recipe/shipwrights_manual.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["minecraft:book", "arsenal:driftwood"],
    "result": {"id": "arsenal:shipwrights_manual"}})

# Cannon Bronze: copper melted down with an old cannonball (ship battles drop them), or with plain iron.
w(f'{D}/recipe/cannon_bronze_ingot.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["minecraft:copper_ingot", "minecraft:copper_ingot", "onepiece:cannonball"],
    "result": {"id": "arsenal:cannon_bronze_ingot", "count": 2}})
w(f'{D}/recipe/cannon_bronze_ingot_from_iron.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:iron_ingot"],
    "result": {"id": "arsenal:cannon_bronze_ingot", "count": 2}})
# Dial Rounds (ammo/DialRound): a Sky Island Dial packed with eight of the One Piece Mod's Bullets.
for kind in ["flame", "impact", "breath", "flash"]:
    w(f'{D}/recipe/{kind}_dial_round.json', {"type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": [f"onepiece:{kind}_dial"] + ["onepiece:bullet"] * 8,
        "result": {"id": f"arsenal:{kind}_dial_round", "count": 8}})

# One recipe per part per material, from materials.json. Each part has its own shape, and none of them
# matches a vanilla recipe for the same ingredient (an iron sword is iron, iron, stick, so no part is).
# Navy materials have none: their parts are made at captured Navy stations (Marine Standard Steel at the
# Quartermaster's Desk; Kairoseki, Pacifista Alloy and Vegapunk Compound at the Marine Base Smeltery; see
# NavyCastingBlock). Hides and leathers only make armor parts, and Black Blade Steel isn't crafted at all.
L, S, K, N, F, G = "minecraft:leather", "minecraft:string", "minecraft:stick", "minecraft:iron_nugget", "minecraft:flint", "minecraft:gunpowder"
DS = "onepiece:dial_shell"  # Skypiea's Dial shells: the Dial cores are built around one
parts = {
    "katana_blade": (["M", "M", "M"], {}),
    "tsuba": ([" M ", "M M", " M "], {}),
    "hilt": (["M", "L"], {"L": L}),
    "barrel": (["M", "M"], {}),
    "firing_lock": ([" F", "MM"], {"F": F}),
    "stock": (["MM", " M"], {}),
    "long_barrel": (["MMM", "  F"], {"F": F}),
    "cannon_bore": (["MMM", "MGM"], {"G": G}),
    "fuse": (["S", "G", "M"], {"S": S, "G": G}),
    "gun_carriage": (["MMM", "K K"], {"K": K}),
    "cannon_shell": ([" M ", "MGM", " M "], {"G": G}),
    "musket_ball": (["M", "G"], {"G": G}),
    "cutlass_blade": (["  M", " M ", "M  "], {}),
    "sabre_blade": (["M", "M", "N"], {"N": N}),
    "axe_head": (["MMM", "M  "], {}),
    "jitte_rod": (["M", "N", "M"], {"N": N}),
    "club_head": (["MMM", "MKM"], {"K": K}),  # a stick for the haft, or iron or copper would make bars
    "spearhead": ([" M ", "MMM"], {}),
    "knuckles": (["MMM", "N N"], {"N": N}),
    # The casing's material around a Dial Shell. A Reject Dial is rarer: four shells and gunpowder for its kick.
    "impact_core": ([" M ", "MDM", " M "], {"D": DS}),
    "reject_core": (["DMD", "MGM", "DMD"], {"D": DS, "G": G}),
    "hand_guard": (["MNM", "MMM"], {"N": N}),  # a nugget in the gap, or iron would make a Minecart
    "binding": (["SMS"], {"S": S}),
    "studs": (["N N", " M ", "N N"], {"N": N}),
    "grip": (["L", "M", "L"], {"L": L}),
    "haft": (["K", "M", "K"], {"K": K}),
    # Armor shells have a leather pad in the middle, so they don't clash with vanilla armor or each other.
    "crown": (["MMM", "MLM"], {"L": L}),
    "coat_plates": (["MLM", "MMM", "MMM"], {"L": L}),
    "sash_weave": (["MMM", "MLM", "M M"], {"L": L}),
    "boot_soles": (["MLM", "M M"], {"L": L}),
    "lining": (["LML"], {"L": L}),
    # Vanilla's shield shape with a leather pad where its iron goes; gauntlet plates are boot soles upside down.
    "shield_face": (["MLM", "MMM", " M "], {"L": L}),
    "gauntlet_plates": (["M M", "MLM"], {"L": L}),
}
# Shapes that would clash with a vanilla recipe for one material get their own (GameTests check none clash).
CLASHES = {
    ("katana_blade", "minecraft:copper_ingot"): (["M ", "M ", "MN"], {"N": N}),  # three copper is a Lightning Rod
    ("long_barrel", "minecraft:flint"): (["MMM", "  N"], {"N": N}),  # a flint lock on flint is the Axe Head mirrored
}
ARMOR_PARTS = {"crown", "coat_plates", "sash_weave", "boot_soles", "lining", "gauntlet_plates"}
COUNTS = {"cannon_shell": 4, "musket_ball": 8}  # parts that come more than one to a craft
for m in MATERIALS:
    if not m.get("craftable", True) or m["faction"] == "navy":
        continue
    for part, shape in parts.items():
        if m.get("armor_only") and part not in ARMOR_PARTS:
            continue
        pattern, extra = CLASHES.get((part, m["item"]), shape)
        w(f'{D}/recipe/{part}/{m["id"]}.json', {"type": "minecraft:crafting_shaped", "category": "equipment",
            "group": f"arsenal_{part}", "key": {"M": m["item"], **extra}, "pattern": pattern,
            "result": {"id": f"arsenal:{part}", **({"count": COUNTS[part]} if part in COUNTS else {}),
                       "components": {"arsenal:part_material": m["id"]}}})
