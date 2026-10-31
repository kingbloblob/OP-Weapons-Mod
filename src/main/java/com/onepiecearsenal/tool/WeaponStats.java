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

/**
 * Works out a hand weapon's numbers (cutlass, sabre, axe, jitte, kanabo, spear) from its three parts and saves
 * them on the item. The numbers for each type are in WeaponType.
 */
public final class WeaponStats {
	/** Vanilla's ids for a weapon's own damage and speed, so the tooltip shows them like any sword's. */
	private static final Identifier DAMAGE_MODIFIER = Identifier.withDefaultNamespace("base_attack_damage");
	private static final Identifier SPEED_MODIFIER = Identifier.withDefaultNamespace("base_attack_speed");
	private static final Identifier KNOCKBACK_MODIFIER = ArsenalMod.id("kanabo_knockback");
	private static final Identifier REACH_MODIFIER = ArsenalMod.id("spear_reach");

	private WeaponStats() {
	}

	public static float damage(WeaponType type, ArsenalMaterial head) {
		return head.damage * type.damageScale + type.bonusDamage;
	}

	/** Builds a finished weapon. */
	public static ItemStack build(WeaponType type, ArsenalMaterial head, ArsenalMaterial binding, ArsenalMaterial handle) {
		ItemStack weapon = new ItemStack(ArsenalItems.WEAPONS.get(type));
		weapon.set(ArsenalComponents.WEAPON_PARTS, List.of(head.id(), binding.id(), handle.id()));

		Set<Trait> traits = traits(head, binding, handle);
		float speed = type.baseSpeed + (binding.speed + handle.speed) / 2;
		if (traits.contains(Trait.LIGHT)) {
			speed += 0.1f;
		}
		int durability = Math.round((head.durability + binding.durability + handle.durability) / 3.0f * type.durabilityScale);

		ItemAttributeModifiers modifiers = ItemAttributeModifiers.EMPTY
				.withModifierAdded(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(DAMAGE_MODIFIER, damage(type, head), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.withModifierAdded(Attributes.ATTACK_SPEED,
						new AttributeModifier(SPEED_MODIFIER, speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
		if (type == WeaponType.KANABO) {
			modifiers = modifiers.withModifierAdded(Attributes.ATTACK_KNOCKBACK,
					new AttributeModifier(KNOCKBACK_MODIFIER, 1.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
		}
		if (type == WeaponType.SPEAR) {
			modifiers = modifiers.withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE,
					new AttributeModifier(REACH_MODIFIER, 1.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
		}
		weapon.set(DataComponents.ATTRIBUTE_MODIFIERS, TraitStats.modifiers(modifiers, traits, EquipmentSlotGroup.MAINHAND));
		weapon.set(DataComponents.MAX_DAMAGE, TraitStats.durability(durability, traits));
		TraitStats.finish(weapon, traits);
		return weapon;
	}

	/** The weapon's materials as head, binding, handle. Empty for a weapon that wasn't built from parts. */
	public static List<ArsenalMaterial> parts(ItemStack weapon) {
		List<ArsenalMaterial> parts = new ArrayList<>();
		for (String id : weapon.getOrDefault(ArsenalComponents.WEAPON_PARTS, List.<String>of())) {
			ArsenalMaterial material = ArsenalMaterial.byId(id);
			if (material == null) {
				return List.of();
			}
			parts.add(material);
		}
		return parts.size() == 3 ? parts : List.of();
	}

	/** The head's head trait, and the handle traits of the binding and handle. */
	public static Set<Trait> traits(ItemStack weapon) {
		List<ArsenalMaterial> parts = parts(weapon);
		return parts.isEmpty() ? EnumSet.noneOf(Trait.class) : Mastery.withBound(weapon, traits(parts.get(0), parts.get(1), parts.get(2)));
	}

	private static Set<Trait> traits(ArsenalMaterial head, ArsenalMaterial binding, ArsenalMaterial handle) {
		return EnumSet.of(head.headTrait, binding.handleTrait, handle.handleTrait);
	}
}
