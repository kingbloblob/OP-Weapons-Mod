package com.onepiecearsenal.test;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.salvage.Salvage;
import com.onepiecearsenal.station.ShipwrightsAnvilBlock;
import com.onepiecearsenal.tool.ArsenalBucketItem;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.Restat;
import com.onepiecearsenal.tool.ToolStats;
import com.onepiecearsenal.tool.ToolType;
import com.onepiecearsenal.upgrade.Upgrade;
import com.onepiecearsenal.upgrade.Upgrades;

/** Part-built tools (tool/ToolType, ToolStats, ToolEffects). Run with ./gradlew runGameTest. */
public class ToolTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;

	private static ItemStack tool(ToolType type, ArsenalMaterial head) {
		List<ArsenalMaterial> parts = new ArrayList<>(List.of(head));
		while (parts.size() < type.parts().length) {
			parts.add(IRON);
		}
		return ToolStats.build(type, parts);
	}

	@GameTest
	public void theAnvilForgesEveryTool(GameTestHelper helper) {
		ServerPlayer smith = helper.makeMockServerPlayerInLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		for (ToolType type : ToolType.values()) {
			smith.getInventory().clearContent();
			for (PartType part : type.parts()) {
				smith.getInventory().add(PartItem.of(ArsenalItems.partItem(part), IRON));
			}
			InteractionResult result = ShipwrightsAnvilBlock.forge(helper.getLevel(), pos, smith, false, false);
			helper.assertTrue(result.consumesAction(), "the anvil should forge a " + type.displayName + " from its parts");
			helper.assertTrue(smith.getInventory().countItem(ArsenalItems.TOOLS.get(type)) == 1, "the smith should be holding a " + type.displayName);
			ItemStack made = ItemStack.EMPTY;
			for (int slot = 0; slot < smith.getInventory().getContainerSize(); slot++) {
				if (smith.getInventory().getItem(slot).is(ArsenalItems.TOOLS.get(type))) {
					made = smith.getInventory().getItem(slot);
				}
			}
			helper.assertTrue(ToolStats.parts(made).size() == type.parts().length, type.displayName + " should remember its parts");
		}
		helper.succeed();
	}

	@GameTest
	public void theHeadDecidesWhatItDigs(GameTestHelper helper) {
		ItemStack driftwood = tool(ToolType.PICKAXE, ArsenalMaterial.DRIFTWOOD);
		ItemStack iron = tool(ToolType.PICKAXE, IRON);
		ItemStack diamond = tool(ToolType.PICKAXE, ArsenalMaterial.byId("diamond"));
		ItemStack kairoseki = tool(ToolType.PICKAXE, ArsenalMaterial.KAIROSEKI);
		BlockState ironOre = Blocks.IRON_ORE.defaultBlockState();
		BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
		helper.assertFalse(driftwood.isCorrectToolForDrops(ironOre), "a Driftwood pick can't get iron ore");
		helper.assertTrue(iron.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "a Ship Iron pick digs diamond ore, like an iron one");
		helper.assertFalse(iron.isCorrectToolForDrops(obsidian), "a Ship Iron pick can't dig obsidian");
		helper.assertTrue(diamond.isCorrectToolForDrops(obsidian), "a Diamond pick digs obsidian");
		helper.assertTrue(iron.getDestroySpeed(Blocks.STONE.defaultBlockState()) == IRON.miningSpeed, "a Ship Iron pick digs stone at its mining speed");
		helper.assertTrue(iron.getDestroySpeed(Blocks.DIRT.defaultBlockState()) == 1.0f, "a pick doesn't speed up digging dirt");
		helper.assertTrue(tool(ToolType.SHOVEL, IRON).getDestroySpeed(Blocks.DIRT.defaultBlockState()) == IRON.miningSpeed, "a shovel does");
		BlockState vein = BuiltInRegistries.BLOCK.getValue(Identifier.parse("onepiece:sea_prism_vein")).defaultBlockState();
		helper.assertFalse(diamond.isCorrectToolForDrops(vein), "a Diamond pick can't cut a Sea Prism Vein");
		helper.assertTrue(kairoseki.isCorrectToolForDrops(vein), "a Kairoseki pick cuts a Sea Prism Vein, like the One Piece Mod's Seastone pick");
		helper.succeed();
	}

	@GameTest
	public void aBrokenToolDigsLikeABareHand(GameTestHelper helper) {
		ItemStack pick = tool(ToolType.PICKAXE, ArsenalMaterial.byId("diamond"));
		pick.setDamageValue(pick.getMaxDamage() + 5);
		helper.assertTrue(Breakage.isBroken(pick), "a worn-out pick should break, not vanish");
		helper.assertTrue(pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 1.0f, "a broken pick digs like a hand");
		helper.assertFalse(pick.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()), "a broken pick gets no stone drops");
		helper.succeed();
	}

	@GameTest
	public void toolsAreGearLikeTheRest(GameTestHelper helper) {
		ItemStack pick = tool(ToolType.PICKAXE, ArsenalMaterial.byId("tamahagane"));
		helper.assertTrue(Upgrades.gearOf(pick) == Upgrade.Gear.TOOL, "a forged pick is a tool for upgrades");
		helper.assertTrue(Upgrades.whyNot(pick, Upgrade.byId("honed_bit")) == null, "Honed Bit fits a pick");
		helper.assertTrue(Upgrades.whyNot(pick, Upgrade.byId("reinforced")) == null, "Reinforced fits a pick");
		helper.assertTrue(Upgrades.whyNot(pick, Upgrade.byId("honed_edge")) != null, "Honed Edge is for weapons");
		helper.assertTrue(Upgrades.whyNot(KatanaStats.build(IRON, IRON, IRON), Upgrade.byId("honed_bit")) != null, "Honed Bit is for tools");
		helper.assertTrue(Upgrades.add(pick, Upgrade.byId("honed_bit")), "Honed Bit goes on");

		List<ItemStack> parts = Salvage.parts(pick);
		helper.assertTrue(parts.size() == 3 && parts.get(0).getItem() == ArsenalItems.PICK_HEAD, "a pick comes apart into its head, binding and haft");
		helper.assertTrue(Upgrades.level(Salvage.history(parts.get(0)), Upgrade.byId("honed_bit")) == 1, "its head remembers the Honed Bit");
		helper.succeed();
	}

	@GameTest
	public void bucketsHoldMoreTheBetterTheirShell(GameTestHelper helper) {
		helper.assertTrue(ToolStats.capacity(ArsenalMaterial.DRIFTWOOD) == 1, "a tier 1 bucket holds 1");
		helper.assertTrue(ToolStats.capacity(ArsenalMaterial.byId("tamahagane")) == 3, "a tier 3 bucket holds 3");
		helper.assertTrue(ToolStats.capacity(ArsenalMaterial.VEGAPUNK_COMPOUND) == 4, "a tier 5 bucket holds 4");
		helper.assertFalse(ToolStats.holdsLava(ArsenalMaterial.DRIFTWOOD), "a Driftwood bucket can't carry lava");
		ItemStack bucket = tool(ToolType.BUCKET, ArsenalMaterial.byId("tamahagane"));
		ArsenalBucketItem.setContents(bucket, "water", 2);
		bucket.remove(com.onepiecearsenal.ArsenalComponents.STATS_STAMP);
		Restat.refresh(bucket);
		helper.assertTrue("water".equals(ArsenalBucketItem.fluid(bucket)) && ArsenalBucketItem.amount(bucket) == 2,
				"bringing a bucket's stats up to date keeps what's in it");
		helper.succeed();
	}

	@GameTest
	public void theSledgehammerSmashesThreeByThree(GameTestHelper helper) {
		ServerPlayer smith = helper.makeMockServerPlayerInLevel();
		smith.setGameMode(GameType.SURVIVAL);
		smith.setYRot(0.0f); // Facing south, so the square is across x and y.
		smith.setXRot(0.0f);
		smith.setItemSlot(EquipmentSlot.MAINHAND, tool(ToolType.SLEDGEHAMMER, IRON));
		BlockPos centre = new BlockPos(2, 3, 2);
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				helper.setBlock(centre.offset(x, y, 0), Blocks.STONE);
			}
		}
		helper.setBlock(centre.offset(0, 0, 1), Blocks.STONE); // Behind: should stay.
		smith.gameMode.destroyBlock(helper.absolutePos(centre));
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				helper.assertBlockPresent(Blocks.AIR, centre.offset(x, y, 0));
			}
		}
		helper.assertBlockPresent(Blocks.STONE, centre.offset(0, 0, 1));
		helper.assertTrue(smith.getMainHandItem().getDamageValue() == 9, "each smashed block wears it a point, got " + smith.getMainHandItem().getDamageValue());
		helper.succeed();
	}
}
