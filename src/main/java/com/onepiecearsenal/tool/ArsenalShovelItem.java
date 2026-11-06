package com.onepiecearsenal.tool;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/** A shovel built from parts: makes paths and puts out campfires like a vanilla shovel. Its numbers come from its parts (see ToolStats). */
public class ArsenalShovelItem extends ShovelItem implements ArsenalTool {
	private final ToolType type;

	public ArsenalShovelItem(ToolType type, Properties properties) {
		super(ToolMaterial.IRON, 0.0f, -3.0f, properties);
		this.type = type;
	}

	@Override
	public ToolType toolType() {
		return type;
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
