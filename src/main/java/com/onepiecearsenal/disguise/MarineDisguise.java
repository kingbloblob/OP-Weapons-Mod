package com.onepiecearsenal.disguise;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Faction;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.ArsenalArmorItem;

/**
 * Dress as a Marine and the Navy leaves you alone. Wear a full set of Arsenal armor (hat, coat, sash and boots)
 * whose shells are all Navy materials, and Marines stop hunting you, as long as your bounty isn't too famous
 * for the uniform. Hit a Marine and the disguise is blown for a while. Captains see through a basic uniform.
 *
 * MarineEntityMixin asks this class before a Marine picks a player as a target. Change the numbers here to tune it.
 */
public class MarineDisguise {
	/** How well the uniform hides you, from the lowest-tier shell in the set. */
	public enum Quality {
		NONE(0, ""),
		/** Tier 1 to 2 shells (Marine Standard Steel). */
		BASIC(10_000_000L, "recruit's uniform"),
		/** Tier 3 shells (Kairoseki). */
		OFFICER(100_000_000L, "officer's uniform"),
		/** Tier 4 and up, for materials still to come. */
		ADMIRAL(500_000_000L, "admiral's uniform");

		/** The highest bounty this uniform can hide. Above it, famous pirates get recognized. */
		public final long hidesBountyUpTo;
		public final String displayName;

		Quality(long hidesBountyUpTo, String displayName) {
			this.hidesBountyUpTo = hidesBountyUpTo;
			this.displayName = displayName;
		}
	}

	/** How long hitting a Marine blows your cover. 20 ticks = 1 second. */
	private static final int BLOWN_TICKS = 1200;
	/** How far away a Marine that is already chasing you can be and still lose you when the uniform goes on. */
	private static final double LOSE_TRACK_RANGE = 48.0;
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	/** When each player's cover stops being blown (server game time). */
	private static final Map<UUID, Long> BLOWN_UNTIL = new HashMap<>();
	/** What each player was told last, so the message only shows when something changes. */
	private static final Map<UUID, String> LAST_STATE = new HashMap<>();

	public static void initialize() {
		// Hitting a Marine blows your cover, whether with a sword, a gun or a fist.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (OnePieceBridge.isMarine(entity) && source.getEntity() instanceof ServerPlayer attacker && quality(attacker) != Quality.NONE) {
				BLOWN_UNTIL.put(attacker.getUUID(), attacker.level().getGameTime() + BLOWN_TICKS);
			}
		});

		// Once a second: tell players when their disguise starts or stops working.
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : level.getPlayers(p -> true)) {
				Quality quality = quality(player);
				String state;
				if (quality == Quality.NONE) {
					state = "";
				} else if (isBlown(player)) {
					state = "blown";
				} else if (OnePieceBridge.bounty(player) > quality.hidesBountyUpTo) {
					state = "famous";
				} else {
					state = "hidden";
				}
				if (state.equals("hidden")) {
					// Marines already chasing you lose you once the uniform goes on.
					for (Mob marine : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(LOSE_TRACK_RANGE),
							mob -> mob.getTarget() == player && OnePieceBridge.isMarine(mob) && fools(mob, player))) {
						marine.setTarget(null);
					}
				}
				String last = LAST_STATE.put(player.getUUID(), state);
				if (last == null || last.equals(state)) {
					continue;
				}
				switch (state) {
					case "hidden" -> player.sendOverlayMessage(Component.literal("Your " + quality.displayName + " hides you from the Marines.").withStyle(ChatFormatting.AQUA));
					case "famous" -> player.sendOverlayMessage(Component.literal("Your bounty is too famous for a " + quality.displayName + ".").withStyle(ChatFormatting.RED));
					case "blown" -> player.sendOverlayMessage(Component.literal("Your disguise is blown!").withStyle(ChatFormatting.RED));
					default -> { }
				}
			}
		});
	}

	/** The uniform a player wears, or NONE unless all four armor pieces are Arsenal armor with Navy shells. */
	public static Quality quality(Player player) {
		int lowestTier = Integer.MAX_VALUE;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack piece = player.getItemBySlot(slot);
			if (!(piece.getItem() instanceof ArsenalArmorItem)) {
				return Quality.NONE;
			}
			List<ArsenalMaterial> parts = ArmorStats.parts(piece);
			if (parts.isEmpty() || parts.get(0).faction != Faction.NAVY) {
				return Quality.NONE;
			}
			lowestTier = Math.min(lowestTier, parts.get(0).tier);
		}
		if (lowestTier >= 4) {
			return Quality.ADMIRAL;
		}
		return lowestTier >= 3 ? Quality.OFFICER : Quality.BASIC;
	}

	public static boolean isBlown(Player player) {
		Long until = BLOWN_UNTIL.get(player.getUUID());
		return until != null && player.level().getGameTime() < until;
	}

	/** Whether this Marine is fooled by the player's uniform right now. */
	public static boolean fools(LivingEntity marine, Player player) {
		Quality quality = quality(player);
		if (quality == Quality.NONE || isBlown(player) || OnePieceBridge.bounty(player) > quality.hidesBountyUpTo) {
			return false;
		}
		// A Captain has seen too many recruits to be fooled by a borrowed uniform.
		return !(quality == Quality.BASIC && OnePieceBridge.isMarineCaptain(marine));
	}
}
