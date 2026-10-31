package com.onepiecearsenal.tool;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.material.TraitStats;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * Works out a shield's numbers from its three parts and saves them on the item. A shield blocks like vanilla's
 * (raise it with right-click), and its parts decide how well:
 * - The Shield Face (the head) sets how wide it covers (90 degrees at tier 1, 10 more a tier) and how long an axe can
 *   knock it down for (heavier faces shrug it off sooner). Its trait strikes back at whoever hits the raised shield.
 * - The rim (a Binding) and strap (a Grip) set how fast it comes up: light materials raise it quicker.
 * - All three share the durability, like a weapon's, and more of it, since every blocked hit wears it.
 *
 * Vanilla's shield, for comparison: 90 degrees, raised in 0.25 seconds, 336 durability, axes knock it down for 5 seconds.
 */
public final class ShieldStats {
	/** Durability = the parts' durability averaged, times this. Ship Iron gives 350, about a vanilla shield. */
	public static final float DURABILITY_SCALE = 1.4f;
	/** Seconds to raise it with plain parts (speed 0), and how much each point of the rim and strap's speed takes off. */
	private static final float BASE_DELAY = 0.25f;
	private static final float DELAY_PER_SPEED = 0.5f;
	/** How wide a tier 1 face covers, and how much wider each tier above. */
	private static final float BASE_ANGLE = 90.0f;
	private static final float ANGLE_PER_TIER = 10.0f;
	/** Share of the face material's damage its counter deals, for the traits that scale with a hit (Keen, Impact...). */
	private static final float COUNTER_SHARE = 0.5f;
	/** What a Spined face pricks a melee attacker for when it blocks: twice an armor piece's, it's all spikes. */
	private static final float SPINED_COUNTER = 2.0f;

	private ShieldStats() {
	}

	/** The parts in the order they are saved on the shield: face, rim, strap. */
	public static PartType[] parts() {
		return new PartType[] {PartType.SHIELD_FACE, PartType.BINDING, PartType.GRIP};
	}

	/** Seconds from right-click until it blocks. */
	public static float blockDelay(ArsenalMaterial rim, ArsenalMaterial strap) {
		float delay = BASE_DELAY - (rim.speed + strap.speed) / 2 * DELAY_PER_SPEED;
		if (rim.handleTrait == Trait.LIGHT || strap.handleTrait == Trait.LIGHT) {
			delay -= 0.05f;
		}
		return Math.max(0.05f, Math.min(0.5f, delay));
	}

	/** Degrees in front of you it covers. */
	public static float angle(ArsenalMaterial face) {
		return BASE_ANGLE + ANGLE_PER_TIER * (face.tier - 1);
	}

	/** How long an axe knocks it down for, as a share of vanilla's 5 seconds. */
	public static float disableScale(ArsenalMaterial face, Set<Trait> traits) {
		float scale = Math.max(0.4f, 1.0f - 0.15f * (face.tier - 1));
		if (traits.contains(Trait.HEAVY) || traits.contains(Trait.LEGENDARY_HULL)) {
			scale *= 0.75f;
		}
		return scale;
	}

	/** What the face's counter hits for, for the traits that scale with a hit. */
	public static float counterDamage(ArsenalMaterial face) {
		return face.damage * COUNTER_SHARE;
	}

	/** Builds a finished shield. */
	public static ItemStack build(ArsenalMaterial face, ArsenalMaterial rim, ArsenalMaterial strap) {
		ItemStack shield = new ItemStack(ArsenalItems.SHIELD);
		shield.set(ArsenalComponents.SHIELD_PARTS, List.of(face.id(), rim.id(), strap.id()));

		Set<Trait> traits = traits(face, rim, strap);
		shield.set(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
				blockDelay(rim, strap),
				disableScale(face, traits),
				List.of(new BlocksAttacks.DamageReduction(angle(face), Optional.empty(), 0.0f, 1.0f)),
				new BlocksAttacks.ItemDamageFunction(3.0f, 1.0f, 1.0f),
				Optional.of(DamageTypeTags.BYPASSES_SHIELD),
				Optional.of(SoundEvents.SHIELD_BLOCK),
				Optional.of(SoundEvents.SHIELD_BREAK)));

		// Held traits (Heavy's toughness, Legendary Hull's knockback resistance...) count from the offhand, where it's carried.
		shield.set(DataComponents.ATTRIBUTE_MODIFIERS, TraitStats.modifiers(ItemAttributeModifiers.EMPTY, traits, EquipmentSlotGroup.OFFHAND));
		int durability = Math.round((face.durability + rim.durability + strap.durability) / 3.0f * DURABILITY_SCALE);
		shield.set(DataComponents.MAX_DAMAGE, TraitStats.durability(durability, traits));
		TraitStats.finish(shield, traits);
		return shield;
	}

	/** The shield's materials as face, rim, strap. Empty for a shield that wasn't built from parts. */
	public static List<ArsenalMaterial> parts(ItemStack shield) {
		List<ArsenalMaterial> parts = new ArrayList<>();
		for (String id : shield.getOrDefault(ArsenalComponents.SHIELD_PARTS, List.<String>of())) {
			ArsenalMaterial material = ArsenalMaterial.byId(id);
			if (material == null) {
				return List.of();
			}
			parts.add(material);
		}
		return parts.size() == 3 ? parts : List.of();
	}

	/** The face's head trait, and the handle traits of the rim and strap. */
	public static Set<Trait> traits(ItemStack shield) {
		List<ArsenalMaterial> parts = parts(shield);
		return parts.isEmpty() ? EnumSet.noneOf(Trait.class) : traits(parts.get(0), parts.get(1), parts.get(2));
	}

	private static Set<Trait> traits(ArsenalMaterial face, ArsenalMaterial rim, ArsenalMaterial strap) {
		return EnumSet.of(face.headTrait, rim.handleTrait, strap.handleTrait);
	}

	/**
	 * The raised shield just stopped a melee hit: the face strikes back. Its traits go off on the attacker as if the
	 * blocker had hit them (a Kairoseki face drains a Devil Fruit user, a flint face may set them alight, and so on),
	 * a Spined face pricks them, and Spiked Boss upgrades add their own damage.
	 */
	public static void counter(ServerPlayer blocker, ServerLevel level, LivingEntity attacker, ItemStack shield) {
		List<ArsenalMaterial> parts = parts(shield);
		if (parts.isEmpty() || Breakage.isBroken(shield)) {
			return;
		}
		Set<Trait> traits = TraitEffects.traits(shield);
		TraitEffects.onHit(blocker, level, attacker, traits, counterDamage(parts.get(0)), false);
		float spikes = (float) Upgrades.total(shield, "block_thorns");
		if (traits.contains(Trait.SPINED)) {
			spikes += SPINED_COUNTER;
		}
		if (spikes > 0) {
			TraitEffects.extraHit(blocker, level, attacker, spikes, blocker.damageSources().thorns(blocker));
		}
	}
}
