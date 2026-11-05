package com.onepiecearsenal.station;

import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Faction;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;

/**
 * The Shipwright's Forge: where parts of the best materials (see HighTier) are made, since a crafting table can't get
 * them hot enough.
 *
 * Right-click it with fuel to stoke it (see heatOf: coal, sulfur, blaze rods, coal blocks, lava). Then right-click
 * holding any part as the pattern, with the material in your offhand (or anywhere in your inventory): you keep the
 * pattern and get a copy made of that material. It takes as much material as the part's crafting recipe would, and one
 * heat per piece of material. Dial cores also take their Dial Shells.
 *
 * Heat is kept in the block (HEAT, up to MAX_HEAT); the forge glows while it has any.
 */
public class ShipwrightsForgeBlock extends HorizontalDirectionalBlock {
	public static final int MAX_HEAT = 15;
	public static final IntegerProperty HEAT = IntegerProperty.create("heat", 0, MAX_HEAT);

	/** Heat per item. Wood and other weak fuels don't burn hot enough. */
	private static final Map<Item, Integer> FUEL = Map.of(
			Items.COAL, 2, Items.CHARCOAL, 2, Items.BLAZE_POWDER, 2, Items.BLAZE_ROD, 4, Items.MAGMA_BLOCK, 4,
			Items.COAL_BLOCK, 15, Items.LAVA_BUCKET, 15);
	/** The One Piece Mod's sulfur burns as hot as coal. AIR if it isn't there. */
	private static final Identifier SULFUR = Identifier.parse("onepiece:sulfur");
	private static final Identifier DIAL_SHELL = Identifier.parse("onepiece:dial_shell");

	public ShipwrightsForgeBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HEAT, 0));
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HEAT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Heat one of this item adds to the forge, or 0 if it isn't a forge fuel. */
	public static int heatOf(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		Item sulfur = BuiltInRegistries.ITEM.getValue(SULFUR);
		if (sulfur != Items.AIR && stack.is(sulfur)) {
			return 2;
		}
		return FUEL.getOrDefault(stack.getItem(), 0);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer smith) {
			smith.sendOverlayMessage(Component.literal(status(state)
					+ " Stoke it with coal, sulfur, blaze rods or lava, then hold a part as the pattern and the material in your offhand.")
					.withStyle(ChatFormatting.YELLOW));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND || (heatOf(stack) == 0 && !(stack.getItem() instanceof PartItem))) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (!(player instanceof ServerPlayer smith)) {
			return InteractionResult.SUCCESS;
		}
		if (stack.getItem() instanceof PartItem part) {
			return forge(level, pos, state, smith, part);
		}
		return stoke(level, pos, state, smith, stack);
	}

	/** Burns one fuel item from the stack, if the forge has room for its heat. */
	public static InteractionResult stoke(Level level, BlockPos pos, BlockState state, ServerPlayer smith, ItemStack fuel) {
		int heat = state.getValue(HEAT);
		if (heat >= MAX_HEAT) {
			smith.sendOverlayMessage(Component.literal("The forge is as hot as it gets.").withStyle(ChatFormatting.YELLOW));
			return InteractionResult.FAIL;
		}
		int added = heatOf(fuel);
		if (fuel.is(Items.LAVA_BUCKET)) {
			smith.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
		} else {
			fuel.shrink(1);
		}
		BlockState hotter = state.setValue(HEAT, Math.min(MAX_HEAT, heat + added));
		level.setBlock(pos, hotter, Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
		smith.sendOverlayMessage(Component.literal(status(hotter)).withStyle(ChatFormatting.GOLD));
		return InteractionResult.SUCCESS;
	}

	/** Makes a copy of the pattern part out of a forge material from the smith's inventory. */
	public static InteractionResult forge(Level level, BlockPos pos, BlockState state, ServerPlayer smith, PartItem part) {
		PartType type = part.type();
		Inventory inventory = smith.getInventory();
		int cost = NavyCastingBlock.cost(type);
		ArsenalMaterial material = choose(smith, type, cost);
		if (material == null) {
			return InteractionResult.FAIL; // choose() said why.
		}
		Item ingot = material.repairItem();
		String materialName = new ItemStack(ingot).getHoverName().getString();
		if (inventory.countItem(ingot) < cost) {
			fail(smith, "A " + material.displayName + " " + type.displayName + " takes " + cost + " " + materialName + ".");
			return InteractionResult.FAIL;
		}
		int shells = type == PartType.IMPACT_CORE ? 1 : type == PartType.REJECT_CORE ? 4 : 0;
		Item shell = BuiltInRegistries.ITEM.getValue(DIAL_SHELL);
		if (shells > 0 && inventory.countItem(shell) < shells) {
			fail(smith, "A " + type.displayName + " is built around " + shells + " Dial Shell" + (shells > 1 ? "s" : "") + ".");
			return InteractionResult.FAIL;
		}
		int heat = state.getValue(HEAT);
		if (heat < cost) {
			fail(smith, "Not hot enough: a " + type.displayName + " takes " + cost + " heat, and the forge has " + heat
					+ ". Stoke it with coal, sulfur, blaze rods or lava.");
			return InteractionResult.FAIL;
		}
		take(inventory, ingot, cost);
		take(inventory, shell, shells);
		level.setBlock(pos, state.setValue(HEAT, heat - cost), Block.UPDATE_ALL);
		ItemStack copy = PartItem.of(part, material);
		copy.setCount(NavyCastingBlock.yield(type));
		int made = copy.getCount();
		String name = copy.getHoverName().getString(); // inventory.add empties the stack, so read the name first.
		if (!inventory.add(copy)) {
			smith.drop(copy, false, Prediction.SERVER_ONLY);
		}
		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 0.8f);
		level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.0f);
		smith.sendOverlayMessage(Component.literal("Forged " + (made > 1 ? made + " " + name + "s" : "a " + name) + "!")
				.withStyle(ChatFormatting.GREEN));
		return InteractionResult.SUCCESS;
	}

	/**
	 * The material to make the part of: the one in the offhand, or else the first forge material the smith carries
	 * enough of. Null (after telling the smith why) if there's none to use.
	 */
	private static ArsenalMaterial choose(ServerPlayer smith, PartType type, int cost) {
		List<ArsenalMaterial> forgeable = HighTier.forgeMaterials();
		ItemStack offhand = smith.getOffhandItem();
		if (!offhand.isEmpty()) {
			ArsenalMaterial refused = null;
			for (ArsenalMaterial material : ArsenalMaterial.values()) {
				if (!offhand.is(material.repairItem())) {
					continue;
				}
				if (forgeable.contains(material) && (!material.armorOnly || type.isArmor())) {
					return material;
				}
				refused = refused == null ? material : refused;
			}
			if (refused != null) {
				fail(smith, whyNot(refused, type));
				return null;
			}
		}
		for (ArsenalMaterial material : forgeable) {
			if ((!material.armorOnly || type.isArmor()) && smith.getInventory().countItem(material.repairItem()) >= cost) {
				return material;
			}
		}
		fail(smith, "Hold the material in your offhand: " + String.join(", ", forgeable.stream().map(m -> m.displayName).toList()) + ".");
		return null;
	}

	private static String whyNot(ArsenalMaterial material, PartType type) {
		if (material.faction == Faction.NAVY) {
			return material.displayName + " is Navy stuff: it's cast at their own stations.";
		}
		if (!material.craftable) {
			return "Nobody can forge " + material.displayName + ". It's a blade that awakens, not a material.";
		}
		if (material.armorOnly) {
			return material.displayName + " is for armor and linings, not a " + type.displayName + ".";
		}
		return material.displayName + " doesn't need the forge: make its parts at a crafting table.";
	}

	private static String status(BlockState state) {
		int heat = state.getValue(HEAT);
		return heat == 0 ? "The forge is cold." : "Forge heat: " + heat + "/" + MAX_HEAT + ".";
	}

	private static void fail(ServerPlayer smith, String message) {
		smith.sendOverlayMessage(Component.literal(message).withStyle(ChatFormatting.RED));
	}

	private static void take(Inventory inventory, Item item, int count) {
		for (int i = 0; i < inventory.getContainerSize() && count > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				int taken = Math.min(count, stack.getCount());
				stack.shrink(taken);
				count -= taken;
			}
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(HEAT) == 0) {
			return;
		}
		// Flames in the mouth on the front, and smoke from the top.
		Direction front = state.getValue(FACING);
		double x = pos.getX() + 0.5 + front.getStepX() * 0.52 + (random.nextDouble() - 0.5) * 0.5 * Math.abs(front.getStepZ());
		double z = pos.getZ() + 0.5 + front.getStepZ() * 0.52 + (random.nextDouble() - 0.5) * 0.5 * Math.abs(front.getStepX());
		double y = pos.getY() + 0.2 + random.nextDouble() * 0.3;
		level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0.0, 0.05, 0.0);
		}
		if (random.nextInt(8) == 0) {
			level.playLocalSound(pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0f, 1.0f, false);
		}
	}
}
