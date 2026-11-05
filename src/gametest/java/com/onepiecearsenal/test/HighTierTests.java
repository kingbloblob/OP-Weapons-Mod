package com.onepiecearsenal.test;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.station.HighTier;
import com.onepiecearsenal.station.NavyCastingBlock;
import com.onepiecearsenal.station.ShipwrightsAnvilBlock;
import com.onepiecearsenal.station.ShipwrightsForgeBlock;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.town.Blacksmiths;

/** The Shipwright's Forge and Grand Line Anvil for tier 4 and 5 materials (station/HighTier). Run with ./gradlew runGameTest. */
public class HighTierTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;
	private static final BlockPos AT = new BlockPos(1, 1, 1);

	private static ArsenalMaterial netherite() {
		return ArsenalMaterial.byId("netherite");
	}

	@GameTest
	public void highTierPartsHaveNoCraftingRecipes(GameTestHelper helper) {
		List<String> forgeable = HighTier.forgeMaterials().stream().map(ArsenalMaterial::id).toList();
		helper.assertTrue(forgeable.containsAll(List.of("wapometal", "adam_wood", "kokuto", "dragon_scale", "netherite")),
				"the forge should work every craftable tier 4 pirate or shared material, got " + forgeable);
		helper.assertFalse(forgeable.contains("tamahagane") || forgeable.contains("vegapunk_compound") || forgeable.contains("black_blade"),
				"tier 3, Navy and uncraftable materials aren't forge materials");
		boolean ironBlade = false;
		for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
			String path = holder.id().identifier().getPath();
			for (String id : forgeable) {
				helper.assertFalse(path.endsWith("/" + id), holder.id().identifier() + " shouldn't exist: " + id + " parts are made at the forge");
			}
			ironBlade |= path.equals("katana_blade/ship_iron");
		}
		helper.assertTrue(ironBlade, "lower tiers still craft their parts");
		helper.succeed();
	}

	@GameTest
	public void forgeNeedsHeatThenCopiesThePattern(GameTestHelper helper) {
		helper.setBlock(AT, ArsenalItems.SHIPWRIGHTS_FORGE);
		BlockPos pos = helper.absolutePos(AT);
		ServerPlayer smith = smith(helper);
		ItemStack pattern = PartItem.of(ArsenalItems.KATANA_BLADE, IRON);
		smith.setItemInHand(InteractionHand.MAIN_HAND, pattern);
		smith.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.NETHERITE_INGOT, 5));
		PartItem part = (PartItem) ArsenalItems.KATANA_BLADE;

		InteractionResult cold = ShipwrightsForgeBlock.forge(helper.getLevel(), pos, helper.getBlockState(AT), smith, part);
		helper.assertTrue(cold == InteractionResult.FAIL, "a cold forge shouldn't make anything");
		helper.assertTrue(smith.getOffhandItem().getCount() == 5, "nothing should be taken from a cold forge");

		ItemStack coal = new ItemStack(Items.COAL, 1);
		ShipwrightsForgeBlock.stoke(helper.getLevel(), pos, helper.getBlockState(AT), smith, coal);
		ShipwrightsForgeBlock.stoke(helper.getLevel(), pos, helper.getBlockState(AT), smith, new ItemStack(Items.BLAZE_ROD));
		helper.assertTrue(coal.isEmpty(), "stoking burns the fuel");
		helper.assertTrue(helper.getBlockState(AT).getValue(ShipwrightsForgeBlock.HEAT) == 6, "coal and a blaze rod should give 6 heat");

		InteractionResult hot = ShipwrightsForgeBlock.forge(helper.getLevel(), pos, helper.getBlockState(AT), smith, part);
		int cost = NavyCastingBlock.cost(PartType.KATANA_BLADE);
		helper.assertTrue(hot == InteractionResult.SUCCESS, "a hot forge should make a netherite blade");
		helper.assertTrue(smith.getOffhandItem().getCount() == 5 - cost, "it should take " + cost + " netherite");
		helper.assertTrue(helper.getBlockState(AT).getValue(ShipwrightsForgeBlock.HEAT) == 6 - cost, "and " + cost + " heat");
		helper.assertTrue(hasPart(smith, PartType.KATANA_BLADE, netherite()), "a netherite katana blade should be in the inventory");
		helper.assertTrue(PartItem.material(smith.getMainHandItem()) == IRON, "the pattern should be kept");
		helper.succeed();
	}

	@GameTest
	public void forgeRefusesMaterialsThatDontNeedIt(GameTestHelper helper) {
		helper.setBlock(AT, ArsenalItems.SHIPWRIGHTS_FORGE.defaultBlockState().setValue(ShipwrightsForgeBlock.HEAT, 15));
		ServerPlayer smith = smith(helper);
		smith.setItemInHand(InteractionHand.MAIN_HAND, PartItem.of(ArsenalItems.KATANA_BLADE, IRON));
		smith.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_INGOT, 5));
		InteractionResult result = ShipwrightsForgeBlock.forge(helper.getLevel(), helper.absolutePos(AT), helper.getBlockState(AT), smith,
				(PartItem) ArsenalItems.KATANA_BLADE);
		helper.assertTrue(result == InteractionResult.FAIL, "iron parts come from a crafting table, not the forge");
		helper.assertTrue(smith.getOffhandItem().getCount() == 5, "nothing should be taken");
		helper.succeed();
	}

	@GameTest
	public void onlyTheGrandLineAnvilPutsHighTierGearTogether(GameTestHelper helper) {
		ServerPlayer smith = smith(helper);
		smith.getInventory().add(PartItem.of(ArsenalItems.KATANA_BLADE, IRON));
		smith.getInventory().add(PartItem.of(ArsenalItems.TSUBA, IRON));
		smith.getInventory().add(PartItem.of(ArsenalItems.HILT, netherite())); // Even a high-tier handle needs it.
		BlockPos pos = helper.absolutePos(AT);

		helper.assertTrue(ShipwrightsAnvilBlock.forge(helper.getLevel(), pos, smith, false, false) == InteractionResult.FAIL,
				"a Shipwright's Anvil shouldn't put a netherite hilt on a katana");
		helper.assertTrue(smith.getInventory().countItem(ArsenalItems.KATANA) == 0, "no katana yet");
		helper.assertTrue(ShipwrightsAnvilBlock.forge(helper.getLevel(), pos, smith, false, true) == InteractionResult.SUCCESS,
				"a Grand Line Anvil should");
		helper.assertTrue(smith.getInventory().countItem(ArsenalItems.KATANA) == 1, "a katana should be forged");
		helper.succeed();
	}

	@GameTest
	public void onlyTheGrandLineAnvilMendsHighTierGear(GameTestHelper helper) {
		helper.setBlock(AT, ArsenalItems.SHIPWRIGHTS_ANVIL);
		helper.setBlock(AT.east(), ArsenalItems.GRAND_LINE_ANVIL);
		ServerPlayer smith = smith(helper);
		ItemStack katana = KatanaStats.build(netherite(), IRON, IRON);
		katana.setDamageValue(katana.getMaxDamage() / 2);
		smith.setItemInHand(InteractionHand.MAIN_HAND, katana);
		smith.getInventory().add(new ItemStack(Items.NETHERITE_INGOT, 4));
		helper.assertTrue(HighTier.is(katana), "a netherite katana is high-tier gear");

		helper.assertTrue(use(helper, AT, smith) == InteractionResult.FAIL, "a Shipwright's Anvil shouldn't mend it");
		helper.assertTrue(smith.getMainHandItem().isDamaged() && smith.getInventory().countItem(Items.NETHERITE_INGOT) == 4, "nothing should change");
		helper.assertTrue(use(helper, AT.east(), smith) == InteractionResult.SUCCESS, "a Grand Line Anvil should mend it");
		helper.assertTrue(smith.getMainHandItem().getDamageValue() < katana.getMaxDamage() / 2, "it should be mended");

		ItemStack iron = KatanaStats.build(IRON, IRON, IRON);
		iron.setDamageValue(iron.getMaxDamage() / 2);
		smith.setItemInHand(InteractionHand.MAIN_HAND, iron);
		smith.getInventory().add(new ItemStack(Items.IRON_INGOT, 4));
		helper.assertTrue(use(helper, AT.east(), smith) == InteractionResult.SUCCESS, "the Grand Line Anvil still mends everyday gear");
		helper.succeed();
	}

	@GameTest
	public void townSmithsWontWorkHighTierMaterials(GameTestHelper helper) {
		ServerPlayer customer = smith(helper);
		customer.setItemInHand(InteractionHand.MAIN_HAND, PartItem.of(ArsenalItems.KATANA_BLADE, IRON));
		customer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.NETHERITE_INGOT, 5));
		customer.getInventory().add(new ItemStack(OnePieceBridge.berry(), 50));
		List<String> said = new ArrayList<>();
		helper.assertFalse(Blacksmiths.serve(helper.getLevel(), helper.absolutePos(AT), customer, "", said::add), "the smith shouldn't forge netherite");
		helper.assertTrue(customer.getOffhandItem().getCount() == 5, "nothing should be taken");
		helper.assertTrue(said.size() == 1 && said.get(0).contains("Forge"), "the smith should send them to a Shipwright's Forge, got " + said);

		ItemStack katana = KatanaStats.build(netherite(), IRON, IRON);
		katana.setDamageValue(10);
		customer.setItemInHand(InteractionHand.MAIN_HAND, katana);
		said.clear();
		helper.assertFalse(Blacksmiths.serve(helper.getLevel(), helper.absolutePos(AT), customer, "", said::add), "or mend netherite gear");
		helper.assertTrue(said.size() == 1 && said.get(0).contains("Grand Line Anvil"), "the smith should say why, got " + said);
		helper.succeed();
	}

	private static InteractionResult use(GameTestHelper helper, BlockPos relative, ServerPlayer smith) {
		BlockPos pos = helper.absolutePos(relative);
		BlockState state = helper.getBlockState(relative);
		ShipwrightsAnvilBlock anvil = (ShipwrightsAnvilBlock) state.getBlock();
		return anvil.useItemOn(smith.getMainHandItem(), state, helper.getLevel(), pos, smith, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	private static ServerPlayer smith(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		return player;
	}

	private static boolean hasPart(ServerPlayer player, PartType type, ArsenalMaterial material) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.getItem() instanceof PartItem part && part.type() == type && PartItem.material(stack) == material) {
				return true;
			}
		}
		return false;
	}
}
