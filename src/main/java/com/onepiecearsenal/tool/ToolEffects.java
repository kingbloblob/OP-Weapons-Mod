package com.onepiecearsenal.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import com.onepiecearsenal.salvage.Breakage;

/** What tools do beyond vanilla's rules: the Sledgehammer's 3x3. */
public final class ToolEffects {
	/** How much harder than the block you hit a neighbour can be and still be smashed with it (stone next to obsidian isn't). */
	private static final float HARDNESS_LEEWAY = 2.0f;
	/** True while the Sledgehammer is breaking the blocks around the first, so those don't set it off again. */
	private static boolean smashing = false;

	private ToolEffects() {
	}

	public static void initialize() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (smashing || !(player instanceof ServerPlayer smith) || player.isShiftKeyDown() || smith.gameMode.isCreative()) {
				return;
			}
			ItemStack tool = player.getMainHandItem();
			if (!(tool.getItem() instanceof ArsenalTool item) || item.toolType() != ToolType.SLEDGEHAMMER
					|| ToolStats.parts(tool).isEmpty() || Breakage.isBroken(tool) || !tool.isCorrectToolForDrops(state)) {
				return;
			}
			smash(smith, tool, pos, state);
		});
	}

	/** Breaks the 8 blocks around pos in the plane facing the smith, as if they'd dug each one with the hammer. */
	private static void smash(ServerPlayer smith, ItemStack tool, BlockPos pos, BlockState hit) {
		Direction.Axis axis = Direction.orderedByNearest(smith)[0].getAxis();
		float limit = hit.getDestroySpeed(smith.level(), pos) + HARDNESS_LEEWAY;
		smashing = true;
		try {
			for (int a = -1; a <= 1; a++) {
				for (int b = -1; b <= 1; b++) {
					if (a == 0 && b == 0) {
						continue;
					}
					BlockPos other = switch (axis) {
						case X -> pos.offset(0, a, b);
						case Y -> pos.offset(a, 0, b);
						case Z -> pos.offset(a, b, 0);
					};
					BlockState state = smith.level().getBlockState(other);
					float hardness = state.getDestroySpeed(smith.level(), other);
					if (state.isAir() || hardness < 0 || hardness > limit || !tool.isCorrectToolForDrops(state)) {
						continue;
					}
					smith.gameMode.destroyBlock(other);
					if (Breakage.isBroken(tool) || smith.getMainHandItem() != tool) {
						return;
					}
				}
			}
		} finally {
			smashing = false;
		}
	}
}
