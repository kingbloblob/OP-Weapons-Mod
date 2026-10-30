package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.loot.HunterGear;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.ArsenalArmorItem;
import com.onepiecearsenal.tool.ArsenalWeaponItem;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecemod.bounty.BountyFeature;
import com.onepiecemod.bounty.BountyHunterEntity;
import com.onepiecemod.bounty.HunterFruit;

/** The gear the main mod's bounty hunters carry (loot/HunterGear). Run with ./gradlew runGameTest. */
public class HunterGearTests {
	private static final int HUNTERS = 150;
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	@GameTest
	public void huntersCarryGearForTheirThreatLevel(GameTestHelper helper) {
		RandomSource random = RandomSource.create(11);
		for (int threat = 3; threat <= 5; threat++) {
			HunterGear.Loadout loadout = HunterGear.loadout(threat);
			int kairoseki = 0;
			for (int i = 0; i < HUNTERS; i++) {
				BountyHunterEntity hunter = hunter(helper, threat, random);
				HunterGear.outfit(hunter, random);

				ItemStack weapon = hunter.getMainHandItem();
				helper.assertTrue(weapon.getItem() instanceof KatanaItem || weapon.getItem() instanceof ArsenalWeaponItem,
						"a threat " + threat + " hunter should hold an Arsenal weapon, got " + weapon);
				ArsenalMaterial head = weapon.getItem() instanceof KatanaItem ? KatanaStats.parts(weapon).get(0) : WeaponStats.parts(weapon).get(0);
				if (head == ArsenalMaterial.KAIROSEKI) {
					kairoseki++;
				} else {
					helper.assertTrue(loadout.weaponHeads().contains(head), "threat " + threat + " hunters shouldn't carry " + head.displayName);
				}
				helper.assertTrue(weapon.has(DataComponents.LORE), "the weapon should say who carried it");

				int pieces = 0;
				for (EquipmentSlot slot : ARMOR) {
					ItemStack piece = hunter.getItemBySlot(slot);
					if (!piece.isEmpty()) {
						helper.assertTrue(piece.getItem() instanceof ArsenalArmorItem, "hunter armor should be Arsenal armor, got " + piece);
						helper.assertTrue(loadout.shells().contains(ArmorStats.parts(piece).get(0)), "unexpected armor shell at threat " + threat);
						pieces++;
					}
				}
				helper.assertTrue(pieces == loadout.armorPieces(), "threat " + threat + " hunters should wear " + loadout.armorPieces() + " pieces, got " + pieces);

				// The weapon plus the balance modifier should add up to the sword the hunter used to carry.
				double balance = hunter.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(ArsenalMod.id("hunter_weapon_balance")).amount();
				helper.assertTrue(Math.abs(balance + HunterGear.weaponDamage(weapon) - loadout.swordDamage()) < 0.01,
						"a threat " + threat + " hunter should hit like their old sword");
			}
			if (threat < 5) {
				helper.assertTrue(kairoseki == 0, "only threat 5 hunters carry Kairoseki");
			} else {
				// 30%: allow plenty of room for chance.
				helper.assertTrue(kairoseki > HUNTERS * 0.15 && kairoseki < HUNTERS * 0.45, "expected about 30% Kairoseki, got " + kairoseki + "/" + HUNTERS);
			}
		}
		helper.succeed();
	}

	@GameTest
	public void loreNamesTheHunterNotTheirFruit(GameTestHelper helper) {
		BountyHunterEntity hunter = hunter(helper, 4, RandomSource.create(3));
		HunterGear.outfit(hunter, hunter.getRandom());
		List<String> lore = hunter.getMainHandItem().get(DataComponents.LORE).lines().stream().map(line -> line.getString()).toList();
		String last = lore.get(lore.size() - 1);
		helper.assertTrue(last.startsWith("Carried by ") && last.endsWith(", bounty hunter") && !last.contains("("),
				"lore should name the hunter, got '" + last + "'");
		helper.succeed();
	}

	private static BountyHunterEntity hunter(GameTestHelper helper, int threat, RandomSource random) {
		BountyHunterEntity hunter = BountyFeature.BOUNTY_HUNTER.create(helper.getLevel(), EntitySpawnReason.EVENT);
		helper.assertTrue(hunter != null, "the main mod should have bounty hunters");
		hunter.setup(HunterFruit.random(random, threat), threat);
		return hunter;
	}
}
