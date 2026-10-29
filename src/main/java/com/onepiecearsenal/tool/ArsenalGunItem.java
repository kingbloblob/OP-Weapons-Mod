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
 * A flintlock, musket or hand cannon built from a barrel, a firing part and a stock at the Shipwright's Anvil
 * (see GunType). It is a One Piece Mod GunItem, so it fires that mod's Bullets, Seastone Bullets or Cannonballs,
 * and takes its Scopes, Long Barrels, Hair Triggers, Dials and gun enchantments.
 *
 * The item is made with "unit" stats (see unit()). The One Piece Mod's GunItem applies its upgrades and
 * enchantments to those, and effectiveStats() turns the result into multipliers on the parts' stats,
 * so every upgrade works the same way it does on the main mod's guns.
 */
public class ArsenalGunItem extends GunItem {
	/** 100-tick reload: easy to read the upgrades' cooldown change back out of. */
	private static final int UNIT_COOLDOWN = 100;

	/** The gun that is firing right now, so its traits go on the shot even from the offhand. */
	private static ItemStack firing = ItemStack.EMPTY;

	private final GunType type;

	public ArsenalGunItem(GunType type, Properties properties) {
		super(unit(type), type.hold, properties);
		this.type = type;
	}

	/** 0 damage, 100-tick reload, 1 block range, 1 spread, and this kind's ammo. */
	private static Stats unit(GunType type) {
		return new Stats(0.0f, UNIT_COOLDOWN, 1.0, 1.0f, 1, type.ammo, 0, 0, type.recoil);
	}

	public GunType type() {
		return type;
	}

	/**
	 * The One Piece Mod's shots hit the moment the trigger is pulled, inside use(), so remembering the gun
	 * here tells TraitEffects which gun fired, whichever hand it's in.
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

	/** The gun whose shot is landing right now, or an empty stack outside a shot. */
	public static ItemStack firing() {
		return firing;
	}

	@Override
	public Stats effectiveStats(ItemStack gun) {
		Stats parts = GunStats.stats(gun);
		if (parts == null) {
			parts = new Stats(1.0f, 40, 16.0, 0.1f, 1, type.ammo, 0, 0, type.recoil); // An unforged gun barely works.
		}
		Stats upgrades = super.effectiveStats(gun);
		return new Stats(parts.damage() + upgrades.damage(),
				Math.max(4, Math.round(parts.cooldown() * upgrades.cooldown() / (float) UNIT_COOLDOWN)),
				parts.range() * upgrades.range(),
				parts.spread() * upgrades.spread(),
				1, type.ammo, parts.magazine(), parts.reload(), parts.recoil());
	}

	@Override
	public Component getName(ItemStack stack) {
		List<ArsenalMaterial> parts = GunStats.parts(stack);
		return Component.literal(parts.isEmpty() ? "Unforged " + type.displayName : parts.get(0).displayName + " " + type.displayName);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag flag) {
		List<ArsenalMaterial> parts = GunStats.parts(stack);
		if (parts.isEmpty()) {
			textConsumer.accept(Component.literal("Build one at a Shipwright's Anvil.").withStyle(ChatFormatting.GRAY));
			return;
		}
		Stats stats = GunStats.stats(stack);
		textConsumer.accept(Component.literal(String.format("%.0f damage, %.2fs reload, %.0f block range",
				stats.damage(), stats.cooldown() / 20.0, stats.range())).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal(type.barrel.displayName + ": " + parts.get(0).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal(type.lock.displayName + ": " + parts.get(1).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal(type.stock.displayName + ": " + parts.get(2).displayName).withStyle(ChatFormatting.GRAY));
		for (Trait trait : GunStats.traits(stack)) {
			textConsumer.accept(Component.literal(trait.displayName + ": " + trait.description + (trait.active ? "" : " (coming soon)"))
					.withStyle(ChatFormatting.GOLD));
		}
		textConsumer.accept(Component.literal(type.ammo == Ammo.Kind.CANNONBALL ? "Needs Cannonballs." : "Needs Bullets or Seastone Bullets.")
				.withStyle(ChatFormatting.DARK_GRAY));
		WeaponUpgrades.describe(stack).forEach(textConsumer);
	}
}
