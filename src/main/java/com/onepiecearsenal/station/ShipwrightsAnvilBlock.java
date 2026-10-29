package com.onepiecearsenal.station;

import java.util.List;

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
import com.onepiecearsenal.material.Faction;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.KatanaStats;

/**
 * The pirate assembly station. Right-click it with an empty hand while you carry a full set of parts
 * anywhere in your inventory, and it joins them: a katana blade, tsuba and hilt make a katana; a barrel, firing
 * lock and stock make a flintlock; an armor shell (hat crown, coat plates, sash weave or boot soles) and a lining
 * make that piece of armor. Like the One Piece Mod's stations, the parts don't need to be in a grid. If you carry
 * several sets, they are forged in that order, one per click.
 *
 * Navy gear (a blade, barrel or armor shell of a Navy material) can't be put together here: that takes a
 * Marine Armory Bench captured from a Marine base, which runs the same forge with navy = true.
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
		return forge(level, pos, smith, false);
	}

	/** Forges the first full set of parts the smith carries. Navy-material head parts only count when navy is true. */
	public static InteractionResult forge(Level level, BlockPos pos, ServerPlayer smith, boolean navy) {
		Inventory inventory = smith.getInventory();
		int[] katana = findParts(inventory, navy, PartType.KATANA_BLADE, PartType.TSUBA, PartType.HILT);
		int[] gun = findParts(inventory, navy, PartType.BARREL, PartType.LOCK, PartType.STOCK);
		ItemStack weapon;
		int[] used;
		if (katana != null) {
			weapon = KatanaStats.build(material(inventory, katana[0]), material(inventory, katana[1]), material(inventory, katana[2]));
			used = katana;
		} else if (gun != null) {
			weapon = GunStats.build(material(inventory, gun[0]), material(inventory, gun[1]), material(inventory, gun[2]));
			used = gun;
		} else {
			int[] armor = findArmor(inventory, navy);
			if (armor == null && !navy && hasNavySet(inventory)) {
				smith.sendOverlayMessage(Component.literal("Navy gear can only be put together at a Marine Armory Bench. Capture one from a Marine base.")
						.withStyle(ChatFormatting.RED));
				return InteractionResult.FAIL;
			}
			if (armor == null) {
				smith.sendOverlayMessage(Component.literal("You need a full set of parts: blade, tsuba and hilt; barrel, firing lock and stock; or an armor shell and a lining.")
						.withStyle(ChatFormatting.RED));
				return InteractionResult.FAIL;
			}
			PartType shell = ((PartItem) inventory.getItem(armor[0]).getItem()).type();
			weapon = ArmorStats.build(shell, material(inventory, armor[0]), material(inventory, armor[1]));
			used = armor;
		}

		for (int slot : used) {
			inventory.removeItem(slot, 1);
		}
		String name = weapon.getHoverName().getString(); // inventory.add empties the stack, so read the name first.
		if (!inventory.add(weapon)) {
			smith.drop(weapon, false, Prediction.SERVER_ONLY); // Inventory full: drop it at their feet.
		}
		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
		smith.sendOverlayMessage(Component.literal("Forged a " + name + "!").withStyle(ChatFormatting.GREEN));
		return InteractionResult.SUCCESS;
	}

	/** The slots holding one of each of these parts, in the same order, or null if one is missing. */
	private static int[] findParts(Inventory inventory, boolean navy, PartType... types) {
		int[] slots = new int[types.length];
		for (int i = 0; i < types.length; i++) {
			slots[i] = findPart(inventory, types[i], navy);
			if (slots[i] < 0) {
				return null;
			}
		}
		return slots;
	}

	/** The slots of the first armor shell found (hat, coat, sash, boots order) and a lining, or null. */
	private static int[] findArmor(Inventory inventory, boolean navy) {
		for (PartType shell : List.of(PartType.CROWN, PartType.COAT_PLATES, PartType.SASH_WEAVE, PartType.BOOT_SOLES)) {
			int[] slots = findParts(inventory, navy, shell, PartType.LINING);
			if (slots != null) {
				return slots;
			}
		}
		return null;
	}

	/** True if the smith carries a full set that only a Marine Armory Bench can forge. */
	private static boolean hasNavySet(Inventory inventory) {
		return findParts(inventory, true, PartType.KATANA_BLADE, PartType.TSUBA, PartType.HILT) != null
				|| findParts(inventory, true, PartType.BARREL, PartType.LOCK, PartType.STOCK) != null
				|| findArmor(inventory, true) != null;
	}

	private static ArsenalMaterial material(Inventory inventory, int slot) {
		return PartItem.material(inventory.getItem(slot));
	}

	/** The first inventory slot holding this kind of part with a known material, or -1. Skips Navy head parts unless navy. */
	private static int findPart(Inventory inventory, PartType type, boolean navy) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			ArsenalMaterial material = PartItem.material(stack);
			if (stack.getItem() instanceof PartItem part && part.type() == type && material != null
					&& (navy || !type.head || material.faction != Faction.NAVY)) {
				return i;
			}
		}
		return -1;
	}
}
