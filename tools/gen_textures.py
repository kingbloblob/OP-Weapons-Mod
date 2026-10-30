# Draws Arsenal's item and armor textures (16x16 pixel art) into src/main/resources/assets/arsenal/textures.
# Run from the repo root: python3 tools/gen_textures.py src/main/resources
#
# Every part and weapon is drawn once as a shape, then coloured per material, the way Tinkers' Construct does it.
# To give a new material its look, add it to MATERIALS (and to OUTFITS for how its armor looks when worn),
# then run this and tools/gen_resources.py.
import os, sys
from PIL import Image

OUT = os.path.join(sys.argv[1], 'assets/arsenal/textures')


def hexc(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def ramp(*hexes):
    return [hexc(h) for h in hexes]


# Each material has two colour ramps, darkest (the outline) to brightest:
#   main  = the material itself (blades, guards, barrels, locks, stocks, armor shells)
#   cloth = what a part made of it is wrapped or sewn with (hilt wraps, linings, sashes)
MATERIALS = {
    'driftwood': {
        'main': ramp('3a2a1f', '6a5039', '8e7255', 'b19a7a', 'd6c6a6'),
        'cloth': ramp('3d2a16', '7a5a32', 'a6834d', 'c9aa6e', 'e6d39c'),
    },
    'ship_iron': {
        'main': ramp('1f1f24', '4a4c53', '6f7279', '9a9ca2', 'c9cacd'),
        'cloth': ramp('240b0b', '561818', '832620', 'ab3d33', 'cf6a55'),
        'rust': ramp('5a2a12', '8a4a24', 'b0652e'),
    },
    'marine_steel': {
        'main': ramp('1a2232', '465a74', '7b93ae', 'b4c6d9', 'eef4fa'),
        'cloth': ramp('0f1a3a', '22408c', '3b62c4', 'dfe6f2', 'ffffff'),
    },
    'kairoseki': {
        'main': ramp('10181a', '27393b', '3b5856', '5b857d', '9bd1c0'),
        'cloth': ramp('120f1d', '2e2648', '4a3f75', '6f62a3', '9c90cc'),
    },
    'cannon_bronze': {  # cast bronze, wrapped in tarred rope
        'main': ramp('3a220e', '7a4a1c', 'a86c2c', 'cf9448', 'f0c47a'),
        'cloth': ramp('20180f', '44352a', '685242', '8d7560', 'b39d84'),
    },
    'sea_king_bone': {  # bleached bone, bound in Sea King hide
        'main': ramp('4a4232', '8c826a', 'bdb497', 'dcd5bb', 'f5f1e0'),
        'cloth': ramp('0f2a26', '1f4f47', '2f7266', '4a9a89', '7cc4b1'),
    },
    'sea_king_hide': {  # teal scales
        'main': ramp('0b221f', '1a4a42', '2b6e61', '45978a', '7ccab6'),
        'cloth': ramp('0f2a26', '1f4f47', '2f7266', '4a9a89', '7cc4b1'),
    },
    'treasure_gold': {  # gold, with royal purple velvet
        'main': ramp('5a400c', '8a6418', 'c99a32', 'e8c050', 'fbe58a'),
        'cloth': ramp('1e0c26', '42184f', '652a75', '8a43a0', 'b06ec6'),
    },
    'tamahagane': {  # Wano steel, warm sheen, indigo silk
        'main': ramp('1b1a1f', '46434c', '77727a', 'aaa4a8', 'ece4dc'),
        'cloth': ramp('0d1230', '1c2660', '2c3b8c', '4a5ab0', '7a86cf'),
    },
    'pacifista_alloy': {  # dark gunmetal, hazard yellow
        'main': ramp('15171b', '33373e', '52575f', '767c85', 'a3aab4'),
        'cloth': ramp('2a2208', '5e4c10', '9a7d18', 'd1ad2a', 'f5dc6a'),
    },
    'island_whale_leather': {  # Laboon's blue-grey hide, brown stitching
        'main': ramp('161d26', '2f3d4f', '4a5d75', '6c829c', '9aaec4'),
        'cloth': ramp('2a1a12', '5a3a26', '85583a', 'a87856', 'c99c7c'),
    },
    'wapometal': {  # bright, slightly violet metal, Drum fur
        'main': ramp('22202a', '4e4b5e', '7f7b94', 'b5b0c8', 'eae6fb'),
        'cloth': ramp('3a3530', '6e655b', '9e9384', 'c9bfae', 'eee7da'),
    },
    'adam_wood': {  # rich red-brown wood, sailcloth
        'main': ramp('2a1408', '5a2e12', '8a4a1e', 'b26a30', 'd89050'),
        'cloth': ramp('3d3628', '7a705a', 'aea385', 'd6cdb0', 'f2ecd8'),
    },
    'dial_shell': {  # Skypiea's spiral shells, cream and coral, on sky-blue cord
        'main': ramp('4a3428', '9a6e58', 'd2a088', 'ecc8b0', 'fbeee2'),
        'cloth': ramp('0e2a40', '22568a', '3c86c4', '6cb2e6', 'b0dcf8'),
    },
    'sea_pearl': {  # Fishman Island pearl, pink-white lustre, coral-red cord
        'main': ramp('4a4450', '948ca4', 'cbc4d8', 'ebe4f2', 'fffaff'),
        'cloth': ramp('2a0a10', '6a1a24', 'a82e38', 'd65058', 'f08a88'),
    },
    'sabaody_resin': {  # Yarukiman resin, amber, with soap-bubble cloth
        'main': ramp('3a1e06', '7a4410', 'b06e1e', 'd89a3a', 'f6cc78'),
        'cloth': ramp('2a3a4a', '6a8aa8', 'a0c8e0', 'd0eaf6', 'f6e6fa'),
    },
    'shandora_gold': {  # the bright gold of the City of Gold, with Shandian green
        'main': ramp('5a3a04', '9a6a0c', 'd8a018', 'f4cc3a', 'fff08a'),
        'cloth': ramp('0c2014', '1c4a2a', '2c7040', '48985a', '7cc488'),
    },
    'kokuto': {  # black steel with a cold blue sheen, grey silk
        'main': ramp('060709', '15181e', '262c36', '3c4656', '6a7a92'),
        'cloth': ramp('121214', '2a2a30', '44444c', '62626c', '86868f'),
    },
    'dragon_scale': {  # Punk Hazard dragon, red scales, charcoal leather
        'main': ramp('2a0604', '6a140a', 'a82a12', 'd85024', 'f68a4a'),
        'cloth': ramp('100c0c', '262020', '3c3434', '564c4c', '766a6a'),
    },
    'black_blade': {  # black with a crimson sheen
        'main': ramp('050406', '141015', '261c24', '3d2c38', '7a4a5c'),
        'cloth': ramp('1a0507', '3e0c10', '64141a', '8c2028', 'b83a40'),
    },
    'vegapunk_compound': {  # Egghead white-cyan, magenta
        'main': ramp('1a2a33', '3d6a7d', '5fa3b8', '9dd8e6', 'e8fbff'),
        'cloth': ramp('2a0a2a', '5a1a5e', '8a2a92', 'b84ac0', 'e07ae6'),
    },
}

FIXED = {
    'k': hexc('16121a'),   # outline black
    'g': hexc('8a6418'),   # gold, dark
    'h': hexc('e2b84a'),   # gold, light
    'W': hexc('f4f6f8'),   # white
    'V': hexc('c9d1dc'),   # white in shadow
    'L': hexc('5a3a22'),   # leather
    'M': hexc('8a5c34'),   # leather, light
}


def blank():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def save(img, path):
    full = os.path.join(OUT, path + '.png')
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


# ---- Masks ----
# A mask is 16 rows of 16 characters. '.' is clear. The other characters pick a colour:
#   1-5  ramp 'main' of the first material,  a-e  ramp 'cloth' of the first material
#   p-t  ramp 'main' of the second material, F-J its 'cloth', v-z  ramp 'main' of the third material, A-E its 'cloth'
#   anything in FIXED is that fixed colour
GROUPS = {
    **{c: (0, 'main', i) for i, c in enumerate('12345')},
    **{c: (0, 'cloth', i) for i, c in enumerate('abcde')},
    **{c: (1, 'main', i) for i, c in enumerate('pqrst')},
    **{c: (1, 'cloth', i) for i, c in enumerate('FGHIJ')},
    **{c: (2, 'main', i) for i, c in enumerate('vwxyz')},
    **{c: (2, 'cloth', i) for i, c in enumerate('ABCDE')},
}


def check(name, mask):
    rows = mask.strip('\n').split('\n')
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), f'{name}: mask must be 16x16, got {[len(r) for r in rows]}'
    return rows


def paint(rows, mats, only_group=None):
    """Colours a mask with the given materials. only_group keeps one material's pixels (one layer of a weapon)."""
    img = blank()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            if ch in GROUPS:
                g, rampname, i = GROUPS[ch]
                if only_group is not None and g != only_group:
                    continue
                img.putpixel((x, y), MATERIALS[mats[g]][rampname][i])
            elif ch in FIXED:
                if only_group is None or only_group == fixed_group(rows, x, y):
                    img.putpixel((x, y), FIXED[ch])
            else:
                raise ValueError(f'unknown mask character {ch!r}')
    return img


def fixed_group(rows, x, y):
    """A fixed-colour pixel belongs to the layer of the nearest coloured pixel."""
    best, bestd = 0, 99
    for yy, row in enumerate(rows):
        for xx, ch in enumerate(row):
            if ch in GROUPS:
                d = abs(xx - x) + abs(yy - y)
                if d < bestd:
                    best, bestd = GROUPS[ch][0], d
    return best


# ---- Weapons: one mask, split into one layer per part so each layer takes its part's material ----

# Blade (1-5), tsuba (p-t), hilt wrap (a-e) with its pommel (v-z: the hilt's own material, group 2 = hilt).
KATANA = check('katana', '''
..............14
.............145
............135.
...........135..
..........145...
.........145....
........135.....
.......135......
...s..145.......
...qs242........
....qr..........
...BDqr.........
..BC..pq........
.BD.............
BC..............
v...............
''')

# Barrel (1-5), firing lock (p-t), stock (v-z).
FLINTLOCK = check('flintlock', '''
................
................
................
..pp............
...qp.........1.
....qp111111111.
...psrr44444443.
...prqq33333332.
..vxpppp1111111.
.vyxxv.q........
.vyxv..p........
vyxv............
vywv............
vxwv............
vvv.............
................
''')

# ---- Parts (one material each) ----

KATANA_BLADE = check('katana_blade', '''
..............14
.............145
............135.
...........135..
..........145...
.........145....
........135.....
.......135......
......145.......
.....242........
....12..........
...12...........
..11............
................
................
................
''')

def tsuba_mask():
    """A round guard seen from the front, lit from the top left, with the slot the blade goes through."""
    rows = []
    for y in range(16):
        row = ''
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 6.2:
                row += '.'
            elif d > 5.2:
                row += '1'
            elif 7 <= x <= 8 and 5 <= y <= 10:
                row += '1'
            elif x == 9 and 6 <= y <= 10:
                row += '5'
            else:
                light = (x + y) - 15
                row += '5' if d > 4.2 and light < -4 else '4' if light < -2 else '3' if light < 3 else '2'
        rows.append(row)
    return rows


TSUBA = tsuba_mask()

HILT = check('hilt', '''
................
................
................
............11..
...........1441.
..........12331.
.........aedb1..
........adcb....
.......aedb.....
......adcb......
.....aedb.......
....adcb........
...1433.........
...1321.........
....11..........
................
''')

def barrel_mask():
    """A gun barrel lying flat: a tube with two iron bands and a flared muzzle on the right."""
    rows = ['.' * 16 for _ in range(16)]
    grid = [list(r) for r in rows]
    for x in range(1, 13):
        band = x in (4, 9)
        for y, shade in zip(range(5, 10), '1' + ('543' if band else '432') + '1'):
            grid[y][x] = shade
    grid[6][0] = grid[7][0] = grid[8][0] = '1'
    for x in range(12, 15):
        grid[4][x] = grid[10][x] = '1'
        for y, shade in zip(range(5, 10), '54332'):
            grid[y][x] = shade
    for y in range(5, 10):
        grid[y][15] = '1'
    grid[7][14] = grid[7][15] = '1'
    return [''.join(r) for r in grid]


BARREL = barrel_mask()

FIRING_LOCK = check('firing_lock', '''
................
................
..11............
.1kk1...........
..1451......11..
..1341.....1451.
..131......1341.
..1311111111311.
.13444444444441.
.13333333333321.
.12222222222221.
..122111111221..
...11......11...
................
................
................
''')

STOCK = check('stock', '''
................
................
................
................
................
.........111111.
........13444441
.......134333331
......1343222221
.....13432111111
....134321......
...134321.......
..134321........
..13221.........
...111..........
................
''')

CROWN = check('crown', '''
................
................
................
................
.....111111.....
....13444431....
...1344444431...
...1344444431...
...1334444331...
..113333333311..
.12222222222221.
1233333333333321
.11111111111111.
................
................
................
''')

COAT_PLATES = check('coat_plates', '''
................
................
...1111111111...
..134444444431..
..131444444131..
..133333333331..
..112222222211..
..134444444431..
..131444444131..
..133333333331..
..112222222211..
..134444444431..
..131444444131..
..133333333331..
...1111111111...
................
''')

SASH_WEAVE = check('sash_weave', '''
................
................
................
....aaaaaaaa....
...acdcdcdcdca..
..acbcbcbcbcbcaa
..adcdcdcdcdcdca
..acbcbcbcbcbcda
..adcdcdcdcdcdca
..abbbbbbbbbbbca
...aaaaaaaaaab.a
............abca
.............aba
..............a.
................
................
''')

BOOT_SOLES = check('boot_soles', '''
................
................
...111....111...
..14431..14431..
..13331..13331..
..13321..13321..
..13321..13321..
..13221..13221..
...1221...1221..
...1221...1221..
...13321..13321.
...133321.133321
...122221.122221
....11111..11111
................
................
''')

LINING = check('lining', '''
................
................
................
..aaaaaaaaaaaa..
..adddddddddca..
..adeedeedeeca..
..adddddddddca..
..acccccccccca..
..acdcdcdcdcba..
..acccccccccba..
..abbbbbbbbbba..
...aaaaaaaaaaa..
................
................
................
................
''')

# ---- Raw materials ----

DRIFTWOOD = check('driftwood', '''
................
................
.............11.
............1431
...........14321
..........1432..
....11...1432...
...1441.1432....
....11314321....
.......14321....
......14321.....
.....14321......
....14321.......
...1431.........
...111..........
................
''')

MARINE_STEEL_INGOT = check('marine_steel_ingot', '''
................
................
................
................
.......1111111..
......1455555411
.....14444444431
....144111444321
...1444414144321
..1444414144321.
..13333131333 21
..1222222222221.
...11111111111..
................
................
................
'''.replace(' ', '3'))

# The Shipwright's Manual (the guide book): a leather cover with crossed swords in gold, pages showing at the side.
MANUAL = check('shipwrights_manual', '''
..kkkkkkkkkkk...
..kMMMMMMMMMkk..
..kMLLLLLLLMkWk.
..kMLhLLLhLMkWk.
..kMLLhLhLLMkVk.
..kMLLLgLLLMkWk.
..kMLLhLhLLMkWk.
..kMLhLLLhLMkVk.
..kMgLLLLLgMkWk.
..kMLLLLLLLMkWk.
..kMLLLLLLLMkVk.
..kMLhhhhhLMkWk.
..kMLLLLLLLMkWk.
..kMMMMMMMMMkVk.
..kkkkkkkkkkkWk.
...kkkkkkkkkkk..
''')

# ---- Armor ----
# What each shell material looks like as clothing. Pirate materials make pirate clothes; Navy materials make
# Navy uniforms, so a full Navy set looks like the Marine disguise it is.
#   hat/hat2, coat/coat2, legs/legs2, boots/boots2 are colour ramps (darkest first); style picks the cut.


OUTFITS = {
    # A castaway's straw hat, red vest, denim shorts and sandals.
    'driftwood': {
        'style': 'castaway',
        'hat': ramp('5a4214', 'a07a2c', 'cfa64a', 'e8c86a', 'f7e3a0'),
        'hat2': ramp('3a0c0c', '7a1a16', 'b02a20', 'd8483a', 'f07a62'),
        'coat': ramp('3a0c0c', '7a1a16', 'a8261e', 'cc3d30', 'e8644e'),
        'coat2': ramp('8a5a3c', 'c48a64', 'e0a87e', 'f0c49a', 'fad8b4'),
        'legs': ramp('101a33', '2a3f6e', '3f5d9a', '5f80bf', '8aa6dc'),
        'legs2': ramp('3d2a16', '7a5a32', 'a6834d', 'c9aa6e', 'e6d39c'),
        'boots': ramp('2a180c', '5a3a22', '8a5c34', 'a87448', 'c89466'),
        'boots2': ramp('8a5a3c', 'c48a64', 'e0a87e', 'f0c49a', 'fad8b4'),
    },
    # A pirate captain's black tricorn, crimson coat with gold buttons, brown breeches and buckled boots.
    'ship_iron': {
        'style': 'captain',
        'hat': ramp('0c0a0e', '1d1a22', '2e2a35', '45404f', '615a6e'),
        'hat2': ramp('8a6418', 'a87e22', 'c99a32', 'e2b84a', 'f5d77a'),
        'coat': ramp('1e0707', '4a1010', '7a1d18', 'a3302a', 'c95446'),
        'coat2': ramp('8a6418', 'a87e22', 'c99a32', 'e2b84a', 'f5d77a'),
        'legs': ramp('1c140f', '3a2a1f', '554032', '6e5644', '8a7058'),
        'legs2': ramp('1e0707', '4a1010', '7a1d18', 'a3302a', 'c95446'),
        'boots': ramp('0c0a0e', '1d1a22', '2e2a35', '45404f', '615a6e'),
        'boots2': ramp('1f1f24', '4a4c53', '6f7279', '9a9ca2', 'c9cacd'),
    },
    # A Marine recruit's white cap with the blue band, white sailor top with the blue collar, blue trousers.
    'marine_steel': {
        'style': 'recruit',
        'hat': ramp('2a3040', '9aa6b8', 'c9d2de', 'e9eef4', 'ffffff'),
        'hat2': ramp('0f1a3a', '22408c', '3b62c4', '6d8fe0', 'a9c0f5'),
        'coat': ramp('2a3040', '9aa6b8', 'c9d2de', 'e9eef4', 'ffffff'),
        'coat2': ramp('0f1a3a', '22408c', '3b62c4', '6d8fe0', 'a9c0f5'),
        'legs': ramp('0f1a3a', '1d3270', '2a479a', '3d5fbc', '6d8fe0'),
        'legs2': ramp('0c0a0e', '1d1a22', '2e2a35', '45404f', '615a6e'),
        'boots': ramp('0c0a0e', '1d1a22', '2e2a35', '45404f', '615a6e'),
        'boots2': ramp('0c0a0e', '1d1a22', '2e2a35', '45404f', '615a6e'),
    },
    # A Navy officer's peaked cap and white "Justice" coat over a dark suit, with seastone fittings.
    'kairoseki': {
        'style': 'officer',
        'hat': ramp('2a3040', '9aa6b8', 'c9d2de', 'e9eef4', 'ffffff'),
        'hat2': ramp('10181a', '27393b', '3b5856', '5b857d', '9bd1c0'),
        'coat': ramp('2a3040', '9aa6b8', 'c9d2de', 'e9eef4', 'ffffff'),
        'coat2': ramp('11161c', '232c36', '36424f', '4c5a6a', '6a7a8c'),
        'legs': ramp('11161c', '232c36', '36424f', '4c5a6a', '6a7a8c'),
        'legs2': ramp('0c0a0e', '1d1a22', '2e2a35', '45404f', '615a6e'),
        'boots': ramp('0c0a0e', '1d1a22', '2e2a35', '45404f', '615a6e'),
        'boots2': ramp('10181a', '27393b', '3b5856', '5b857d', '9bd1c0'),
    },
}


def outfit(style, hat, hat2, coat, coat2, legs, legs2, boots, boots2):
    return {'style': style, 'hat': hat, 'hat2': hat2, 'coat': coat, 'coat2': coat2,
            'legs': legs, 'legs2': legs2, 'boots': boots, 'boots2': boots2}


def main_of(m):
    return MATERIALS[m]['main']


def cloth_of(m):
    return MATERIALS[m]['cloth']


SKIN = ramp('8a5a3c', 'c48a64', 'e0a87e', 'f0c49a', 'fad8b4')
BLACK = ramp('0c0a0e', '1d1a22', '2e2a35', '45404f', '615a6e')
BROWN = ramp('1c140f', '3a2a1f', '554032', '6e5644', '8a7058')
STRAW = OUTFITS['driftwood']['hat']
WHITE = OUTFITS['marine_steel']['coat']

OUTFITS.update({
    # A ship's gunner: olive coat with bronze buttons.
    'cannon_bronze': outfit('captain', BROWN, main_of('cannon_bronze'),
                            ramp('141a0e', '2c3a1c', '42552a', '5c733c', '7e9656'), main_of('cannon_bronze'),
                            BROWN, cloth_of('cannon_bronze'), BLACK, main_of('cannon_bronze')),
    # A Sea King hunter: a bone hat and a hide vest.
    'sea_king_bone': outfit('castaway', main_of('sea_king_bone'), cloth_of('sea_king_bone'),
                            cloth_of('sea_king_bone'), SKIN, BROWN, main_of('sea_king_bone'),
                            main_of('sea_king_bone'), SKIN),
    'sea_king_hide': outfit('captain', main_of('sea_king_hide'), main_of('sea_king_bone'),
                            main_of('sea_king_hide'), main_of('sea_king_bone'), BROWN, main_of('sea_king_hide'),
                            ramp('061412', '0f2a26', '1a4a42', '2b6e61', '45978a'), main_of('sea_king_bone')),
    # A treasure-drunk pirate lord in purple velvet and gold.
    'treasure_gold': outfit('captain', cloth_of('treasure_gold'), main_of('treasure_gold'),
                            cloth_of('treasure_gold'), main_of('treasure_gold'), BLACK, main_of('treasure_gold'),
                            BLACK, main_of('treasure_gold')),
    # A Wano samurai: a horned kabuto, a laced steel do with sode on the shoulders, kusazuri over grey hakama,
    # and suneate shin guards over white tabi. The plates are the steel itself, the lacing is the indigo silk.
    'tamahagane': outfit('samurai', main_of('tamahagane'), cloth_of('tamahagane'), main_of('tamahagane'),
                         cloth_of('tamahagane'), ramp('1a1a1f', '35353d', '4e4e58', '6a6a76', '8c8c99'),
                         cloth_of('tamahagane'), main_of('tamahagane'), cloth_of('tamahagane')),
    # Pacifista plating: gunmetal with hazard yellow, cut like a Marine uniform.
    'pacifista_alloy': outfit('recruit', main_of('pacifista_alloy'), cloth_of('pacifista_alloy'),
                              main_of('pacifista_alloy'), cloth_of('pacifista_alloy'),
                              ramp('0c0d10', '1d2025', '2c3037', '3e434c', '555b66'), BLACK, BLACK, BLACK),
    # A whaler's blue-grey leathers.
    'island_whale_leather': outfit('captain', main_of('island_whale_leather'), cloth_of('island_whale_leather'),
                                   main_of('island_whale_leather'), cloth_of('island_whale_leather'),
                                   cloth_of('island_whale_leather'), main_of('island_whale_leather'),
                                   cloth_of('island_whale_leather'), main_of('island_whale_leather')),
    # A Drum king's silver and fur.
    'wapometal': outfit('captain', main_of('wapometal'), cloth_of('wapometal'), main_of('wapometal'),
                        cloth_of('wapometal'), ramp('15121e', '2c2540', '433a5e', '5c5180', '7a6ea0'),
                        main_of('wapometal'), BLACK, main_of('wapometal')),
    # A shipwright's Adam wood hat, sailcloth shorts and wooden sandals.
    'adam_wood': outfit('castaway', main_of('adam_wood'), cloth_of('adam_wood'), main_of('adam_wood'), SKIN,
                        cloth_of('adam_wood'), main_of('adam_wood'), main_of('adam_wood'), SKIN),
    # A swordsman's black and crimson.
    'black_blade': outfit('captain', main_of('black_blade'), cloth_of('black_blade'), main_of('black_blade'),
                          cloth_of('black_blade'), BLACK, cloth_of('black_blade'), BLACK, cloth_of('black_blade')),
    # A Skypiean in a cloud-white cap, a sky-blue vest and shell-trimmed sandals.
    'dial_shell': outfit('castaway', WHITE, main_of('dial_shell'), cloth_of('dial_shell'), SKIN,
                         WHITE, cloth_of('dial_shell'), main_of('dial_shell'), SKIN),
    # A Fishman Island captain in coral red, studded with pearl.
    'sea_pearl': outfit('captain', main_of('sea_pearl'), cloth_of('sea_pearl'), cloth_of('sea_pearl'), main_of('sea_pearl'),
                        ramp('061420', '0e2a40', '1a4a6a', '2b6e94', '4a96bc'), main_of('sea_pearl'),
                        BLACK, main_of('sea_pearl')),
    # A Sabaody coater's resin-stiffened oilskins.
    'sabaody_resin': outfit('castaway', main_of('sabaody_resin'), cloth_of('sabaody_resin'), main_of('sabaody_resin'),
                            cloth_of('sabaody_resin'), cloth_of('sabaody_resin'), main_of('sabaody_resin'),
                            main_of('sabaody_resin'), SKIN),
    # A Shandian warrior's gold and green.
    'shandora_gold': outfit('castaway', main_of('shandora_gold'), cloth_of('shandora_gold'), cloth_of('shandora_gold'),
                            main_of('shandora_gold'), BROWN, main_of('shandora_gold'), main_of('shandora_gold'), SKIN),
    # A Wano swordsman in black steel armor laced with grey.
    'kokuto': outfit('samurai', main_of('kokuto'), cloth_of('kokuto'), main_of('kokuto'), cloth_of('kokuto'),
                     ramp('0a0a0c', '1a1a1f', '2a2a32', '3c3c46', '52525e'), cloth_of('kokuto'),
                     main_of('kokuto'), cloth_of('kokuto')),
    # A dragon hunter's red scale coat over charcoal leather.
    'dragon_scale': outfit('captain', main_of('dragon_scale'), cloth_of('dragon_scale'), main_of('dragon_scale'),
                           cloth_of('dragon_scale'), cloth_of('dragon_scale'), main_of('dragon_scale'),
                           cloth_of('dragon_scale'), main_of('dragon_scale')),
    # An Egghead scientist's white coat over a cyan suit.
    'vegapunk_compound': outfit('officer', WHITE, cloth_of('vegapunk_compound'), WHITE, main_of('vegapunk_compound'),
                                ramp('0e1a20', '1d3540', '2c5060', '3f6e80', '5a90a4'), BLACK, BLACK,
                                main_of('vegapunk_compound')) | {'mark': False},  # a lab coat, not a Justice coat
})

GOLD = ramp('5a400c', '8a6418', 'c99a32', 'e2b84a', 'f5d77a')
TEAL = ramp('10181a', '27393b', '3b5856', '5b857d', '9bd1c0')

# Armor icons: 1-5 = the piece's first ramp, a-e = its second ramp, p-t = gold, v-z = the shell's own metal,
# plus FIXED colours.
HATS = {
    'castaway': check('straw_hat', '''
................
................
................
................
......1111......
.....143441.....
....14434341....
....1bccccb1....
..113444344311..
.13443444434431.
1344434444344431
.12233333333221.
..112222222211..
....11111111....
................
................
'''),
    'captain': check('tricorn', '''
................
................
................
................
......1111......
....11333311....
...1333WWW331...
q..1333kWk331..q
rq.133W3V3W331qr
r33q133333331q3r
1333qqqqqqqq3331
.11333333333311.
...1111111111...
................
................
................
'''),
    'recruit': check('marine_cap', '''
................
................
................
................
................
.....111111.....
....15555541....
...1555555541...
...1544444441...
...1cccccccc1...
...1bebebebb1...
..113333333311..
...1111111111...
................
................
................
'''),
    'officer': check('officer_cap', '''
................
................
................
................
...1111111111...
..155555555551..
.14444444444441.
.11333333333311.
..1bbbbssbbbb1..
..1aaaarqaaaa1..
...kkkkkkkkkk...
....kkVkkkkk....
................
................
................
................
'''),
    'samurai': check('kabuto', '''
................
..p..........p..
..q..........q..
..rq..1111..qr..
...rq145541qr...
....1rq44qr1....
...144rssr331...
..133333333331..
.1cb1......1bc1.
.134b......b431.
1cbcb......bcbc1
13441......14431
1bcbc......cbcb1
.1111......1111.
................
................
'''),
}

FIXED['T'] = hexc('5b857d')  # seastone teal
FIXED['S'] = hexc('e0a87e')  # skin
FIXED['s'] = hexc('c48a64')  # skin in shadow

# Shapes for the other three pieces. Letters name regions; each style colours them. x is a seam.
COAT_SHAPE = check('coat', '''
................
..SSSC....CSSS..
.SSSCC....CCSSS.
SSSxLCCCCCCRxSSS
SSSxLLLPPRRRxSSS
SSSxLLLPPRRRxSSS
SSSxLLLPPRRRxSSS
SSSxLLLPPRRRxSSS
SUUxLLLPPRRRxUUS
SSSxLLLPPRRRxSSS
...xLLLPPRRRx...
...LLLLPPRRRR...
...LLLLPPRRRR...
...HHHHHHHHHH...
...HHHHHHHHHH...
................
''')

LEGS_SHAPE = check('legs', '''
................
...BBBBBBBBBB...
...TTTTTTTTTT...
...TTTTTTTTTT...
...LLLLTTRRRR...
...LLLL..RRRR...
...LLLL..RRRR...
...LLLL..RRRR...
...LLLL..RRRR...
...LLLL..RRRR...
...LLLL..RRRR...
...LLLL..RRRR...
...LLLL..RRRR...
...KKKK..KKKK...
...KKKK..KKKK...
................
''')

BOOTS_SHAPE = check('boots', '''
................
................
................
..UUUU....UUUU..
..BBBB....BBBB..
..BBBB....BBBB..
..BBBB....BBBB..
..BBBB....BBBB..
..BBBB....BBBB..
..BBBB....BBBB..
..BBBB....BBBB..
..BBBBB...BBBBB.
..BBBBBB..BBBBBB
..SSSSSS..SSSSSS
................
................
''')


def lit(x, y, light='4', mid='3', dark='2'):
    """Shade for a piece of cloth lit from the upper left."""
    return light if x <= 5 and y <= 4 else dark if x >= 10 or y >= 12 else mid


def coat_cell(style, r, x, y):
    if r == 'x':
        return '1'
    if style == 'castaway':  # an open, sleeveless vest over a bare chest
        if r in 'SUC':
            return '.'
        if r == 'P' or (r in 'LR' and x in (6, 9)) or (r == 'C'):
            return 'd' if x < 8 else 'c'
        return lit(x, y)
    if style == 'captain':
        if r in 'UC' or r == 'H':
            return 'r' if r == 'H' else 's' if x < 8 else 'r'
        if r == 'P':
            return 's' if y in (5, 8, 11) else '2'
        return lit(x, y)
    if style == 'recruit':  # the blue collar comes down in a V
        if r in 'CU':
            return 'c'
        if r == 'P':
            return 'b' if y <= 5 else '3'
        return lit(x, y, '5', '4', '3')
    if style == 'officer':  # the white coat hangs open over a dark suit and tie
        if r == 'S' and y <= 2:
            return 's' if x < 8 else 'r'
        if r == 'U':
            return 'r'
        if r == 'P':
            return 'T' if y <= 9 else 'b'
        if r in 'LR' and x in (6, 9):
            return 'c' if x < 8 else 'b'
        return lit(x, y, '5', '4', '3')
    if style == 'samurai':  # a do of laced steel plates, sode on the shoulders, kusazuri at the hem
        if r == 'C':
            return 'c'
        if r == 'U':
            return 'b'
        if r == 'S':
            if y >= 8:
                return 'c' if x < 8 else 'b'  # the kimono sleeve below the sode
            return ('c' if x % 2 else 'b') if y % 2 == 0 else lit(x, y)
        if r == 'H':
            return '1' if x % 3 == 0 else 'c' if y == 13 else '3'
        if y % 3 == 0:
            return 'c' if x % 2 else 'b'  # a row of lacing between plates
        if r == 'P' and y in (4, 5):
            return 'q'  # the clan crest
        return lit(x, y, '5' if y % 3 == 1 else '4', '4' if y % 3 == 1 else '3', '3' if y % 3 == 1 else '2')
    raise ValueError(style)


def legs_cell(style, r, x, y):
    if style == 'castaway':  # rolled-up shorts with a rope belt
        if y >= 9:
            return '.'
        if r == 'B':
            return 'd' if x % 2 else 'c'
        return lit(x, y) if y < 8 else '4'
    if style == 'samurai':  # kusazuri plates over the hips, then wide grey hakama
        if r == 'B':
            return 'b' if x % 2 else 'c'  # the obi
        if y <= 5 and r != '.':
            if x % 3 == 2:
                return 'v'
            return 'c' if y == 4 else 'y' if y == 2 else 'x'
        if r == 'K':
            return '2'
        return '2' if x in (5, 10) else lit(x, y)  # the pleats
    if r == 'B':
        if style == 'officer' and x in (7, 8):
            return 's'
        if style == 'captain':
            return 'c' if x < 8 else 'b'
        return 'b'
    if style == 'captain' and r in 'TR' and x == 11 and y <= 7:
        return 'b'  # the end of the sash hangs down the right hip
    if r == 'K':
        return '2'
    return lit(x, y)


def boots_cell(style, r, x, y):
    if style == 'castaway':  # sandals: bare feet, straps and a sole
        if y < 9:
            return '.'
        if r == 'S':
            return '2'
        return '3' if y in (9, 11) else 'd' if x < 8 else 'c'
    if style == 'samurai':  # suneate splints tied over the shin, white tabi
        if r == 'U':
            return 'c'
        if r == 'S':
            return 'M'  # a straw sandal
        if y >= 10:
            return 'c' if y == 11 and x in (4, 12) else 'W' if y == 10 or x in (3, 4, 10, 11) else 'V'
        if y in (5, 8):
            return 'b'
        return '4' if x in (3, 11) else '3' if x in (4, 12) else '2'
    if r == 'U':
        return '5' if style == 'captain' else '4'
    if r == 'S':
        return '1'
    if style == 'captain' and y == 6:
        return 'd' if x in (3, 11) else 'b'  # iron buckles
    if style == 'officer' and y >= 11 and r == 'B':
        return 'd' if x < 8 else 'c'  # seastone toe caps
    return lit(x, y, '4', '3', '2')


def shape_mask(shape, cell, style):
    rows = [[cell(style, ch, x, y) if ch != '.' else '.' for x, ch in enumerate(row)] for y, row in enumerate(shape)]
    out = []
    for y, row in enumerate(rows):
        line = ''
        for x, ch in enumerate(row):
            if ch == '.':
                line += '.'
                continue
            edge = x in (0, 15) or y in (0, 15) or '.' in (rows[y - 1][x], rows[y + 1][x], row[x - 1], row[x + 1])
            line += outline_of(ch) if edge else ch
        out.append(line)
    return out


def outline_of(ch):
    if ch in '12345':
        return '1'
    if ch in 'vwxyz':
        return 'v'
    if ch in 'abcde':
        return 'a'
    if ch in 'pqrst':
        return 'p'
    return 'k'


def paint_outfit(rows, first, second, metal=None):
    img = blank()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            if ch in '12345':
                c = first['12345'.index(ch)]
            elif ch in 'abcde':
                c = second['abcde'.index(ch)]
            elif ch in 'pqrst':
                c = GOLD['pqrst'.index(ch)]
            elif ch in 'vwxyz':
                c = metal['vwxyz'.index(ch)]
            else:
                c = FIXED[ch]
            img.putpixel((x, y), c)
    return img


def armor_icons(material):
    o = OUTFITS[material]
    st = o['style']
    return {
        'hat': paint_outfit(HATS[st], o['hat'], o['hat2']),
        'coat': paint_outfit(shape_mask(COAT_SHAPE, coat_cell, st), o['coat'], o['coat2']),
        'sash': paint_outfit(shape_mask(LEGS_SHAPE, legs_cell, st), o['legs'], o['legs2'], main_of(material)),
        'boots': paint_outfit(shape_mask(BOOTS_SHAPE, boots_cell, st), o['boots'], o['boots2']),
    }


# ---- Worn armor (the 64x32 armor layers, laid out like vanilla's) ----

def faces(u, v, w, h, d):
    """Where each face of a model box sits on the texture: (x, y, width, height)."""
    return {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h),
    }


HEAD = faces(0, 0, 8, 8, 8)
BODY = faces(16, 16, 8, 12, 4)
ARM = faces(40, 16, 4, 12, 4)
LEG = faces(0, 16, 4, 12, 4)
FACE_LIGHT = {'top': 1, 'front': 0, 'right': 0, 'left': -1, 'back': -1, 'bottom': -2}


def grain(x, y):
    """A little per-pixel wobble so cloth isn't one flat colour."""
    n = (x * 374761393 + y * 668265263) & 0xffffffff
    n = ((n ^ (n >> 13)) * 1274126177) & 0xffffffff
    n = (n ^ (n >> 16)) & 0xff
    return -1 if n < 30 else 1 if n > 235 else 0


def paint_box(img, box, fn):
    for face, (fx, fy, fw, fh) in box.items():
        for y in range(fh):
            for x in range(fw):
                c = fn(face, x, y, fw, fh)
                if c is None:
                    continue
                if isinstance(c, tuple) and len(c) == 2:
                    r, i = c
                    i = max(0, min(4, i + FACE_LIGHT[face] + grain(fx + x, fy + y)))
                    c = r[i]
                img.putpixel((fx + x, fy + y), c)


# 正義 ("Justice") down the back of the officer's coat, one character above the other.
JUSTICE = [  # starts a row down, clear of the collar
    '.......',
    '.#####.',
    '...#...',
    '...###.',
    '...#...',
    '.#####.',
    '..#.#..',
    '.#####.',
    '...#...',
    '.#####.',
    '..###..',
    '.#.#.#.',
]


def worn(material):
    o = OUTFITS[material]
    st = o['style']
    hat, hat2, coat, coat2 = o['hat'], o['hat2'], o['coat'], o['coat2']
    legs, legs2, boots, boots2 = o['legs'], o['legs2'], o['boots'], o['boots2']
    metal = MATERIALS[material]['main']
    white, black = FIXED['W'], FIXED['k']
    layer1 = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    layer2 = Image.new('RGBA', (64, 32), (0, 0, 0, 0))

    def head(face, x, y, w, h):
        # The front stops at the brim (row 3) so the eyes (row 4) show; the sides and back come down further.
        if face == 'bottom' or (face == 'front' and y > 3):
            return None
        if face == 'top':
            if st == 'castaway':
                return (hat, 3 if (x + y) % 3 else 2)
            if st == 'recruit' and x in (3, 4) and y in (3, 4):
                return (hat2, 2)
            if st == 'samurai':  # a ridged bowl round a gilt ring
                return GOLD[2] if x in (3, 4) and y in (3, 4) else (hat, 3 if x % 2 else 2)
            return (hat, 3)
        if st == 'castaway':
            if face == 'front':
                return (hat2, 2) if y == 2 else (hat, 2) if y == 3 else (hat, 3 if (x + y) % 2 else 2)
            if y <= 3:
                return (hat, 3 if (x + y) % 2 else 2)
            return (hat2, 2) if y == 4 else (hat, 2) if y == 5 else None
        if st == 'captain':
            if face == 'front':
                if y == 3:
                    return (hat2, 3)
                if x in (3, 4) and y <= 2:
                    return black if y == 1 else white
                return (hat, 2)
            if y == 4:
                return (hat2, 3)
            return (hat, 2) if y <= 5 else None
        if st == 'recruit':
            if face == 'front':
                if y <= 1:
                    return (hat, 3)
                return white if y == 2 and x % 2 == 1 and 0 < x < 7 else (hat2, 2)
            if y <= 2:
                return (hat, 3)
            return (hat2, 2) if y <= 4 else None
        if st == 'samurai':
            if face == 'front':  # the crescent crest rises from the brim
                if (y, x) in ((0, 0), (0, 7), (1, 1), (1, 6)) or (y == 2 and 2 <= x <= 5):
                    return GOLD[4] if y < 2 or x in (3, 4) else GOLD[3]
                if y == 3:
                    return (hat, 1)  # the brim
                return (hat, 3 if x % 2 else 2)
            if y <= 2:
                return (hat, 3 if x % 2 else 2)
            if y == 3:
                return (hat, 1)
            # the shikoro: plates and lacing hanging down over the neck
            return (hat2, 2 if x % 2 else 1) if y % 2 else (hat, 2)
        if st == 'officer':
            if face == 'front':
                if y == 0:
                    return (hat, 3)
                if y <= 2:
                    return GOLD[3] if x in (3, 4) else (hat2, 2)
                return FIXED['V'] if x == 2 else black  # the visor
            if y <= 2:
                return (hat, 3)
            return (hat2, 2) if y <= 4 else None

    def body(face, x, y, w, h):
        if face == 'bottom':
            return None
        if st == 'castaway':
            if face == 'front' and 2 <= x <= 5:
                return None
            return (coat, 2 if y >= 10 else 3)
        if st == 'captain':
            if face == 'front':
                if y == 0:
                    return (coat2, 3)
                if y == 9:
                    return GOLD[3] if x in (3, 4) else FIXED['L']
                if x in (3, 4) and y in (2, 5, 8):
                    return GOLD[4] if x == 3 else GOLD[3]
                if x in (3, 4):
                    return (coat, 2)
            if face in ('right', 'left', 'back') and y == 9:
                return FIXED['L']
            if face == 'back' and x in (3, 4) and y >= 10:
                return (coat, 1)
            return (coat, 3)
        if st == 'recruit':
            if face == 'front':
                if (y == 0 and x not in (3, 4)) or (y == 1 and x in (1, 2, 5, 6)) or (y == 2 and x in (2, 5)) or (y == 3 and x in (3, 4)):
                    return (coat2, 2)
            if face == 'back' and y <= 4:
                return white if y == 3 else (coat2, 2)
            if face in ('right', 'left') and y <= 1:
                return (coat2, 2)
            if face == 'top':
                return (coat2, 2) if y <= 1 or x in (0, 7) else (coat, 3)
            return (coat, 3)
        if st == 'officer':
            if face == 'front' and 2 <= x <= 5:
                if x in (3, 4):
                    return white if y == 0 else FIXED['T'] if y <= 6 else (coat2, 2)
                return (coat2, 2)
            if face == 'back' and o.get('mark', True) and x < 7 and JUSTICE[y][x] == '#':
                return black
            return (coat, 3)
        if st == 'samurai':
            if face == 'top':
                return (coat2, 2) if x in (2, 5) else (coat, 3)  # the shoulder cords
            if y == 0:
                return (coat2, 2)
            if y == 9:
                return (coat2, 1)  # the obi
            if y >= 10:  # the kusazuri, split into hanging plates
                return (coat, 1) if face in ('front', 'back') and x % 3 == 2 else (coat, 3 if y == 10 else 2)
            if face == 'back' and x in (3, 4) and y in (4, 5):
                return (coat2, 4)  # the agemaki knot
            if y % 3 == 0:
                return (coat2, 2 if x % 2 else 1)
            if face == 'front' and x in (3, 4) and y == 4:
                return GOLD[3]  # the clan crest
            return (coat, 3 if y % 3 == 1 else 2)

    def arm(face, x, y, w, h):
        if face == 'bottom' or st == 'castaway':
            return None
        if st == 'captain':
            if face == 'top' or (y <= 1 and face != 'bottom'):
                return (MATERIALS[material]['main'], 3)  # iron pauldrons, from the shell's own metal
            if y >= 9:
                return (coat2, 3 if y == 10 else 2)
            return (coat, 3)
        if st == 'recruit':
            return (coat2, 2) if y >= 10 else (coat, 3)
        if st == 'samurai':
            if face == 'top':
                return (coat, 2)
            if y <= 5:  # the sode, a laced shoulder board
                return (coat2, 2 if x % 2 else 1) if y % 2 else (coat, 3 if y == 0 else 2)
            if y <= 8:
                return (coat2, 2)  # the sleeve
            return (coat2, 1) if y == 9 else (coat, 3 if x % 2 else 2)  # the kote
        if st == 'officer':
            if face == 'top' or y == 0:
                return GOLD[3] if (x + y) % 2 == 0 else GOLD[2]
            if y == 1 and face != 'top':
                return GOLD[2] if x % 2 == 0 else None  # epaulette fringe
            return (coat, 3)

    def boot(face, x, y, w, h):
        if face == 'top':
            return None
        if face == 'bottom':
            return (boots, 1)
        if st == 'castaway':
            if y == 11:
                return (boots, 2)
            if y == 9 and face in ('front', 'right', 'left'):
                return (boots, 3)
            return None
        if st == 'captain':
            if y < 5:
                return None
            if y == 5:
                return (legs, 4)  # the folded-down cuff
            if y == 8 and face == 'front' and x in (1, 2):
                return (boots2, 4)
            return (boots, 2)
        if st == 'recruit':
            return None if y < 8 else (boots, 3 if y == 8 else 2)
        if st == 'samurai':
            if y < 5:
                return None
            if y >= 10:
                return FIXED['W'] if y == 10 else FIXED['V']  # tabi
            if y in (5, 8):
                return (boots2, 2)  # the ties
            return (boots, 4 if face == 'front' and x in (1, 2) else 3 if x % 2 else 2)
        if st == 'officer':
            if y < 7:
                return None
            if face == 'front' and y >= 10:
                return (boots2, 3)
            if face == 'front' and x == 1 and y == 8:
                return (boots, 4)
            return (boots, 2)

    def waist(face, x, y, w, h):
        if face in ('top', 'bottom') or y < 8:
            return None
        if y == 8:
            if st == 'officer' and face == 'front' and x in (3, 4):
                return GOLD[3]
            return (legs2, 3 if st == 'castaway' and x % 2 else 2)
        if st == 'samurai' and y >= 9:  # the kusazuri, split into hanging plates
            if face in ('front', 'back') and x % 3 == 2:
                return (metal, 1)
            return (legs2, 3 if x % 2 else 2) if y == 10 else (metal, 3 if y == 9 else 2)
        if st == 'captain' and y == 9:
            return (legs2, 2)
        if st == 'captain' and face == 'front' and x == 6:
            return (legs2, 2)  # the end of the sash
        return (legs, 3)

    def leg(face, x, y, w, h):
        if face in ('top',):
            return None
        if face == 'bottom':
            return None if st == 'castaway' else (legs, 1)
        if st == 'castaway':
            return None if y > 5 else (legs, 4 if y == 5 else 3)
        if st == 'officer' and face == 'front' and x == 1:
            return (legs, 4)  # pressed crease
        if st == 'samurai':
            if face == 'front' and y <= 3:  # the haidate, a laced apron over the thighs
                return (metal, 3) if y % 2 == 0 else (legs2, 3 if x % 2 else 2)
            return (legs, 2 if x % 2 else 3)  # hakama pleats
        return (legs, 3)

    paint_box(layer1, HEAD, head)
    paint_box(layer1, BODY, body)
    paint_box(layer1, ARM, arm)
    paint_box(layer1, LEG, boot)
    paint_box(layer2, BODY, waist)
    paint_box(layer2, LEG, leg)
    return layer1, layer2


# ---- Raw materials added with the bigger material roster ----

INGOT = check('ingot', '''
................
................
................
................
.......1111111..
......1455555411
.....14444444431
....144444444321
...1444444444321
..1444444444321.
..1333333333321.
..1222222222221.
...11111111111..
................
................
................
''')

SEA_KING_BONE = check('sea_king_bone', '''
................
...........111..
..........14451.
.........1445541
.........1454431
........1454331.
.......145431...
......145431....
.....145431.....
....145431......
.114543 1.......
1455431.........
1454431.........
.1443321........
..1331..........
...11...........
'''.replace(' ', '3'))

HIDE = check('hide', '''
................
................
...11.....11....
..14411111441...
..1444444444431.
...14333333331..
...1d3d3d3d3d31.
..14333333333331
..13333333333321
..1d3d3d3d3d3d21
...1333333333321
...1222222222221
..12221111112221
..111......1111.
................
................
''')

ALLOY_PLATE = check('alloy_plate', '''
................
................
..111111111111..
..152444444251..
..144444444441..
..1cc33cc33cc1..
..1c33cc33cc31..
..133cc33cc331..
..1cc33cc33cc1..
..144444444441..
..144444444441..
..133333333331..
..152222222251..
..111111111111..
................
................
''')

VIAL = check('vial', '''
................
......aaaa......
......bdcb......
......aaaa......
.......VV.......
.......kk.......
......kWVk......
.....kW445k.....
....kW44443k....
....k444433k....
....k443333k....
....k433332k....
....k333222k....
.....k2222k.....
......kkkk......
................
''')

# ---- New parts ----

LONG_BARREL = check('long_barrel', '''
..............11
.............141
............1431
...........14321
..........11321.
.........14211..
........14321...
.......14321....
......14321.....
.....11321......
....14211.......
...14321........
..14321.........
.14321..........
1221............
111.............
''')

CANNON_BORE = check('cannon_bore', '''
................
................
................
..........1111..
.1111111111441..
1144444444445411
1433343333433431
1433343333433431
1322232222322321
1222222222222221
1122222222221211
.11111111111111.
..........1111..
................
................
................
''')

FUSE = check('fuse', '''
................
............hW..
...........hgh..
..........ac....
.........ad.....
...aaaa.ac......
..adddca.a......
.adcaaacda......
.acaddcaca......
.acadcaaca......
.acaaaacda......
..adddddca......
...acccca.......
....aaaa........
................
................
''')

GUN_CARRIAGE = check('gun_carriage', '''
................
................
................
................
................
................
..1111....1111..
..1441111114411.
..144444444441..
..133333333331..
..122222222221..
..1kk111111kk1..
.1k55k1..1k55k1.
.1k53k1..1k53k1.
..1kk1....1kk1..
................
''')

# Cannon ammo: an iron-banded ball with its fuse cap.
CANNON_SHELL = check('cannon_shell', '''
................
................
........W.......
.......hg.......
......1111......
....11455411....
...1455444431...
...1454444331...
..144444433321..
..143444333221..
..133333332221..
...1333322221...
...1222222221...
....11222211....
......1111......
................
''')

CUTLASS_BLADE = check('cutlass_blade', '''
..............11
.............141
............1451
...........13451
..........13451.
.........1345...
........1345....
.......1345.....
......135.......
.....242........
....12..........
...12...........
..11............
................
................
................
''')

SABRE_BLADE = check('sabre_blade', '''
...............1
..............15
.............145
............145.
...........145..
..........145...
.........135....
........135.....
.......135......
......135.......
.....242........
....12..........
...12...........
..11............
................
................
''')

AXE_HEAD = check('axe_head', '''
................
...111..........
..15441.........
.1544431........
.15444331111111.
154443333333k31.
154433222222k21.
154432222222k21.
154432211111111.
.1543221........
.154321.........
..15421.........
...1531.........
....11..........
................
................
''')

JITTE_ROD = check('jitte_rod', '''
..............11
.............141
............1431
...........1431.
..........1431..
.........1431...
........1431....
.......1431.....
......1431......
.....1431.11....
....1431.141....
...14311431.....
..1431111.......
..131...........
...1............
................
''')

CLUB_HEAD = check('club_head', '''
................
..........111...
.........14541..
........1445441.
.......15444531.
......144454321.
.....1544453321.
....14454433221.
...154443322211.
..1445433221....
..154433221.....
..14532321......
..1433221.......
...13221........
....111.........
................
''')

SPEARHEAD = check('spearhead', '''
................
.............11.
............1451
...........14541
..........145431
.........145431.
........145431..
.......145431...
........1431....
.......1221.....
......1331......
.....1221.......
....1331........
....111.........
................
................
''')

HAND_GUARD = check('hand_guard', '''
................
................
.......111......
......14441.....
.....143 41.....
....14311341....
...1431..1431...
...131....131...
...131....131...
...131....131...
...1331..1331...
....13311331....
.....132231.....
......1111......
................
................
'''.replace(' ', '3'))

BINDING = check('binding', '''
................
................
................
....aaaaaaaa....
...adcdcdcdcda..
..adcbcbcbcbcda.
..acdcdcdcdcdca.
..abcbcbcbcbcba.
..acdcdcdcdcdca.
..abcbcbcbcbcba.
..acdcdcdcdcdca.
...abbbbbbbbba..
....aaaaaaaa....
................
................
................
''')

STUDS = check('studs', '''
................
................
...11......11...
..1451....1451..
.145431..145431.
.143221..143221.
..1221....1221..
...11..11..11...
......1451......
.....145431.....
.....143221.....
......1221......
.......11.......
................
................
................
''')

GRIP = check('grip', '''
................
................
................
................
...........11...
..........1441..
.........ae331..
........adcb1...
.......aedb.....
......adcb......
.....aedb.......
....1433........
....1321........
.....11.........
................
................
''')

HAFT = check('haft', '''
..............11
.............141
............1431
...........1431.
..........1431..
.........1431...
........1431....
.......1431.....
......1431......
.....1431.......
....1431........
...1431.........
..1431..........
.1431...........
1221............
111.............
''')


# Hand weapons: head (1-5), middle part (p-t, F-J), handle (v-z, A-E).

CUTLASS = check('cutlass', '''
..............11
.............141
............1451
...........13451
..........13451.
.........1345...
........1345....
.......1345.....
...s..135.......
...qs242........
....qr..........
...BDqr.........
..BC..qr........
.BD..qp.........
BC..qp..........
vwqp............
''')

MARINE_SABRE = check('marine_sabre', '''
...............1
..............15
.............145
............145.
...........145..
..........145...
.........135....
........135.....
..rs...135......
..qrs.242.......
...qrst.........
...BDqr.........
..BC..qr........
.BD..qp.........
BC..qp..........
vwqp............
''')

BOARDING_AXE = check('boarding_axe', '''
.....1111.......
....154441......
...15444431.....
...1544433IG....
...154431IHF....
....1541HGF.....
.....11xwF......
......xw........
.....xw.........
....xw..........
...xw...........
..xw............
.xw.............
xw..............
w...............
................
''')

JITTE = check('jitte', '''
..............11
.............141
............1431
...........1431.
..........1431..
.........1431...
........1431....
.......1431.....
......1431.11...
.....1431.141...
....1431I1431...
....HIGI1331....
...BHIHG111.....
..BDGH..........
.BC.............
vw..............
''')

KANABO = check('kanabo', '''
..........1111..
.........1s4s41.
........14444t31
.......1s4s43s31
......144443t321
.....1s4s43s3321
....1444432t3221
....1s4s3s32221.
.....144332211..
.....xw1111.....
....xw..........
...xw...........
..xw............
.xw.............
yw..............
v...............
''')

SPEAR = check('spear', '''
.............11.
............1451
...........14541
..........145431
.........145431.
........145431..
.......14531....
.......HIG1.....
......HIGx......
.....xw.........
....xw..........
...xw...........
..xw............
.xw.............
xw..............
w...............
''')

# The Ship Cannon item: bore (1-5), fuse (F-J and its gold match), gun carriage (v-z).

SHIP_CANNON = check('ship_cannon', '''
................
................
...hW...........
...gh...........
....F.......111.
...1G1111111141.
..14444444444451
.143343333334331
1133333333333331
.122222222222221
..11222222222121
...11111111.111.
..vwwwwwwwwv....
.vyyyyyyyyyyv...
.vxkkxxxxkkxv...
..kk.....kk.....
''')

# Guns: barrel (1-5), lock or fuse (p-t, F-J), stock (v-z).

MUSKET = check('musket', '''
................
................
................
................
................
.....pp.........
......qp11111111
vvvv.psr44444444
vyyxvprq33333332
vyxxxxpp11111111
vxxwvq..........
vwvv............
vv..............
................
................
................
''')

HAND_CANNON = check('hand_cannon', '''
................
................
..hW............
..gh............
...F.........11.
...G111111111441
..14444444444451
.114333333333431
.113333333333331
..12222222222321
..11222222222221
.vx1111111111441
.vyxv.......11..
vyxv............
vxv.............
vv..............
''')


# ---- Navy stations (blocks): Marine-white and blue, with a Jolly Roger daubed on once a pirate captures one ----

PAINT = ramp('6e7682', '9aa3b0', 'c4ccd6', 'e2e7ed', 'f6f8fa')
NAVY = ramp('0f1a3a', '1d3270', '2a479a', '3d5fbc', '6d8fe0')
STEEL = MATERIALS['marine_steel']['main']
DARK_WOOD = ramp('1a100a', '2e1d12', '432c1c', '5a3d28', '735036')
GULL = [  # the Marine seagull
    '.#......#.',
    '##.####.##',
    '.########.',
    '...####...',
    '....##....',
]
JOLLY_ROGER = [  # a white skull and crossbones on black
    '##########',
    '#W######W#',
    '##W.WW.W##',
    '###WWWW###',
    '###W.W.W##',
    '####WWW###',
    '##W#####W#',
    '#W#######W',
    '##########',
]


def block():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 255))


def fill(img, x0, y0, x1, y1, r, base):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            img.putpixel((x, y), r[max(0, min(4, base + grain(x * 3 + y, y * 5 + x)))])


def planks(img, r, y0=0, y1=15):
    """Horizontal boards, 4 pixels high, with dark seams and staggered ends."""
    fill(img, 0, y0, 15, y1, r, 2)
    for y in range(y0, y1 + 1):
        if (y - y0) % 4 == 3:
            for x in range(16):
                img.putpixel((x, y), r[1])
        else:
            img.putpixel(((y // 4) * 7 % 16, y), r[1])


def bricks(img, r):
    fill(img, 0, 0, 15, 15, r, 3)
    for y in range(16):
        for x in range(16):
            if y % 4 == 3 or (x + (4 if (y // 4) % 2 else 0)) % 8 == 7:
                img.putpixel((x, y), r[1])


def stamp(img, pattern, x0, y0, colour, others=None):
    for y, row in enumerate(pattern):
        for x, ch in enumerate(row):
            if ch == '#':
                img.putpixel((x0 + x, y0 + y), colour)
            elif ch != '.' and others:
                img.putpixel((x0 + x, y0 + y), others[ch])


def jolly_roger(img, x0, y0):
    stamp(img, JOLLY_ROGER, x0, y0, FIXED['k'], {'W': FIXED['W']})


def stations():
    """Each station's faces, as {texture name: image}. '_captured' versions carry the pirates' flag."""
    out = {}
    for captured in (False, True):
        tag = '_captured' if captured else ''
        # Marine Armory Bench: a riveted steel top on a white-painted bench with a blue stripe, tools on the front.
        top = block()
        fill(top, 0, 0, 15, 15, STEEL, 3)
        for t in range(16):
            for x, y in ((t, 0), (t, 15), (0, t), (15, t)):
                top.putpixel((x, y), STEEL[1])
        for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
            top.putpixel((x, y), STEEL[4])
        if captured:
            jolly_roger(top, 3, 3)
        else:
            stamp(top, GULL, 3, 5, NAVY[2])
        out[f'marine_armory_bench_top{tag}'] = top
        side = block()
        planks(side, PAINT, 3, 15)
        fill(side, 0, 0, 15, 2, STEEL, 2)
        for x in range(16):
            side.putpixel((x, 7), NAVY[2])
            side.putpixel((x, 8), NAVY[1])
        if captured:
            jolly_roger(side, 3, 5)
        out[f'marine_armory_bench_side{tag}'] = side
        front = side.copy()
        if not captured:
            for y in range(4, 12):  # a hammer and tongs hanging on the front
                front.putpixel((4, y), DARK_WOOD[3])
                front.putpixel((11, y), STEEL[1])
            for x in range(2, 7):
                front.putpixel((x, 4), STEEL[1])
                front.putpixel((x, 5), STEEL[2])
            front.putpixel((10, 12), STEEL[1])
            front.putpixel((12, 12), STEEL[1])
        out[f'marine_armory_bench_front{tag}'] = front
        # Quartermaster's Desk: a dark wood desk with the supply ledger and the Navy's stamp on top.
        top = block()
        planks(top, DARK_WOOD)
        fill(top, 3, 2, 12, 13, PAINT, 4)
        for y in range(4, 12, 2):
            for x in range(5, 11):
                top.putpixel((x, y), PAINT[1])
        if captured:
            stamp(top, ['#.#', '.#.', '#.#'], 9, 9, FIXED['k'])  # crossed out
            stamp(top, ['.##.', '#..#', '.##.', '#..#'], 4, 9, FIXED['k'])
        else:
            stamp(top, ['.##.', '####', '.##.'], 9, 10, NAVY[2])
        out[f'quartermasters_desk_top{tag}'] = top
        side = block()
        planks(side, DARK_WOOD, 2, 15)
        fill(side, 0, 0, 15, 1, NAVY, 2)
        if captured:
            jolly_roger(side, 3, 4)
        out[f'quartermasters_desk_side{tag}'] = side
        front = side.copy()
        if not captured:
            for x0 in (2, 9):  # two drawers with gold pulls
                for x in range(x0, x0 + 5):
                    front.putpixel((x, 5), DARK_WOOD[0])
                    front.putpixel((x, 10), DARK_WOOD[0])
                for y in range(5, 11):
                    front.putpixel((x0, y), DARK_WOOD[0])
                    front.putpixel((x0 + 4, y), DARK_WOOD[0])
                front.putpixel((x0 + 2, 7), GOLD[3])
                front.putpixel((x0 + 2, 8), GOLD[2])
        out[f'quartermasters_desk_front{tag}'] = front
        # Marine Base Smeltery: white stone brick with a blue band, an iron grate on top, a furnace mouth in front.
        side = block()
        bricks(side, PAINT)
        fill(side, 0, 3, 15, 4, NAVY, 2)
        if captured:
            jolly_roger(side, 3, 6)
        out[f'marine_base_smeltery_side{tag}'] = side
        front = side.copy()
        if not captured:
            fill(front, 3, 7, 12, 14, STEEL, 1)
            fill(front, 4, 8, 11, 14, BLACK, 0)
            for x in range(4, 12, 2):
                for y in range(8, 15):
                    front.putpixel((x, y), STEEL[2])
        out[f'marine_base_smeltery_front{tag}'] = front
        top = block()
        fill(top, 0, 0, 15, 15, STEEL, 2)
        for y in range(2, 14, 3):
            for x in range(2, 14):
                top.putpixel((x, y), FIXED['k'])
        out[f'marine_base_smeltery_top{tag}'] = top
    bottom = block()
    planks(bottom, DARK_WOOD)
    out['navy_station_bottom'] = bottom
    return out


# ---- Write everything ----

import json

PARTS = {
    'katana_blade': KATANA_BLADE, 'tsuba': TSUBA, 'hilt': HILT,
    'barrel': BARREL, 'firing_lock': FIRING_LOCK, 'stock': STOCK,
    'long_barrel': LONG_BARREL, 'cannon_bore': CANNON_BORE, 'fuse': FUSE,
    'gun_carriage': GUN_CARRIAGE, 'cannon_shell': CANNON_SHELL,
    'cutlass_blade': CUTLASS_BLADE, 'sabre_blade': SABRE_BLADE, 'axe_head': AXE_HEAD, 'jitte_rod': JITTE_ROD,
    'club_head': CLUB_HEAD, 'spearhead': SPEARHEAD, 'hand_guard': HAND_GUARD, 'binding': BINDING, 'studs': STUDS,
    'grip': GRIP, 'haft': HAFT,
}
ARMOR_PARTS = {'crown': CROWN, 'coat_plates': COAT_PLATES, 'sash_weave': SASH_WEAVE, 'boot_soles': BOOT_SOLES,
               'lining': LINING}
# Each weapon's layers, in the order its parts are saved on the item (katana_parts, gun_parts, weapon_parts).
WEAPONS = {
    'katana': (KATANA, ['blade', 'tsuba', 'hilt']),
    'flintlock': (FLINTLOCK, ['barrel', 'lock', 'stock']),
    'musket': (MUSKET, ['barrel', 'lock', 'stock']),
    'hand_cannon': (HAND_CANNON, ['bore', 'fuse', 'stock']),
    'ship_cannon': (SHIP_CANNON, ['bore', 'fuse', 'carriage']),
    'cutlass': (CUTLASS, ['blade', 'guard', 'grip']),
    'marine_sabre': (MARINE_SABRE, ['blade', 'guard', 'grip']),
    'boarding_axe': (BOARDING_AXE, ['head', 'binding', 'haft']),
    'jitte': (JITTE, ['rod', 'binding', 'grip']),
    'kanabo': (KANABO, ['head', 'studs', 'haft']),
    'spear': (SPEAR, ['head', 'binding', 'haft']),
}
# Raw materials: (mask, the material whose colours it uses).
RAW = {
    'driftwood': (DRIFTWOOD, 'driftwood'),
    'marine_steel_ingot': (MARINE_STEEL_INGOT, 'marine_steel'),
    'cannon_bronze_ingot': (INGOT, 'cannon_bronze'),
    'sea_king_bone': (SEA_KING_BONE, 'sea_king_bone'),
    'island_whale_leather': (HIDE, 'island_whale_leather'),
    'pacifista_alloy': (ALLOY_PLATE, 'pacifista_alloy'),
    'vegapunk_compound': (VIAL, 'vegapunk_compound'),
    'shipwrights_manual': (MANUAL, 'driftwood'),  # fixed colours only
}

if __name__ == '__main__':
    # The materials the mod has (src/main/resources/arsenal/materials.json). Each needs colours and an outfit here.
    roster = json.load(open(os.path.join(sys.argv[1], 'arsenal/materials.json')))['materials']
    for m in roster:
        assert m['id'] in MATERIALS and m['id'] in OUTFITS, f"draw {m['id']}: add it to MATERIALS and OUTFITS"
    for name, (mask, mat) in RAW.items():
        save(paint(mask, [mat] * 3), f'item/{name}')
    for m in roster:
        mat = m['id']
        parts = ARMOR_PARTS if m.get('armor_only') else {**PARTS, **ARMOR_PARTS}
        for part, mask in parts.items():
            save(paint(mask, [mat] * 3), f'item/part/{part}/{mat}')
        if not m.get('armor_only'):
            for weapon, (mask, layers) in WEAPONS.items():
                for group, layer in enumerate(layers):
                    save(paint(mask, [mat] * 3, only_group=group), f'item/{weapon}/{layer}/{mat}')
        for piece, img in armor_icons(mat).items():
            save(img, f'item/armor/{piece}/{mat}')
        layer1, layer2 = worn(mat)
        save(layer1, f'entity/equipment/humanoid/{mat}')
        save(layer2, f'entity/equipment/humanoid_leggings/{mat}')
    for name, img in stations().items():
        save(img, f'block/{name}')
