package com.onepiecearsenal.tool;

import com.onepiecearsenal.part.PartType;
import com.onepiecemod.weapon.Ammo;
import com.onepiecemod.weapon.GunItem;

/**
 * The kinds of Arsenal guns. Each is built from a barrel, a firing part and a stock, and turns its parts'
 * materials into numbers the same way (see GunStats); these numbers make each kind feel different.
 * For comparison, the One Piece Mod's Flintlock Pistol does 8 damage, reloads in 1.25 seconds and reaches 40 blocks,
 * and its Hand Cannon does 12 damage, reloads in 3 seconds and reaches 48 blocks.
 *
 * damage = (base damage + the barrel material's damage) x damage scale.
 * reload = base reload, minus 20 ticks per point of the firing part's speed.
 * range = base range + 8 blocks per barrel tier. spread = (0.06 - 0.01 per stock tier) x spread scale.
 */
public enum GunType {
	//           id             name           barrel                  firing part     stock            hold                ammo
	FLINTLOCK("flintlock", "Flintlock", PartType.BARREL, PartType.LOCK, PartType.STOCK, GunItem.Hold.PISTOL, Ammo.Kind.BULLET,
			// base damage, damage scale, reload ticks, base range, spread scale, durability scale, kick
			3.0f, 1.0f, 25, 32.0, 1.0f, 1.2f, 3.0f),
	/** A long gun: hits harder and further and aims better, but reloads slowly. */
	MUSKET("musket", "Musket", PartType.LONG_BARREL, PartType.LOCK, PartType.STOCK, GunItem.Hold.RIFLE, Ammo.Kind.BULLET,
			3.0f, 1.3f, 40, 48.0, 0.5f, 1.4f, 5.0f),
	/** Fires the One Piece Mod's Cannonballs. Slow and heavy. */
	HAND_CANNON("hand_cannon", "Hand Cannon", PartType.CANNON_BORE, PartType.FUSE, PartType.STOCK, GunItem.Hold.RIFLE, Ammo.Kind.CANNONBALL,
			6.0f, 1.0f, 60, 36.0, 0.6f, 1.0f, 8.0f);

	private final String id;
	public final String displayName;
	public final PartType barrel;
	public final PartType lock;
	public final PartType stock;
	public final GunItem.Hold hold;
	public final Ammo.Kind ammo;
	public final float baseDamage;
	public final float damageScale;
	public final int baseCooldown;
	public final double baseRange;
	public final float spreadScale;
	public final float durabilityScale;
	public final float recoil;

	GunType(String id, String displayName, PartType barrel, PartType lock, PartType stock, GunItem.Hold hold, Ammo.Kind ammo,
			float baseDamage, float damageScale, int baseCooldown, double baseRange, float spreadScale, float durabilityScale, float recoil) {
		this.id = id;
		this.displayName = displayName;
		this.barrel = barrel;
		this.lock = lock;
		this.stock = stock;
		this.hold = hold;
		this.ammo = ammo;
		this.baseDamage = baseDamage;
		this.damageScale = damageScale;
		this.baseCooldown = baseCooldown;
		this.baseRange = baseRange;
		this.spreadScale = spreadScale;
		this.durabilityScale = durabilityScale;
		this.recoil = recoil;
	}

	/** The item id, like "musket". */
	public String id() {
		return id;
	}

	/** The parts in the order they are saved on the gun: barrel, firing part, stock. */
	public PartType[] parts() {
		return new PartType[] {barrel, lock, stock};
	}

	/** The first gun kind that uses this part, for the part's tooltip. Null if no gun uses it. */
	public static GunType using(PartType part) {
		for (GunType type : values()) {
			if (type.barrel == part || type.lock == part || type.stock == part) {
				return type;
			}
		}
		return null;
	}
}
