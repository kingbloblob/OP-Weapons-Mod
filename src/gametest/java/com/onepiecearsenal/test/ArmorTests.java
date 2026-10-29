package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;

/**
 * Worn Arsenal armor: every piece of a set counts. Run with ./gradlew runGameTest.
 */
public class ArmorTests {
	private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final List<PartType> SHELLS = List.of(PartType.CROWN, PartType.COAT_PLATES, PartType.SASH_WEAVE, PartType.BOOT_SOLES);

	@GameTest
	public void fullSetAddsUpEveryPiece(GameTestHelper helper) {
		ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 1, 1));
		ArsenalMaterial shell = ArsenalMaterial.KAIROSEKI;
		double armor = 0;
		for (int i = 0; i < SLOTS.size(); i++) {
			stand.setItemSlot(SLOTS.get(i), ArmorStats.build(SHELLS.get(i), shell, ArsenalMaterial.SHIP_IRON));
			armor += ArmorStats.defense(shell, ArmorStats.typeOf(SHELLS.get(i)));
		}
		double expectedArmor = armor;
		double expectedToughness = ArmorStats.toughness(shell) * SLOTS.size();
		helper.succeedWhen(() -> {
			helper.assertTrue(stand.getAttributeValue(Attributes.ARMOR) == expectedArmor,
					"a full set should give " + expectedArmor + " armor, got " + stand.getAttributeValue(Attributes.ARMOR));
			helper.assertTrue(stand.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == expectedToughness,
					"a full set should give " + expectedToughness + " toughness, got " + stand.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		});
	}
}
