package com.onepiecearsenal.mastery;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.tool.ArsenalGunItem;
import com.onepiecearsenal.upgrade.Upgrade.Gear;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * Weapon mastery: every forged katana, hand weapon and gun learns from the fights it wins. Beating foes with it
 * earns mastery XP (more for armed foes and bosses), which climbs five ranks from Untested to Mastered.
 *
 * A Mastered weapon opens a mastery slot, a fourth trait on top of the three from its parts. Fill it at the
 * Shipwright's Anvil: hold the weapon, put any part in your offhand and right-click. The part is used up and its
 * trait (a head part's head trait, any other part's handle trait) is bound into the slot. Binding another part
 * swaps the old trait out.
 *
 * Katanas also keep their blade levels (Meito); a kill counts toward both. The Folded handle trait makes both go
 * 25% faster. Change the numbers here to tune it.
 */
public final class Mastery {
	/** Total mastery XP for each rank. A kill is worth 1, 3 for an armed foe, +5 per boss tier, like blade XP. */
	private static final int[] XP_FOR_RANK = {0, 10, 30, 70, 140, 250};
	private static final String[] RANK_NAMES = {"Untested", "Blooded", "Seasoned", "Veteran", "Feared", "Mastered"};
	public static final int MAX_RANK = XP_FOR_RANK.length - 1;
	private static final int KILL_XP = 1;
	private static final int ARMED_XP = 2;
	private static final int BOSS_XP_PER_TIER = 5;
	/** Folded: how much faster a weapon learns. */
	private static final float FOLDED_BONUS = 0.25f;

	/** Traits that only work when they're forged into the gear: they change its numbers as it's built. */
	private static final Set<Trait> FORGED_ONLY = EnumSet.of(Trait.RIVETED, Trait.UNBREAKABLE_GRAIN, Trait.LIGHT, Trait.HEAVY,
			Trait.LEGENDARY_HULL, Trait.ULTRA_ALLOY);
	/** Traits that only work on armor. */
	private static final Set<Trait> ARMOR_ONLY = EnumSet.of(Trait.WATERPROOF, Trait.ENDURANCE, Trait.PLATED, Trait.DRAGONHIDE,
			Trait.BUBBLY, Trait.SPINED);
	/** Traits that only work on melee critical hits, so a gun can't use them. */
	private static final Set<Trait> MELEE_ONLY = EnumSet.of(Trait.SERRATED, Trait.VOLCANIC_GLASS);

	private Mastery() {
	}

	public static void initialize() {
		// The weapon that beat a foe learns from it: the gun whose shot landed, or the weapon in the main hand.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(source.getEntity() instanceof ServerPlayer player) || player == entity) {
				return;
			}
			boolean shot = source.is(DamageTypes.THROWN);
			ItemStack weapon = shot ? ArsenalGunItem.firing() : ItemStack.EMPTY;
			if (weapon.isEmpty()) {
				weapon = player.getMainHandItem();
			}
			// A gun learns from its shots, not from being swung like a club.
			if (weapon.getItem() instanceof ArsenalGunItem != shot) {
				return;
			}
			if (canMaster(weapon) && !Breakage.isBroken(weapon)) {
				onKill(player, weapon, entity);
			}
		});
	}

	// ---- Reading a weapon ----

	/** True for forged katanas, hand weapons and guns. Armor and unforged items have no mastery. */
	public static boolean canMaster(ItemStack stack) {
		Gear gear = Upgrades.gearOf(stack);
		return gear == Gear.KATANA || gear == Gear.WEAPON || gear == Gear.GUN;
	}

	public static WeaponMastery mastery(ItemStack stack) {
		return stack.getOrDefault(ArsenalComponents.MASTERY, WeaponMastery.EMPTY);
	}

	public static int xp(ItemStack stack) {
		return mastery(stack).xp();
	}

	/** Total XP to reach a rank (0 to MAX_RANK). */
	public static int xpForRank(int rank) {
		return XP_FOR_RANK[Math.min(rank, MAX_RANK)];
	}

	public static int rank(ItemStack stack) {
		int xp = xp(stack);
		int rank = 0;
		while (rank < MAX_RANK && xp >= XP_FOR_RANK[rank + 1]) {
			rank++;
		}
		return rank;
	}

	public static String rankName(int rank) {
		return RANK_NAMES[rank];
	}

	public static boolean isMastered(ItemStack stack) {
		return rank(stack) >= MAX_RANK;
	}

	/** The trait bound into the weapon's mastery slot, or null if the slot is empty (or not open). */
	public static Trait boundTrait(ItemStack stack) {
		String id = mastery(stack).trait();
		if (id.isEmpty() || !isMastered(stack)) {
			return null;
		}
		try {
			return Trait.valueOf(id.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null; // A trait removed from the mod just stops counting.
		}
	}

	/** The weapon's traits from its parts, plus its mastery trait. KatanaStats, WeaponStats and GunStats call this. */
	public static Set<Trait> withBound(ItemStack stack, Set<Trait> fromParts) {
		Trait bound = boundTrait(stack);
		if (bound != null) {
			fromParts.add(bound);
		}
		return fromParts;
	}

	// ---- Learning ----

	/** XP for a kill: armed foes and bosses teach more. 0 for things not worth a weapon's attention, like cows. */
	public static int killXp(LivingEntity victim) {
		boolean armed = !victim.getMainHandItem().isEmpty();
		int bossTier = OnePieceBridge.bossTier(victim);
		if (!armed && bossTier == 0 && !(victim instanceof Enemy) && !(victim instanceof Player) && !OnePieceBridge.isNpc(victim)) {
			return 0;
		}
		return KILL_XP + (armed ? ARMED_XP : 0) + bossTier * BOSS_XP_PER_TIER;
	}

	/**
	 * XP after the Folded trait's bonus. The bonus is a quarter of the XP; a leftover fraction of a point is a chance
	 * at one more, so a Folded weapon learns exactly 25% faster on average.
	 */
	public static int withFolded(int xp, boolean folded, RandomSource random) {
		if (!folded || xp <= 0) {
			return xp;
		}
		float bonus = xp * FOLDED_BONUS;
		int whole = (int) bonus;
		return xp + whole + (random.nextFloat() < bonus - whole ? 1 : 0);
	}

	/** Adds XP to the weapon and says so if it ranked up. Returns the new rank. */
	public static int addXp(ItemStack weapon, int xp) {
		int cap = XP_FOR_RANK[MAX_RANK];
		weapon.set(ArsenalComponents.MASTERY, mastery(weapon).withXp(Math.min(cap, xp(weapon) + xp)));
		return rank(weapon);
	}

	private static void onKill(ServerPlayer player, ItemStack weapon, LivingEntity victim) {
		if (isMastered(weapon)) {
			return;
		}
		int xp = withFolded(killXp(victim), TraitEffects.traits(weapon).contains(Trait.FOLDED), player.getRandom());
		if (xp <= 0) {
			return;
		}
		int before = rank(weapon);
		int after = addXp(weapon, xp);
		if (after <= before) {
			return;
		}
		String name = weapon.getHoverName().getString();
		if (after < MAX_RANK) {
			player.sendSystemMessage(Component.literal(name + " is now " + rankName(after) + " (mastery " + after + "/" + MAX_RANK + ").")
					.withStyle(ChatFormatting.GREEN));
			return;
		}
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.8f);
		player.sendSystemMessage(Component.literal(name + " is Mastered! Its mastery slot is open: at a Shipwright's Anvil, hold a part "
				+ "in your offhand to bind that part's trait into it.").withStyle(ChatFormatting.GOLD));
	}

	// ---- The mastery slot ----

	/** The trait a part would bind: a head part's head trait, any other part's handle trait. Null for a blank part. */
	public static Trait traitOf(ItemStack part) {
		if (!(part.getItem() instanceof PartItem item)) {
			return null;
		}
		ArsenalMaterial material = PartItem.material(part);
		if (material == null) {
			return null;
		}
		return item.type().head ? material.headTrait : material.handleTrait;
	}

	/** Why this trait can't be bound into this weapon's mastery slot, or null if it can. */
	public static String whyNot(ItemStack weapon, Trait trait) {
		if (!canMaster(weapon)) {
			return "Only forged weapons and guns have a mastery slot.";
		}
		if (!isMastered(weapon)) {
			int rank = rank(weapon);
			return weapon.getHoverName().getString() + " isn't Mastered yet (" + rankName(rank) + ", " + rank + "/" + MAX_RANK
					+ "). Win more fights with it.";
		}
		if (trait == null) {
			return "A blank part has no trait to bind.";
		}
		if (!trait.active) {
			return trait.displayName + " doesn't do anything yet.";
		}
		if (trait == Trait.BLACK_BLADE) {
			return "Black Blade can't be bound. It only comes from a katana's awakening.";
		}
		if (trait == Trait.FOLDED) {
			return "Folded helps a weapon learn, and this one has nothing left to learn.";
		}
		if (FORGED_ONLY.contains(trait)) {
			return trait.displayName + " only works when it's forged into the gear.";
		}
		if (ARMOR_ONLY.contains(trait)) {
			return trait.displayName + " only works on armor.";
		}
		if (MELEE_ONLY.contains(trait) && Upgrades.gearOf(weapon) == Gear.GUN) {
			return trait.displayName + " only works on melee critical hits, so a gun can't use it.";
		}
		if (trait == boundTrait(weapon)) {
			return "Its mastery slot already holds " + trait.displayName + ".";
		}
		if (TraitEffects.traits(weapon).contains(trait)) {
			return weapon.getHoverName().getString() + " already has " + trait.displayName + " from its parts.";
		}
		return null;
	}

	/** Binds the trait into the mastery slot, replacing any trait there. Returns false (and changes nothing) if it can't. */
	public static boolean bind(ItemStack weapon, Trait trait) {
		if (whyNot(weapon, trait) != null) {
			return false;
		}
		weapon.set(ArsenalComponents.MASTERY, mastery(weapon).withTrait(trait.name().toLowerCase(Locale.ROOT)));
		return true;
	}

	/**
	 * The smith right-clicked the Shipwright's Anvil holding a weapon, with a part in the offhand. Binds the part's
	 * trait if the weapon is Mastered. Returns null if this isn't a binding (armor, or a damaged weapon that isn't
	 * Mastered yet), so the anvil can mend or forge instead.
	 */
	public static InteractionResult work(Level level, BlockPos pos, ServerPlayer smith, ItemStack weapon, ItemStack part) {
		if (!canMaster(weapon) || (!isMastered(weapon) && weapon.isDamaged())) {
			return null;
		}
		Trait trait = traitOf(part);
		String problem = whyNot(weapon, trait);
		if (problem != null) {
			smith.sendOverlayMessage(Component.literal(problem).withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		Trait old = boundTrait(weapon);
		bind(weapon, trait);
		part.shrink(1);
		level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 0.8f);
		level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
		smith.sendOverlayMessage(Component.literal(trait.displayName + " bound into its mastery slot"
				+ (old == null ? "." : ", in place of " + old.displayName + ".")).withStyle(ChatFormatting.GREEN));
		return InteractionResult.SUCCESS;
	}

	// ---- Tooltip ----

	/** The mastery lines on a weapon's tooltip. */
	public static void describe(ItemStack stack, Consumer<Component> lines) {
		if (!canMaster(stack)) {
			return;
		}
		int rank = rank(stack);
		if (rank < MAX_RANK) {
			lines.accept(Component.literal("Mastery: " + rankName(rank) + " (" + rank + "/" + MAX_RANK + "), " + xp(stack) + "/"
					+ xpForRank(rank + 1) + " XP").withStyle(ChatFormatting.GREEN));
			return;
		}
		Trait bound = boundTrait(stack);
		if (bound == null) {
			lines.accept(Component.literal("Mastered: mastery slot open").withStyle(ChatFormatting.GREEN));
			lines.accept(Component.literal(" Hold a part in your offhand at a Shipwright's Anvil to bind its trait").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			lines.accept(Component.literal("Mastered: mastery slot holds " + bound.displayName).withStyle(ChatFormatting.GREEN));
		}
	}
}
