package com.onepiecearsenal.tool;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecemod.weapon.Ammo;
import com.onepiecemod.weapon.GunItem;

/**
 * Works out a flintlock's numbers from its three parts. Change the numbers here to tune Arsenal guns.
 * For comparison, the One Piece Mod's Flintlock Pistol does 8 damage, reloads in 1.25 seconds,
 * reaches 40 blocks with 0.04 spread, and lasts 300 shots.
 *
 * barrel: damage = 3 + the material's damage (Ship Iron = 8), range = 32 + 8 per tier.
 * firing lock: reload = 25 ticks, minus 20 ticks per point of the material's speed.
 * stock: spread = 0.06 minus 0.01 per tier (higher tiers aim better).
 */
public final class GunStats {
	private static final float BASE_DAMAGE = 3.0f;
	private static final int BASE_COOLDOWN = 25;

	private GunStats() {
	}

	public static float damage(ArsenalMaterial barrel) {
		return BASE_DAMAGE + barrel.damage;
	}

	public static double range(ArsenalMaterial barrel) {
		return 32.0 + 8.0 * barrel.tier;
	}

	public static int cooldown(ArsenalMaterial lock) {
		int cooldown = Math.round(BASE_COOLDOWN - 20 * lock.speed);
		return lock.handleTrait == Trait.LIGHT ? cooldown - 2 : cooldown;
	}

	public static float spread(ArsenalMaterial stock) {
		return Math.max(0.0f, 0.06f - 0.01f * stock.tier);
	}

	/** Builds a finished flintlock. */
	public static ItemStack build(ArsenalMaterial barrel, ArsenalMaterial lock, ArsenalMaterial stock) {
		ItemStack gun = new ItemStack(ArsenalItems.FLINTLOCK);
		gun.set(ArsenalComponents.GUN_PARTS, List.of(barrel.id(), lock.id(), stock.id()));
		int durability = Math.round((barrel.durability + lock.durability + stock.durability) / 3.0f * 1.2f);
		if (traits(barrel, lock, stock).contains(Trait.RIVETED)) {
			durability = Math.round(durability * 1.1f);
		}
		gun.set(DataComponents.MAX_DAMAGE, durability);
		return gun;
	}

	/** The gun's stats before upgrades and enchantments, or null for a gun that wasn't built from parts. */
	public static GunItem.Stats stats(ItemStack gun) {
		List<ArsenalMaterial> parts = parts(gun);
		if (parts.isEmpty()) {
			return null;
		}
		return new GunItem.Stats(damage(parts.get(0)), cooldown(parts.get(1)), range(parts.get(0)), spread(parts.get(2)), 1, Ammo.Kind.BULLET,
				0, 0, 3.0f); // Single shot: the firing lock's cooldown is the reload, so no magazine.
	}

	/** The gun's materials as barrel, firing lock, stock. Empty for a gun that wasn't built from parts. */
	public static List<ArsenalMaterial> parts(ItemStack gun) {
		List<ArsenalMaterial> parts = new ArrayList<>();
		for (String id : gun.getOrDefault(ArsenalComponents.GUN_PARTS, List.<String>of())) {
			ArsenalMaterial material = ArsenalMaterial.byId(id);
			if (material == null) {
				return List.of();
			}
			parts.add(material);
		}
		return parts.size() == 3 ? parts : List.of();
	}

	/** The barrel's head trait, and the handle traits of the firing lock and stock. */
	public static Set<Trait> traits(ItemStack gun) {
		List<ArsenalMaterial> parts = parts(gun);
		return parts.isEmpty() ? EnumSet.noneOf(Trait.class) : traits(parts.get(0), parts.get(1), parts.get(2));
	}

	private static Set<Trait> traits(ArsenalMaterial barrel, ArsenalMaterial lock, ArsenalMaterial stock) {
		return EnumSet.of(barrel.headTrait, lock.handleTrait, stock.handleTrait);
	}
}
