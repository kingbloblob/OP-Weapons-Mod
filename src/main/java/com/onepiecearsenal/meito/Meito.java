package com.onepiecearsenal.meito;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
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
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
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
	private static final Identifier LEVEL_MODIFIER = ArsenalMod.id("blade_level");
	/** Blade XP for any enemy, and the extra for one holding a weapon or for each boss tier. */
	private static final int KILL_XP = 1;
	private static final int ARMED_XP = 2;
	private static final int BOSS_XP_PER_TIER = 5;

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
					&& player.getMainHandItem().getItem() instanceof KatanaItem) {
				onKill(player, player.getMainHandItem(), entity);
			}
		});

		// Once a second: a graded blade weighs down a player it refuses.
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
				if (grade > 0 && !worthy(player, grade)) {
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

	/** "Haki level 3 or a 300,000,000 Berry bounty". */
	public static String wieldRequirement(int grade) {
		return String.format("Haki level %d or a %,d Berry bounty", WIELD_HAKI[grade], WIELD_BOUNTY[grade]);
	}

	// ---- The blade ----

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
			return "You can't hone a blade you couldn't wield. " + OnePieceBridge.gradeName(target) + " needs " + wieldRequirement(target) + ".";
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
