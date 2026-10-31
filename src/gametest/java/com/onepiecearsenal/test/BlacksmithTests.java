package com.onepiecearsenal.test;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.station.NavyCastingBlock;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.town.Blacksmiths;

/** Town blacksmiths forging parts, putting gear together and mending it (town/Blacksmiths). Run with ./gradlew runGameTest. */
public class BlacksmithTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;
	private static final BlockPos AT = new BlockPos(1, 1, 1);

	@GameTest
	public void smithForgesAPartForLessMaterial(GameTestHelper helper) {
		ServerPlayer customer = customer(helper);
		customer.setItemInHand(InteractionHand.MAIN_HAND, PartItem.of(ArsenalItems.KATANA_BLADE, ArsenalMaterial.DRIFTWOOD));
		customer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_INGOT, 10));
		customer.getInventory().add(new ItemStack(OnePieceBridge.berry(), 10));
		List<String> said = new ArrayList<>();

		helper.assertTrue(Blacksmiths.serve(helper.getLevel(), AT, customer, "", said::add), "the smith should forge it: " + said);
		int cost = NavyCastingBlock.cost(PartType.KATANA_BLADE) - 1;
		helper.assertTrue(count(customer.getInventory(), Items.IRON_INGOT) == 10 - cost, "a katana blade should take " + cost + " iron, one less than the recipe");
		helper.assertTrue(count(customer.getInventory(), OnePieceBridge.berry()) == 10 - Blacksmiths.forgeFee(IRON, ""), "the fee should be paid");
		helper.assertTrue(hasPart(customer.getInventory(), PartType.KATANA_BLADE, IRON), "a Ship Iron katana blade should be in the inventory");
		helper.assertTrue(PartItem.material(customer.getMainHandItem()) == ArsenalMaterial.DRIFTWOOD, "the pattern should be kept");
		helper.succeed();
	}

	@GameTest
	public void specialtyPartsTakeLessAgain(GameTestHelper helper) {
		int plain = Blacksmiths.materialCost(PartType.KATANA_BLADE, "");
		helper.assertTrue(Blacksmiths.specialty("flower_capital", PartType.KATANA_BLADE), "katana blades should be a Flower Capital specialty");
		helper.assertTrue(Blacksmiths.materialCost(PartType.KATANA_BLADE, "flower_capital") == plain - 1, "a specialty takes one less again");
		helper.assertTrue(Blacksmiths.materialCost(PartType.HILT, "flower_capital") == 1, "never less than one");
		helper.assertTrue(Blacksmiths.forgeFee(IRON, "mock_town") > Blacksmiths.forgeFee(IRON, ""), "Mock Town charges more");
		helper.succeed();
	}

	@GameTest
	public void smithWontWorkNavyMaterials(GameTestHelper helper) {
		ServerPlayer customer = customer(helper);
		customer.setItemInHand(InteractionHand.MAIN_HAND, PartItem.of(ArsenalItems.KATANA_BLADE, IRON));
		customer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ArsenalMaterial.MARINE_STEEL.repairItem(), 10));
		customer.getInventory().add(new ItemStack(OnePieceBridge.berry(), 10));
		List<String> said = new ArrayList<>();

		helper.assertFalse(Blacksmiths.serve(helper.getLevel(), AT, customer, "", said::add), "the smith should refuse Marine steel");
		helper.assertTrue(count(customer.getInventory(), ArsenalMaterial.MARINE_STEEL.repairItem()) == 10, "nothing should be taken");
		helper.assertTrue(count(customer.getInventory(), OnePieceBridge.berry()) == 10, "no fee for refused work");
		helper.assertTrue(said.size() == 1 && said.get(0).contains("Navy"), "the smith should say why, got " + said);
		helper.succeed();
	}

	@GameTest
	public void smithAssemblesAFullSet(GameTestHelper helper) {
		ServerPlayer customer = customer(helper);
		customer.setItemInHand(InteractionHand.MAIN_HAND, PartItem.of(ArsenalItems.KATANA_BLADE, IRON));
		customer.getInventory().add(PartItem.of(ArsenalItems.TSUBA, IRON));
		customer.getInventory().add(PartItem.of(ArsenalItems.HILT, IRON));
		customer.getInventory().add(new ItemStack(OnePieceBridge.berry(), 10));
		List<String> said = new ArrayList<>();

		helper.assertTrue(Blacksmiths.serve(helper.getLevel(), AT, customer, "", said::add), "the smith should put it together: " + said);
		boolean katana = false;
		for (int i = 0; i < customer.getInventory().getContainerSize(); i++) {
			katana |= !KatanaStats.parts(customer.getInventory().getItem(i)).isEmpty();
		}
		helper.assertTrue(katana, "a forged katana should be in the inventory");
		helper.assertTrue(count(customer.getInventory(), OnePieceBridge.berry()) < 10, "putting it together should cost Berries");
		helper.succeed();
	}

	@GameTest
	public void smithMendsTwiceAsWellAsTheAnvil(GameTestHelper helper) {
		ServerPlayer customer = customer(helper);
		ItemStack katana = KatanaStats.build(IRON, IRON, IRON);
		katana.setDamageValue(katana.getMaxDamage() - 1); // Broken.
		customer.setItemInHand(InteractionHand.MAIN_HAND, katana);
		customer.getInventory().add(new ItemStack(Items.IRON_INGOT, 10));
		customer.getInventory().add(new ItemStack(OnePieceBridge.berry(), 10));
		List<String> said = new ArrayList<>();

		helper.assertTrue(Blacksmiths.wantsSmithing(katana), "a smith should want to work on broken gear");
		helper.assertTrue(Blacksmiths.serve(helper.getLevel(), AT, customer, "", said::add), "the smith should mend it: " + said);
		helper.assertFalse(customer.getMainHandItem().isDamaged(), "it should be good as new");
		helper.assertTrue(count(customer.getInventory(), Items.IRON_INGOT) == 8, "two ingots should mend it from broken, got "
				+ (10 - count(customer.getInventory(), Items.IRON_INGOT)));
		helper.assertTrue(count(customer.getInventory(), OnePieceBridge.berry()) == 8, "a Berry per ingot");
		helper.assertFalse(Blacksmiths.wantsSmithing(customer.getMainHandItem()), "mended gear goes back to the smith's usual trades");
		helper.succeed();
	}

	@GameTest
	public void noBerriesNoWork(GameTestHelper helper) {
		ServerPlayer customer = customer(helper);
		ItemStack katana = KatanaStats.build(IRON, IRON, IRON);
		katana.setDamageValue(katana.getMaxDamage() / 2);
		customer.setItemInHand(InteractionHand.MAIN_HAND, katana);
		customer.getInventory().add(new ItemStack(Items.IRON_INGOT, 10));
		List<String> said = new ArrayList<>();

		helper.assertFalse(Blacksmiths.serve(helper.getLevel(), AT, customer, "", said::add), "no Berries, no mending");
		helper.assertTrue(count(customer.getInventory(), Items.IRON_INGOT) == 10, "nothing should be taken");
		helper.succeed();
	}

	private static ServerPlayer customer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL); // Creative players don't pay.
		return player;
	}

	private static int count(Inventory inventory, Item item) {
		int count = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item) && !(stack.getItem() instanceof PartItem)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static boolean hasPart(Inventory inventory, PartType type, ArsenalMaterial material) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.getItem() instanceof PartItem part && part.type() == type && PartItem.material(stack) == material) {
				return true;
			}
		}
		return false;
	}
}
