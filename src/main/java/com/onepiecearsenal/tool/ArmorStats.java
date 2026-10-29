package com.onepiecearsenal.tool;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.material.TraitStats;
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
	/** The ids every piece shared before each slot got its own; see fixSharedIds. */
	private static final Identifier OLD_ARMOR_MODIFIER = ArsenalMod.id("armor_plates");
	private static final Identifier OLD_TOUGHNESS_MODIFIER = ArsenalMod.id("armor_toughness");

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

	/**
	 * A coat lasts 16 x the square root of its materials' durability: Ship Iron (250) gives 253 like an iron chestplate,
	 * Kairoseki (1100) 531 like diamond. A straight copy of the weapon number would make tier 3 coats outlast netherite.
	 */
	public static int durability(ArsenalMaterial shell, ArsenalMaterial lining, ArmorType type) {
		float share = switch (type) {
			case HELMET -> 0.69f;
			case CHESTPLATE -> 1.0f;
			case LEGGINGS -> 0.94f;
			default -> 0.81f;
		};
		int durability = Math.round(16.0f * (float) Math.sqrt((shell.durability + lining.durability) / 2.0f) * share);
		return TraitStats.durability(durability, EnumSet.of(shell.headTrait, lining.handleTrait));
	}

	/** Builds a finished armor piece. */
	public static ItemStack build(PartType shellType, ArsenalMaterial shell, ArsenalMaterial lining) {
		ArmorType type = typeOf(shellType);
		ItemStack piece = new ItemStack(itemOf(type));
		piece.set(ArsenalComponents.ARMOR_PARTS, List.of(shell.id(), lining.id()));

		EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
		ItemAttributeModifiers modifiers = ItemAttributeModifiers.EMPTY
				.withModifierAdded(Attributes.ARMOR,
						new AttributeModifier(id("armor_plates", slot), defense(shell, type), AttributeModifier.Operation.ADD_VALUE), slot);
		if (toughness(shell) > 0) {
			modifiers = modifiers.withModifierAdded(Attributes.ARMOR_TOUGHNESS,
					new AttributeModifier(id("armor_toughness", slot), toughness(shell), AttributeModifier.Operation.ADD_VALUE), slot);
		}
		Set<Trait> traits = EnumSet.of(shell.headTrait, lining.handleTrait);
		piece.set(DataComponents.ATTRIBUTE_MODIFIERS, TraitStats.modifiers(modifiers, traits, slot));
		piece.set(DataComponents.MAX_DAMAGE, durability(shell, lining, type));
		TraitStats.finish(piece, traits);

		// Worn, it looks like its shell: assets/arsenal/equipment/<material>.json (drawn by tools/gen_textures.py).
		Equippable equippable = piece.get(DataComponents.EQUIPPABLE);
		if (equippable != null) {
			piece.set(DataComponents.EQUIPPABLE, Equippable.builder(equippable.slot())
					.setEquipSound(equippable.equipSound())
					.setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, ArsenalMod.id(shell.id())))
					.build());
		}
		return piece;
	}

	/**
	 * One id per slot. Modifiers with the same id replace each other when worn, so with a shared id a full set
	 * only counted one piece's defense.
	 */
	private static Identifier id(String name, EquipmentSlotGroup slot) {
		return ArsenalMod.id(name + "." + slot.getSerializedName());
	}

	/**
	 * Gives a piece forged before each slot had its own id its slot's ids, so it adds to the rest of the set
	 * instead of replacing it. Keeps the amounts, damage, enchantments and name. Called while the piece is in a
	 * player's inventory (ArsenalArmorItem.inventoryTick). Returns true if the piece needed it.
	 */
	public static boolean fixSharedIds(ItemStack piece) {
		ItemAttributeModifiers old = piece.get(DataComponents.ATTRIBUTE_MODIFIERS);
		if (old == null || old.modifiers().stream().noneMatch(entry -> isOld(entry.modifier()))) {
			return false;
		}
		ItemAttributeModifiers fixed = ItemAttributeModifiers.EMPTY;
		for (ItemAttributeModifiers.Entry entry : old.modifiers()) {
			AttributeModifier modifier = entry.modifier();
			if (isOld(modifier)) {
				modifier = new AttributeModifier(id(modifier.id().getPath(), entry.slot()), modifier.amount(), modifier.operation());
			}
			fixed = fixed.withModifierAdded(entry.attribute(), modifier, entry.slot());
		}
		piece.set(DataComponents.ATTRIBUTE_MODIFIERS, fixed);
		return true;
	}

	private static boolean isOld(AttributeModifier modifier) {
		return modifier.is(OLD_ARMOR_MODIFIER) || modifier.is(OLD_TOUGHNESS_MODIFIER);
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
