package com.onepiecearsenal.loot;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.tool.WeaponType;

/**
 * The Navy's top brass carry gear worth taking. Beat them yourself and they sometimes drop it:
 * - Vice Admirals and Commanders: a rare Navy-material part, and now and then a Vice Admiral's graded blade.
 * - Famous Marines (Fujitora, Smoker, Akainu...): their own named gear, only on a first win at that difficulty,
 *   the same rule the main mod uses for its boss prizes, so rematches can't farm it.
 * The crew a boss calls in mid-fight never drops any of it. Graded blades still refuse a wielder who isn't worthy
 * (see Meito). Change the chances here to tune it.
 */
public final class BossGear {
	/** Chance a beaten Vice Admiral drops a Kairoseki or Pacifista Alloy part. */
	private static final float VICE_ADMIRAL_PART = 0.12f;
	/** Chance a beaten Vice Admiral drops their Skillful Grade blade instead. */
	private static final float VICE_ADMIRAL_BLADE = 0.02f;
	/** Chance a beaten Commander or Captain (elite_marine kit) drops a Marine Steel or Kairoseki part. */
	private static final float ELITE_PART = 0.04f;
	/** Chance a famous Marine drops their gear on a first win, by fight difficulty: Normal, Hard, Expert. */
	private static final float[] SIGNATURE_CHANCE = {0.2f, 0.4f, 0.75f};

	/** The head parts a rare drop can be: the ones that decide a piece of gear's strength. */
	private static final List<Item> RARE_PARTS = List.of(ArsenalItems.KATANA_BLADE, ArsenalItems.SABRE_BLADE,
			ArsenalItems.JITTE_ROD, ArsenalItems.BARREL, ArsenalItems.CROWN, ArsenalItems.COAT_PLATES);

	/** A famous Marine's own gear. who = the main mod character ids that carry it. */
	public record Signature(List<String> who, String owners, String name, String description, Supplier<ItemStack> gear) {
	}

	public static final List<Signature> SIGNATURES = List.of(
			new Signature(List.of("fujitora"), "Admiral Fujitora", "Fujitora's Shikomizue",
					"a Great Grade cane sword with a Tamahagane blade",
					() -> graded(KatanaStats.build(ArsenalMaterial.TAMAHAGANE, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.ADAM_WOOD), 2)),
			new Signature(List.of("tashigi"), "Tashigi", "Shigure",
					"a Skillful Grade Tamahagane katana",
					() -> graded(KatanaStats.build(ArsenalMaterial.TAMAHAGANE, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.MARINE_STEEL), 1)),
			new Signature(List.of("smoker"), "Smoker", "Smoker's Jitte",
					"a jitte with a Kairoseki rod that drains Devil Fruit users",
					() -> WeaponStats.build(WeaponType.JITTE, ArsenalMaterial.KAIROSEKI, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.MARINE_STEEL)),
			new Signature(List.of("akainu", "kizaru", "aokiji", "ryokugyu", "garp", "sengoku"), "the Admirals, Garp and Sengoku", "Admiral's Coat",
					"a Vegapunk Compound coat lined with Marine Steel",
					() -> ArmorStats.build(PartType.COAT_PLATES, ArsenalMaterial.VEGAPUNK_COMPOUND, ArsenalMaterial.MARINE_STEEL)),
			new Signature(List.of("lucci"), "Rob Lucci", "Lucci's Rokushiki Boots",
					"Pacifista Alloy boots lined with Island Whale Leather, for Black Leg kicks",
					() -> ArmorStats.build(PartType.BOOT_SOLES, ArsenalMaterial.PACIFISTA_ALLOY, ArsenalMaterial.ISLAND_WHALE_LEATHER)),
			new Signature(List.of("magellan"), "Magellan", "Magellan's Warden Hat",
					"an Impel Down hat with a Kairoseki crown",
					() -> ArmorStats.build(PartType.CROWN, ArsenalMaterial.KAIROSEKI, ArsenalMaterial.SEA_KING_HIDE))
	);

	private BossGear() {
	}

	public static void initialize() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer player && entity.level() instanceof ServerLevel level) {
				ItemStack drop = dropFor(entity, entity.getRandom());
				if (!drop.isEmpty()) {
					entity.spawnAtLocation(level, drop);
					player.sendSystemMessage(Component.literal(entity.getDisplayName().getString() + " dropped " + drop.getHoverName().getString() + "!")
							.withStyle(ChatFormatting.GOLD));
				}
			}
		});
	}

	/** What this beaten Marine drops, or nothing. Rolled once per death. */
	public static ItemStack dropFor(LivingEntity entity, RandomSource random) {
		if (OnePieceBridge.isBossMinion(entity)) {
			return ItemStack.EMPTY;
		}
		String boss = OnePieceBridge.bossId(entity);
		if (!boss.isEmpty()) {
			Signature signature = signatureOf(boss);
			if (signature == null || !OnePieceBridge.isFirstBossWin(entity)
					|| random.nextFloat() >= SIGNATURE_CHANCE[Math.clamp(OnePieceBridge.bossDifficulty(entity), 0, 2)]) {
				return ItemStack.EMPTY;
			}
			return named(signature.gear().get(), signature.name(), "Taken from " + entity.getDisplayName().getString());
		}
		switch (OnePieceBridge.marineKit(entity)) {
			case "vice_admiral" -> {
				float roll = random.nextFloat();
				if (roll < VICE_ADMIRAL_BLADE) {
					ItemStack blade = graded(KatanaStats.build(ArsenalMaterial.KAIROSEKI, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.MARINE_STEEL), 1);
					return named(blade, "Vice Admiral's Blade", "Taken from a Vice Admiral");
				}
				if (roll < VICE_ADMIRAL_BLADE + VICE_ADMIRAL_PART) {
					return rarePart(random, random.nextBoolean() ? ArsenalMaterial.KAIROSEKI : ArsenalMaterial.PACIFISTA_ALLOY);
				}
			}
			case "elite_marine" -> {
				if (random.nextFloat() < ELITE_PART) {
					return rarePart(random, random.nextFloat() < 0.6f ? ArsenalMaterial.MARINE_STEEL : ArsenalMaterial.KAIROSEKI);
				}
			}
			default -> {
			}
		}
		return ItemStack.EMPTY;
	}

	/** The named gear this main mod character carries, or null for anyone else. */
	public static Signature signatureOf(String characterId) {
		for (Signature signature : SIGNATURES) {
			if (signature.who().contains(characterId)) {
				return signature;
			}
		}
		return null;
	}

	private static ItemStack rarePart(RandomSource random, ArsenalMaterial material) {
		return PartItem.of(RARE_PARTS.get(random.nextInt(RARE_PARTS.size())), material);
	}

	private static ItemStack graded(ItemStack sword, int grade) {
		OnePieceBridge.setSwordGrade(sword, grade);
		return sword;
	}

	/** Names the gear after its owner. It keeps its parts, so it repairs and levels like any other. */
	private static ItemStack named(ItemStack gear, String name, String lore) {
		gear.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(style -> style.withItalic(false).withColor(ChatFormatting.GOLD)));
		gear.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(lore).withStyle(ChatFormatting.GRAY))));
		return gear;
	}
}
