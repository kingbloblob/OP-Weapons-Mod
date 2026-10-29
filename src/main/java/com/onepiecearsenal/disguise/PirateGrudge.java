package com.onepiecearsenal.disguise;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.onepiecearsenal.compat.OnePieceBridge;

/**
 * The other side of a Marine uniform: pirates see a Marine. Enemy pirates who can see a player in a full Navy
 * uniform drop whatever they're fighting (Marines included) and go for the player instead, blown cover or not.
 * Emperor crews are left to their Emperor's mood (see the main mod's EmperorTerritories).
 *
 * Change the numbers here to tune it. Checked once a second.
 */
public class PirateGrudge {
	/** How far pirates spot a Marine uniform. */
	private static final double SPOT_RANGE = 24.0;
	/** How long before a player hears the pirates' shout again. 20 ticks = 1 second. */
	private static final int SHOUT_EVERY = 200;

	private static final Map<UUID, Long> LAST_SHOUT = new HashMap<>();

	public static void initialize() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_SHOUT.remove(handler.player.getUUID()));

		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : level.getPlayers(p -> !p.isCreative() && !p.isSpectator())) {
				if (MarineDisguise.quality(player) == MarineDisguise.Quality.NONE) {
					continue;
				}
				List<Mob> pirates = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(SPOT_RANGE),
						mob -> OnePieceBridge.isPirateNpc(mob) && !OnePieceBridge.isEmperorCrew(mob)
								&& !(mob.getTarget() instanceof Player) && mob.hasLineOfSight(player));
				if (pirates.isEmpty()) {
					continue;
				}
				for (Mob pirate : pirates) {
					pirate.setTarget(player);
				}
				long now = level.getGameTime();
				Long last = LAST_SHOUT.get(player.getUUID());
				if (last == null || now - last >= SHOUT_EVERY) {
					LAST_SHOUT.put(player.getUUID(), now);
					player.sendSystemMessage(Component.literal("<" + pirates.get(0).getDisplayName().getString() + "> A Navy dog! Get 'em!")
							.withStyle(ChatFormatting.DARK_RED));
				}
			}
		});
	}
}
