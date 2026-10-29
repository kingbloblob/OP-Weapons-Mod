package com.onepiecearsenal.station;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Navy's assembly station: the Shipwright's Anvil's forge, but it also puts together Navy gear, meaning
 * katanas, flintlocks and armor whose blade, barrel or shell is a Navy material. That's how pirates make
 * Marine uniforms for the disguise. Captured from Marine bases (see NavyStationBlock).
 */
public class MarineArmoryBenchBlock extends NavyStationBlock {
	public MarineArmoryBenchBlock(Properties properties) {
		super("Marine Armory Bench", properties);
	}

	@Override
	protected InteractionResult work(BlockState state, Level level, BlockPos pos, ServerPlayer player) {
		return ShipwrightsAnvilBlock.forge(level, pos, player, true);
	}
}
