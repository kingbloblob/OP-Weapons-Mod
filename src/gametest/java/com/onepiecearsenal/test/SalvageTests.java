package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.salvage.Salvage;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.upgrade.Upgrade;
import com.onepiecearsenal.upgrade.Upgrades;

/** Broken gear and taking gear apart at the Shipwright's Anvil (Breakage and Salvage). Run with ./gradlew runGameTest. */
public class SalvageTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;
	private static final ArsenalMaterial KAIROSEKI = ArsenalMaterial.KAIROSEKI;

	@GameTest
	public void wornOutGearBreaksInsteadOfVanishing(GameTestHelper helper) {
		ServerPlayer fighter = helper.makeMockServerPlayerInLevel();
		ItemStack katana = KatanaStats.build(KAIROSEKI, IRON, IRON);
		fighter.setItemSlot(EquipmentSlot.MAINHAND, katana);
		fighter.getMainHandItem().hurtAndBreak(katana.getMaxDamage() + 50, fighter, EquipmentSlot.MAINHAND);

		ItemStack held = fighter.getMainHandItem();
		helper.assertFalse(held.isEmpty(), "a worn-out katana should still be there");
		helper.assertTrue(held.getDamageValue() == held.getMaxDamage() - 1, "it should stop at its last point of durability");
		helper.assertTrue(Breakage.isBroken(held), "it should be broken");
		helper.assertTrue(held.getHoverName().getString().startsWith("Broken "), "its name should say Broken, got " + held.getHoverName().getString());
		helper.assertTrue(TraitEffects.traits(held).isEmpty(), "a broken katana's traits should go quiet");
		helper.succeed();
	}

	@GameTest
	public void brokenArmorStopsNothing(GameTestHelper helper) {
		ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 1, 1));
		ItemStack coat = ArmorStats.build(PartType.COAT_PLATES, IRON, IRON);
		coat.setDamageValue(coat.getMaxDamage() + 10);
		stand.setItemSlot(EquipmentSlot.CHEST, coat);
		helper.succeedWhen(() -> {
			helper.assertTrue(Breakage.isBroken(stand.getItemBySlot(EquipmentSlot.CHEST)), "the coat should be broken, not gone");
			helper.assertTrue(stand.getAttributeValue(Attributes.ARMOR) == 0, "a broken coat should give no armor, got " + stand.getAttributeValue(Attributes.ARMOR));
		});
	}

	@GameTest
	public void workingGearComesApartIntoEveryPart(GameTestHelper helper) {
		ServerPlayer smith = helper.makeMockServerPlayerInLevel();
		smith.setItemSlot(EquipmentSlot.MAINHAND, KatanaStats.build(KAIROSEKI, IRON, ArsenalMaterial.DRIFTWOOD));
		Salvage.takeApart(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), smith);

		helper.assertTrue(smith.getMainHandItem().getItem() != ArsenalItems.KATANA, "the katana should be gone from the hand");
		helper.assertTrue(smith.getInventory().countItem(ArsenalItems.KATANA_BLADE) == 1, "it should give back its blade");
		helper.assertTrue(smith.getInventory().countItem(ArsenalItems.TSUBA) == 1, "it should give back its tsuba");
		helper.assertTrue(smith.getInventory().countItem(ArsenalItems.HILT) == 1, "it should give back its hilt");
		helper.succeed();
	}

	@GameTest
	public void brokenGearLosesThePartHoldingItTogether(GameTestHelper helper) {
		ItemStack katana = KatanaStats.build(KAIROSEKI, IRON, ArsenalMaterial.DRIFTWOOD);
		Upgrades.add(katana, Upgrade.byId("honed_edge"));
		katana.setDamageValue(katana.getMaxDamage());

		List<ItemStack> parts = Salvage.parts(katana);
		helper.assertTrue(parts.size() == 2, "a broken katana should give back 2 parts, got " + parts.size());
		helper.assertTrue(parts.get(0).getItem() == ArsenalItems.KATANA_BLADE && PartItem.material(parts.get(0)) == KAIROSEKI,
				"the first part should be its Kairoseki blade");
		helper.assertTrue(parts.get(1).getItem() == ArsenalItems.HILT && PartItem.material(parts.get(1)) == ArsenalMaterial.DRIFTWOOD,
				"the tsuba should snap and the Driftwood hilt come back");
		ItemStack history = Salvage.history(parts.get(0));
		helper.assertTrue(Upgrades.level(history, Upgrade.byId("honed_edge")) == 1, "the blade should remember its Honed Edge");
		helper.assertTrue(history.getDamageValue() == 0, "a broken katana's blade should come back unworn");
		helper.succeed();
	}

	@GameTest
	public void aSalvagedBladeForgesBackIntoItsKatana(GameTestHelper helper) {
		ItemStack katana = KatanaStats.build(KAIROSEKI, IRON, IRON);
		Upgrades.add(katana, Upgrade.byId("honed_edge"));
		katana.setDamageValue(katana.getMaxDamage() / 2);
		float worn = katana.getDamageValue() / (float) katana.getMaxDamage();

		ItemStack blade = Salvage.parts(katana).get(0);
		ItemStack forged = Salvage.restore(blade, KatanaStats.build(KAIROSEKI, ArsenalMaterial.DRIFTWOOD, IRON));

		helper.assertTrue(KatanaStats.parts(forged).get(1) == ArsenalMaterial.DRIFTWOOD, "it should have its new Driftwood tsuba");
		helper.assertTrue(Upgrades.level(forged, Upgrade.byId("honed_edge")) == 1, "it should keep its Honed Edge");
		float nowWorn = forged.getDamageValue() / (float) forged.getMaxDamage();
		helper.assertTrue(Math.abs(nowWorn - worn) < 0.02f, "it should be as worn as before (" + worn + "), got " + nowWorn);
		helper.assertFalse(forged.has(ArsenalComponents.SALVAGED_FROM), "the forged katana shouldn't carry a history of its own");

		ItemStack fresh = Salvage.restore(PartItem.of(ArsenalItems.KATANA_BLADE, KAIROSEKI), KatanaStats.build(KAIROSEKI, IRON, IRON));
		helper.assertTrue(Upgrades.usedSlots(fresh) == 0 && fresh.getDamageValue() == 0, "a fresh blade makes a fresh katana");
		helper.succeed();
	}
}
