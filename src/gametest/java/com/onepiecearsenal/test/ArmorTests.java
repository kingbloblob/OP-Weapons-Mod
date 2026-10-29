package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalMod;
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

	@GameTest
	public void oldPiecesAreFixed(GameTestHelper helper) {
		ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 1, 1));
		ArsenalMaterial shell = ArsenalMaterial.KAIROSEKI;
		double armor = 0;
		for (int i = 0; i < SLOTS.size(); i++) {
			ItemStack piece = asForgedBeforeTheFix(ArmorStats.build(SHELLS.get(i), shell, ArsenalMaterial.SHIP_IRON));
			helper.assertTrue(ArmorStats.fixSharedIds(piece), "an old piece should need fixing");
			helper.assertFalse(ArmorStats.fixSharedIds(piece), "a fixed piece shouldn't need fixing again");
			stand.setItemSlot(SLOTS.get(i), piece);
			armor += ArmorStats.defense(shell, ArmorStats.typeOf(SHELLS.get(i)));
		}
		double expectedArmor = armor;
		double expectedToughness = ArmorStats.toughness(shell) * SLOTS.size();
		helper.succeedWhen(() -> {
			helper.assertTrue(stand.getAttributeValue(Attributes.ARMOR) == expectedArmor,
					"a fixed old set should give " + expectedArmor + " armor, got " + stand.getAttributeValue(Attributes.ARMOR));
			helper.assertTrue(stand.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == expectedToughness,
					"a fixed old set should give " + expectedToughness + " toughness, got " + stand.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		});
	}

	/** Puts back the shared ids every piece had before each slot got its own. */
	private static ItemStack asForgedBeforeTheFix(ItemStack piece) {
		ItemAttributeModifiers modifiers = ItemAttributeModifiers.EMPTY;
		for (ItemAttributeModifiers.Entry entry : piece.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers()) {
			AttributeModifier modifier = entry.modifier();
			String path = modifier.id().getPath();
			if (path.startsWith("armor_plates.") || path.startsWith("armor_toughness.")) {
				modifier = new AttributeModifier(ArsenalMod.id(path.substring(0, path.indexOf('.'))), modifier.amount(), modifier.operation());
			}
			modifiers = modifiers.withModifierAdded(entry.attribute(), modifier, entry.slot());
		}
		piece.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
		return piece;
	}
}
