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
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.tool.WeaponType;

/**
 * The pirate assembly station. Right-click it with an empty hand while you carry a full set of parts
 * anywhere in your inventory, and it joins them: a katana blade, tsuba and hilt make a katana; a barrel, firing
 * part and stock make a gun (see GunType); a head, binding and handle make a cutlass, sabre, axe, jitte, kanabo
 * or spear (see WeaponType); an armor shell (hat crown, coat plates, sash weave or boot soles) and a lining
 * make that piece of armor. Like the One Piece Mod's stations, the parts don't need to be in a grid. If you carry
 * several sets, they are forged in that order, one per click.
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
		ItemStack weapon = null;
		int[] used = findParts(inventory, PartType.KATANA_BLADE, PartType.TSUBA, PartType.HILT);
		if (used != null) {
			weapon = KatanaStats.build(material(inventory, used[0]), material(inventory, used[1]), material(inventory, used[2]), level.registryAccess());
		}
		for (GunType type : GunType.values()) {
			if (weapon == null && (used = findParts(inventory, type.parts())) != null) {
				weapon = GunStats.build(type, material(inventory, used[0]), material(inventory, used[1]), material(inventory, used[2]), level.registryAccess());
			}
		}
		for (WeaponType type : WeaponType.values()) {
			if (weapon == null && (used = findParts(inventory, type.parts())) != null) {
				weapon = WeaponStats.build(type, material(inventory, used[0]), material(inventory, used[1]), material(inventory, used[2]), level.registryAccess());
			}
		}
		if (weapon == null && (used = findArmor(inventory)) != null) {
			PartType shell = ((PartItem) inventory.getItem(used[0]).getItem()).type();
			weapon = ArmorStats.build(shell, material(inventory, used[0]), material(inventory, used[1]), level.registryAccess());
		}
		if (weapon == null) {
			smith.sendOverlayMessage(Component.literal("You need a full set of parts, like a blade, tsuba and hilt, or an armor shell and a lining.")
					.withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
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

	/** The slots of the first armor shell found (hat, coat, sash, boots order) and a lining, or null. */
	private static int[] findArmor(Inventory inventory) {
		for (PartType shell : List.of(PartType.CROWN, PartType.COAT_PLATES, PartType.SASH_WEAVE, PartType.BOOT_SOLES)) {
			int[] slots = findParts(inventory, shell, PartType.LINING);
			if (slots != null) {
				return slots;
			}
		}
		return null;
	}

	private static ArsenalMaterial material(Inventory inventory, int slot) {
		return PartItem.material(inventory.getItem(slot));
	}

	/** The first inventory slot holding this kind of part with a known material that can make it, or -1. */
	private static int findPart(Inventory inventory, PartType type) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			ArsenalMaterial material = PartItem.material(stack);
			if (stack.getItem() instanceof PartItem part && part.type() == type && material != null && (!material.armorOnly || type.isArmor())) {
				return i;
			}
		}
		return -1;
	}
}
