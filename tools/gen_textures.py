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
#   p-t  ramp 'main' of the second material, v-z  ramp 'main' of the third material, A-E its 'cloth'
#   anything in FIXED is that fixed colour
GROUPS = {
    **{c: (0, 'main', i) for i, c in enumerate('12345')},
    **{c: (0, 'cloth', i) for i, c in enumerate('abcde')},
    **{c: (1, 'main', i) for i, c in enumerate('pqrst')},
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

GOLD = ramp('5a400c', '8a6418', 'c99a32', 'e2b84a', 'f5d77a')
TEAL = ramp('10181a', '27393b', '3b5856', '5b857d', '9bd1c0')

# Armor icons: 1-5 = the piece's first ramp, a-e = its second ramp, p-t = gold, plus FIXED colours.
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
    raise ValueError(style)


def legs_cell(style, r, x, y):
    if style == 'castaway':  # rolled-up shorts with a rope belt
        if y >= 9:
            return '.'
        if r == 'B':
            return 'd' if x % 2 else 'c'
        return lit(x, y) if y < 8 else '4'
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
    if ch in 'abcde':
        return 'a'
    if ch in 'pqrst':
        return 'p'
    return 'k'


def paint_outfit(rows, first, second):
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
        'sash': paint_outfit(shape_mask(LEGS_SHAPE, legs_cell, st), o['legs'], o['legs2']),
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


GLYPHS = [  # 正義 ("Justice"), as small as it gets, for the back of the officer's coat
    ['###', '.#.', '.##', '.#.', '###'],
    ['#.#', '###', '.#.', '###', '#.#'],
]


def worn(material):
    o = OUTFITS[material]
    st = o['style']
    hat, hat2, coat, coat2 = o['hat'], o['hat2'], o['coat'], o['coat2']
    legs, legs2, boots, boots2 = o['legs'], o['legs2'], o['boots'], o['boots2']
    white, black = FIXED['W'], FIXED['k']
    layer1 = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    layer2 = Image.new('RGBA', (64, 32), (0, 0, 0, 0))

    def head(face, x, y, w, h):
        if face == 'bottom':
            return None
        if face == 'top':
            if st == 'castaway':
                return (hat, 3 if (x + y) % 3 else 2)
            if st == 'recruit' and x in (3, 4) and y in (3, 4):
                return (hat2, 2)
            return (hat, 3)
        if st == 'castaway':
            if y <= 3:
                return (hat, 3 if (x + y) % 2 else 2)
            return (hat2, 2) if y == 4 else (hat, 2) if y == 5 else None
        if st == 'captain':
            if face == 'front' and 1 <= y <= 3 and x in (3, 4):
                return black if y == 2 else white
            if face == 'front' and y == 3 and x in (2, 5):
                return FIXED['V']
            if y == 4:
                return (hat2, 3)
            return (hat, 2) if y <= 5 else None
        if st == 'recruit':
            if y <= 2:
                return (hat, 3)
            if y == 3:
                return (hat2, 2)
            if y == 4:
                return white if face == 'front' and x % 2 == 1 and 0 < x < 7 else (hat2, 2)
            return None
        if st == 'officer':
            if y <= 2:
                return (hat, 3)
            if y <= 4:
                return GOLD[3] if face == 'front' and x in (3, 4) else (hat2, 2)
            if y == 5 and face == 'front':
                return black if x not in (2,) else FIXED['V']
            return None

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
            if face == 'back' and 3 <= y <= 7:
                g = GLYPHS[0] if x < 4 else GLYPHS[1]
                gx = x if x < 4 else x - 4
                if 0 <= gx - (0 if x < 4 else 1) < 3 and g[y - 3][gx - (0 if x < 4 else 1)] == '#':
                    return black
            return (coat, 3)

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
        return (legs, 3)

    paint_box(layer1, HEAD, head)
    paint_box(layer1, BODY, body)
    paint_box(layer1, ARM, arm)
    paint_box(layer1, LEG, boot)
    paint_box(layer2, BODY, waist)
    paint_box(layer2, LEG, leg)
    return layer1, layer2


# ---- Write everything ----

PARTS = {
    'katana_blade': KATANA_BLADE, 'tsuba': TSUBA, 'hilt': HILT,
    'barrel': BARREL, 'firing_lock': FIRING_LOCK, 'stock': STOCK,
    'crown': CROWN, 'coat_plates': COAT_PLATES, 'sash_weave': SASH_WEAVE, 'boot_soles': BOOT_SOLES, 'lining': LINING,
}
# Each weapon's layers, in the order its parts are saved on the item.
WEAPONS = {
    'katana': (KATANA, ['blade', 'tsuba', 'hilt']),
    'flintlock': (FLINTLOCK, ['barrel', 'lock', 'stock']),
}

if __name__ == '__main__':
    save(paint(DRIFTWOOD, ['driftwood'] * 3), 'item/driftwood')
    save(paint(MARINE_STEEL_INGOT, ['marine_steel'] * 3), 'item/marine_steel_ingot')
    for mat in MATERIALS:
        for part, mask in PARTS.items():
            save(paint(mask, [mat] * 3), f'item/part/{part}/{mat}')
        for weapon, (mask, layers) in WEAPONS.items():
            for group, layer in enumerate(layers):
                save(paint(mask, [mat] * 3, only_group=group), f'item/{weapon}/{layer}/{mat}')
        for piece, img in armor_icons(mat).items():
            save(img, f'item/armor/{piece}/{mat}')
        layer1, layer2 = worn(mat)
        save(layer1, f'entity/equipment/humanoid/{mat}')
        save(layer2, f'entity/equipment/humanoid_leggings/{mat}')
