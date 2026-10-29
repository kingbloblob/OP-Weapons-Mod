package com.onepiecearsenal.cannon;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecemod.shipcombat.CannonAddons;

/**
 * Works out a Ship Cannon's numbers from its three parts, as multipliers of the One Piece Mod's plain Cannon
 * (1 = the same; its cannonballs knock 24 hull points off a ship, and its crew reloads in 2.5 seconds).
 * A Ship Iron cannon shoots exactly like a plain one.
 *
 * damage = 0.6 + 0.08 x the bore material's damage (Driftwood 0.84, Ship Iron 1.0, Vegapunk Compound 1.2).
 * range = 0.92 + 0.08 x the bore's tier (muzzle speed: tier 1 = 1.0, tier 5 = 1.32).
 * reload = 1 - the fuse material's speed, 0.05 less if it's Light (Driftwood 0.75, Ship Iron 1.0, Cannon Bronze 1.1).
 * spread = 1.15 - 0.15 x the carriage's tier (tier 1 = 1.0, tier 5 = 0.4).
 */
public final class CannonStats {
	private CannonStats() {
	}

	public static float damage(ArsenalMaterial bore) {
		return 0.6f + 0.08f * bore.damage;
	}

	public static float range(ArsenalMaterial bore) {
		return 0.92f + 0.08f * bore.tier;
	}

	public static float reload(ArsenalMaterial fuse) {
		return 1.0f - fuse.speed - (fuse.handleTrait == Trait.LIGHT ? 0.05f : 0.0f);
	}

	public static float spread(ArsenalMaterial carriage) {
		return Math.max(0.2f, 1.15f - 0.15f * carriage.tier);
	}

	public static CannonAddons.Profile profile(List<ArsenalMaterial> parts) {
		if (parts.size() != 3) {
			return CannonAddons.Profile.PLAIN;
		}
		return new CannonAddons.Profile(damage(parts.get(0)), range(parts.get(0)), reload(parts.get(1)), spread(parts.get(2)));
	}

	/** Builds a Ship Cannon item, ready to place. */
	public static ItemStack build(ArsenalMaterial bore, ArsenalMaterial fuse, ArsenalMaterial carriage) {
		ItemStack cannon = new ItemStack(ArsenalItems.SHIP_CANNON);
		cannon.set(ArsenalComponents.CANNON_PARTS, List.of(bore.id(), fuse.id(), carriage.id()));
		return cannon;
	}

	/** The materials as bore, fuse, carriage. Empty for a cannon that wasn't built from parts. */
	public static List<ArsenalMaterial> parts(List<String> ids) {
		List<ArsenalMaterial> parts = new ArrayList<>();
		for (String id : ids) {
			ArsenalMaterial material = ArsenalMaterial.byId(id);
			if (material == null) {
				return List.of();
			}
			parts.add(material);
		}
		return parts.size() == 3 ? parts : List.of();
	}

	public static List<ArsenalMaterial> parts(ItemStack cannon) {
		return parts(cannon.getOrDefault(ArsenalComponents.CANNON_PARTS, List.<String>of()));
	}
}
