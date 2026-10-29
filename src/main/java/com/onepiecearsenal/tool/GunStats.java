package com.onepiecearsenal.tool;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.material.TraitStats;
import com.onepiecemod.weapon.GunItem;

/**
 * Works out a gun's numbers from its three parts. The numbers for each kind of gun are in GunType.
 * With the default flintlock numbers: Ship Iron barrel = 8 damage, 40 blocks; Ship Iron lock = 1.25s reload.
 */
public final class GunStats {
	private GunStats() {
	}

	public static float damage(GunType type, ArsenalMaterial barrel) {
		return (type.baseDamage + barrel.damage) * type.damageScale;
	}

	public static double range(GunType type, ArsenalMaterial barrel) {
		return type.baseRange + 8.0 * barrel.tier;
	}

	public static int cooldown(GunType type, ArsenalMaterial lock) {
		int cooldown = Math.round(type.baseCooldown - 20 * lock.speed);
		return lock.handleTrait == Trait.LIGHT ? cooldown - 2 : cooldown;
	}

	public static float spread(GunType type, ArsenalMaterial stock) {
		return Math.max(0.0f, (0.06f - 0.01f * stock.tier) * type.spreadScale);
	}

	/** Builds a finished flintlock. */
	public static ItemStack build(ArsenalMaterial barrel, ArsenalMaterial lock, ArsenalMaterial stock, HolderLookup.Provider registries) {
		return build(GunType.FLINTLOCK, barrel, lock, stock, registries);
	}

	/** Builds a finished gun. */
	public static ItemStack build(GunType type, ArsenalMaterial barrel, ArsenalMaterial lock, ArsenalMaterial stock, HolderLookup.Provider registries) {
		ItemStack gun = new ItemStack(ArsenalItems.GUNS.get(type));
		gun.set(ArsenalComponents.GUN_PARTS, List.of(barrel.id(), lock.id(), stock.id()));
		Set<Trait> traits = traits(barrel, lock, stock);
		int durability = Math.round((barrel.durability + lock.durability + stock.durability) / 3.0f * type.durabilityScale);
		gun.set(DataComponents.MAX_DAMAGE, TraitStats.durability(durability, traits));
		TraitStats.finish(gun, traits, registries);
		return gun;
	}

	/** The gun's stats before upgrades and enchantments, or null for a gun that wasn't built from parts. */
	public static GunItem.Stats stats(ItemStack gun) {
		List<ArsenalMaterial> parts = parts(gun);
		if (parts.isEmpty() || !(gun.getItem() instanceof ArsenalGunItem item)) {
			return null;
		}
		GunType type = item.type();
		return new GunItem.Stats(damage(type, parts.get(0)), cooldown(type, parts.get(1)), range(type, parts.get(0)),
				spread(type, parts.get(2)), 1, type.ammo,
				0, 0, type.recoil); // Single shot: the firing part's cooldown is the reload, so no magazine.
	}

	/** The gun's materials as barrel, firing part, stock. Empty for a gun that wasn't built from parts. */
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

	/** The barrel's head trait, and the handle traits of the firing part and stock. */
	public static Set<Trait> traits(ItemStack gun) {
		List<ArsenalMaterial> parts = parts(gun);
		return parts.isEmpty() ? EnumSet.noneOf(Trait.class) : traits(parts.get(0), parts.get(1), parts.get(2));
	}

	private static Set<Trait> traits(ArsenalMaterial barrel, ArsenalMaterial lock, ArsenalMaterial stock) {
		return EnumSet.of(barrel.headTrait, lock.handleTrait, stock.handleTrait);
	}
}
