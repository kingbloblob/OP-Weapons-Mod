package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.upgrade.Upgrade;
import com.onepiecearsenal.upgrade.Upgrades;
import com.onepiecemod.weapon.GunItem;

/** Repairs and upgrades at the Shipwright's Anvil (Upgrades and upgrades.json). Run with ./gradlew runGameTest. */
public class UpgradeTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;

	@GameTest
	public void upgradesAddUpAndFillTheSlots(GameTestHelper helper) {
		ItemStack katana = KatanaStats.build(ArsenalMaterial.TAMAHAGANE, IRON, IRON);
		double before = total(katana, Attributes.ATTACK_DAMAGE);
		Upgrade honed = Upgrade.byId("honed_edge");
		for (int i = 0; i < 3; i++) {
			helper.assertTrue(Upgrades.add(katana, honed), "Honed Edge level " + (i + 1) + " should go on");
		}
		helper.assertTrue(Math.abs(total(katana, Attributes.ATTACK_DAMAGE) - before - 2.25) < 0.001,
				"three levels of Honed Edge should add 2.25 damage, got " + (total(katana, Attributes.ATTACK_DAMAGE) - before));
		helper.assertFalse(Upgrades.add(katana, honed), "Honed Edge stops at level III");
		helper.assertTrue(Upgrades.usedSlots(katana) == 3, "three levels should use three slots");
		helper.assertFalse(Upgrades.add(katana, Upgrade.byId("wrapped_grip")), "a katana with every slot used takes no more");
		helper.succeed();
	}

	@GameTest
	public void upgradesOnlyFitTheirGear(GameTestHelper helper) {
		ItemStack coat = ArmorStats.build(PartType.COAT_PLATES, IRON, IRON);
		helper.assertTrue(Upgrades.whyNot(coat, Upgrade.byId("tide_runner")) != null, "Tide-Runner Soles only fit boots");
		helper.assertTrue(Upgrades.whyNot(coat, Upgrade.byId("powder_chamber")) != null, "Powder Chamber only fits guns");
		helper.assertTrue(Upgrades.whyNot(coat, Upgrade.byId("hull_plating")) == null, "Hull Plating fits a coat");
		helper.assertTrue(Upgrades.gearOf(new ItemStack(ArsenalItems.KATANA)) == null, "an unforged katana can't be upgraded");
		helper.succeed();
	}

	@GameTest
	public void hullPlatingCountsOnEveryPiece(GameTestHelper helper) {
		ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 1, 1));
		List<EquipmentSlot> slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
		List<PartType> shells = List.of(PartType.CROWN, PartType.COAT_PLATES, PartType.SASH_WEAVE, PartType.BOOT_SOLES);
		double armor = 0;
		for (int i = 0; i < slots.size(); i++) {
			ItemStack piece = ArmorStats.build(shells.get(i), IRON, IRON);
			Upgrades.add(piece, Upgrade.byId("hull_plating"));
			Upgrades.add(piece, Upgrade.byId("hull_plating"));
			stand.setItemSlot(slots.get(i), piece);
			armor += ArmorStats.defense(IRON, ArmorStats.typeOf(shells.get(i))) + 2;
		}
		double expected = armor;
		helper.succeedWhen(() -> helper.assertTrue(stand.getAttributeValue(Attributes.ARMOR) == expected,
				"a Hull Plated II set should give " + expected + " armor, got " + stand.getAttributeValue(Attributes.ARMOR)));
	}

	@GameTest
	public void reinforcedSurvivesTheBlackBladeAwakening(GameTestHelper helper) {
		ItemStack katana = KatanaStats.build(ArsenalMaterial.KAIROSEKI, IRON, ArsenalMaterial.DRIFTWOOD);
		int base = katana.getMaxDamage();
		Upgrades.add(katana, Upgrade.byId("reinforced"));
		Upgrades.add(katana, Upgrade.byId("honed_edge"));
		helper.assertTrue(katana.getMaxDamage() == Math.round(base * 1.2f), "Reinforced I should add 20% durability");
		double honed = total(katana, Attributes.ATTACK_DAMAGE) - ArsenalMaterial.KAIROSEKI.damage;

		KatanaStats.awaken(katana);

		int blackBase = KatanaStats.build(ArsenalMaterial.BLACK_BLADE, IRON, ArsenalMaterial.DRIFTWOOD).getMaxDamage();
		helper.assertTrue(katana.getMaxDamage() == Math.round(blackBase * 1.2f),
				"an awakened Reinforced blade should keep its 20% (expected " + Math.round(blackBase * 1.2f) + ", got " + katana.getMaxDamage() + ")");
		helper.assertTrue(Math.abs(total(katana, Attributes.ATTACK_DAMAGE) - ArsenalMaterial.BLACK_BLADE.damage - honed) < 0.001,
				"an awakened blade should keep its Honed Edge");
		helper.succeed();
	}

	@GameTest
	public void gunUpgradesChangeTheShot(GameTestHelper helper) {
		ItemStack gun = GunStats.build(GunType.FLINTLOCK, IRON, IRON, IRON);
		GunItem.Stats before = GunStats.stats(gun);
		Upgrades.add(gun, Upgrade.byId("powder_chamber"));
		Upgrades.add(gun, Upgrade.byId("waxed_lock"));
		Upgrades.add(gun, Upgrade.byId("rifled_bore"));
		GunItem.Stats after = GunStats.stats(gun);
		helper.assertTrue(Math.abs(after.damage() - before.damage() - 1.5f) < 0.001, "Powder Chamber I should add 1.5 damage");
		helper.assertTrue(after.cooldown() < before.cooldown(), "Waxed Lock should cut the reload");
		helper.assertTrue(after.range() > before.range() && after.spread() < before.spread(), "Rifled Bore should reach further and spread less");
		helper.succeed();
	}

	@GameTest
	public void anvilMendsWithTheBladeMaterialAndUpgradesFromTheOffhand(GameTestHelper helper) {
		ServerPlayer smith = helper.makeMockServerPlayerInLevel();
		BlockPos anvil = helper.absolutePos(new BlockPos(1, 1, 1));
		ItemStack katana = KatanaStats.build(IRON, IRON, IRON);
		katana.setDamageValue(katana.getMaxDamage() - 1);
		smith.setItemSlot(EquipmentSlot.MAINHAND, katana);
		smith.getInventory().add(new ItemStack(Items.IRON_INGOT, 10));

		InteractionResult mended = Upgrades.work(helper.getLevel(), anvil, smith, smith.getMainHandItem());
		helper.assertTrue(mended == InteractionResult.SUCCESS, "the anvil should mend a Ship Iron katana with Iron Ingots");
		helper.assertTrue(smith.getMainHandItem().getDamageValue() == 0, "four Iron Ingots should mend it all the way");
		helper.assertTrue(smith.getInventory().countItem(Items.IRON_INGOT) == 6, "mending it should take 4 Iron Ingots, "
				+ (10 - smith.getInventory().countItem(Items.IRON_INGOT)) + " were taken");

		smith.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.QUARTZ, 8));
		InteractionResult upgraded = Upgrades.work(helper.getLevel(), anvil, smith, smith.getMainHandItem());
		helper.assertTrue(upgraded == InteractionResult.SUCCESS, "Quartz in the offhand should add Honed Edge");
		helper.assertTrue(Upgrades.level(smith.getMainHandItem(), Upgrade.byId("honed_edge")) == 1, "it should be Honed Edge I");
		helper.assertTrue(smith.getOffhandItem().isEmpty(), "Honed Edge should take all 8 Quartz");
		helper.assertTrue(Upgrades.work(helper.getLevel(), anvil, smith, smith.getMainHandItem()) == null,
				"an undamaged katana with nothing in the offhand has nothing to do");
		helper.succeed();
	}

	/** The item's total bonus to one attribute from its own modifiers. */
	private static double total(ItemStack stack, Holder<Attribute> attribute) {
		double total = 0;
		for (ItemAttributeModifiers.Entry entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			if (entry.attribute().is(attribute)) {
				total += entry.modifier().amount();
			}
		}
		return total;
	}
}
