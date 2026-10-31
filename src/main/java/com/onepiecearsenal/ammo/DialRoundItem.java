package com.onepiecearsenal.ammo;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A Dial Round (see DialRound): a bullet for any pistol or rifle. */
public class DialRoundItem extends Item {
	private final DialRound round;

	public DialRoundItem(DialRound round, Properties properties) {
		super(properties);
		this.round = round;
	}

	public DialRound round() {
		return round;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag flag) {
		textConsumer.accept(Component.literal(round.description).withStyle(ChatFormatting.GOLD));
		textConsumer.accept(Component.literal("A bullet for any pistol or rifle. Hold it in your off hand to fire it first.").withStyle(ChatFormatting.DARK_GRAY));
	}
}
