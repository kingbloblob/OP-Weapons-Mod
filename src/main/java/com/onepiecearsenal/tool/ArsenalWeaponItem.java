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

import com.onepiecearsenal.mastery.Mastery;
import com.onepiecearsenal.fruit.FruitWeapons;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.skill.WeaponSkill;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * A cutlass, sabre, boarding axe, jitte, kanabo, spear, pair of Fishman Gloves or Dial palm built from a head, a binding and a handle at the
 * Shipwright's Anvil. Its name, numbers and traits all come from the parts (see WeaponStats), and what makes
 * each type different is in WeaponType and WeaponEffects.
 */
public class ArsenalWeaponItem extends Item {
	private final WeaponType type;

	public ArsenalWeaponItem(WeaponType type, Properties properties) {
		super(properties);
		this.type = type;
	}

	public WeaponType type() {
		return type;
	}

	/** Gear forged before a balance change catches up with the new numbers (see Restat). */
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		Restat.refresh(stack);
		Breakage.notice(stack, owner);
	}

	@Override
	public Component getName(ItemStack stack) {
		List<ArsenalMaterial> parts = WeaponStats.parts(stack);
		return parts.isEmpty() ? Component.literal("Unforged " + type.displayName) : Breakage.name(stack, parts.get(0).displayName + " " + type.displayName);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag flag) {
		List<ArsenalMaterial> parts = WeaponStats.parts(stack);
		if (parts.isEmpty()) {
			textConsumer.accept(Component.literal("Build one at a Shipwright's Anvil.").withStyle(ChatFormatting.GRAY));
			return;
		}
		Breakage.describe(stack, textConsumer);
		textConsumer.accept(Component.literal(type.head.displayName + ": " + parts.get(0).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal(type.binding.displayName + ": " + parts.get(1).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal(type.handle.displayName + ": " + parts.get(2).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal(type.ability).withStyle(ChatFormatting.AQUA));
		DialCharge.describe(stack, textConsumer);
		WeaponSkill.describe(stack, textConsumer);
		FruitWeapons.describe(stack, textConsumer);
		for (Trait trait : WeaponStats.traits(stack)) {
			textConsumer.accept(Component.literal(trait.displayName + ": " + trait.description + (trait.active ? "" : " (coming soon)"))
					.withStyle(ChatFormatting.GOLD));
		}
		Upgrades.describe(stack, textConsumer);
		Mastery.describe(stack, textConsumer);
	}
}
