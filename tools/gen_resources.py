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
w(f'{A}/lang/en_us.json', {
    "item.arsenal.driftwood": "Driftwood",
    "item.arsenal.marine_steel_ingot": "Marine Standard Steel Ingot",
    "item.arsenal.katana_blade": "Katana Blade",
    "item.arsenal.tsuba": "Tsuba",
    "item.arsenal.hilt": "Hilt",
    "item.arsenal.katana": "Katana",
    "item.arsenal.barrel": "Barrel",
    "item.arsenal.firing_lock": "Firing Lock",
    "item.arsenal.stock": "Stock",
    "item.arsenal.flintlock": "Flintlock",
    "item.arsenal.crown": "Hat Crown",
    "item.arsenal.coat_plates": "Coat Plates",
    "item.arsenal.sash_weave": "Sash Weave",
    "item.arsenal.boot_soles": "Boot Soles",
    "item.arsenal.lining": "Lining",
    "item.arsenal.hat": "Hat",
    "item.arsenal.coat": "Coat",
    "item.arsenal.sash": "Sash",
    "item.arsenal.boots": "Boots",
    "block.arsenal.shipwrights_anvil": "Shipwright's Anvil"
})
# Item art is drawn by tools/gen_textures.py, one texture per material for every part and weapon layer.
# The item model picks the texture from the material saved on the stack (the "arsenal:part_material" property,
# registered in client/ArsenalClient). Weapons stack one layer per part, so a Kairoseki blade on a Ship Iron
# hilt looks like one. A new material needs adding to MATERIALS here and to gen_textures.py.
MATERIALS = ["driftwood", "ship_iron", "marine_steel", "kairoseki"]
FALLBACK = "ship_iron"  # what a blank part (from the creative menu) looks like


def model(path, parent="minecraft:item/generated"):
    w(f'{A}/models/item/{path}.json', {"parent": parent, "textures": {"layer0": f"arsenal:item/{path}"}})
    return {"type": "minecraft:model", "model": f"arsenal:item/{path}"}


def by_material(folder, index=None, parent="minecraft:item/generated"):
    """Picks folder/<material> by the material of the part at index (or of the part itself)."""
    select = {"type": "minecraft:select", "property": "arsenal:part_material"}
    if index is not None:
        select["index"] = index
    select["cases"] = [{"when": m, "model": model(f"{folder}/{m}", parent)} for m in MATERIALS]
    select["fallback"] = {"type": "minecraft:model", "model": f"arsenal:item/{folder}/{FALLBACK}"}
    return select


for name in ["driftwood", "marine_steel_ingot"]:
    w(f'{A}/items/{name}.json', {"model": model(name)})
for part in ["katana_blade", "tsuba", "hilt", "barrel", "firing_lock", "stock",
             "crown", "coat_plates", "sash_weave", "boot_soles", "lining"]:
    w(f'{A}/items/{part}.json', {"model": by_material(f"part/{part}")})
for weapon, layers in {"katana": ["blade", "tsuba", "hilt"], "flintlock": ["barrel", "lock", "stock"]}.items():
    w(f'{A}/items/{weapon}.json', {"model": {"type": "minecraft:composite", "models": [
        by_material(f"{weapon}/{layer}", i, "minecraft:item/handheld") for i, layer in enumerate(layers)]}})
for piece in ["hat", "coat", "sash", "boots"]:
    w(f'{A}/items/{piece}.json', {"model": by_material(f"armor/{piece}", 0)})
# The worn look of Arsenal armor, one per shell material (ArmorStats picks it when a piece is forged).
for m in MATERIALS:
    w(f'{A}/equipment/{m}.json', {"layers": {"humanoid": [{"texture": f"arsenal:{m}"}],
                                             "humanoid_leggings": [{"texture": f"arsenal:{m}"}]}})
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

D = 'data/arsenal'
w(f'{D}/loot_table/blocks/shipwrights_anvil.json', {"type": "minecraft:block", "pools": [{
    "rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
    "entries": [{"type": "minecraft:item", "name": "arsenal:shipwrights_anvil"}]}],
    "random_sequence": "arsenal:blocks/shipwrights_anvil"})
w('data/minecraft/tags/item/swords.json', {"replace": False, "values": ["arsenal:katana"]})
# The One Piece Mod's gun enchantments (Quick Draw, Sniper's Eye) and Unbreaking/Mending work on Arsenal guns.
w('data/onepiece/tags/item/guns.json', {"replace": False, "values": ["arsenal:flintlock"]})
w('data/minecraft/tags/item/enchantable/durability.json', {"replace": False, "values": ["arsenal:flintlock"]})
# Any part material repairs Arsenal armor at an anvil (the main mod's tags do the same per set).
w(f'{D}/tags/item/repairs_arsenal_armor.json', {"replace": False, "values": [
    "arsenal:driftwood", "minecraft:iron_ingot", "arsenal:marine_steel_ingot", "onepiece:seastone_ingot"]})
w('data/minecraft/tags/item/enchantable/armor.json', {"replace": False, "values": ["arsenal:hat", "arsenal:coat", "arsenal:sash", "arsenal:boots"]})
w('data/minecraft/tags/item/head_armor.json', {"replace": False, "values": ["arsenal:hat"]})
w('data/minecraft/tags/item/chest_armor.json', {"replace": False, "values": ["arsenal:coat"]})
w('data/minecraft/tags/item/leg_armor.json', {"replace": False, "values": ["arsenal:sash"]})
w('data/minecraft/tags/item/foot_armor.json', {"replace": False, "values": ["arsenal:boots"]})
w('data/minecraft/tags/block/mineable/pickaxe.json', {"replace": False, "values": ["arsenal:shipwrights_anvil"]})

w(f'{D}/recipe/shipwrights_anvil.json', {"type": "minecraft:crafting_shaped", "category": "misc",
    "key": {"A": "minecraft:anvil", "P": "#minecraft:planks"}, "pattern": ["A", "P"],
    "result": {"id": "arsenal:shipwrights_anvil"}})
w(f'{D}/recipe/driftwood.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["minecraft:stick", "minecraft:stick", "minecraft:kelp"],
    "result": {"id": "arsenal:driftwood", "count": 2}})

# One recipe per part per material. Marine Standard Steel has no recipe for its ingot: it's taken from Marines.
materials = {"driftwood": "arsenal:driftwood", "ship_iron": "minecraft:iron_ingot",
             "marine_steel": "arsenal:marine_steel_ingot", "kairoseki": "onepiece:seastone_ingot"}
parts = {
    "katana_blade": (["M", "M", "M"], {}),
    "tsuba": ([" M ", "M M", " M "], {}),
    "hilt": (["M", "L"], {"L": "minecraft:leather"}),
    "barrel": (["M", "M"], {}),
    "firing_lock": ([" F", "MM"], {"F": "minecraft:flint"}),
    "stock": (["MM", " M"], {}),
    "crown": (["MMM", "M M"], {}),
    "coat_plates": (["M M", "MMM", "MMM"], {}),
    "sash_weave": (["MMM", "M M"], {}),
    "boot_soles": (["M M", "M M"], {}),
    "lining": (["LML"], {"L": "minecraft:leather"}),
}
for mat, ingredient in materials.items():
    for part, (pattern, extra) in parts.items():
        w(f'{D}/recipe/{part}/{mat}.json', {"type": "minecraft:crafting_shaped", "category": "equipment",
            "group": f"arsenal_{part}", "key": {"M": ingredient, **extra}, "pattern": pattern,
            "result": {"id": f"arsenal:{part}", "components": {"arsenal:part_material": mat}}})
