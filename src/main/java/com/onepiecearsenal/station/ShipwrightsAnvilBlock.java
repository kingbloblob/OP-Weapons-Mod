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
import net.minecraft.world.InteractionHand;
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

import com.onepiecearsenal.cannon.CannonStats;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Faction;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.salvage.Salvage;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.tool.WeaponType;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * The pirate assembly station. Right-click it with an empty hand while you carry a full set of parts
 * anywhere in your inventory, and it joins them: a katana blade, tsuba and hilt make a katana; a barrel, firing
 * part and stock make a gun (see GunType); a cannon bore, fuse and gun carriage make a Ship Cannon; a head, binding and handle make a cutlass, axe, kanabo or spear
 * (see WeaponType); an armor shell (hat crown, coat plates, sash weave or boot soles) and a lining
 * make that piece of armor. Like the One Piece Mod's stations, the parts don't need to be in a grid. If you carry
 * several sets, they are forged in that order, one per click.
 *
 * Navy gear (a blade, barrel or armor shell of a Navy material, or a Marine Sabre or Jitte) can't be put together
 * here: that takes a Marine Armory Bench captured from a Marine base, which runs the same forge with navy = true.
 *
 * Right-click it holding forged gear (any gear, Navy too) to mend or upgrade it instead: see Upgrades. Sneak and
 * right-click to take it apart for its parts: see Salvage. A head part salvaged that way is forged back into its old gear.
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

	/**
	 * Holding forged gear: the anvil upgrades it with what's in your offhand, or mends it (see Upgrades). With nothing
	 * to do to it, it forges as usual if you carry a full set of parts.
	 */
	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND || Upgrades.gearOf(stack) == null) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (!(player instanceof ServerPlayer smith)) {
			return InteractionResult.SUCCESS;
		}
		InteractionResult result = Upgrades.work(level, pos, smith, stack);
		if (result != null) {
			return result;
		}
		if (findSet(smith.getInventory(), false) != null) {
			return forge(level, pos, smith, false);
		}
		smith.sendOverlayMessage(Upgrades.help(stack));
		return InteractionResult.FAIL;
	}

	/** Forges the first full set of parts the smith carries. Navy-material head parts and Navy weapon types only count when navy is true. */
	public static InteractionResult forge(Level level, BlockPos pos, ServerPlayer smith, boolean navy) {
		Inventory inventory = smith.getInventory();
		Forged forged = findSet(inventory, navy);
		if (forged == null && !navy && findSet(inventory, true) != null) {
			smith.sendOverlayMessage(Component.literal("Navy gear can only be put together at a Marine Armory Bench. Capture one from a Marine base.")
					.withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		if (forged == null) {
			smith.sendOverlayMessage(Component.literal("You need a full set of parts, like a blade, tsuba and hilt, or an armor shell and a lining.")
					.withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		ItemStack weapon = forged.item();
		for (int slot : forged.slots()) {
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

	/** True if the inventory holds a full set of parts that forge() would put together (Navy sets only count when navy is true). */
	public static boolean hasSet(Inventory inventory, boolean navy) {
		return findSet(inventory, navy) != null;
	}

	/** What a full set of parts makes, and the slots it uses up. */
	private record Forged(ItemStack item, int[] slots) {
		/** The slots' first part is the head: a salvaged one brings back the gear it came from. */
		Forged(Inventory inventory, ItemStack built, int[] slots) {
			this(Salvage.restore(inventory.getItem(slots[0]), built), slots);
		}
	}

	/** The first full set the smith carries, in this order: katana, guns, ship cannon, other hand weapons, armor. Null if none. */
	private static Forged findSet(Inventory inventory, boolean navy) {
		int[] used = findParts(inventory, navy, PartType.KATANA_BLADE, PartType.TSUBA, PartType.HILT);
		if (used != null) {
			return new Forged(inventory, KatanaStats.build(material(inventory, used[0]), material(inventory, used[1]), material(inventory, used[2])), used);
		}
		for (GunType type : GunType.values()) {
			if ((used = findParts(inventory, navy, type.parts())) != null) {
				return new Forged(inventory, GunStats.build(type, material(inventory, used[0]), material(inventory, used[1]), material(inventory, used[2])), used);
			}
		}
		if ((used = findParts(inventory, navy, PartType.CANNON_BORE, PartType.FUSE, PartType.GUN_CARRIAGE)) != null) {
			return new Forged(inventory, CannonStats.build(material(inventory, used[0]), material(inventory, used[1]), material(inventory, used[2])), used);
		}
		for (WeaponType type : WeaponType.values()) {
			// Marine Sabres and Jittes are Navy designs: only the Marine Armory Bench puts them together.
			if ((navy || type.faction != Faction.NAVY) && (used = findParts(inventory, navy, type.parts())) != null) {
				return new Forged(inventory, WeaponStats.build(type, material(inventory, used[0]), material(inventory, used[1]), material(inventory, used[2])), used);
			}
		}
		if ((used = findArmor(inventory, navy)) != null) {
			PartType shell = ((PartItem) inventory.getItem(used[0]).getItem()).type();
			return new Forged(inventory, ArmorStats.build(shell, material(inventory, used[0]), material(inventory, used[1])), used);
		}
		return null;
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

	private static ArsenalMaterial material(Inventory inventory, int slot) {
		return PartItem.material(inventory.getItem(slot));
	}

	/**
	 * The first inventory slot holding this kind of part with a known material that can make it, or -1.
	 * Skips Navy head parts unless navy, and hides and leathers for anything but armor.
	 */
	private static int findPart(Inventory inventory, PartType type, boolean navy) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			ArsenalMaterial material = PartItem.material(stack);
			if (stack.getItem() instanceof PartItem part && part.type() == type && material != null
					&& (navy || !type.head || material.faction != Faction.NAVY) && (!material.armorOnly || type.isArmor())) {
				return i;
			}
		}
		return -1;
	}
}
