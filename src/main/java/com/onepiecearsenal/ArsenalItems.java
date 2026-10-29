package com.onepiecearsenal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.station.ShipwrightsAnvilBlock;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;

/**
 * Every Arsenal item and block. Parts and katanas are one item each; their material is saved on the stack.
 */
public class ArsenalItems {
	private static final List<Item> PART_ITEMS = new ArrayList<>();

	// ---- Raw materials (the rest come from the One Piece Mod or vanilla, see ArsenalMaterial) ----

	/** Washed up on beaches and broken off shipwrecks. */
	public static final Item DRIFTWOOD = register("driftwood", Item::new, new Item.Properties());
	/** The Navy's standard-issue steel. Taken from Marines: players can't make it. */
	public static final Item MARINE_STEEL_INGOT = register("marine_steel_ingot", Item::new, new Item.Properties());

	// ---- Parts ----

	public static final Item KATANA_BLADE = part(PartType.KATANA_BLADE);
	public static final Item TSUBA = part(PartType.TSUBA);
	public static final Item HILT = part(PartType.HILT);

	// ---- Weapons ----

	/** Starts with iron sword numbers; KatanaStats replaces them with the parts' numbers when it is forged. */
	public static final Item KATANA = register("katana", KatanaItem::new,
			new Item.Properties().sword(ToolMaterial.IRON, 0.0f, -2.4f));

	// ---- Stations ----

	public static final Block SHIPWRIGHTS_ANVIL = block("shipwrights_anvil", ShipwrightsAnvilBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f).sound(SoundType.ANVIL).requiresCorrectToolForDrops());

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
			BuiltInRegistries.CREATIVE_MODE_TAB.key(), ArsenalMod.id("arsenal"));

	static void initialize() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
				.icon(() -> KatanaStats.build(ArsenalMaterial.KAIROSEKI, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON))
				.title(Component.literal("One Piece: Arsenal"))
				.displayItems((params, output) -> {
					output.accept(SHIPWRIGHTS_ANVIL);
					output.accept(DRIFTWOOD);
					output.accept(MARINE_STEEL_INGOT);
					for (Item part : PART_ITEMS) {
						for (ArsenalMaterial material : ArsenalMaterial.values()) {
							output.accept(PartItem.of(part, material));
						}
					}
					// One katana of each material, with a Ship Iron tsuba and hilt.
					for (ArsenalMaterial material : ArsenalMaterial.values()) {
						output.accept(KatanaStats.build(material, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON));
					}
				})
				.build());
	}

	// ---- Helpers ----

	private static Item part(PartType type) {
		Item item = register(type.id(), properties -> new PartItem(type, properties), new Item.Properties());
		PART_ITEMS.add(item);
		return item;
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ArsenalMod.id(name));
		Item item = factory.apply(properties.setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}

	/** Registers a block and the item you hold to place it. */
	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ArsenalMod.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		register(name, itemProperties -> new BlockItem(block, itemProperties), new Item.Properties().useBlockDescriptionPrefix());
		return block;
	}
}
