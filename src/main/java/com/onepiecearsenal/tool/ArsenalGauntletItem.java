package com.onepiecearsenal.tool;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * A pair of gauntlets built from Gauntlet Plates and a Lining at the Shipwright's Anvil: armor worn in the offhand
 * (right-click puts them on there). GauntletStats puts the real numbers on each stack when it's forged.
 */
public class ArsenalGauntletItem extends Item {
	public ArsenalGauntletItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		Restat.refresh(stack);
		Breakage.notice(stack, owner);
		Upgrades.tick(stack, level.getGameTime()); // Ambergris Polish.
	}

	@Override
	public Component getName(ItemStack stack) {
		List<ArsenalMaterial> parts = GauntletStats.parts(stack);
		return parts.isEmpty() ? Component.literal("Unmade Gauntlets") : Breakage.name(stack, parts.get(0).displayName + " Gauntlets");
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag flag) {
		List<ArsenalMaterial> parts = GauntletStats.parts(stack);
		if (parts.isEmpty()) {
			textConsumer.accept(Component.literal("Build them at a Shipwright's Anvil.").withStyle(ChatFormatting.GRAY));
			return;
		}
		Breakage.describe(stack, textConsumer);
		textConsumer.accept(Component.literal("Plates: " + parts.get(0).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal("Lining: " + parts.get(1).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal(String.format("Guard: while sneaking, melee hits from the front lose %d%% of their damage.",
				Math.round(GauntletStats.GUARD_SHARE * 100))).withStyle(ChatFormatting.AQUA));
		for (Trait trait : GauntletStats.traits(stack)) {
			textConsumer.accept(Component.literal(trait.displayName + ": " + trait.description + (trait.active ? "" : " (coming soon)"))
					.withStyle(ChatFormatting.GOLD));
		}
		Upgrades.describe(stack, textConsumer);
	}
}
