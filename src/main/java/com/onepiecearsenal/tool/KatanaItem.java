package com.onepiecearsenal.tool;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.meito.Meito;

/**
 * A katana built from a blade, a tsuba and a hilt at the Shipwright's Anvil. Its name, numbers and traits
 * all come from the parts (see KatanaStats). It is in the minecraft:swords tag, so the One Piece Mod's
 * whetstones, Dials and Haki treat it like any other sword.
 */
public class KatanaItem extends Item {
	public KatanaItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		List<ArsenalMaterial> parts = KatanaStats.parts(stack);
		return Component.literal(parts.isEmpty() ? "Unforged Katana" : parts.get(0).displayName + " Katana");
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		List<ArsenalMaterial> parts = KatanaStats.parts(stack);
		if (parts.isEmpty()) {
			textConsumer.accept(Component.literal("Build one at a Shipwright's Anvil.").withStyle(ChatFormatting.GRAY));
			return;
		}
		textConsumer.accept(Component.literal("Blade: " + parts.get(0).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal("Tsuba: " + parts.get(1).displayName).withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.literal("Hilt: " + parts.get(2).displayName).withStyle(ChatFormatting.GRAY));
		for (Trait trait : KatanaStats.traits(stack)) {
			textConsumer.accept(Component.literal(trait.displayName + ": " + trait.description + (trait.active ? "" : " (coming soon)"))
					.withStyle(ChatFormatting.GOLD));
		}

		int level = Meito.level(stack);
		int maxLevel = Meito.maxLevel(stack);
		String progress = level >= maxLevel ? " (grade limit)"
				: " (" + Meito.record(stack).xp() + "/" + Meito.xpForLevel(level + 1) + " XP)";
		textConsumer.accept(Component.literal("Blade level " + level + "/" + maxLevel + progress).withStyle(ChatFormatting.AQUA));
		int grade = OnePieceBridge.swordGrade(stack);
		if (grade > 0) {
			textConsumer.accept(Component.literal("Wielder needs " + Meito.wieldRequirement(grade)).withStyle(ChatFormatting.DARK_AQUA));
		}
		List<Meito.Need> needs = Meito.promotionNeeds(stack);
		if (!needs.isEmpty()) {
			textConsumer.accept(Component.literal("To reach " + OnePieceBridge.gradeName(grade + 1) + ":").withStyle(ChatFormatting.GRAY));
			for (Meito.Need need : needs) {
				textConsumer.accept(Component.literal((need.met() ? " \u2714 " : " \u2718 ") + need.text())
						.withStyle(need.met() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
			}
			textConsumer.accept(Component.literal(" and a wielder with " + Meito.wieldRequirement(grade + 1)).withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
