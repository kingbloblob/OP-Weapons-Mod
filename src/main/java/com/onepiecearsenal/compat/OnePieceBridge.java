package com.onepiecearsenal.compat;

import java.util.Set;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.onepiecemod.bounty.Bounty;
import com.onepiecemod.bounty.MarineEntity;
import com.onepiecemod.bounty.MarineHunters;
import com.onepiecemod.bounty.MarineRank;
import com.onepiecemod.character.CharacterEntity;
import com.onepiecemod.character.CharacterFeature;
import com.onepiecemod.haki.HakiAttachments;
import com.onepiecemod.haki.HakiData;
import com.onepiecemod.npc.HumanNpc;
import com.onepiecemod.weapon.WeaponItems;
import com.onepiecemod.weapon.WeaponUpgrades;
import com.onepiecemod.world.SeastoneEffects;
import com.onepiecemod.worldevent.EmperorTerritories;

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

	/**
	 * True for pirates of one of the Four Emperors' crews, spawned in their territory. Their Emperor decides whether
	 * they fight you (EmperorTerritories), so Arsenal leaves them alone.
	 */
	public static boolean isEmperorCrew(LivingEntity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(EmperorTerritories.CREW_TAG)) {
				return true;
			}
		}
		return false;
	}

	/** True for the main mod's Marines and World Government agents. */
	public static boolean isMarine(LivingEntity entity) {
		return entity instanceof MarineEntity;
	}

	/** Main mod NPC kits (npc_kits.json) of Commanders and Captains, and of CP agents. */
	private static final Set<String> SENIOR_KITS = Set.of("elite_marine", "cp_agent");
	/** Kits of Rear and Vice Admirals, CP0, and the Pacifista and Seraphim who scan you against Vegapunk's records. */
	private static final Set<String> TOP_KITS = Set.of("vice_admiral", "cp0_agent", "pacifista", "seraphim");

	/**
	 * How hard a Marine is to fool: 0 for the rank and file, 1 for Captains and CP agents, 2 for admirals, CP0
	 * and Vegapunk's weapons. Read from the Marine's kit, since most Marines are spawned from one (guards, patrols,
	 * Buster Calls) and only hunters, raiders and boarders get a MarineRank.
	 */
	public static int marineInsight(LivingEntity entity) {
		if (!(entity instanceof MarineEntity marine)) {
			return 0;
		}
		String kit = marine.getKitId();
		if (TOP_KITS.contains(kit)) {
			return 2;
		}
		return SENIOR_KITS.contains(kit) || marine.getRank() == MarineRank.CAPTAIN ? 1 : 0;
	}

	/** The Marine's main mod NPC kit, like "marine_recruit" or "vice_admiral". Empty for anything else. */
	public static String marineKit(LivingEntity entity) {
		return entity instanceof MarineEntity marine ? marine.getKitId() : "";
	}

	/** True if this Marine was sent to hunt down this player. */
	public static boolean isHunting(Mob marine, Player player) {
		return MarineEntity.isHunting(marine, player);
	}

	/** True for the main mod's people (pirates, Marines, townsfolk, story characters). */
	public static boolean isNpc(LivingEntity entity) {
		return entity instanceof HumanNpc;
	}

	/** The main mod's money. */
	public static Item berry() {
		return CharacterFeature.BERRY;
	}

	/** Raises the player's bounty and tells them why, the way the main mod's own deeds do. */
	public static void raiseBounty(ServerPlayer player, long amount, String reason) {
		Bounty.raise(player, amount, reason, false);
	}

	/** Sends a Marine squad after the player, sized by their bounty (at least a pair of recruits). False if one is already on its way. */
	public static boolean sendMarineSquad(ServerPlayer player) {
		return MarineHunters.forceHunt(player);
	}

	/** The player's bounty in Berries. */
	public static long bounty(Player player) {
		return Bounty.get(player);
	}

	/** The player's best Haki level of any type, 0 (not awakened) to 5 (master). */
	public static int hakiLevel(Player player) {
		HakiData haki = HakiAttachments.get(player);
		return Math.max(haki.armamentLevel(), Math.max(haki.observationLevel(), haki.conquerorLevel()));
	}

	/** The tier (1 to 5) of a main mod boss, like 5 for an Emperor or Admiral. 0 if it isn't a boss. */
	public static int bossTier(LivingEntity entity) {
		return entity instanceof CharacterEntity character && character.isBoss() && character.getData() != null
				? Math.max(1, character.getData().tier) : 0;
	}

	/** True for anything in the minecraft:swords tag, Arsenal katanas included. */
	public static boolean isSword(ItemStack stack) {
		return WeaponUpgrades.isSword(stack);
	}

	/** The sword's grade: 0 ungraded, 1 Skillful, 2 Great, 3 Supreme. Famous blades like Yoru start graded. */
	public static int swordGrade(ItemStack stack) {
		return WeaponUpgrades.grade(stack);
	}

	/** "Great Grade (O Wazamono)" and so on. */
	public static String gradeName(int grade) {
		return WeaponUpgrades.GRADE_NAMES[grade];
	}

	/** The grade this whetstone raises a sword to (1 to 3), or 0 if it isn't a whetstone. */
	public static int whetstoneGrade(ItemStack stack) {
		if (stack.is(WeaponItems.SKILLFUL_WHETSTONE)) {
			return 1;
		}
		if (stack.is(WeaponItems.GREAT_WHETSTONE)) {
			return 2;
		}
		return stack.is(WeaponItems.SUPREME_WHETSTONE) ? 3 : 0;
	}
}
