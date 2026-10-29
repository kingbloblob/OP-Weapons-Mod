package com.onepiecearsenal.cannon;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import com.onepiecearsenal.material.ArsenalMaterial;

/**
 * A Ship Cannon to place, like "Cannon Bronze Ship Cannon". Its tooltip lists its parts and how it compares to
 * the One Piece Mod's plain Cannon.
 */
public class ShipCannonItem extends BlockItem {
	public ShipCannonItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		List<ArsenalMaterial> parts = CannonStats.parts(stack);
		return Component.literal(parts.isEmpty() ? "Blank Ship Cannon" : parts.get(0).displayName + " Ship Cannon");
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		List<ArsenalMaterial> parts = CannonStats.parts(stack);
		if (parts.isEmpty()) {
			textConsumer.accept(Component.literal("Forge one from a Cannon Bore, a Fuse and a Gun Carriage.").withStyle(ChatFormatting.GRAY));
			return;
		}
		ArsenalMaterial bore = parts.get(0), fuse = parts.get(1), carriage = parts.get(2);
		textConsumer.accept(Component.literal("Bore: " + bore.displayName + " · Fuse: " + fuse.displayName + " · Carriage: " + carriage.displayName)
				.withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal("Compared to a plain Cannon:").withStyle(ChatFormatting.GRAY));
		textConsumer.accept(percent("Damage", CannonStats.damage(bore), false));
		textConsumer.accept(percent("Range", CannonStats.range(bore), false));
		textConsumer.accept(percent("Reload time", CannonStats.reload(fuse), true));
		textConsumer.accept(percent("Spread", CannonStats.spread(carriage), true));
		textConsumer.accept(Component.literal("Fires like any cannon: M aboard a ship, or right-click it with ammo on land.")
				.withStyle(ChatFormatting.DARK_GRAY));
	}

	/** "Damage: +24%", green when better. lowerIsBetter for reload and spread. */
	private static Component percent(String name, float multiplier, boolean lowerIsBetter) {
		int change = Math.round((multiplier - 1.0f) * 100);
		boolean better = lowerIsBetter ? change < 0 : change > 0;
		ChatFormatting colour = change == 0 ? ChatFormatting.BLUE : better ? ChatFormatting.GREEN : ChatFormatting.RED;
		return Component.literal("  " + name + ": " + (change > 0 ? "+" : "") + change + "%").withStyle(colour);
	}
}
