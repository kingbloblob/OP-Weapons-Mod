package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.armorset.SetBonus;
import com.onepiecearsenal.armorset.SetBonuses;
import com.onepiecearsenal.disguise.MarineDisguise;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecemod.bounty.Bounty;
import com.onepiecemod.npc.NpcFeature;
import com.onepiecemod.power.ModAttachments;

/**
 * Armor set bonuses (arsenal/set_bonuses.json), checked in a real world. Run with ./gradlew runGameTest.
 */
public class SetBonusTests {
	private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final List<PartType> SHELLS = List.of(PartType.CROWN, PartType.COAT_PLATES, PartType.SASH_WEAVE, PartType.BOOT_SOLES);

	@GameTest
	public void everySetLoads(GameTestHelper helper) {
		helper.assertTrue(SetBonus.values().size() >= 10, "set_bonuses.json should have its sets, found " + SetBonus.values().size());
		for (String id : List.of("regulation_uniform", "seastone_bulwark", "jolly_roger")) {
			helper.assertTrue(SetBonus.byId(id) != null, "set_bonuses.json should have " + id);
		}
		helper.succeed();
	}

	@GameTest
	public void threePiecesAreNotASet(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		dress(player, ArsenalMaterial.KAIROSEKI, ArsenalMaterial.SHIP_IRON);
		helper.assertTrue(SetBonuses.active(player).contains(SetBonus.byId("seastone_bulwark")), "four Kairoseki shells are a Seastone Bulwark");
		player.setItemSlot(EquipmentSlot.FEET, ArmorStats.build(PartType.BOOT_SOLES, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON));
		helper.runAfterDelay(1, () -> {
			helper.assertFalse(SetBonuses.active(player).contains(SetBonus.byId("seastone_bulwark")), "three Kairoseki shells aren't a set");
			helper.succeed();
		});
	}

	@GameTest
	public void kairosekiResistsDevilFruits(GameTestHelper helper) {
		Player wearer = helper.makeMockPlayer(GameType.SURVIVAL);
		dress(wearer, ArsenalMaterial.KAIROSEKI, ArsenalMaterial.SHIP_IRON);
		Player fruitUser = helper.makeMockPlayer(GameType.SURVIVAL);
		fruitUser.setAttached(ModAttachments.DEVIL_FRUIT, "gomu_gomu_no_mi");
		Player plainPirate = helper.makeMockPlayer(GameType.SURVIVAL);
		assertNear(helper, SetBonuses.adjustDamage(wearer, wearer.damageSources().playerAttack(fruitUser), 10.0f), 6.5f,
				"a full Kairoseki set should take 35% off a fruit user's hit");
		assertNear(helper, SetBonuses.adjustDamage(wearer, wearer.damageSources().playerAttack(plainPirate), 10.0f), 10.0f,
				"a full Kairoseki set shouldn't change a hit from someone without a fruit");
		helper.succeed();
	}

	@GameTest
	public void pacifistaFrameStopsBullets(GameTestHelper helper) {
		Player wearer = helper.makeMockPlayer(GameType.SURVIVAL);
		dress(wearer, ArsenalMaterial.PACIFISTA_ALLOY, ArsenalMaterial.SHIP_IRON);
		Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
		// The main mod's bullets are thrown damage (Shot.java).
		assertNear(helper, SetBonuses.adjustDamage(wearer, wearer.damageSources().thrown(shooter, shooter), 8.0f), 6.0f,
				"a Pacifista Frame should take 25% off a shot");
		helper.succeed();
	}

	@GameTest
	public void jollyRogerHitsMarinesHarder(GameTestHelper helper) {
		Player pirate = helper.makeMockPlayer(GameType.SURVIVAL);
		dress(pirate, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.DRIFTWOOD);
		Mob marine = marine(helper);
		assertNear(helper, SetBonuses.adjustDamage(marine, marine.damageSources().playerAttack(pirate), 10.0f), 11.0f,
				"a full pirate set should hit Marines 10% harder");
		helper.succeed();
	}

	@GameTest
	public void regulationUniformHidesMore(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Bounty.set(player, 15_000_000L);
		dress(player, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.SHIP_IRON);
		Mob marine = marine(helper);
		helper.assertFalse(MarineDisguise.fools(marine, player), "a recruit's uniform with pirate linings can't hide a 15M bounty");
		dress(player, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.MARINE_STEEL);
		helper.runAfterDelay(1, () -> {
			helper.assertTrue(MarineDisguise.fools(marine, player), "a Regulation Uniform hides twice the bounty, so 15M passes");
			helper.assertTrue(SetBonuses.suspicion(player) == 0.5, "a Regulation Uniform should halve suspicion");
			helper.succeed();
		});
	}

	@GameTest
	public void setAttributesComeAndGo(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		double before = player.getAttributeValue(Attributes.MAX_HEALTH);
		dress(player, ArsenalMaterial.ISLAND_WHALE_LEATHER, ArsenalMaterial.SHIP_IRON);
		SetBonuses.apply(player);
		helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == before + 4, "an Island Whale set should give +4 max health");
		player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
		helper.runAfterDelay(1, () -> {
			SetBonuses.apply(player);
			helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == before, "taking off a piece should take the bonus away");
			helper.succeed();
		});
	}

	private static void dress(LivingEntity wearer, ArsenalMaterial shell, ArsenalMaterial lining) {
		for (int i = 0; i < SLOTS.size(); i++) {
			wearer.setItemSlot(SLOTS.get(i), ArmorStats.build(SHELLS.get(i), shell, lining));
		}
	}

	private static Mob marine(GameTestHelper helper) {
		Mob marine = NpcFeature.spawn(helper.getLevel(), "marine", helper.absolutePos(new BlockPos(1, 1, 1)), 0f);
		helper.assertTrue(marine != null, "the main mod should have a 'marine' kit");
		return marine;
	}

	private static void assertNear(GameTestHelper helper, float actual, float expected, String message) {
		helper.assertTrue(Math.abs(actual - expected) < 0.01f, message + " (expected " + expected + ", got " + actual + ")");
	}
}
