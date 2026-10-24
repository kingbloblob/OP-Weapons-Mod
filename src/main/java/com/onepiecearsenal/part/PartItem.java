package com.onepiecearsenal.part;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.GunStats;

/**
 * One part, like a Ship Iron Katana Blade. There is one item per part type; the material is saved on the stack.
 */
public class PartItem extends Item {
	private final PartType type;

	public PartItem(PartType type, Properties properties) {
		super(properties);
		this.type = type;
	}

	public PartType type() {
		return type;
	}

	/** The part's material, or null for a blank part (from the creative menu). */
	public static ArsenalMaterial material(ItemStack stack) {
		String id = stack.get(ArsenalComponents.PART_MATERIAL);
		return id == null ? null : ArsenalMaterial.byId(id);
	}

	public static ItemStack of(Item part, ArsenalMaterial material) {
		ItemStack stack = new ItemStack(part);
		stack.set(ArsenalComponents.PART_MATERIAL, material.id());
		return stack;
	}

	@Override
	public Component getName(ItemStack stack) {
		ArsenalMaterial material = material(stack);
		return Component.literal(material == null ? "Blank " + type.displayName : material.displayName + " " + type.displayName);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		ArsenalMaterial material = material(stack);
		if (material == null) {
			return;
		}
		textConsumer.accept(Component.literal("Tier " + material.tier + " · " + material.faction.displayName).withStyle(ChatFormatting.GRAY));
		switch (this.type) {
			case KATANA_BLADE -> textConsumer.accept(Component.literal("Damage: " + material.damage).withStyle(ChatFormatting.BLUE));
			case TSUBA, HILT -> textConsumer.accept(Component.literal("Speed: " + (material.speed >= 0 ? "+" : "") + material.speed).withStyle(ChatFormatting.BLUE));
			case BARREL -> textConsumer.accept(Component.literal(String.format("Shot damage: %.0f, range: %.0f blocks",
					GunStats.damage(material), GunStats.range(material))).withStyle(ChatFormatting.BLUE));
			case LOCK -> textConsumer.accept(Component.literal(String.format("Reload: %.2fs", GunStats.cooldown(material) / 20.0))
					.withStyle(ChatFormatting.BLUE));
			case STOCK -> textConsumer.accept(Component.literal(String.format("Spread: %.2f", GunStats.spread(material)))
					.withStyle(ChatFormatting.BLUE));
			case CROWN, COAT_PLATES, SASH_WEAVE, BOOT_SOLES -> {
				ArmorType armor = ArmorStats.typeOf(this.type);
				textConsumer.accept(Component.literal("Defense: " + ArmorStats.defense(material, armor)
						+ (ArmorStats.toughness(material) > 0 ? ", toughness: " + (int) ArmorStats.toughness(material) : "")).withStyle(ChatFormatting.BLUE));
			}
			case LINING -> textConsumer.accept(Component.literal("Comfort: " + material.displayName.toLowerCase() + " padding").withStyle(ChatFormatting.BLUE));
		}
		textConsumer.accept(Component.literal("Durability: " + material.durability).withStyle(ChatFormatting.BLUE));
		Trait trait = this.type.head ? material.headTrait : material.handleTrait;
		textConsumer.accept(Component.literal(trait.displayName + ": " + trait.description + (trait.active ? "" : " (coming soon)"))
				.withStyle(ChatFormatting.GOLD));
	}
}
