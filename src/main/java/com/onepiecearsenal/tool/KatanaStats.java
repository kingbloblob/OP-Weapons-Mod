package com.onepiecearsenal.tool;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.mastery.Mastery;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.material.TraitStats;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * Works out a katana's numbers from its three parts and saves them on the item.
 * The One Piece Mod's whetstones still work on it afterwards: they add their grade bonus on top.
 */
public final class KatanaStats {
	/** Attack speed of a normal sword. The guard and hilt add or take away from this. */
	private static final float BASE_SPEED = -2.4f;
	private static final Identifier DAMAGE_MODIFIER = ArsenalMod.id("katana_damage");
	private static final Identifier SPEED_MODIFIER = ArsenalMod.id("katana_speed");

	private KatanaStats() {
	}

	/** Builds a finished katana. */
	public static ItemStack build(ArsenalMaterial blade, ArsenalMaterial tsuba, ArsenalMaterial hilt) {
		ItemStack katana = new ItemStack(ArsenalItems.KATANA);
		katana.set(ArsenalComponents.KATANA_PARTS, List.of(blade.id(), tsuba.id(), hilt.id()));

		Set<Trait> traits = traits(blade, tsuba, hilt);
		float speed = BASE_SPEED + (tsuba.speed + hilt.speed) / 2;
		if (traits.contains(Trait.LIGHT)) {
			speed += 0.1f;
		}
		int durability = TraitStats.durability((blade.durability + tsuba.durability + hilt.durability) / 3, traits);

		ItemAttributeModifiers modifiers = ItemAttributeModifiers.EMPTY
				.withModifierAdded(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(DAMAGE_MODIFIER, blade.damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.withModifierAdded(Attributes.ATTACK_SPEED,
						new AttributeModifier(SPEED_MODIFIER, speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
		katana.set(DataComponents.ATTRIBUTE_MODIFIERS, TraitStats.modifiers(modifiers, traits, EquipmentSlotGroup.MAINHAND));
		katana.set(DataComponents.MAX_DAMAGE, durability);
		TraitStats.finish(katana, traits);
		return katana;
	}

	/**
	 * A Black Blade awakening: the katana's blade turns to Black Blade Steel where it is, so it keeps its tsuba, hilt,
	 * grade, blade level, enchantments, anvil upgrades and name. Its damage and durability become the new blade's, and it's fully mended.
	 */
	public static void awaken(ItemStack katana) {
		List<ArsenalMaterial> parts = parts(katana);
		if (parts.isEmpty()) {
			return;
		}
		ArsenalMaterial blade = ArsenalMaterial.BLACK_BLADE;
		ArsenalMaterial tsuba = parts.get(1);
		ArsenalMaterial hilt = parts.get(2);
		katana.set(ArsenalComponents.KATANA_PARTS, List.of(blade.id(), tsuba.id(), hilt.id()));

		ItemAttributeModifiers old = katana.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		List<ItemAttributeModifiers.Entry> entries = new ArrayList<>();
		for (ItemAttributeModifiers.Entry entry : old.modifiers()) {
			if (!entry.modifier().is(DAMAGE_MODIFIER)) {
				entries.add(entry);
			}
		}
		katana.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(entries).withModifierAdded(Attributes.ATTACK_DAMAGE,
				new AttributeModifier(DAMAGE_MODIFIER, blade.damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND));

		Set<Trait> traits = traits(blade, tsuba, hilt);
		// Its anvil upgrades stay: their attribute modifiers were kept above, and Reinforced still adds its share.
		katana.set(DataComponents.MAX_DAMAGE, Upgrades.durability(katana,
				TraitStats.durability((blade.durability + tsuba.durability + hilt.durability) / 3, traits)));
		katana.setDamageValue(0);
		TraitStats.finish(katana, traits);
	}

	/** True once a katana's blade has awakened into a Black Blade. */
	public static boolean isBlackBlade(ItemStack katana) {
		List<ArsenalMaterial> parts = parts(katana);
		return !parts.isEmpty() && parts.get(0) == ArsenalMaterial.BLACK_BLADE;
	}

	/** The katana's materials as blade, tsuba, hilt. Empty for a katana that wasn't built from parts. */
	public static List<ArsenalMaterial> parts(ItemStack katana) {
		List<ArsenalMaterial> parts = new ArrayList<>();
		for (String id : katana.getOrDefault(ArsenalComponents.KATANA_PARTS, List.<String>of())) {
			ArsenalMaterial material = ArsenalMaterial.byId(id);
			if (material == null) {
				return List.of();
			}
			parts.add(material);
		}
		return parts.size() == 3 ? parts : List.of();
	}

	/** The traits a katana has: the blade's head trait, and the handle traits of its tsuba and hilt. */
	public static Set<Trait> traits(ItemStack katana) {
		List<ArsenalMaterial> parts = parts(katana);
		return parts.isEmpty() ? EnumSet.noneOf(Trait.class) : Mastery.withBound(katana, traits(parts.get(0), parts.get(1), parts.get(2)));
	}

	private static Set<Trait> traits(ArsenalMaterial blade, ArsenalMaterial tsuba, ArsenalMaterial hilt) {
		return EnumSet.of(blade.headTrait, tsuba.handleTrait, hilt.handleTrait);
	}
}
