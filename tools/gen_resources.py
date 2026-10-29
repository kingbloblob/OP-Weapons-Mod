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
           "jitte": "Jitte", "kanabo": "Kanabo", "spear": "Spear"}
GUNS = {"flintlock": "Flintlock", "musket": "Musket", "hand_cannon": "Hand Cannon"}
lang = {
    "item.arsenal.driftwood": "Driftwood",
    "item.arsenal.marine_steel_ingot": "Marine Standard Steel Ingot",
    "item.arsenal.cannon_bronze_ingot": "Cannon Bronze Ingot",
    "item.arsenal.sea_king_bone": "Sea King Bone",
    "item.arsenal.island_whale_leather": "Island Whale Leather",
    "item.arsenal.pacifista_alloy": "Pacifista Alloy",
    "item.arsenal.vegapunk_compound": "Vegapunk Compound",
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
    "item.arsenal.cutlass_blade": "Cutlass Blade",
    "item.arsenal.sabre_blade": "Sabre Blade",
    "item.arsenal.axe_head": "Axe Head",
    "item.arsenal.jitte_rod": "Jitte Rod",
    "item.arsenal.club_head": "Kanabo Head",
    "item.arsenal.spearhead": "Spearhead",
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
    "block.arsenal.shipwrights_anvil": "Shipwright's Anvil",
    "block.arsenal.marine_armory_bench": "Marine Armory Bench",
    "block.arsenal.quartermasters_desk": "Quartermaster's Desk",
    "block.arsenal.marine_base_smeltery": "Marine Base Smeltery"
}
for name, title in {**WEAPONS, **GUNS}.items():
    lang[f"item.arsenal.{name}"] = title
w(f'{A}/lang/en_us.json', lang)
# Item art is drawn by tools/gen_textures.py, one texture per material for every part and weapon layer.
# The item model picks the texture from the material saved on the stack (the "arsenal:part_material" property,
# registered in client/ArsenalClient). Weapons stack one layer per part, so a Kairoseki blade on a Ship Iron
# hilt looks like one. gen_textures.py draws every material in materials.json (it stops if one has no colours).
TEXTURED = [m["id"] for m in MATERIALS]
WEAPON_TEXTURED = [m["id"] for m in MATERIALS if not m.get("armor_only")]
FALLBACK = "ship_iron"  # what a blank part (from the creative menu) looks like


def model(path, parent="minecraft:item/generated"):
    w(f'{A}/models/item/{path}.json', {"parent": parent, "textures": {"layer0": f"arsenal:item/{path}"}})
    return {"type": "minecraft:model", "model": f"arsenal:item/{path}"}


def by_material(folder, index=None, parent="minecraft:item/generated", materials=None):
    """Picks folder/<material> by the material of the part at index (or of the part itself)."""
    select = {"type": "minecraft:select", "property": "arsenal:part_material"}
    if index is not None:
        select["index"] = index
    select["cases"] = [{"when": m, "model": model(f"{folder}/{m}", parent)} for m in (materials or TEXTURED)]
    select["fallback"] = {"type": "minecraft:model", "model": f"arsenal:item/{folder}/{FALLBACK}"}
    return select


for name in ["driftwood", "marine_steel_ingot", "cannon_bronze_ingot", "sea_king_bone", "island_whale_leather",
             "pacifista_alloy", "vegapunk_compound"]:
    w(f'{A}/items/{name}.json', {"model": model(name)})
for part in ["katana_blade", "tsuba", "hilt", "barrel", "firing_lock", "stock", "long_barrel", "cannon_bore", "fuse",
             "cutlass_blade", "sabre_blade", "axe_head", "jitte_rod", "club_head", "spearhead", "hand_guard",
             "binding", "studs", "grip", "haft"]:
    w(f'{A}/items/{part}.json', {"model": by_material(f"part/{part}", materials=WEAPON_TEXTURED)})
for part in ["crown", "coat_plates", "sash_weave", "boot_soles", "lining"]:
    w(f'{A}/items/{part}.json', {"model": by_material(f"part/{part}")})
# Each weapon's layers, in the order its parts are saved (katana_parts, gun_parts or weapon_parts).
weapon_layers = {
    "katana": ["blade", "tsuba", "hilt"],
    "flintlock": ["barrel", "lock", "stock"], "musket": ["barrel", "lock", "stock"],
    "hand_cannon": ["bore", "fuse", "stock"],
    "cutlass": ["blade", "guard", "grip"], "marine_sabre": ["blade", "guard", "grip"],
    "boarding_axe": ["head", "binding", "haft"], "jitte": ["rod", "binding", "grip"],
    "kanabo": ["head", "studs", "haft"], "spear": ["head", "binding", "haft"],
}
for weapon, layers in weapon_layers.items():
    w(f'{A}/items/{weapon}.json', {"model": {"type": "minecraft:composite", "models": [
        by_material(f"{weapon}/{layer}", i, "minecraft:item/handheld", WEAPON_TEXTURED) for i, layer in enumerate(layers)]}})
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

D = 'data/arsenal'
w(f'{D}/loot_table/blocks/shipwrights_anvil.json', {"type": "minecraft:block", "pools": [{
    "rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
    "entries": [{"type": "minecraft:item", "name": "arsenal:shipwrights_anvil"}]}],
    "random_sequence": "arsenal:blocks/shipwrights_anvil"})
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
OTHER_WEAPONS = ["arsenal:jitte", "arsenal:kanabo", "arsenal:spear"]
w('data/minecraft/tags/item/enchantable/sword.json', {"replace": False, "values": OTHER_WEAPONS})
w('data/minecraft/tags/item/enchantable/sharp_weapon.json', {"replace": False, "values": OTHER_WEAPONS})
# The One Piece Mod's gun enchantments (Quick Draw, Sniper's Eye) and Unbreaking/Mending work on Arsenal guns.
w('data/onepiece/tags/item/guns.json', {"replace": False, "values": [f"arsenal:{g}" for g in GUNS]})
w('data/minecraft/tags/item/enchantable/durability.json', {"replace": False,
    "values": [f"arsenal:{g}" for g in GUNS] + OTHER_WEAPONS})
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
    "arsenal:shipwrights_anvil", "arsenal:marine_armory_bench", "arsenal:marine_base_smeltery"]})
w('data/minecraft/tags/block/mineable/axe.json', {"replace": False, "values": ["arsenal:quartermasters_desk"]})

w(f'{D}/recipe/shipwrights_anvil.json', {"type": "minecraft:crafting_shaped", "category": "misc",
    "key": {"A": "minecraft:anvil", "P": "#minecraft:planks"}, "pattern": ["A", "P"],
    "result": {"id": "arsenal:shipwrights_anvil"}})
w(f'{D}/recipe/driftwood.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["minecraft:stick", "minecraft:stick", "minecraft:kelp"],
    "result": {"id": "arsenal:driftwood", "count": 2}})

# Cannon Bronze: copper melted down with an old cannonball (ship battles drop them), or with plain iron.
w(f'{D}/recipe/cannon_bronze_ingot.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["minecraft:copper_ingot", "minecraft:copper_ingot", "onepiece:cannonball"],
    "result": {"id": "arsenal:cannon_bronze_ingot", "count": 2}})
w(f'{D}/recipe/cannon_bronze_ingot_from_iron.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:iron_ingot"],
    "result": {"id": "arsenal:cannon_bronze_ingot", "count": 2}})

# One recipe per part per material, from materials.json. Each part has its own shape, and none of them
# matches a vanilla recipe for the same ingredient (an iron sword is iron, iron, stick, so no part is).
# Navy materials have none: their parts are made at captured Navy stations (Marine Standard Steel at the
# Quartermaster's Desk; Kairoseki, Pacifista Alloy and Vegapunk Compound at the Marine Base Smeltery; see
# NavyCastingBlock). Hides and leathers only make armor parts, and Black Blade Steel isn't crafted at all.
L, S, K, N, F, G = "minecraft:leather", "minecraft:string", "minecraft:stick", "minecraft:iron_nugget", "minecraft:flint", "minecraft:gunpowder"
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
    "cutlass_blade": (["  M", " M ", "M  "], {}),
    "sabre_blade": (["M", "M", "N"], {"N": N}),
    "axe_head": (["MMM", "M  "], {}),
    "jitte_rod": (["M", "N", "M"], {"N": N}),
    "club_head": (["MMM", "MMM"], {}),
    "spearhead": ([" M ", "MMM"], {}),
    "hand_guard": (["M M", "MMM"], {}),
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
}
ARMOR_PARTS = {"crown", "coat_plates", "sash_weave", "boot_soles", "lining"}
for m in MATERIALS:
    if not m.get("craftable", True) or m["faction"] == "navy":
        continue
    for part, (pattern, extra) in parts.items():
        if m.get("armor_only") and part not in ARMOR_PARTS:
            continue
        w(f'{D}/recipe/{part}/{m["id"]}.json', {"type": "minecraft:crafting_shaped", "category": "equipment",
            "group": f"arsenal_{part}", "key": {"M": m["item"], **extra}, "pattern": pattern,
            "result": {"id": f"arsenal:{part}", "components": {"arsenal:part_material": m["id"]}}})
