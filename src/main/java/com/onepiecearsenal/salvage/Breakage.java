package com.onepiecearsenal.salvage;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * Forged gear doesn't vanish when it wears out, the Tinkers' Construct way: it stops one point short and is Broken.
 * Broken gear gives none of its attributes (a broken katana hits like a fist, broken armor stops nothing), its traits
 * and upgrade effects go quiet, and a broken gun won't fire. Mend it at a Shipwright's Anvil as usual, or take it
 * apart there for its parts (see Salvage).
 *
 * ItemStackMixin does the stopping short and hides the attributes; this class decides what counts.
 */
public final class Breakage {
	private Breakage() {
	}

	/** True for forged gear worn down to its last point. */
	public static boolean isBroken(ItemStack stack) {
		int max = stack.getMaxDamage();
		return max > 1 && stack.getDamageValue() >= max - 1 && Upgrades.gearOf(stack) != null;
	}

	/**
	 * Called by ItemStackMixin whenever an item's wear is set. Wear that would break forged gear stops one point short,
	 * so the item stays, and the item remembers it just broke so its owner hears it (see notice).
	 */
	public static int keepBroken(ItemStack stack, int damage) {
		int max = stack.getMaxDamage();
		if (max <= 1 || damage < max - 1 || Upgrades.gearOf(stack) == null) {
			return damage; // Cheap checks first: this runs for every item that takes wear.
		}
		if (stack.getDamageValue() < max - 1) {
			stack.set(ArsenalComponents.JUST_BROKE, true);
		}
		return max - 1;
	}

	/** From the gear's inventoryTick: if it broke since the last tick, tell whoever carries it. */
	public static void notice(ItemStack stack, Entity owner) {
		if (!stack.has(ArsenalComponents.JUST_BROKE)) {
			return;
		}
		stack.remove(ArsenalComponents.JUST_BROKE);
		if (isBroken(stack) && owner instanceof ServerPlayer player) {
			player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 0.9f);
			tellBroken(player, stack);
		}
	}

	/** The overlay message for broken gear someone tried to use. */
	public static void tellBroken(ServerPlayer player, ItemStack stack) {
		player.sendOverlayMessage(Component.literal(stack.getHoverName().getString()
				+ "! Mend it at a Shipwright's Anvil, or sneak and right-click the anvil to take it apart.").withStyle(ChatFormatting.RED));
	}

	/** The gear's name, with "Broken" in front while it is. */
	public static Component name(ItemStack stack, String name) {
		return Component.literal(isBroken(stack) ? "Broken " + name : name);
	}

	/** The tooltip line for broken gear. Nothing for gear that works. */
	public static void describe(ItemStack stack, Consumer<Component> lines) {
		if (isBroken(stack)) {
			lines.accept(Component.literal("Broken: no stats, traits or upgrades until it's mended at a Shipwright's Anvil.")
					.withStyle(ChatFormatting.RED));
			lines.accept(Component.literal("Sneak and right-click the anvil to take it apart for its parts.").withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
