package com.onepiecearsenal.station;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.KatanaStats;

/**
 * The pirate assembly station. Right-click it with an empty hand while you carry a katana blade, a tsuba
 * and a hilt anywhere in your inventory, and it joins them into a katana. Like the One Piece Mod's
 * stations, the parts don't need to be in a grid.
 */
public class ShipwrightsAnvilBlock extends HorizontalDirectionalBlock {
	public ShipwrightsAnvilBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer smith)) {
			return InteractionResult.SUCCESS;
		}
		Inventory inventory = smith.getInventory();
		int blade = findPart(inventory, PartType.KATANA_BLADE);
		int tsuba = findPart(inventory, PartType.TSUBA);
		int hilt = findPart(inventory, PartType.HILT);
		if (blade < 0 || tsuba < 0 || hilt < 0) {
			smith.sendOverlayMessage(Component.literal("You need a katana blade, a tsuba and a hilt.").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		ItemStack katana = KatanaStats.build(PartItem.material(inventory.getItem(blade)),
				PartItem.material(inventory.getItem(tsuba)), PartItem.material(inventory.getItem(hilt)));
		inventory.removeItem(blade, 1);
		inventory.removeItem(tsuba, 1);
		inventory.removeItem(hilt, 1);
		if (!inventory.add(katana)) {
			smith.drop(katana, false, Prediction.SERVER_ONLY); // Inventory full: drop it at their feet.
		}
		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
		smith.sendOverlayMessage(Component.literal("Forged a " + katana.getHoverName().getString() + "!").withStyle(ChatFormatting.GREEN));
		return InteractionResult.SUCCESS;
	}

	/** The first inventory slot holding this kind of part with a known material, or -1. */
	private static int findPart(Inventory inventory, PartType type) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.getItem() instanceof PartItem part && part.type() == type && PartItem.material(stack) != null) {
				return i;
			}
		}
		return -1;
	}
}
