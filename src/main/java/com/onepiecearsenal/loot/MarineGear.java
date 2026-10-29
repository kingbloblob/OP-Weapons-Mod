package com.onepiecearsenal.loot;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;

/**
 * Some Marines carry Navy-issue Arsenal weapons instead of the main mod kit's sword or pistol: a Marine Standard
 * Steel katana (Kairoseki-bladed for senior officers) or a Marine Standard Steel flintlock. Beat them and the weapon
 * sometimes drops, worn from use, so Navy gear can be captured, not just forged.
 *
 * Each Marine is checked once, the first time it's added to a world. Change the chances here to tune it.
 */
public class MarineGear {
	/** True once a Marine has been checked, so reloading its chunk doesn't roll again. Saved with the Marine. */
	public static final AttachmentType<Boolean> OUTFITTED = AttachmentRegistry.create(
			ArsenalMod.id("marine_outfitted"),
			builder -> builder.persistent(Codec.BOOL)
	);

	/** How often a Navy-issue weapon drops. The main mod's own guns and blades drop 4% of the time. */
	private static final float DROP_CHANCE = 0.08f;
	/** Chance a Marine with a flintlock pistol carries an Arsenal flintlock instead. */
	private static final float FLINTLOCK_CHANCE = 0.2f;
	private static final Identifier FLINTLOCK_PISTOL = Identifier.fromNamespaceAndPath("onepiece", "flintlock_pistol");

	public static void initialize() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob marine && OnePieceBridge.isMarine(marine) && !marine.hasAttached(OUTFITTED)) {
				marine.setAttached(OUTFITTED, true);
				outfit(marine, marine.getRandom());
			}
		});
	}

	/** Maybe swaps the Marine's sword or pistol for an Arsenal one. */
	public static void outfit(Mob marine, RandomSource random) {
		String kit = OnePieceBridge.marineKit(marine);
		ItemStack held = marine.getMainHandItem();
		ItemStack gear = ItemStack.EMPTY;
		if (held.is(ItemTags.SWORDS) && !(held.getItem() instanceof KatanaItem)) {
			if (random.nextFloat() < katanaChance(kit)) {
				// Senior officers' blades are Kairoseki, as strong as the diamond and netherite swords they'd carry.
				ArsenalMaterial blade = OnePieceBridge.marineInsight(marine) > 0 ? ArsenalMaterial.KAIROSEKI : ArsenalMaterial.MARINE_STEEL;
				gear = KatanaStats.build(blade, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.MARINE_STEEL);
			}
		} else if (BuiltInRegistries.ITEM.getKey(held.getItem()).equals(FLINTLOCK_PISTOL) && random.nextFloat() < FLINTLOCK_CHANCE) {
			gear = GunStats.build(ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.MARINE_STEEL);
		}
		if (!gear.isEmpty()) {
			marine.setItemSlot(EquipmentSlot.MAINHAND, gear);
			marine.setDropChance(EquipmentSlot.MAINHAND, DROP_CHANCE);
		}
	}

	/** Chance a sword-carrying Marine of this main mod kit carries an Arsenal katana instead. */
	private static float katanaChance(String kit) {
		return switch (kit) {
			case "marine" -> 0.1f;
			case "marine_officer", "cp_agent", "jailer" -> 0.25f;
			case "elite_marine" -> 0.4f;
			case "vice_admiral", "cp0_agent", "seraphim" -> 0.5f;
			default -> 0.0f; // Recruits carry wooden and stone swords: nothing worth swapping.
		};
	}
}
