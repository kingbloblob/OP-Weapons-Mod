package com.onepiecearsenal.compat;

import net.minecraft.world.entity.LivingEntity;

import com.onepiecemod.npc.HumanNpc;
import com.onepiecemod.world.SeastoneEffects;

/**
 * Every place Arsenal reads or changes the One Piece Mod goes through here. If the main mod
 * renames something, this is the only file that needs fixing.
 */
public final class OnePieceBridge {
	private OnePieceBridge() {
	}

	/** True for players who ate a fruit, and for story characters who have one. */
	public static boolean isFruitUser(LivingEntity entity) {
		return SeastoneEffects.isFruitUser(entity);
	}

	/** The main mod's seastone weakness, the same one its cuffs and jitte use. 20 ticks = 1 second. */
	public static void drainFruitPower(LivingEntity entity, int ticks) {
		SeastoneEffects.drain(entity, ticks);
	}

	/** True for the main mod's pirate NPCs (not Marines, not townsfolk). */
	public static boolean isPirateNpc(LivingEntity entity) {
		return entity instanceof HumanNpc npc && !npc.isMarineSide();
	}
}
