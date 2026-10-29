package com.onepiecearsenal.tool;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * A hat, coat, sash or pair of boots built from a shell and a lining at the Shipwright's Anvil.
 * The item is registered as plain armor (so it equips and renders like any other); ArmorStats puts the
 * real defense and durability on each stack when it is forged.
 */
public class ArsenalArmorItem extends Item {
	private final String pieceName;

	public ArsenalArmorItem(String pieceName, Properties properties) {
		super(properties);
		this.pieceName = pieceName;
	}

	@Override
	public Component getName(ItemStack stack) {
		List<ArsenalMaterial> parts = ArmorStats.parts(stack);
		return Component.literal(parts.isEmpty() ? "Unmade " + pieceName : parts.get(0).displayName + " " + pieceName);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		List<ArsenalMaterial> parts = ArmorStats.parts(stack);
		if (parts.isEmpty()) {
			textConsumer.accept(Component.literal("Build one at a Shipwright's Anvil.").withStyle(ChatFormatting.GRAY));
			return;
		}
		textConsumer.accept(Component.literal("Shell: " + parts.get(0).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal("Lining: " + parts.get(1).displayName).withStyle(ChatFormatting.GRAY));
		for (Trait trait : ArmorStats.traits(stack)) {
			textConsumer.accept(Component.literal(trait.displayName + ": " + trait.description + (trait.active ? "" : " (coming soon)"))
					.withStyle(ChatFormatting.GOLD));
		}
		Upgrades.describe(stack, textConsumer);
	}
}
