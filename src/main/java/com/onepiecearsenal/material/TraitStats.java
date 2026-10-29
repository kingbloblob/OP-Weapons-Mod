package com.onepiecearsenal.material;

import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import com.onepiecearsenal.ArsenalMod;

/**
 * The traits that change a weapon's or armor piece's numbers when it is forged, instead of doing something
 * in a fight (those are in TraitEffects). Every forged item goes through these, so a trait works the same
 * on a katana, a gun, an axe or a coat.
 */
public final class TraitStats {
	private TraitStats() {
	}

	/** The durability after Riveted (+10%) and Unbreakable Grain (x2). */
	public static int durability(int durability, Set<Trait> traits) {
		if (traits.contains(Trait.RIVETED)) {
			durability = Math.round(durability * 1.1f);
		}
		if (traits.contains(Trait.UNBREAKABLE_GRAIN)) {
			durability *= 2;
		}
		return durability;
	}

	/** Adds the attribute traits (Heavy, Legendary Hull, Waterproof) for the slot the item works in. */
	public static ItemAttributeModifiers modifiers(ItemAttributeModifiers modifiers, Set<Trait> traits, EquipmentSlotGroup slot) {
		if (traits.contains(Trait.HEAVY)) {
			modifiers = modifiers.withModifierAdded(Attributes.ARMOR_TOUGHNESS,
					new AttributeModifier(id("heavy", slot), 1.0, AttributeModifier.Operation.ADD_VALUE), slot);
		}
		if (traits.contains(Trait.LEGENDARY_HULL)) {
			modifiers = modifiers.withModifierAdded(Attributes.KNOCKBACK_RESISTANCE,
					new AttributeModifier(id("legendary_hull", slot), 0.15, AttributeModifier.Operation.ADD_VALUE), slot);
		}
		if (traits.contains(Trait.WATERPROOF) && slot != EquipmentSlotGroup.MAINHAND) {
			modifiers = modifiers.withModifierAdded(Attributes.WATER_MOVEMENT_EFFICIENCY,
					new AttributeModifier(id("waterproof", slot), 0.2, AttributeModifier.Operation.ADD_VALUE), slot);
		}
		return modifiers;
	}

	/** One id per slot, so a hat and a coat that are both Heavy each count. */
	private static Identifier id(String name, EquipmentSlotGroup slot) {
		return ArsenalMod.id(name + "." + slot.getSerializedName());
	}

	/** The traits saved straight onto the item: Ultra-alloy makes it fireproof, like netherite. */
	public static void finish(ItemStack stack, Set<Trait> traits) {
		if (traits.contains(Trait.ULTRA_ALLOY)) {
			stack.set(DataComponents.DAMAGE_RESISTANT, new DamageResistant(DamageTypeTags.IS_FIRE));
		}
	}
}
