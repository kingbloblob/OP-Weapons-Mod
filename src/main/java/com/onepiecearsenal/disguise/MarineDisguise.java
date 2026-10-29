package com.onepiecearsenal.disguise;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Faction;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.ArsenalArmorItem;

/**
 * Dress as a Marine and the Navy leaves you alone. Wear a full set of Arsenal armor (hat, coat, sash and boots)
 * whose shells are all Navy materials, and Marines stop hunting you, as long as your bounty isn't too famous
 * for the uniform. Hit a Marine and the disguise is blown for a while. Senior Marines see through a cheap uniform
 * (see OnePieceBridge.marineInsight).
 *
 * MobMixin asks this class before a Marine picks a player as a target. Change the numbers here to tune it.
 *
 * Named Marines (Garp, Akainu, Smoker...) are the main mod's story characters, not Marines, so no uniform
 * fools them. That's on purpose.
 */
public class MarineDisguise {
	/** How well the uniform hides you, from the lowest-tier shell in the set. */
	public enum Quality {
		NONE(0, ""),
		/** Tier 1 to 2 shells (Marine Standard Steel). Fools the rank and file. */
		BASIC(10_000_000L, "recruit's uniform"),
		/** Tier 3 shells (Kairoseki). Also fools Captains and CP agents. */
		OFFICER(100_000_000L, "officer's uniform"),
		/** Tier 4 and up, for materials still to come. Fools everyone. */
		ADMIRAL(500_000_000L, "admiral's uniform");

		/** The highest bounty this uniform can hide. Above it, famous pirates get recognized. */
		public final long hidesBountyUpTo;
		public final String displayName;

		Quality(long hidesBountyUpTo, String displayName) {
			this.hidesBountyUpTo = hidesBountyUpTo;
			this.displayName = displayName;
		}

		/** True if a Marine this hard to fool (OnePieceBridge.marineInsight) falls for this uniform. */
		public boolean fools(int insight) {
			return ordinal() > insight;
		}
	}

	/** How long hitting a Marine blows your cover. 20 ticks = 1 second. */
	private static final int BLOWN_TICKS = 1200;
	/** How far away a Marine that is already chasing you can be and still lose you when the uniform goes on. */
	private static final double LOSE_TRACK_RANGE = 48.0;
	/** Hunters sent after you look this far for you (the main mod's MarineEntity uses the same range). */
	private static final double HUNT_RANGE = 96.0;
	/** How long hunters search for a disguised pirate before the squad goes home, like when they lose you. */
	private static final int HUNTERS_GIVE_UP_TICKS = 1200;
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	/** When the player's cover stops being blown (game time). Saved with the player, so logging out doesn't reset it. */
	public static final AttachmentType<Long> BLOWN_UNTIL = AttachmentRegistry.create(
			ArsenalMod.id("disguise_blown_until"),
			builder -> builder.persistent(Codec.LONG)
	);

	/** What each player was told last, so the message only shows when something changes. */
	private static final Map<UUID, String> LAST_STATE = new HashMap<>();
	/** How long each hunter has been fooled by the pirate it was sent for. Forgets Marines that unload. */
	private static final Map<Mob, Integer> FOOLED_HUNTERS = new WeakHashMap<>();

	public static void initialize() {
		// Hitting a Marine blows your cover, with a sword, a gun or a fist, even if it blocks the hit.
		// Checked before the hit lands, so the Marine is free to hit back.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (OnePieceBridge.isMarine(entity) && source.getEntity() instanceof ServerPlayer attacker && quality(attacker) != Quality.NONE) {
				attacker.setAttached(BLOWN_UNTIL, attacker.level().getGameTime() + BLOWN_TICKS);
			}
			return true;
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_STATE.remove(handler.player.getUUID()));

		// Once a second: tell players when their disguise starts or stops working.
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 20 != 0) {
				return;
			}
			Set<Mob> stillFooled = new HashSet<>();
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
					// Hunters sent after you can't tell you apart from their own. After a minute they go home.
					boolean gaveUp = false;
					for (Mob hunter : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(HUNT_RANGE),
							mob -> OnePieceBridge.isHunting(mob, player) && fools(mob, player))) {
						int ticks = FOOLED_HUNTERS.merge(hunter, 20, Integer::sum);
						if (ticks >= HUNTERS_GIVE_UP_TICKS) {
							hunter.discard();
							gaveUp = true;
						} else {
							stillFooled.add(hunter);
						}
					}
					if (gaveUp) {
						player.sendSystemMessage(Component.literal("The Marines hunting you give up the search.").withStyle(ChatFormatting.AQUA));
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
			// A hunter that sees through you again starts its minute over next time.
			FOOLED_HUNTERS.keySet().removeIf(mob -> mob.level() == level && !stillFooled.contains(mob));
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
		Long until = player.getAttached(BLOWN_UNTIL);
		return until != null && player.level().getGameTime() < until;
	}

	/** Whether this Marine is fooled by the player's uniform right now. */
	public static boolean fools(LivingEntity marine, Player player) {
		Quality quality = quality(player);
		if (quality == Quality.NONE || isBlown(player) || OnePieceBridge.bounty(player) > quality.hidesBountyUpTo) {
			return false;
		}
		// Captains have seen too many recruits to be fooled by a borrowed uniform, and admirals know their officers.
		return quality.fools(OnePieceBridge.marineInsight(marine));
	}
}
