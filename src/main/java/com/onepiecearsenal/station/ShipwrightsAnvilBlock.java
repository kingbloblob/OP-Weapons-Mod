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

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.KatanaStats;

/**
 * The pirate assembly station. Right-click it with an empty hand while you carry a full set of parts
 * anywhere in your inventory, and it joins them into a weapon: a katana blade, tsuba and hilt make a katana;
 * a barrel, firing lock and stock make a flintlock. Like the One Piece Mod's stations, the parts don't need
 * to be in a grid. If you carry both sets, the katana is forged first.
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
		int[] katana = findParts(inventory, PartType.KATANA_BLADE, PartType.TSUBA, PartType.HILT);
		int[] gun = findParts(inventory, PartType.BARREL, PartType.LOCK, PartType.STOCK);
		ItemStack weapon;
		int[] used;
		if (katana != null) {
			weapon = KatanaStats.build(material(inventory, katana[0]), material(inventory, katana[1]), material(inventory, katana[2]));
			used = katana;
		} else if (gun != null) {
			weapon = GunStats.build(material(inventory, gun[0]), material(inventory, gun[1]), material(inventory, gun[2]));
			used = gun;
		} else {
			smith.sendOverlayMessage(Component.literal("You need a katana blade, tsuba and hilt, or a barrel, firing lock and stock.")
					.withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		for (int slot : used) {
			inventory.removeItem(slot, 1);
		}
		if (!inventory.add(weapon)) {
			smith.drop(weapon, false, Prediction.SERVER_ONLY); // Inventory full: drop it at their feet.
		}
		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
		smith.sendOverlayMessage(Component.literal("Forged a " + weapon.getHoverName().getString() + "!").withStyle(ChatFormatting.GREEN));
		return InteractionResult.SUCCESS;
	}

	/** The slots holding one of each of these parts, in the same order, or null if one is missing. */
	private static int[] findParts(Inventory inventory, PartType... types) {
		int[] slots = new int[types.length];
		for (int i = 0; i < types.length; i++) {
			slots[i] = findPart(inventory, types[i]);
			if (slots[i] < 0) {
				return null;
			}
		}
		return slots;
	}

	private static ArsenalMaterial material(Inventory inventory, int slot) {
		return PartItem.material(inventory.getItem(slot));
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
