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
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.onepiecearsenal.armorset.SetBonuses;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Faction;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;

/**
 * A uniform doesn't fool Marines forever. Marines close by who can see you slowly get suspicious, faster when
 * there are several of them, when they're senior, when you carry pirate-made weapons, or when your bounty is close
 * to what the uniform can hide. Walk away and they forget. Stay too long and one of them calls you out: your cover
 * is blown and the Marines who were watching attack.
 *
 * Change the numbers here to tune it. Suspicion goes up once a second.
 */
public class Suspicion {
	/** How close a Marine has to be to look you over. */
	private static final double WATCH_RANGE = 8.0;
	/** At most this many Marines count at once. */
	private static final int MAX_WATCHERS = 3;
	/** The player is warned when suspicion reaches this. */
	private static final int NOTICED_AT = 10;
	/** A Marine calls the player out at this. With one recruit watching and nothing else, that's 20 seconds. */
	private static final int CAUGHT_AT = 20;

	private static final Map<UUID, Float> SUSPICION = new HashMap<>();

	public static void initialize() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SUSPICION.remove(handler.player.getUUID()));

		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : level.getPlayers(p -> !p.isCreative() && !p.isSpectator())) {
				MarineDisguise.Quality cover = MarineDisguise.cover(player);
				if (cover == MarineDisguise.Quality.NONE) {
					SUSPICION.remove(player.getUUID());
					continue;
				}
				List<Mob> watchers = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(WATCH_RANGE),
						mob -> OnePieceBridge.isMarine(mob) && mob.getTarget() == null && mob.hasLineOfSight(player));
				float before = SUSPICION.getOrDefault(player.getUUID(), 0.0f);
				float after = watchers.isEmpty() ? Math.max(0.0f, before - 1.0f) : before + gain(player, cover, watchers);

				if (after >= CAUGHT_AT) {
					caught(player, watchers);
					SUSPICION.remove(player.getUUID());
					continue;
				}
				if (before < NOTICED_AT && after >= NOTICED_AT) {
					player.sendOverlayMessage(Component.literal("A Marine is eyeing you suspiciously...").withStyle(ChatFormatting.YELLOW));
				}
				if (after <= 0.0f) {
					SUSPICION.remove(player.getUUID());
				} else {
					SUSPICION.put(player.getUUID(), after);
				}
			}
		});
	}

	/** How much more suspicious the watching Marines get this second. A Regulation Uniform set slows it (SetBonuses). */
	private static float gain(Player player, MarineDisguise.Quality cover, List<Mob> watchers) {
		int gain = 0;
		for (int i = 0; i < Math.min(MAX_WATCHERS, watchers.size()); i++) {
			gain += 1 + OnePieceBridge.marineInsight(watchers.get(i));
		}
		if (carriesPirateGear(player)) {
			gain *= 2; // No Marine carries a pirate's blade.
		}
		if (OnePieceBridge.bounty(player) > MarineDisguise.hidesBountyUpTo(player, cover) / 2) {
			gain += 1; // That face looks familiar...
		}
		return gain * (float) SetBonuses.suspicion(player);
	}

	/** The closest watcher calls the player out, and everyone who was watching attacks. */
	private static void caught(ServerPlayer player, List<Mob> watchers) {
		Mob closest = watchers.get(0);
		for (Mob watcher : watchers) {
			if (watcher.distanceToSqr(player) < closest.distanceToSqr(player)) {
				closest = watcher;
			}
		}
		MarineDisguise.blow(player);
		player.sendSystemMessage(Component.literal("<" + closest.getDisplayName().getString() + "> Hey! You're no Marine!")
				.withStyle(ChatFormatting.RED));
		for (Mob watcher : watchers) {
			watcher.setTarget(player);
		}
	}

	/** True if the player holds an Arsenal weapon or gun forged with a pirate blade, head or barrel. */
	public static boolean carriesPirateGear(Player player) {
		return isPirateMade(player.getMainHandItem()) || isPirateMade(player.getOffhandItem());
	}

	private static boolean isPirateMade(ItemStack stack) {
		List<ArsenalMaterial> parts = KatanaStats.parts(stack);
		if (parts.isEmpty()) {
			parts = WeaponStats.parts(stack); // Cutlass, Boarding Axe, Kanabo and the rest.
		}
		if (parts.isEmpty()) {
			parts = GunStats.parts(stack);
		}
		return !parts.isEmpty() && parts.get(0).faction == Faction.PIRATE;
	}
}
