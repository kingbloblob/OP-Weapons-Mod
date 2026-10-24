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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorType;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.part.PartType;

/**
 * Works out an armor piece's numbers from its shell and lining and saves them on the item.
 * The shell (crown, coat plates, sash weave or boot soles) gives the defense and the head trait;
 * the lining gives its handle trait. Both share the durability.
 *
 * For comparison, iron armor is 2/6/5/2 defense and 165/240/225/195 durability.
 * A material's "armor" number is its whole-set strength: 4 = iron, 5 = diamond.
 */
public final class ArmorStats {
	private static final Identifier ARMOR_MODIFIER = ArsenalMod.id("armor_plates");
	private static final Identifier TOUGHNESS_MODIFIER = ArsenalMod.id("armor_toughness");

	private ArmorStats() {
	}

	/** Which piece a shell part becomes. */
	public static ArmorType typeOf(PartType shell) {
		return switch (shell) {
			case CROWN -> ArmorType.HELMET;
			case COAT_PLATES -> ArmorType.CHESTPLATE;
			case SASH_WEAVE -> ArmorType.LEGGINGS;
			case BOOT_SOLES -> ArmorType.BOOTS;
			default -> throw new IllegalArgumentException(shell + " is not an armor shell");
		};
	}

	public static Item itemOf(ArmorType type) {
		return switch (type) {
			case HELMET -> ArsenalItems.HAT;
			case CHESTPLATE -> ArsenalItems.COAT;
			case LEGGINGS -> ArsenalItems.SASH;
			default -> ArsenalItems.BOOTS;
		};
	}

	/** Defense points for one piece: the material's set strength shared out like vanilla armor (2/6/5/2 of 15). */
	public static int defense(ArsenalMaterial shell, ArmorType type) {
		float share = switch (type) {
			case HELMET -> 0.5f;
			case CHESTPLATE -> 1.5f;
			case LEGGINGS -> 1.25f;
			default -> 0.5f;
		};
		return Math.max(1, Math.round(shell.armor * share));
	}

	/** Toughness: tier 3 and up protect against strong hits, 1 point per tier above 2. */
	public static float toughness(ArsenalMaterial shell) {
		return Math.max(0, shell.tier - 2);
	}

	public static int durability(ArsenalMaterial shell, ArsenalMaterial lining, ArmorType type) {
		float share = switch (type) {
			case HELMET -> 0.69f;
			case CHESTPLATE -> 1.0f;
			case LEGGINGS -> 0.94f;
			default -> 0.81f;
		};
		int durability = Math.round((shell.durability + lining.durability) / 2.0f * share);
		if (lining.handleTrait == Trait.RIVETED || shell.headTrait == Trait.RIVETED) {
			durability = Math.round(durability * 1.1f);
		}
		return durability;
	}

	/** Builds a finished armor piece. */
	public static ItemStack build(PartType shellType, ArsenalMaterial shell, ArsenalMaterial lining) {
		ArmorType type = typeOf(shellType);
		ItemStack piece = new ItemStack(itemOf(type));
		piece.set(ArsenalComponents.ARMOR_PARTS, List.of(shell.id(), lining.id()));

		EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
		ItemAttributeModifiers modifiers = ItemAttributeModifiers.EMPTY
				.withModifierAdded(Attributes.ARMOR,
						new AttributeModifier(ARMOR_MODIFIER, defense(shell, type), AttributeModifier.Operation.ADD_VALUE), slot);
		if (toughness(shell) > 0) {
			modifiers = modifiers.withModifierAdded(Attributes.ARMOR_TOUGHNESS,
					new AttributeModifier(TOUGHNESS_MODIFIER, toughness(shell), AttributeModifier.Operation.ADD_VALUE), slot);
		}
		piece.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
		piece.set(DataComponents.MAX_DAMAGE, durability(shell, lining, type));
		return piece;
	}

	/** The piece's materials as shell, lining. Empty for a piece that wasn't built from parts. */
	public static List<ArsenalMaterial> parts(ItemStack piece) {
		List<ArsenalMaterial> parts = new ArrayList<>();
		for (String id : piece.getOrDefault(ArsenalComponents.ARMOR_PARTS, List.<String>of())) {
			ArsenalMaterial material = ArsenalMaterial.byId(id);
			if (material == null) {
				return List.of();
			}
			parts.add(material);
		}
		return parts.size() == 2 ? parts : List.of();
	}

	/** The shell's head trait and the lining's handle trait. */
	public static Set<Trait> traits(ItemStack piece) {
		List<ArsenalMaterial> parts = parts(piece);
		return parts.isEmpty() ? EnumSet.noneOf(Trait.class) : EnumSet.of(parts.get(0).headTrait, parts.get(1).handleTrait);
	}
}
