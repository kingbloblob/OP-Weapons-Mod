package com.onepiecearsenal.test;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.haki.HakiCoat;
import com.onepiecearsenal.haki.HakiImbue;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecearsenal.tool.KatanaStats;

import com.onepiecemod.haki.HakiAttachments;
import com.onepiecemod.power.ModAttachments;

/** Coating weapons in Armament Haki (haki/HakiImbue). Run with ./gradlew runGameTest. */
public class HakiImbueTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;
	/** Armament Haki training points for levels 1, 2 and 3 (HakiData.LEVEL_XP). */
	private static final int LEVEL_1 = 100;
	private static final int LEVEL_2 = 400;
	private static final int LEVEL_3 = 1000;

	@GameTest
	public void coatingNeedsArmamentLevelTwo(GameTestHelper helper) {
		ServerPlayer player = fighter(helper, LEVEL_1);
		ItemStack katana = KatanaStats.build(IRON, IRON, IRON);
		player.setItemSlot(EquipmentSlot.MAINHAND, katana);
		HakiImbue.press(player);
		helper.assertTrue(!katana.has(ArsenalComponents.HAKI_COAT), "Armament level 1 shouldn't reach into the blade");

		HakiAttachments.set(player, HakiAttachments.get(player).withArmamentXp(LEVEL_2));
		long now = player.level().getGameTime();
		HakiImbue.press(player);
		HakiCoat coat = katana.get(ArsenalComponents.HAKI_COAT);
		helper.assertTrue(coat != null && coat.level() == 2, "Armament level 2 should coat the blade");
		long seconds = (coat.until() - now) / 20;
		helper.assertTrue(seconds == HakiImbue.BASE_SECONDS + 2 * HakiImbue.SECONDS_PER_LEVEL, "a level 2 coat should last 30s, lasts " + seconds);
		helper.succeed();
	}

	@GameTest
	public void unforgedGearCannotBeCoated(GameTestHelper helper) {
		ServerPlayer player = fighter(helper, LEVEL_3);
		helper.assertTrue(!HakiImbue.canCoat(new ItemStack(com.onepiecearsenal.ArsenalItems.KATANA)), "an unforged katana can't be coated");
		helper.assertTrue(HakiImbue.canCoat(GunStats.build(GunType.FLINTLOCK, IRON, IRON, IRON)), "a forged gun can be coated");
		helper.succeed();
	}

	@GameTest
	public void pressingAgainLetsGoAndHakiNeedsToGather(GameTestHelper helper) {
		ServerPlayer player = fighter(helper, LEVEL_2);
		ItemStack katana = KatanaStats.build(IRON, IRON, IRON);
		player.setItemSlot(EquipmentSlot.MAINHAND, katana);
		HakiImbue.press(player);
		helper.assertTrue(katana.has(ArsenalComponents.HAKI_COAT), "the first press should coat the blade");
		HakiImbue.press(player);
		helper.assertTrue(!katana.has(ArsenalComponents.HAKI_COAT), "the second press should let the coat go");
		HakiImbue.press(player);
		helper.assertTrue(!katana.has(ArsenalComponents.HAKI_COAT), "Haki needs time to gather before coating again");
		helper.succeed();
	}

	@GameTest
	public void coatedHitsLandOnLogiaUsers(GameTestHelper helper) {
		ServerPlayer attacker = fighter(helper, LEVEL_2);
		ItemStack katana = KatanaStats.build(IRON, IRON, IRON);
		attacker.setItemSlot(EquipmentSlot.MAINHAND, katana);
		HakiImbue.press(attacker);
		helper.assertTrue(HakiImbue.isCoated(katana, attacker.level().getGameTime()), "the katana should be coated");
		ServerPlayer logia = logiaUser(helper);
		// A Logia body lets half of all hits pass through, so 12 landing in a row only happens if the coat works.
		for (int i = 0; i < 12; i++) {
			logia.damageCooldownTime = 0;
			logia.setHealth(logia.getMaxHealth());
			helper.assertTrue(logia.hurtServer((ServerLevel) logia.level(), attacker.damageSources().playerAttack(attacker), 0.5f),
					"a coated blade's hit " + (i + 1) + " should land on a Logia user");
		}
		helper.succeed();
	}

	@GameTest
	public void blackBladesAlwaysLandOnLogiaUsers(GameTestHelper helper) {
		ServerPlayer attacker = fighter(helper, 0);
		attacker.setItemSlot(EquipmentSlot.MAINHAND, KatanaStats.build(ArsenalMaterial.BLACK_BLADE, IRON, IRON));
		ServerPlayer logia = logiaUser(helper);
		for (int i = 0; i < 12; i++) {
			logia.damageCooldownTime = 0;
			logia.setHealth(logia.getMaxHealth());
			helper.assertTrue(logia.hurtServer((ServerLevel) logia.level(), attacker.damageSources().playerAttack(attacker), 0.5f),
					"a Black Blade's hit " + (i + 1) + " should land on a Logia user");
		}
		helper.succeed();
	}

	@GameTest
	public void aCoatHitsHarderByItsLevel(GameTestHelper helper) {
		ServerPlayer attacker = fighter(helper, LEVEL_3);
		ItemStack katana = KatanaStats.build(IRON, IRON, IRON);
		attacker.setItemSlot(EquipmentSlot.MAINHAND, katana);
		// Compared with the same hit uncoated, so the main mod's own damage rules (races and so on) cancel out.
		float plain = lost(helper, attacker);
		HakiImbue.press(attacker);
		float coated = lost(helper, attacker);
		helper.assertTrue(coated > plain + 2.0f, "a level 3 coat should add 3 damage before armor, went from " + plain + " to " + coated);
		helper.succeed();
	}

	private static float lost(GameTestHelper helper, ServerPlayer attacker) {
		ServerPlayer target = fighter(helper, 0);
		target.hurtServer((ServerLevel) target.level(), attacker.damageSources().playerAttack(attacker), 1.0f);
		return target.getMaxHealth() - target.getHealth();
	}

	@GameTest
	public void aCoatedBladeTakesNoWear(GameTestHelper helper) {
		ServerPlayer player = fighter(helper, LEVEL_2);
		ItemStack katana = KatanaStats.build(IRON, IRON, IRON);
		player.setItemSlot(EquipmentSlot.MAINHAND, katana);
		HakiImbue.press(player);
		helper.runAfterDelay(2, () -> { // Let the server's clock catch up with the coat.
			katana.setDamageValue(5);
			helper.assertTrue(katana.getDamageValue() == 0, "a coated blade shouldn't wear, wore " + katana.getDamageValue());
			HakiImbue.press(player); // Let it go.
			katana.setDamageValue(5);
			helper.assertTrue(katana.getDamageValue() == 5, "an uncoated blade should wear");
			helper.succeed();
		});
	}

	private static ServerPlayer fighter(GameTestHelper helper, int armamentXp) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL); // The mock player starts in creative, where hunger and wear don't count.
		// 26.3 players can't be hurt until their game says it has loaded the world; the mock has no game to say so.
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
		player.teleportTo(at.x, at.y, at.z);
		HakiAttachments.set(player, HakiAttachments.get(player).withArmamentXp(armamentXp));
		return player;
	}

	private static ServerPlayer logiaUser(GameTestHelper helper) {
		ServerPlayer logia = fighter(helper, 0);
		logia.setAttached(ModAttachments.DEVIL_FRUIT, "moku_moku_no_mi");
		return logia;
	}
}
