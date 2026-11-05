package com.onepiecearsenal.station;

import java.util.List;

import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Faction;
import com.onepiecearsenal.tool.Restat;
import com.onepiecearsenal.upgrade.Upgrade;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * The best materials (tier 4 and up: Wapometal, Adam Wood, Kokuto, Dragon Scale, Netherite, Black Blade Steel, Vegapunk
 * Compound) take more heat and force than a crafting table and a Shipwright's Anvil can give:
 * - their parts have no crafting table recipes, and are made at a Shipwright's Forge instead (Navy ones still at the
 *   Marine Base Smeltery);
 * - gear with any part of them is put together, mended and upgraded at a Grand Line Anvil (or, for Navy gear, a
 *   Marine Armory Bench). Taking it apart still works at any Shipwright's Anvil.
 * Town blacksmiths won't touch them either.
 */
public final class HighTier {
	/** The lowest tier that needs the Forge and the Grand Line Anvil. */
	public static final int TIER = 4;

	private HighTier() {
	}

	public static boolean is(ArsenalMaterial material) {
		return material != null && material.tier >= TIER;
	}

	/** True for forged gear with any high-tier part. */
	public static boolean is(ItemStack gear) {
		return strongest(gear) != null;
	}

	/** The gear's first high-tier part material (head first), or null if it has none or isn't forged gear. */
	public static ArsenalMaterial strongest(ItemStack gear) {
		Upgrade.Gear kind = Upgrades.gearOf(gear);
		if (kind == null) {
			return null;
		}
		for (ArsenalMaterial material : Restat.parts(gear, kind)) {
			if (is(material)) {
				return material;
			}
		}
		return null;
	}

	/** The materials a Shipwright's Forge works: high tier, craftable, and not Navy. */
	public static List<ArsenalMaterial> forgeMaterials() {
		return ArsenalMaterial.values().stream()
				.filter(material -> is(material) && material.craftable && material.faction != Faction.NAVY)
				.toList();
	}
}
