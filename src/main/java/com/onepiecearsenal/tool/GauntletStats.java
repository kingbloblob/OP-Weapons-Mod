package com.onepiecearsenal.tool;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.Vec3;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.material.TraitStats;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.salvage.Breakage;

/**
 * Works out a pair of gauntlets' numbers from its plates and lining and saves them on the item. Gauntlets are armor for
 * your forearms, worn in the offhand (right-click to put them on): they're for fighters who'd rather keep a free hand
 * than carry a shield, not a weapon (fists are the One Piece Mod's fighting styles' business).
 * - The Gauntlet Plates (the head) give armor like a hat does, and toughness by tier, like any armor shell. Their trait
 *   works like an armor piece's: Spined plates prick, Kairoseki plates drain, Plated plates deflect shots.
 * - The Lining adds its handle trait.
 * - Guard: while sneaking, melee hits from in front of you lose a quarter of their damage, and wear the gauntlets a little.
 */
public final class GauntletStats {
	/** Share of a melee hit from the front a sneaking wearer's Guard takes off. */
	public static final float GUARD_SHARE = 0.25f;

	private GauntletStats() {
	}

	/** The parts in the order they are saved: plates, lining. */
	public static PartType[] parts() {
		return new PartType[] {PartType.GAUNTLET_PLATES, PartType.LINING};
	}

	/** Armor points: the plates' set strength shared out like a hat's. */
	public static int defense(ArsenalMaterial plates) {
		return ArmorStats.defense(plates, ArmorType.HELMET);
	}

	/** The same square-root curve as armor (ArmorStats.durability), a little sturdier than a hat. */
	public static int durability(ArsenalMaterial plates, ArsenalMaterial lining) {
		int durability = Math.round(16.0f * (float) Math.sqrt((plates.durability + lining.durability) / 2.0f) * 0.75f);
		return TraitStats.durability(durability, EnumSet.of(plates.headTrait, lining.handleTrait));
	}

	/** Builds a finished pair. */
	public static ItemStack build(ArsenalMaterial plates, ArsenalMaterial lining) {
		ItemStack gauntlets = new ItemStack(ArsenalItems.GAUNTLETS);
		gauntlets.set(ArsenalComponents.GAUNTLET_PARTS, List.of(plates.id(), lining.id()));

		EquipmentSlotGroup slot = EquipmentSlotGroup.OFFHAND;
		ItemAttributeModifiers modifiers = ItemAttributeModifiers.EMPTY
				.withModifierAdded(Attributes.ARMOR,
						new AttributeModifier(id("gauntlet_plates"), defense(plates), AttributeModifier.Operation.ADD_VALUE), slot);
		float toughness = ArmorStats.toughness(plates);
		if (toughness > 0) {
			modifiers = modifiers.withModifierAdded(Attributes.ARMOR_TOUGHNESS,
					new AttributeModifier(id("gauntlet_toughness"), toughness, AttributeModifier.Operation.ADD_VALUE), slot);
		}
		Set<Trait> traits = EnumSet.of(plates.headTrait, lining.handleTrait);
		gauntlets.set(DataComponents.ATTRIBUTE_MODIFIERS, TraitStats.modifiers(modifiers, traits, slot));
		gauntlets.set(DataComponents.MAX_DAMAGE, durability(plates, lining));
		TraitStats.finish(gauntlets, traits);
		return gauntlets;
	}

	private static Identifier id(String name) {
		return ArsenalMod.id(name + "." + EquipmentSlotGroup.OFFHAND.getSerializedName());
	}

	/** The pair's materials as plates, lining. Empty for gauntlets that weren't built from parts. */
	public static List<ArsenalMaterial> parts(ItemStack gauntlets) {
		List<ArsenalMaterial> parts = new ArrayList<>();
		for (String id : gauntlets.getOrDefault(ArsenalComponents.GAUNTLET_PARTS, List.<String>of())) {
			ArsenalMaterial material = ArsenalMaterial.byId(id);
			if (material == null) {
				return List.of();
			}
			parts.add(material);
		}
		return parts.size() == 2 ? parts : List.of();
	}

	/** The plates' head trait and the lining's handle trait. */
	public static Set<Trait> traits(ItemStack gauntlets) {
		List<ArsenalMaterial> parts = parts(gauntlets);
		return parts.isEmpty() ? EnumSet.noneOf(Trait.class) : EnumSet.of(parts.get(0).headTrait, parts.get(1).handleTrait);
	}

	/** Working (not broken) forged gauntlets in the entity's offhand, or an empty stack. */
	public static ItemStack worn(LivingEntity entity) {
		ItemStack offhand = entity.getOffhandItem();
		return offhand.getItem() instanceof ArsenalGauntletItem && !parts(offhand).isEmpty() && !Breakage.isBroken(offhand)
				? offhand : ItemStack.EMPTY;
	}

	/**
	 * Guard (from LivingEntityMixin, as a hit starts): a sneaking wearer turns a quarter of a melee hit from the front
	 * aside on their forearms. Returns the damage to take.
	 */
	public static float guard(LivingEntity defender, DamageSource source, float amount) {
		if (amount <= 0 || !defender.isShiftKeyDown() || source.is(DamageTypeTags.BYPASSES_ARMOR)
				|| !(source.getDirectEntity() instanceof LivingEntity attacker) || attacker != source.getEntity()) {
			return amount;
		}
		ItemStack gauntlets = worn(defender);
		if (gauntlets.isEmpty()) {
			return amount;
		}
		Vec3 toAttacker = attacker.position().subtract(defender.position()).with(Direction.Axis.Y, 0).normalize();
		Vec3 facing = defender.getViewVector(1.0f).with(Direction.Axis.Y, 0).normalize();
		if (toAttacker.dot(facing) < 0.0) {
			return amount; // From behind or the side: the arms don't cover it.
		}
		gauntlets.hurtAndBreak(1, defender, EquipmentSlot.OFFHAND);
		defender.level().playSound(null, defender.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
		return amount * (1.0f - GUARD_SHARE);
	}
}
