package com.onepiecearsenal.tool;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * A shield built from a Shield Face, a rim (Binding) and a strap (Grip) at the Shipwright's Anvil. It blocks with
 * vanilla's blocks_attacks component, which ShieldStats sets from the parts when it's forged; an unforged one doesn't
 * block. Carry it in your offhand and hold right-click to raise it.
 */
public class ArsenalShieldItem extends Item {
	public ArsenalShieldItem(Properties properties) {
		super(properties);
	}

	/** A broken shield won't come up. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (Breakage.isBroken(stack)) {
			if (player instanceof ServerPlayer server) {
				Breakage.tellBroken(server, stack);
			}
			return InteractionResult.FAIL;
		}
		return super.use(level, player, hand);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		Restat.refresh(stack);
		Breakage.notice(stack, owner);
		Upgrades.tick(stack, level.getGameTime()); // Ambergris Polish.
		// Broke while raised: it drops.
		if (owner instanceof LivingEntity living && living.isUsingItem() && living.getUseItem() == stack && Breakage.isBroken(stack)) {
			living.stopUsingItem();
		}
	}

	@Override
	public Component getName(ItemStack stack) {
		List<ArsenalMaterial> parts = ShieldStats.parts(stack);
		return parts.isEmpty() ? Component.literal("Unforged Shield") : Breakage.name(stack, parts.get(0).displayName + " Shield");
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag flag) {
		List<ArsenalMaterial> parts = ShieldStats.parts(stack);
		if (parts.isEmpty()) {
			textConsumer.accept(Component.literal("Build one at a Shipwright's Anvil.").withStyle(ChatFormatting.GRAY));
			return;
		}
		Breakage.describe(stack, textConsumer);
		textConsumer.accept(Component.literal("Face: " + parts.get(0).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal("Rim: " + parts.get(1).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal("Strap: " + parts.get(2).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal(String.format("Covers %.0f°, up in %.2fs. Axes knock it down for %.1fs.",
				ShieldStats.angle(parts.get(0)), ShieldStats.blockDelay(parts.get(1), parts.get(2)),
				5.0f * ShieldStats.disableScale(parts.get(0), ShieldStats.traits(stack)))).withStyle(ChatFormatting.BLUE));
		textConsumer.accept(Component.literal("Counter: a melee hit it stops sets off its face's trait on the attacker.")
				.withStyle(ChatFormatting.AQUA));
		for (Trait trait : ShieldStats.traits(stack)) {
			textConsumer.accept(Component.literal(trait.displayName + ": " + trait.description + (trait.active ? "" : " (coming soon)"))
					.withStyle(ChatFormatting.GOLD));
		}
		Upgrades.describe(stack, textConsumer);
	}
}
