package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.disguise.MarineDisguise;
import com.onepiecearsenal.disguise.Suspicion;
import com.onepiecearsenal.loot.MarineGear;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecemod.bounty.Bounty;
import com.onepiecemod.npc.NpcFeature;

/**
 * The Marine disguise, checked in a real world. Run with ./gradlew runGameTest.
 * Each test spawns a Marine from one of the main mod's kits next to a mock player and asks the Marine to target them.
 */
public class DisguiseTests {
	private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final List<PartType> SHELLS = List.of(PartType.CROWN, PartType.COAT_PLATES, PartType.SASH_WEAVE, PartType.BOOT_SOLES);

	@GameTest
	public void uniformFoolsRecruit(GameTestHelper helper) {
		Player player = pirate(helper, 1_000_000L, ArsenalMaterial.MARINE_STEEL);
		Mob marine = marine(helper, "marine");
		marine.setTarget(player);
		helper.assertTrue(marine.getTarget() == null, "a Marine should ignore a pirate in a recruit's uniform");
		helper.succeed();
	}

	@GameTest
	public void noUniformGetsTargeted(GameTestHelper helper) {
		Player player = pirate(helper, 1_000_000L, null);
		Mob marine = marine(helper, "marine");
		marine.setTarget(player);
		helper.assertTrue(marine.getTarget() == player, "a Marine should go after a pirate without a uniform");
		helper.succeed();
	}

	@GameTest
	public void captainSeesThroughRecruitUniform(GameTestHelper helper) {
		Player player = pirate(helper, 1_000_000L, ArsenalMaterial.MARINE_STEEL);
		Mob captain = marine(helper, "elite_marine");
		captain.setTarget(player);
		helper.assertTrue(captain.getTarget() == player, "a Captain should see through a recruit's uniform");
		helper.succeed();
	}

	@GameTest
	public void officerUniformFoolsCaptain(GameTestHelper helper) {
		Player player = pirate(helper, 1_000_000L, ArsenalMaterial.KAIROSEKI);
		Mob captain = marine(helper, "elite_marine");
		captain.setTarget(player);
		helper.assertTrue(captain.getTarget() == null, "an officer's uniform should fool a Captain");
		helper.succeed();
	}

	@GameTest
	public void viceAdmiralSeesThroughOfficerUniform(GameTestHelper helper) {
		Player player = pirate(helper, 1_000_000L, ArsenalMaterial.KAIROSEKI);
		Mob admiral = marine(helper, "vice_admiral");
		admiral.setTarget(player);
		helper.assertTrue(admiral.getTarget() == player, "a Vice Admiral should see through an officer's uniform");
		helper.succeed();
	}

	@GameTest
	public void famousPirateIsRecognized(GameTestHelper helper) {
		Player player = pirate(helper, 50_000_000L, ArsenalMaterial.MARINE_STEEL);
		Mob marine = marine(helper, "marine");
		marine.setTarget(player);
		helper.assertTrue(marine.getTarget() == player, "a 50M bounty is too famous for a recruit's uniform");
		helper.succeed();
	}

	@GameTest
	public void hittingAMarineBlowsCover(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		dress(player, 1_000_000L, ArsenalMaterial.MARINE_STEEL);
		Mob marine = marine(helper, "marine");
		helper.assertTrue(MarineDisguise.fools(marine, player), "a Marine should be fooled before the hit");
		marine.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 1.0f);
		helper.assertTrue(MarineDisguise.isBlown(player), "hitting a Marine should blow the disguise");
		// The mock server player is always in creative, and Mob.setTarget drops creative players,
		// so ask the disguise directly. Same tick as the check above, so this also covers the cover cache.
		helper.assertFalse(MarineDisguise.fools(marine, player), "a Marine should fight back once the disguise is blown");
		helper.succeed();
	}

	@GameTest
	public void pirateBladesLookSuspicious(GameTestHelper helper) {
		Player player = pirate(helper, 0L, ArsenalMaterial.MARINE_STEEL);
		player.setItemSlot(EquipmentSlot.MAINHAND, KatanaStats.build(ArsenalMaterial.DRIFTWOOD, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.MARINE_STEEL));
		helper.assertTrue(Suspicion.carriesPirateGear(player), "a driftwood katana is pirate gear");
		player.setItemSlot(EquipmentSlot.MAINHAND, KatanaStats.build(ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.DRIFTWOOD, ArsenalMaterial.DRIFTWOOD));
		helper.assertFalse(Suspicion.carriesPirateGear(player), "a Marine Steel blade is Navy gear, whatever the handle");
		helper.succeed();
	}

	@GameTest
	public void viceAdmiralsCarryKairosekiKatanas(GameTestHelper helper) {
		Mob admiral = marine(helper, "vice_admiral");
		boolean gotOne = false;
		// Half of sword-carrying Vice Admirals get one, so 40 tries all missing is a 1-in-a-trillion fluke.
		for (int i = 0; i < 40 && !gotOne; i++) {
			admiral.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
			MarineGear.outfit(admiral, admiral.getRandom());
			List<ArsenalMaterial> parts = KatanaStats.parts(admiral.getMainHandItem());
			if (!parts.isEmpty()) {
				helper.assertTrue(parts.get(0) == ArsenalMaterial.KAIROSEKI, "a Vice Admiral's katana should have a Kairoseki blade");
				gotOne = true;
			}
		}
		helper.assertTrue(gotOne, "some Vice Admirals should carry Arsenal katanas");
		helper.succeed();
	}

	/** A mock player with this bounty, in a full uniform with shells of this material (or none if null). */
	private static Player pirate(GameTestHelper helper, long bounty, ArsenalMaterial shell) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		dress(player, bounty, shell);
		return player;
	}

	private static void dress(Player player, long bounty, ArsenalMaterial shell) {
		Bounty.set(player, bounty);
		if (shell != null) {
			for (int i = 0; i < SLOTS.size(); i++) {
				player.setItemSlot(SLOTS.get(i), ArmorStats.build(SHELLS.get(i), shell, ArsenalMaterial.MARINE_STEEL));
			}
		}
	}

	private static Mob marine(GameTestHelper helper, String kit) {
		Mob marine = NpcFeature.spawn(helper.getLevel(), kit, helper.absolutePos(new BlockPos(1, 1, 1)), 0f);
		helper.assertTrue(marine != null, "the main mod should have a '" + kit + "' kit");
		return marine;
	}
}
