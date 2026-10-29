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
    "entrypoints": {"main": ["com.onepiecearsenal.ArsenalMod"]},
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
# Placeholder art: vanilla textures until the real ones are drawn.
items = {
    "driftwood": ("minecraft:item/generated", "minecraft:item/stick"),
    "marine_steel_ingot": ("minecraft:item/generated", "minecraft:item/iron_ingot"),
    "katana_blade": ("minecraft:item/generated", "minecraft:item/iron_sword"),
    "tsuba": ("minecraft:item/generated", "minecraft:item/iron_nugget"),
    "hilt": ("minecraft:item/generated", "minecraft:item/stick"),
    "katana": ("minecraft:item/handheld", "minecraft:item/iron_sword"),
    "barrel": ("minecraft:item/generated", "minecraft:block/iron_bars"),
    "firing_lock": ("minecraft:item/generated", "minecraft:item/flint"),
    "stock": ("minecraft:item/generated", "minecraft:item/stick"),
    "flintlock": ("minecraft:item/handheld", "onepiece:item/flintlock_pistol"),
    "crown": ("minecraft:item/generated", "minecraft:item/leather_helmet"),
    "coat_plates": ("minecraft:item/generated", "minecraft:item/iron_chestplate"),
    "sash_weave": ("minecraft:item/generated", "minecraft:item/string"),
    "boot_soles": ("minecraft:item/generated", "minecraft:item/leather_boots"),
    "lining": ("minecraft:item/generated", "minecraft:item/paper"),
    "hat": ("minecraft:item/generated", "minecraft:item/iron_helmet"),
    "coat": ("minecraft:item/generated", "minecraft:item/iron_chestplate"),
    "sash": ("minecraft:item/generated", "minecraft:item/iron_leggings"),
    "boots": ("minecraft:item/generated", "minecraft:item/iron_boots"),
}
for name, (parent, tex) in items.items():
    w(f'{A}/items/{name}.json', {"model": {"type": "minecraft:model", "model": f"arsenal:item/{name}"}})
    w(f'{A}/models/item/{name}.json', {"parent": parent, "textures": {"layer0": tex}})
# The worn look of Arsenal armor. Placeholder: vanilla iron, until pirate and Navy textures are drawn.
w(f'{A}/equipment/pirate.json', {"layers": {"humanoid": [{"texture": "minecraft:iron"}], "humanoid_leggings": [{"texture": "minecraft:iron"}]}})
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
