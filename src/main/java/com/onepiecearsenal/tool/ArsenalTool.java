package com.onepiecearsenal.tool;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * An Arsenal tool (see ToolType). The tools extend vanilla's own classes where vanilla's tool does something on
 * right-click (an axe strips logs, a shovel makes paths, a hoe tills), so this interface holds what they share.
 */
public interface ArsenalTool {
	ToolType toolType();

	/** "Ship Iron Pickaxe", "Broken Ship Iron Pickaxe", or "Unforged Pickaxe" for one from the creative menu's blank. */
	static Component name(ItemStack stack, ToolType type) {
		List<ArsenalMaterial> parts = ToolStats.parts(stack);
		return parts.isEmpty() ? Component.literal("Unforged " + type.displayName) : Breakage.name(stack, parts.get(0).displayName + " " + type.displayName);
	}

	/** From the tool's inventoryTick: catches up with balance changes and says when it broke. */
	static void tick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		Restat.refresh(stack);
		Breakage.notice(stack, owner);
		Upgrades.tick(stack, level.getGameTime()); // Ambergris Polish.
	}

	/** True (after telling them) if a player tried to use a broken tool. */
	static boolean refuseBroken(ItemStack stack, Player player) {
		if (!Breakage.isBroken(stack)) {
			return false;
		}
		if (player instanceof ServerPlayer server) {
			Breakage.tellBroken(server, stack);
		}
		return true;
	}

	static void describe(ItemStack stack, ToolType type, Consumer<Component> lines) {
		List<ArsenalMaterial> parts = ToolStats.parts(stack);
		if (parts.isEmpty()) {
			lines.accept(Component.literal("Build one at a Shipwright's Anvil.").withStyle(ChatFormatting.GRAY));
			return;
		}
		Breakage.describe(stack, lines);
		for (int i = 0; i < parts.size(); i++) {
			lines.accept(Component.literal(type.parts()[i].displayName + ": " + parts.get(i).displayName).withStyle(ChatFormatting.GRAY));
		}
		ArsenalMaterial head = parts.get(0);
		if (type == ToolType.BUCKET) {
			lines.accept(Component.literal("Holds " + ToolStats.capacity(head) + " bucketful" + (ToolStats.capacity(head) > 1 ? "s" : "")
					+ (ToolStats.holdsLava(head) ? " of water or lava" : " of water (it would burn with lava)")).withStyle(ChatFormatting.BLUE));
			ArsenalBucketItem.describeContents(stack, lines);
		} else if (type != ToolType.SHEARS) {
			lines.accept(Component.literal(String.format("Digs %s, speed %.1f", ToolStats.levelName(head), ToolStats.miningSpeed(type, head)))
					.withStyle(ChatFormatting.BLUE));
		}
		lines.accept(Component.literal(type.ability).withStyle(ChatFormatting.AQUA));
		for (Trait trait : ToolStats.traits(stack)) {
			lines.accept(Component.literal(trait.displayName + ": " + trait.description + (trait.active ? "" : " (coming soon)"))
					.withStyle(ChatFormatting.GOLD));
		}
		Upgrades.describe(stack, lines);
	}
}
