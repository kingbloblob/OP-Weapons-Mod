package com.onepiecearsenal.tool;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.material.TraitStats;

/**
 * Works out a tool's numbers from its parts and saves them on the item (the numbers for each kind are in ToolType).
 * What it can dig comes from its head's mining_level in materials.json, through the same block tags vanilla's tools use:
 * a Ship Iron pick digs what an iron pickaxe does. "seastone" is the One Piece Mod's Seastone tier, which also digs
 * Sea Prism Veins. Abyss Stone is left to the One Piece Mod's own Sea Prism pickaxe.
 */
public final class ToolStats {
	private static final Identifier DAMAGE_MODIFIER = Identifier.withDefaultNamespace("base_attack_damage");
	private static final Identifier SPEED_MODIFIER = Identifier.withDefaultNamespace("base_attack_speed");

	/** materials.json's mining_level names and the block tags of what they can't dig. */
	private static final Map<String, TagKey<Block>> INCORRECT = Map.of(
			"wood", tag("minecraft", "incorrect_for_wooden_tool"),
			"gold", tag("minecraft", "incorrect_for_gold_tool"),
			"stone", tag("minecraft", "incorrect_for_stone_tool"),
			"copper", tag("minecraft", "incorrect_for_copper_tool"),
			"iron", tag("minecraft", "incorrect_for_iron_tool"),
			"diamond", tag("minecraft", "incorrect_for_diamond_tool"),
			"netherite", tag("minecraft", "incorrect_for_netherite_tool"),
			"seastone", tag("onepiece", "incorrect_for_seastone_tool"));
	/** What each mining_level digs up to, for the tooltip. */
	private static final Map<String, String> LEVEL_NAMES = Map.of(
			"wood", "like a wooden pickaxe", "gold", "like a gold pickaxe", "stone", "like a stone pickaxe",
			"copper", "like a copper pickaxe", "iron", "like an iron pickaxe", "diamond", "like a diamond pickaxe",
			"netherite", "like a netherite pickaxe", "seastone", "like a Seastone pickaxe (Sea Prism Veins too)");

	/**
	 * The block sets the tools' rules use, looked up while the game registers its content (the only time new ones can
	 * be made), the same way vanilla's tools get theirs.
	 */
	private static final Map<TagKey<Block>, HolderSet<Block>> BLOCKS = new HashMap<>();
	private static Tool shears;

	private ToolStats() {
	}

	/** Called while Arsenal registers its items. */
	public static void initialize() {
		HolderGetter<Block> blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
		for (TagKey<Block> tag : INCORRECT.values()) {
			BLOCKS.put(tag, blocks.getOrThrow(tag));
		}
		for (ToolType type : ToolType.values()) {
			if (type.mineable != null) {
				BLOCKS.put(type.mineable, blocks.getOrThrow(type.mineable));
			}
		}
		shears = ShearsItem.createToolProperties();
	}

	public static float damage(ToolType type, ArsenalMaterial head) {
		return head.damage * type.damageScale + type.bonusDamage;
	}

	/** How fast the head digs, before the handles' traits. */
	public static float miningSpeed(ToolType type, ArsenalMaterial head) {
		return head.miningSpeed * type.miningScale;
	}

	/** How many bucketfuls a bucket with this shell holds: its tier, up to 4. */
	public static int capacity(ArsenalMaterial shell) {
		return Math.max(1, Math.min(4, shell.tier));
	}

	/** Driftwood catches fire: it can't carry lava. */
	public static boolean holdsLava(ArsenalMaterial shell) {
		return shell != ArsenalMaterial.DRIFTWOOD;
	}

	/** What the head digs up to, like "like an iron pickaxe". */
	public static String levelName(ArsenalMaterial head) {
		return LEVEL_NAMES.getOrDefault(head.miningLevel, "like a wooden pickaxe");
	}

	/** Builds a finished tool. The materials are in the order of type.parts(). */
	public static ItemStack build(ToolType type, List<ArsenalMaterial> parts) {
		ItemStack tool = new ItemStack(ArsenalItems.TOOLS.get(type));
		tool.set(ArsenalComponents.TOOL_PARTS, parts.stream().map(ArsenalMaterial::id).toList());
		ArsenalMaterial head = parts.get(0);
		Set<Trait> traits = traits(parts);

		float speed = type.baseSpeed;
		int durability = 0;
		for (int i = 0; i < parts.size(); i++) {
			durability += parts.get(i).durability;
			if (i > 0) {
				speed += parts.get(i).speed / (parts.size() - 1);
			}
		}
		durability = Math.round(durability / (float) parts.size() * type.durabilityScale);
		if (traits.contains(Trait.LIGHT)) {
			speed += 0.1f;
		}

		ItemAttributeModifiers modifiers = ItemAttributeModifiers.EMPTY;
		if (type != ToolType.SHEARS && type != ToolType.BUCKET) {
			modifiers = modifiers
					.withModifierAdded(Attributes.ATTACK_DAMAGE,
							new AttributeModifier(DAMAGE_MODIFIER, damage(type, head), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.withModifierAdded(Attributes.ATTACK_SPEED,
							new AttributeModifier(SPEED_MODIFIER, speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
		}
		tool.set(DataComponents.ATTRIBUTE_MODIFIERS, TraitStats.modifiers(modifiers, traits, EquipmentSlotGroup.MAINHAND));
		tool.set(DataComponents.MAX_DAMAGE, TraitStats.durability(durability, traits));

		float mining = miningSpeed(type, head) * (traits.contains(Trait.LIGHT) ? 1.1f : 1.0f);
		Tool digging = digging(type, head, mining);
		if (digging != null) {
			tool.set(DataComponents.TOOL, digging);
		}
		TraitStats.finish(tool, traits);
		return tool;
	}

	/** The tool's rules: what it digs, how fast, and what it can't get drops from. Null for the bucket. */
	private static Tool digging(ToolType type, ArsenalMaterial head, float speed) {
		if (type == ToolType.BUCKET || shears == null) {
			return null;
		}
		if (type == ToolType.SHEARS) {
			// Vanilla's shears, faster or slower with the blades' metal (iron shears cut like vanilla ones).
			float scale = Math.max(0.5f, speed / 6.0f);
			List<Tool.Rule> rules = new ArrayList<>();
			for (Tool.Rule rule : shears.rules()) {
				rules.add(new Tool.Rule(rule.blocks(), rule.speed().map(s -> s * scale), rule.correctForDrops()));
			}
			return new Tool(rules, shears.defaultMiningSpeed(), shears.damagePerBlock(), shears.canDestroyBlocksInCreative());
		}
		HolderSet<Block> incorrect = BLOCKS.get(INCORRECT.getOrDefault(head.miningLevel, INCORRECT.get("wood")));
		HolderSet<Block> mineable = BLOCKS.get(type.mineable);
		if (incorrect == null || mineable == null) {
			return null;
		}
		return new Tool(List.of(Tool.Rule.deniesDrops(incorrect), Tool.Rule.minesAndDrops(mineable, speed)), 1.0f, 1, true);
	}

	/** The tool's materials, head first. Empty for a tool that wasn't built from parts. */
	public static List<ArsenalMaterial> parts(ItemStack tool) {
		if (!(tool.getItem() instanceof ArsenalTool item)) {
			return List.of();
		}
		List<ArsenalMaterial> parts = new ArrayList<>();
		for (String id : tool.getOrDefault(ArsenalComponents.TOOL_PARTS, List.<String>of())) {
			ArsenalMaterial material = ArsenalMaterial.byId(id);
			if (material == null) {
				return List.of();
			}
			parts.add(material);
		}
		return parts.size() == item.toolType().parts().length ? parts : List.of();
	}

	/** The head's head trait, and the handle traits of the other parts. */
	public static Set<Trait> traits(ItemStack tool) {
		List<ArsenalMaterial> parts = parts(tool);
		return parts.isEmpty() ? EnumSet.noneOf(Trait.class) : traits(parts);
	}

	private static Set<Trait> traits(List<ArsenalMaterial> parts) {
		Set<Trait> traits = EnumSet.of(parts.get(0).headTrait);
		for (int i = 1; i < parts.size(); i++) {
			traits.add(parts.get(i).handleTrait);
		}
		return traits;
	}

	private static TagKey<Block> tag(String namespace, String path) {
		return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(namespace, path));
	}
}
