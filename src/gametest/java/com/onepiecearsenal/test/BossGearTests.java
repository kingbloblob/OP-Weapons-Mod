package com.onepiecearsenal.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.loot.BossGear;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecemod.character.BossStrikes;
import com.onepiecemod.npc.NpcFeature;
import com.onepiecemod.weapon.WeaponUpgrades;

/** What the Navy's top brass drop (loot/BossGear). Run with ./gradlew runGameTest. */
public class BossGearTests {
	private static final int ROLLS = 4000;

	@GameTest
	public void viceAdmiralsDropRareNavyGear(GameTestHelper helper) {
		Mob admiral = marine(helper, "vice_admiral");
		RandomSource random = RandomSource.create(42);
		int parts = 0;
		int blades = 0;
		for (int i = 0; i < ROLLS; i++) {
			ItemStack drop = BossGear.dropFor(admiral, random);
			if (drop.isEmpty()) {
				continue;
			}
			if (drop.getItem() instanceof KatanaItem) {
				helper.assertTrue(WeaponUpgrades.grade(drop) == 1, "a Vice Admiral's blade should be Skillful Grade");
				helper.assertTrue(KatanaStats.parts(drop).get(0) == ArsenalMaterial.KAIROSEKI, "a Vice Admiral's blade should be Kairoseki");
				helper.assertTrue(drop.has(DataComponents.CUSTOM_NAME), "a Vice Admiral's blade should be named");
				blades++;
			} else {
				ArsenalMaterial material = PartItem.material(drop);
				helper.assertTrue(drop.has(ArsenalComponents.PART_MATERIAL)
						&& (material == ArsenalMaterial.KAIROSEKI || material == ArsenalMaterial.PACIFISTA_ALLOY),
						"a Vice Admiral's part should be Kairoseki or Pacifista Alloy, got " + drop);
				parts++;
			}
		}
		// 12% parts and 2% blades: allow plenty of room for chance.
		helper.assertTrue(parts > ROLLS * 0.08 && parts < ROLLS * 0.16, "expected about 12% parts, got " + parts + "/" + ROLLS);
		helper.assertTrue(blades > 0 && blades < ROLLS * 0.05, "expected about 2% blades, got " + blades + "/" + ROLLS);
		helper.succeed();
	}

	@GameTest
	public void rankAndFileAndBossCrewDropNothing(GameTestHelper helper) {
		Mob recruit = marine(helper, "marine");
		Mob crew = marine(helper, "elite_marine");
		crew.addTag(BossStrikes.MINION_TAG);
		RandomSource random = RandomSource.create(7);
		for (int i = 0; i < ROLLS; i++) {
			helper.assertTrue(BossGear.dropFor(recruit, random).isEmpty(), "plain Marines shouldn't drop boss gear");
			helper.assertTrue(BossGear.dropFor(crew, random).isEmpty(), "a boss's called-in crew shouldn't drop boss gear");
		}
		helper.succeed();
	}

	@GameTest
	public void famousMarinesHaveTheirGear(GameTestHelper helper) {
		for (String id : new String[] {"fujitora", "smoker", "akainu", "kizaru", "aokiji", "ryokugyu", "lucci", "magellan"}) {
			helper.assertTrue(BossGear.signatureOf(id) != null, id + " should carry named gear");
		}
		helper.assertTrue(BossGear.signatureOf("mihawk") == null, "pirates aren't Navy brass");
		for (BossGear.Signature signature : BossGear.SIGNATURES) {
			helper.assertFalse(signature.gear().get().isEmpty(), signature.name() + " should build");
		}
		ItemStack shikomizue = BossGear.signatureOf("fujitora").gear().get();
		helper.assertTrue(WeaponUpgrades.grade(shikomizue) == 2, "Fujitora's blade should be Great Grade");
		helper.succeed();
	}

	private static Mob marine(GameTestHelper helper, String kit) {
		Mob marine = NpcFeature.spawn(helper.getLevel(), kit, helper.absolutePos(new BlockPos(1, 1, 1)), 0f);
		helper.assertTrue(marine != null, "the main mod should have a '" + kit + "' kit");
		return marine;
	}
}
