package com.onepiecearsenal.tool;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecemod.weapon.Ammo;
import com.onepiecemod.weapon.GunItem;
import com.onepiecemod.weapon.WeaponUpgrades;

/**
 * A flintlock built from a barrel, a firing lock and a stock at the Shipwright's Anvil. It is a One Piece Mod
 * GunItem, so it fires that mod's Bullets and Seastone Bullets, and takes its Scopes, Long Barrels,
 * Hair Triggers, Dials and gun enchantments.
 *
 * The item is made with "unit" stats (see UNIT). The One Piece Mod's GunItem applies its upgrades and
 * enchantments to those, and effectiveStats() turns the result into multipliers on the parts' stats,
 * so every upgrade works the same way it does on the main mod's guns.
 */
public class FlintlockItem extends GunItem {
	/** 0 damage, 100-tick reload, 1 block range, 1 spread: easy to read the upgrades back out of. */
	public static final Stats UNIT = new Stats(0.0f, 100, 1.0, 1.0f, 1, Ammo.Kind.BULLET, 0, 0, 3.0f);

	/** The flintlock that is firing right now, so its traits go on the shot even from the offhand. */
	private static ItemStack firing = ItemStack.EMPTY;

	public FlintlockItem(Properties properties) {
		super(UNIT, Hold.PISTOL, properties);
	}

	/**
	 * The One Piece Mod's shots hit the moment the trigger is pulled, inside use(), so remembering the gun
	 * here tells TraitEffects which flintlock fired, whichever hand it's in.
	 */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return super.use(level, player, hand);
		}
		firing = player.getItemInHand(hand);
		try {
			return super.use(level, player, hand);
		} finally {
			firing = ItemStack.EMPTY;
		}
	}

	/** The flintlock whose shot is landing right now, or an empty stack outside a shot. */
	public static ItemStack firing() {
		return firing;
	}

	@Override
	public Stats effectiveStats(ItemStack gun) {
		Stats parts = GunStats.stats(gun);
		if (parts == null) {
			parts = new Stats(1.0f, 40, 16.0, 0.1f, 1, Ammo.Kind.BULLET, 0, 0, 3.0f); // An unforged gun barely works.
		}
		Stats upgrades = super.effectiveStats(gun);
		return new Stats(parts.damage() + upgrades.damage(),
				Math.max(4, Math.round(parts.cooldown() * upgrades.cooldown() / (float) UNIT.cooldown())),
				parts.range() * upgrades.range(),
				parts.spread() * upgrades.spread(),
				1, Ammo.Kind.BULLET, parts.magazine(), parts.reload(), parts.recoil());
	}

	@Override
	public Component getName(ItemStack stack) {
		List<ArsenalMaterial> parts = GunStats.parts(stack);
		return Component.literal(parts.isEmpty() ? "Unforged Flintlock" : parts.get(0).displayName + " Flintlock");
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		List<ArsenalMaterial> parts = GunStats.parts(stack);
		if (parts.isEmpty()) {
			textConsumer.accept(Component.literal("Build one at a Shipwright's Anvil.").withStyle(ChatFormatting.GRAY));
			return;
		}
		Stats stats = GunStats.stats(stack);
		textConsumer.accept(Component.literal(String.format("%.0f damage, %.2fs reload, %.0f block range",
				stats.damage(), stats.cooldown() / 20.0, stats.range())).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal("Barrel: " + parts.get(0).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal("Firing Lock: " + parts.get(1).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal("Stock: " + parts.get(2).displayName).withStyle(ChatFormatting.GRAY));
		for (Trait trait : GunStats.traits(stack)) {
			textConsumer.accept(Component.literal(trait.displayName + ": " + trait.description + (trait.active ? "" : " (coming soon)"))
					.withStyle(ChatFormatting.GOLD));
		}
		textConsumer.accept(Component.literal("Needs Bullets or Seastone Bullets.").withStyle(ChatFormatting.DARK_GRAY));
		WeaponUpgrades.describe(stack).forEach(textConsumer);
	}
}
