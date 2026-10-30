package com.onepiecearsenal.meito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;

/**
 * Meito: the graded blades. Two gates keep them rare.
 *
 * The blade's gate: an Arsenal katana levels up from kills, and its grade caps its level. A One Piece Mod whetstone
 * only raises its grade once the blade is at that cap, made of a strong enough material, and has done a deed
 * (25 armed enemies, then a named boss, then an Emperor or Admiral).
 *
 * The wielder's gate: any graded sword, the main mod's famous blades like Yoru included, refuses a player without
 * enough Haki or a big enough bounty. Holding one anyway makes you weak, and a whetstone won't raise a blade past
 * what you could wield.
 *
 * Black Blade awakening: a Supreme Grade katana that fights long enough coated in its wielder's Armament Haki turns
 * black for good. Its blade becomes Black Blade Steel (KatanaStats.awaken), which carries the Black Blade trait.
 * Only seconds of real fighting count: Armament on at a high enough level, and a hit landed on a foe in the last few seconds.
 *
 * Change the numbers here to tune it.
 */
public final class Meito {
	/** The highest level a blade can reach at each grade: ungraded, Skillful, Great, Supreme. */
	private static final int[] MAX_LEVEL = {5, 10, 15, 20};
	/** The lowest blade material tier for each grade. Index = the grade being promoted to. */
	private static final int[] BLADE_TIER = {0, 2, 3, 3};
	/** Armed enemies a blade must beat before it can become Skillful Grade. */
	private static final int SKILLFUL_ARMED_KILLS = 25;
	/** The boss tier a blade must beat for Supreme Grade: Emperors, Admirals, Mihawk and the like. */
	private static final int SUPREME_BOSS_TIER = 5;

	/** Haki level (any type) a player needs to wield each grade. Or the bounty below, whichever comes first. */
	private static final int[] WIELD_HAKI = {0, 1, 3, 5};
	/** Bounty a player needs to wield each grade: Loguetown, Enies Lobby, Emperor class. */
	private static final long[] WIELD_BOUNTY = {0, 30_000_000L, 300_000_000L, 1_000_000_000L};

	/** Extra attack damage per blade level, so a level 20 Supreme blade gets +2. */
	private static final double DAMAGE_PER_LEVEL = 0.1;
	/** The blade level's damage bonus. Public so Restat keeps it when it re-works a katana's stats. */
	public static final Identifier LEVEL_MODIFIER = ArsenalMod.id("blade_level");
	/** Blade XP for any enemy, and the extra for one holding a weapon or for each boss tier. */
	private static final int KILL_XP = 1;
	private static final int ARMED_XP = 2;
	private static final int BOSS_XP_PER_TIER = 5;

	/** Seconds of Haki-coated fighting a Supreme Grade katana needs to awaken: 2 hours. */
	public static final int AWAKEN_SECONDS = 2 * 60 * 60;
	/** The Armament Haki level the wielder needs switched on for the blade to soak it up. */
	public static final int AWAKEN_ARMAMENT = 4;
	/** A second counts as fighting if the wielder landed a coated hit this many ticks ago or less. */
	private static final int FIGHT_WINDOW_TICKS = 5 * 20;
	/** When each player last landed a Haki-coated katana hit on a foe, in game ticks. */
	private static final Map<UUID, Long> LAST_COATED_HIT = new HashMap<>();

	private Meito() {
	}

	/** One promotion requirement and whether it's met, for the tooltip and the whetstone's refusal. */
	public record Need(String text, boolean met) {
	}

	public static void initialize() {
		// A whetstone on a sword in the off hand: check both gates before the main mod applies the grade.
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer smith)) {
				return InteractionResult.PASS; // The server decides; the client just waits for its answer.
			}
			int target = OnePieceBridge.whetstoneGrade(player.getMainHandItem());
			ItemStack sword = player.getOffhandItem();
			if (target == 0 || !OnePieceBridge.isSword(sword) || OnePieceBridge.swordGrade(sword) != target - 1) {
				return InteractionResult.PASS; // Not ours to judge: the main mod explains wrong-grade whetstones.
			}
			String problem = promotionProblem(smith, sword, target);
			if (problem == null) {
				return InteractionResult.PASS;
			}
			smith.sendOverlayMessage(Component.literal(problem).withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		});

		// A katana that finishes off an enemy learns from it.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer player && player != entity
					&& player.getMainHandItem().getItem() instanceof KatanaItem && !Breakage.isBroken(player.getMainHandItem())) {
				onKill(player, player.getMainHandItem(), entity);
			}
		});

		// A Haki-coated katana hit on a foe keeps the fight going for awakening.
		AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
			if (player instanceof ServerPlayer fighter && target instanceof LivingEntity foe && isFoe(foe)
					&& fighter.getMainHandItem().getItem() instanceof KatanaItem && !Breakage.isBroken(fighter.getMainHandItem())
					&& OnePieceBridge.activeArmamentLevel(fighter) >= AWAKEN_ARMAMENT) {
				LAST_COATED_HIT.put(fighter.getUUID(), level.getGameTime());
			}
			return InteractionResult.PASS;
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_COATED_HIT.remove(handler.getPlayer().getUUID()));

		// Once a second: a graded blade weighs down a player it refuses, and soaks up the Haki of one it accepts.
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 20 != 0) {
				return;
			}
			boolean remind = level.getGameTime() % 100 == 0;
			for (ServerPlayer player : level.getPlayers(p -> !p.isSpectator())) {
				ItemStack sword = player.getMainHandItem();
				if (!OnePieceBridge.isSword(sword)) {
					continue;
				}
				int grade = OnePieceBridge.swordGrade(sword);
				if (worthy(player, grade)) {
					soak(player, level, sword);
				} else if (grade > 0) {
					player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, grade - 1, false, false, true));
					if (remind) {
						player.sendOverlayMessage(Component.literal(sword.getHoverName().getString() + " refuses you. It needs "
								+ wieldRequirement(grade) + ".").withStyle(ChatFormatting.DARK_RED));
					}
				}
			}
		});
	}

	// ---- The wielder ----

	/** True if this player has the Haki or the bounty to wield a blade of this grade. */
	public static boolean worthy(Player player, int grade) {
		return OnePieceBridge.hakiLevel(player) >= WIELD_HAKI[grade] || OnePieceBridge.bounty(player) >= WIELD_BOUNTY[grade];
	}

	/** "Haki 3 or a 300M bounty". Kept short so it fits on the action bar. */
	public static String wieldRequirement(int grade) {
		long bounty = WIELD_BOUNTY[grade];
		String berries = bounty >= 1_000_000_000L ? bounty / 1_000_000_000L + "B" : bounty / 1_000_000L + "M";
		return "Haki " + WIELD_HAKI[grade] + " or a " + berries + " bounty";
	}

	// ---- The blade ----

	/** True for things worth a blade's attention: monsters, people, players and bosses. No XP or Haki for cows. */
	private static boolean isFoe(LivingEntity entity) {
		return entity instanceof Enemy || entity instanceof Player || OnePieceBridge.isNpc(entity) || OnePieceBridge.bossTier(entity) > 0;
	}

	/** True for a Supreme Grade Arsenal katana that hasn't awakened yet. */
	public static boolean canAwaken(ItemStack katana) {
		return katana.getItem() instanceof KatanaItem && OnePieceBridge.swordGrade(katana) >= 3
				&& !KatanaStats.parts(katana).isEmpty() && !KatanaStats.isBlackBlade(katana);
	}

	/** Seconds of Haki-coated fighting this katana needs to awaken. A Kokuto blade is black steel already, so it needs half. */
	public static int awakenSeconds(ItemStack katana) {
		List<ArsenalMaterial> parts = KatanaStats.parts(katana);
		return !parts.isEmpty() && parts.get(0).headTrait == Trait.KOKUTO ? AWAKEN_SECONDS / 2 : AWAKEN_SECONDS;
	}

	/** Seconds of Haki-coated fighting this katana has done toward awakening. */
	public static int hakiSeconds(ItemStack katana) {
		return katana.getOrDefault(ArsenalComponents.HAKI_SOAK, 0);
	}

	/** One more second of the wielder's Haki, if they're mid-fight with it on. Awakens the blade when it's soaked enough. */
	private static void soak(ServerPlayer player, ServerLevel level, ItemStack katana) {
		if (!canAwaken(katana) || OnePieceBridge.activeArmamentLevel(player) < AWAKEN_ARMAMENT) {
			return;
		}
		Long lastHit = LAST_COATED_HIT.get(player.getUUID());
		if (lastHit == null || level.getGameTime() - lastHit > FIGHT_WINDOW_TICKS) {
			return;
		}
		int seconds = hakiSeconds(katana) + 1;
		int needed = awakenSeconds(katana);
		if (seconds < needed) {
			katana.set(ArsenalComponents.HAKI_SOAK, seconds);
			if (seconds % (needed / 4) == 0) {
				player.sendSystemMessage(Component.literal("Your Haki is sinking into " + katana.getHoverName().getString() + ". It's darkening ("
						+ seconds * 100 / needed + "%).").withStyle(ChatFormatting.DARK_PURPLE));
			}
			return;
		}
		String before = katana.getHoverName().getString();
		katana.remove(ArsenalComponents.HAKI_SOAK);
		KatanaStats.awaken(katana);
		level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 1.4f);
		level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 0.8, 0.6, 0.1);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 30, 0.6, 0.8, 0.6, 0.3);
		level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString() + "'s " + before
				+ " has turned black. A new Black Blade is born!").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
	}

	public static BladeRecord record(ItemStack katana) {
		return katana.getOrDefault(ArsenalComponents.BLADE_RECORD, BladeRecord.EMPTY);
	}

	/** Total blade XP to reach a level: 4, 16, 36... up to 1600 for level 20. */
	public static int xpForLevel(int level) {
		return 4 * level * level;
	}

	public static int level(ItemStack katana) {
		int xp = record(katana).xp();
		int level = 0;
		while (level < MAX_LEVEL[MAX_LEVEL.length - 1] && xp >= xpForLevel(level + 1)) {
			level++;
		}
		return level;
	}

	/** The highest level the blade's grade allows. */
	public static int maxLevel(ItemStack katana) {
		return MAX_LEVEL[OnePieceBridge.swordGrade(katana)];
	}

	/** What a katana still needs before a whetstone can raise it to the next grade. Empty at Supreme Grade. */
	public static List<Need> promotionNeeds(ItemStack katana) {
		int grade = OnePieceBridge.swordGrade(katana);
		List<ArsenalMaterial> parts = KatanaStats.parts(katana);
		if (grade >= 3 || parts.isEmpty()) {
			return List.of();
		}
		int target = grade + 1;
		BladeRecord record = record(katana);
		List<Need> needs = new ArrayList<>();
		needs.add(new Need("Blade level " + MAX_LEVEL[grade], level(katana) >= MAX_LEVEL[grade]));
		needs.add(new Need("Tier " + BLADE_TIER[target] + "+ blade material", parts.get(0).tier >= BLADE_TIER[target]));
		needs.add(switch (target) {
			case 1 -> new Need("Beat " + SKILLFUL_ARMED_KILLS + " armed enemies (" + Math.min(record.armedKills(), SKILLFUL_ARMED_KILLS)
					+ "/" + SKILLFUL_ARMED_KILLS + ")", record.armedKills() >= SKILLFUL_ARMED_KILLS);
			case 2 -> new Need("Beat a named boss", record.bossTier() >= 1);
			default -> new Need("Beat an Emperor, Admiral or other tier " + SUPREME_BOSS_TIER + " boss", record.bossTier() >= SUPREME_BOSS_TIER);
		});
		return needs;
	}

	/** Why a whetstone can't raise this sword to the target grade for this player, or null if it can. */
	private static String promotionProblem(Player player, ItemStack sword, int target) {
		if (!worthy(player, target)) {
			return "You couldn't wield it at that grade. Needs " + wieldRequirement(target) + ".";
		}
		if (sword.getItem() instanceof KatanaItem) {
			for (Need need : promotionNeeds(sword)) {
				if (!need.met()) {
					return "The blade isn't ready. It needs: " + need.text() + ".";
				}
			}
		}
		return null;
	}

	private static void onKill(ServerPlayer player, ItemStack katana, LivingEntity victim) {
		int grade = OnePieceBridge.swordGrade(katana);
		if (!worthy(player, grade)) {
			return; // A blade that refuses you learns nothing from you.
		}
		boolean armed = !victim.getMainHandItem().isEmpty();
		int bossTier = OnePieceBridge.bossTier(victim);
		if (!armed && bossTier == 0 && !(victim instanceof Enemy) && !(victim instanceof Player)) {
			return; // No XP for cows.
		}
		BladeRecord old = record(katana);
		int oldLevel = level(katana);
		int cap = xpForLevel(MAX_LEVEL[grade]);
		int xp = Math.min(cap, old.xp() + KILL_XP + (armed ? ARMED_XP : 0) + bossTier * BOSS_XP_PER_TIER);
		katana.set(ArsenalComponents.BLADE_RECORD, new BladeRecord(xp, old.armedKills() + (armed ? 1 : 0), Math.max(old.bossTier(), bossTier)));

		int newLevel = level(katana);
		if (newLevel > oldLevel) {
			setLevelBonus(katana, newLevel);
			player.sendSystemMessage(Component.literal(katana.getHoverName().getString() + " reached blade level " + newLevel + "!")
					.withStyle(ChatFormatting.AQUA));
			if (newLevel == MAX_LEVEL[grade] && grade < 3) {
				player.sendSystemMessage(Component.literal("It has outgrown its grade. Do its deed, then a "
						+ OnePieceBridge.gradeName(grade + 1).split(" ")[0] + " Whetstone can raise it.").withStyle(ChatFormatting.GOLD));
			}
		}
	}

	/** Swaps the katana's blade-level damage bonus for the one at this level, leaving its other numbers alone. */
	private static void setLevelBonus(ItemStack katana, int level) {
		ItemAttributeModifiers old = katana.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		List<ItemAttributeModifiers.Entry> entries = new ArrayList<>();
		for (ItemAttributeModifiers.Entry entry : old.modifiers()) {
			if (!entry.modifier().is(LEVEL_MODIFIER)) {
				entries.add(entry);
			}
		}
		katana.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(entries).withModifierAdded(Attributes.ATTACK_DAMAGE,
				new AttributeModifier(LEVEL_MODIFIER, level * DAMAGE_PER_LEVEL, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND));
	}
}
