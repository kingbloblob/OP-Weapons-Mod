package com.onepiecearsenal.tool;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Shears built from parts. They cut blocks like vanilla shears (ToolStats copies vanilla's rules), and shear sheep,
 * Mooshrooms, Snow Golems and Bogged here, since those check for vanilla's own shears item.
 */
public class ArsenalShearsItem extends ShearsItem implements ArsenalTool {
	private final ToolType type;

	public ArsenalShearsItem(ToolType type, Properties properties) {
		super(properties);
		this.type = type;
	}

	@Override
	public ToolType toolType() {
		return type;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof Shearable shearable) || ToolStats.parts(stack).isEmpty()) {
			return InteractionResult.PASS;
		}
		if (ArsenalTool.refuseBroken(stack, player)) {
			return InteractionResult.FAIL;
		}
		if (!shearable.readyForShearing()) {
			return InteractionResult.PASS;
		}
		if (player.level() instanceof ServerLevel level) {
			shearable.shear(level, SoundSource.PLAYERS, stack);
			target.gameEvent(GameEvent.SHEAR, player);
			stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getPlayer() != null && ArsenalTool.refuseBroken(context.getItemInHand(), context.getPlayer())) {
			return InteractionResult.FAIL;
		}
		return super.useOn(context);
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
