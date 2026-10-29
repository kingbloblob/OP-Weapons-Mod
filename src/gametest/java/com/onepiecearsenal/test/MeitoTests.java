package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.meito.BladeRecord;
import com.onepiecearsenal.meito.Meito;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecemod.weapon.WeaponUpgrades;

/** Black Blade awakening, checked in a real world. Run with ./gradlew runGameTest. */
public class MeitoTests {
	@GameTest
	public void onlySupremeBladesCanAwaken(GameTestHelper helper) {
		ItemStack katana = KatanaStats.build(ArsenalMaterial.KAIROSEKI, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.MARINE_STEEL);
		WeaponUpgrades.setGrade(katana, 2);
		helper.assertFalse(Meito.canAwaken(katana), "a Great Grade blade shouldn't be able to awaken");
		WeaponUpgrades.setGrade(katana, 3);
		helper.assertTrue(Meito.canAwaken(katana), "a Supreme Grade blade should be able to awaken");
		helper.succeed();
	}

	@GameTest
	public void awakeningTurnsTheBladeBlackAndKeepsTheRest(GameTestHelper helper) {
		ItemStack katana = KatanaStats.build(ArsenalMaterial.KAIROSEKI, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.DRIFTWOOD);
		WeaponUpgrades.setGrade(katana, 3);
		katana.set(ArsenalComponents.BLADE_RECORD, new BladeRecord(1600, 30, 5));
		katana.setDamageValue(100);
		double damageBefore = attackDamage(katana);

		KatanaStats.awaken(katana);

		List<ArsenalMaterial> parts = KatanaStats.parts(katana);
		helper.assertTrue(parts.get(0) == ArsenalMaterial.BLACK_BLADE, "the blade should be Black Blade Steel");
		helper.assertTrue(parts.get(1) == ArsenalMaterial.MARINE_STEEL && parts.get(2) == ArsenalMaterial.DRIFTWOOD, "the tsuba and hilt should stay");
		helper.assertTrue(KatanaStats.isBlackBlade(katana) && !Meito.canAwaken(katana), "an awakened blade can't awaken again");
		helper.assertTrue(KatanaStats.traits(katana).contains(Trait.BLACK_BLADE), "it should have the Black Blade trait");
		helper.assertTrue(WeaponUpgrades.grade(katana) == 3, "it should stay Supreme Grade");
		helper.assertTrue(Meito.record(katana).xp() == 1600, "it should keep its blade level");
		helper.assertTrue(katana.getDamageValue() == 0, "it should come out fully mended");
		double expected = damageBefore - ArsenalMaterial.KAIROSEKI.damage + ArsenalMaterial.BLACK_BLADE.damage;
		helper.assertTrue(Math.abs(attackDamage(katana) - expected) < 0.001,
				"only the blade's damage should change (expected " + expected + ", got " + attackDamage(katana) + ")");
		helper.succeed();
	}

	/** The katana's bonus attack damage from all its modifiers: blade, grade and level. */
	private static double attackDamage(ItemStack katana) {
		double total = 0;
		for (ItemAttributeModifiers.Entry entry : katana.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
				ItemAttributeModifiers.EMPTY).modifiers()) {
			if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
				total += entry.modifier().amount();
			}
		}
		return total;
	}
}
