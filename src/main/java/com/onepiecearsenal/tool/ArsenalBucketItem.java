package com.onepiecearsenal.tool;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.material.ArsenalMaterial;

/**
 * A bucket built from a Bucket Shell and a Grip. It holds as many bucketfuls of water or lava as its shell's tier
 * (up to 4), one kind at a time. Right-click a source to fill it, right-click elsewhere (or sneak) to pour one out.
 * Each fill wears it a point. Pouring is done by vanilla's own Water and Lava Buckets, so it waterlogs, evaporates in
 * the Nether and so on just like them.
 */
public class ArsenalBucketItem extends Item implements ArsenalTool {
	private final ToolType type;

	public ArsenalBucketItem(ToolType type, Properties properties) {
		super(properties);
		this.type = type;
	}

	@Override
	public ToolType toolType() {
		return type;
	}

	/** "water", "lava", or null while it's empty. */
	public static String fluid(ItemStack stack) {
		return amount(stack) > 0 ? stack.get(ArsenalComponents.BUCKET_FLUID) : null;
	}

	public static int amount(ItemStack stack) {
		return stack.getOrDefault(ArsenalComponents.BUCKET_AMOUNT, 0);
	}

	/** Sets what it holds. The item model shows the fluid from the custom_model_data string. */
	public static void setContents(ItemStack stack, String fluid, int amount) {
		if (fluid == null || amount <= 0) {
			stack.remove(ArsenalComponents.BUCKET_FLUID);
			stack.remove(ArsenalComponents.BUCKET_AMOUNT);
			stack.remove(DataComponents.CUSTOM_MODEL_DATA);
			return;
		}
		stack.set(ArsenalComponents.BUCKET_FLUID, fluid);
		stack.set(ArsenalComponents.BUCKET_AMOUNT, amount);
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(fluid), List.of()));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		List<ArsenalMaterial> parts = ToolStats.parts(stack);
		if (parts.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (ArsenalTool.refuseBroken(stack, player)) {
			return InteractionResult.FAIL;
		}
		ArsenalMaterial shell = parts.get(0);
		String fluid = fluid(stack);
		int amount = amount(stack);

		// Fill it from a source block, unless it's full or they're sneaking to pour.
		if (amount < ToolStats.capacity(shell) && !player.isShiftKeyDown()) {
			BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
			if (hit.getType() == HitResult.Type.BLOCK) {
				BlockPos pos = hit.getBlockPos();
				FluidState found = level.getFluidState(pos);
				String kind = !found.isSource() ? null : found.is(FluidTags.WATER) ? "water" : found.is(FluidTags.LAVA) ? "lava" : null;
				if (kind != null && (fluid == null || fluid.equals(kind))) {
					if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(hit.getDirection()), hit.getDirection(), stack)) {
						return InteractionResult.FAIL;
					}
					if (kind.equals("lava") && !ToolStats.holdsLava(shell)) {
						if (player instanceof ServerPlayer server) {
							server.sendOverlayMessage(Component.literal("A " + shell.displayName + " bucket would go up in flames. Make the shell of something that won't burn.")
									.withStyle(ChatFormatting.RED));
						}
						return InteractionResult.FAIL;
					}
					BlockState state = level.getBlockState(pos);
					if (state.getBlock() instanceof BucketPickup pickup) {
						if (level.isClientSide()) {
							return InteractionResult.SUCCESS;
						}
						ItemStack taken = pickup.pickupBlock(player, level, pos, state);
						if (!taken.isEmpty()) {
							pickup.getPickupSound().ifPresent(sound -> player.playSound(sound, 1.0f, 1.0f));
							level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
							setContents(stack, kind, amount + 1);
							stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
							return InteractionResult.SUCCESS;
						}
					}
				}
			}
		}

		// Pour one bucketful out where they're looking.
		if (fluid == null) {
			return InteractionResult.PASS;
		}
		BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hit.getBlockPos();
		Direction direction = hit.getDirection();
		BlockPos next = pos.relative(direction);
		if (!level.mayInteract(player, pos) || !player.mayUseItemAt(next, direction, stack)) {
			return InteractionResult.FAIL;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		boolean water = fluid.equals("water");
		BlockPos target = water && level.getBlockState(pos).getBlock() instanceof LiquidBlockContainer ? pos : next;
		BucketItem bucket = (BucketItem) (water ? Items.WATER_BUCKET : Items.LAVA_BUCKET);
		if (!bucket.emptyContents(player, level, target, hit)) {
			return InteractionResult.FAIL;
		}
		setContents(stack, fluid, amount - 1);
		return InteractionResult.SUCCESS;
	}

	/** The tooltip line for what it holds. */
	public static void describeContents(ItemStack stack, Consumer<Component> lines) {
		String fluid = fluid(stack);
		lines.accept(Component.literal(fluid == null ? "Empty" : "Holding " + amount(stack) + " bucketful" + (amount(stack) > 1 ? "s" : "") + " of " + fluid)
				.withStyle(ChatFormatting.GRAY));
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		ArsenalTool.tick(stack, level, owner, slot);
	}

	@Override
	public Component getName(ItemStack stack) {
		return ArsenalTool.name(stack, type);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag flag) {
		ArsenalTool.describe(stack, type, textConsumer);
	}
}
