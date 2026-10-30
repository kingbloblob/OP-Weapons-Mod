package com.onepiecearsenal.compat;

import java.util.List;
import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import com.onepiecemod.character.BossStrikes;
import com.onepiecemod.character.CharacterEntity;
import com.onepiecemod.character.CharacterFeature;
import com.onepiecemod.haki.HakiAttachments;
import com.onepiecemod.haki.HakiData;
import com.onepiecemod.haki.HakiPowers;
import com.onepiecemod.news.NewsLog;
import com.onepiecemod.npc.HumanNpc;
import com.onepiecemod.style.FightingStyle;
import com.onepiecemod.style.StyleAttachments;
import com.onepiecemod.style.StyleMoves;
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

	/**
	 * Prints a story in the News Coo's next paper, the one world events and other players' deeds go in.
	 * announce = also tell everyone in chat right now.
	 */
	public static void reportNews(MinecraftServer server, String headline, String story, boolean announce) {
		NewsLog.report(server, headline, story, announce);
	}

	/** True while the player fights in Black Leg Style with empty hands, so their hits are kicks. */
	public static boolean isKicking(Player player) {
		return StyleAttachments.isUsing(player, FightingStyle.BLACK_LEG) && player.getMainHandItem().isEmpty();
	}

	/** The player's best Haki level of any type, 0 (not awakened) to 5 (master). */
	public static int hakiLevel(Player player) {
		HakiData haki = HakiAttachments.get(player);
		return Math.max(haki.armamentLevel(), Math.max(haki.observationLevel(), haki.conquerorLevel()));
	}

	/** The player's Armament Haki level if they have it switched on, 0 if it's off or not awakened. */
	public static int activeArmamentLevel(Player player) {
		HakiData haki = HakiAttachments.get(player);
		return haki.armamentOn() ? haki.armamentLevel() : 0;
	}

	/** The tier (1 to 5) of a main mod boss, like 5 for an Emperor or Admiral. 0 if it isn't a boss. */
	public static int bossTier(LivingEntity entity) {
		return entity instanceof CharacterEntity character && character.isBoss() && character.getData() != null
				? Math.max(1, character.getData().tier) : 0;
	}

	/** The main mod character id of a boss, like "akainu". Empty for anything that isn't a boss character. */
	public static String bossId(LivingEntity entity) {
		return entity instanceof CharacterEntity character && character.isBoss() ? character.getCharacterId() : "";
	}

	/** The boss fight's difficulty: 0 Normal, 1 Hard, 2 Expert. */
	public static int bossDifficulty(LivingEntity entity) {
		return entity instanceof CharacterEntity character ? character.getDifficulty() : 0;
	}

	/**
	 * False when everyone who beat this boss had already won at this difficulty. The main mod only hands out its
	 * prizes on a first win, so rematches can't farm them. Set as the boss dies.
	 */
	public static boolean isFirstBossWin(LivingEntity entity) {
		return entity instanceof CharacterEntity character && character.isFirstWin();
	}

	/** True for the crew a boss calls in mid-fight. They come back every fight, so they drop nothing rare. */
	public static boolean isBossMinion(LivingEntity entity) {
		return entity.entityTags().contains(BossStrikes.MINION_TAG);
	}

	/** True for anything in the minecraft:swords tag, Arsenal katanas included. */
	public static boolean isSword(ItemStack stack) {
		return WeaponUpgrades.isSword(stack);
	}

	/** The sword's grade: 0 ungraded, 1 Skillful, 2 Great, 3 Supreme. Famous blades like Yoru start graded. */
	public static int swordGrade(ItemStack stack) {
		return WeaponUpgrades.grade(stack);
	}

	/** Sets the sword's grade (0 to 3) and its grade damage bonus, as a whetstone would. */
	public static void setSwordGrade(ItemStack stack, int grade) {
		WeaponUpgrades.setGrade(stack, grade);
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

	/** True while the player's Armament Haki is switched on and coating their hits. */
	public static boolean armamentActive(Player player) {
		return HakiPowers.hasArmamentActive(player);
	}

	// ---- Who a weapon skill may hit: the same rule as the main mod's fighting style techniques ----

	/** Monsters, bosses and anything fighting the player: yes. Crewmates, friendly characters, pets and animals: no. */
	public static boolean isEnemy(Player player, LivingEntity entity) {
		return StyleMoves.isEnemy(player, entity);
	}

	/** Enemies within radius blocks of the player. */
	public static List<LivingEntity> enemiesAround(Player player, ServerLevel level, double radius) {
		return StyleMoves.around(player, level, radius);
	}

	/** Enemies along the line the player is looking at, stopping at the first solid block. */
	public static List<LivingEntity> enemiesInLine(Player player, ServerLevel level, double range, double width, int max) {
		return StyleMoves.inLine(player, level, range, width, max);
	}

	/** Enemies in front of the player that they can see. minDot 1.0 is a thin line, 0.5 is a wide fan. */
	public static List<LivingEntity> enemiesInCone(Player player, ServerLevel level, double range, double minDot, int max) {
		return StyleMoves.inCone(player, level, range, minDot, max);
	}
}
