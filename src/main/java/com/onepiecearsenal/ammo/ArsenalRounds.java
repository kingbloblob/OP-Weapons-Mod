package com.onepiecearsenal.ammo;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecemod.weapon.Ammo;
import com.onepiecemod.weapon.GunAddons;

/**
 * Arsenal's gun ammo: Musket Balls of any material and Dial Rounds. Both are One Piece Mod bullets (GunAddons),
 * so any pistol or rifle fires them, Arsenal-built or not. Like the mod's own ammo, a stack in your off hand is fired first.
 */
public final class ArsenalRounds {
	private ArsenalRounds() {
	}

	public static void initialize() {
		GunAddons.registerRound(ArsenalItems.MUSKET_BALL, Ammo.BULLET, new MusketBalls());
		for (DialRound round : DialRound.values()) {
			GunAddons.registerRound(ArsenalItems.DIAL_ROUNDS.get(round), Ammo.BULLET, round);
		}
	}
}
