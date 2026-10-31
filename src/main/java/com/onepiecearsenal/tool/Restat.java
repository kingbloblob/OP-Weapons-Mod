package com.onepiecearsenal.tool;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.meito.Meito;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.upgrade.Upgrade;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * Brings gear forged before a balance change up to date: while it sits in a player's inventory, a piece whose
 * materials' numbers have changed since it was forged gets its stats worked out again from its parts.
 *
 * It keeps everything that isn't from the parts: the whetstone grade, blade level and record, upgrades, enchantments,
 * name and Haki soak. Wear is kept as a share, so a half-worn katana stays half worn.
 *
 * Changes to materials.json are noticed on their own. A change to a formula in code (KatanaStats, WeaponStats,
 * GunStats, ArmorStats, ShieldStats, GauntletStats, WeaponType, GunType or TraitStats) needs FORMULA_VERSION raised by one.
 */
public final class Restat {
	/** Raise by one whenever a stat formula in code changes, so gear forged before it catches up. */
	private static final int FORMULA_VERSION = 1;

	private Restat() {
	}

	/** Re-works the piece's stats if they're out of date. Returns true if it changed anything. */
	public static boolean refresh(ItemStack stack) {
		Upgrade.Gear gear = Upgrades.gearOf(stack);
		if (gear == null) {
			return false;
		}
		int stamp = stamp(stack, gear);
		if (stack.getOrDefault(ArsenalComponents.STATS_STAMP, 0) == stamp) {
			return false;
		}
		ItemStack fresh = rebuild(stack, gear);
		Upgrades.carryOver(stack, fresh);

		// The parts' modifiers and upgrades come from the fresh build. Others' (the main mod's grade) and the blade level stay.
		ItemAttributeModifiers rebuilt = fresh.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		Set<Identifier> rebuiltIds = new HashSet<>();
		rebuilt.modifiers().forEach(entry -> rebuiltIds.add(entry.modifier().id()));
		for (ItemAttributeModifiers.Entry entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			Identifier id = entry.modifier().id();
			boolean ours = id.getNamespace().equals(ArsenalMod.MOD_ID) && !id.equals(Meito.LEVEL_MODIFIER);
			if (!ours && !rebuiltIds.contains(id)) {
				rebuilt = rebuilt.withModifierAdded(entry.attribute(), entry.modifier(), entry.slot());
			}
		}
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, rebuilt);

		int oldMax = stack.getMaxDamage();
		int newMax = fresh.getMaxDamage();
		if (oldMax > 0 && oldMax != newMax) {
			int worn = Math.round(stack.getDamageValue() / (float) oldMax * newMax);
			stack.set(DataComponents.MAX_DAMAGE, newMax);
			stack.setDamageValue(Math.min(worn, newMax - 1));
		}
		if (fresh.has(DataComponents.DAMAGE_RESISTANT)) {
			stack.set(DataComponents.DAMAGE_RESISTANT, fresh.get(DataComponents.DAMAGE_RESISTANT));
		} else {
			stack.remove(DataComponents.DAMAGE_RESISTANT);
		}
		if (fresh.has(DataComponents.BLOCKS_ATTACKS)) {
			stack.set(DataComponents.BLOCKS_ATTACKS, fresh.get(DataComponents.BLOCKS_ATTACKS)); // A shield's block numbers.
		}
		stack.set(ArsenalComponents.STATS_STAMP, stamp);
		return true;
	}

	/** A fingerprint of everything the stats are worked out from: the item, its parts' numbers and the formulas. */
	public static int stamp(ItemStack stack, Upgrade.Gear gear) {
		int stamp = Objects.hash(FORMULA_VERSION, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		for (ArsenalMaterial part : parts(stack, gear)) {
			stamp = 31 * stamp + part.statsHash();
		}
		// 0 means "never stamped", so a piece forged before stamps existed always gets a refresh.
		return stamp == 0 ? 1 : stamp;
	}

	/** The gear's materials, head part first, in the order its parts component lists them. */
	public static List<ArsenalMaterial> parts(ItemStack stack, Upgrade.Gear gear) {
		return switch (gear) {
			case KATANA -> KatanaStats.parts(stack);
			case WEAPON -> WeaponStats.parts(stack);
			case GUN -> GunStats.parts(stack);
			case SHIELD -> ShieldStats.parts(stack);
			case GAUNTLETS -> GauntletStats.parts(stack);
			default -> ArmorStats.parts(stack);
		};
	}

	/** A new piece of the same kind from the same parts, with today's numbers. */
	private static ItemStack rebuild(ItemStack stack, Upgrade.Gear gear) {
		List<ArsenalMaterial> parts = parts(stack, gear);
		return switch (gear) {
			case KATANA -> KatanaStats.build(parts.get(0), parts.get(1), parts.get(2));
			case WEAPON -> WeaponStats.build(((ArsenalWeaponItem) stack.getItem()).type(), parts.get(0), parts.get(1), parts.get(2));
			case GUN -> GunStats.build(((ArsenalGunItem) stack.getItem()).type(), parts.get(0), parts.get(1), parts.get(2));
			case HAT -> ArmorStats.build(PartType.CROWN, parts.get(0), parts.get(1));
			case COAT -> ArmorStats.build(PartType.COAT_PLATES, parts.get(0), parts.get(1));
			case SASH -> ArmorStats.build(PartType.SASH_WEAVE, parts.get(0), parts.get(1));
			case BOOTS -> ArmorStats.build(PartType.BOOT_SOLES, parts.get(0), parts.get(1));
			case SHIELD -> ShieldStats.build(parts.get(0), parts.get(1), parts.get(2));
			case GAUNTLETS -> GauntletStats.build(parts.get(0), parts.get(1));
		};
	}
}
