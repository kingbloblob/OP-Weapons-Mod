package com.onepiecearsenal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.station.ShipwrightsAnvilBlock;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.ArsenalArmorItem;
import com.onepiecearsenal.tool.FlintlockItem;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;

/**
 * Every Arsenal item and block. Parts and weapons are one item each; their materials are saved on the stack.
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
	public static final Item BARREL = part(PartType.BARREL);
	public static final Item FIRING_LOCK = part(PartType.LOCK);
	public static final Item STOCK = part(PartType.STOCK);
	public static final Item CROWN = part(PartType.CROWN);
	public static final Item COAT_PLATES = part(PartType.COAT_PLATES);
	public static final Item SASH_WEAVE = part(PartType.SASH_WEAVE);
	public static final Item BOOT_SOLES = part(PartType.BOOT_SOLES);
	public static final Item LINING = part(PartType.LINING);

	// ---- Weapons ----

	/** Starts with iron sword numbers; KatanaStats replaces them with the parts' numbers when it is forged. */
	public static final Item KATANA = register("katana", KatanaItem::new,
			new Item.Properties().sword(ToolMaterial.IRON, 0.0f, -2.4f));
	/** A One Piece Mod gun whose stats come from its parts (see FlintlockItem). GunStats sets the durability when forged. */
	public static final Item FLINTLOCK = register("flintlock", FlintlockItem::new,
			new Item.Properties().durability(300).enchantable(10));

	// ---- Armor ----

	/**
	 * Placeholder numbers (iron's) so the pieces equip and render; ArmorStats replaces them with the parts' numbers
	 * when a piece is forged. The look comes from assets/arsenal/equipment/pirate.json.
	 */
	private static final ArmorMaterial PIRATE_GEAR = new ArmorMaterial(15,
			Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2, ArmorType.BODY, 6),
			10, SoundEvents.ARMOR_EQUIP_IRON, 0.0f, 0.0f,
			TagKey.create(Registries.ITEM, ArsenalMod.id("repairs_arsenal_armor")),
			ResourceKey.create(EquipmentAssets.ROOT_ID, ArsenalMod.id("pirate")));

	public static final Item HAT = armor("hat", "Hat", ArmorType.HELMET);
	public static final Item COAT = armor("coat", "Coat", ArmorType.CHESTPLATE);
	public static final Item SASH = armor("sash", "Sash", ArmorType.LEGGINGS);
	public static final Item BOOTS = armor("boots", "Boots", ArmorType.BOOTS);

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
					// One katana and one flintlock of each material, with Ship Iron for the other parts.
					for (ArsenalMaterial material : ArsenalMaterial.values()) {
						output.accept(KatanaStats.build(material, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON));
					}
					for (ArsenalMaterial material : ArsenalMaterial.values()) {
						output.accept(GunStats.build(material, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON));
					}
					// One armor set of each material, with a Driftwood lining.
					for (ArsenalMaterial material : ArsenalMaterial.values()) {
						for (PartType shell : List.of(PartType.CROWN, PartType.COAT_PLATES, PartType.SASH_WEAVE, PartType.BOOT_SOLES)) {
							output.accept(ArmorStats.build(shell, material, ArsenalMaterial.DRIFTWOOD));
						}
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

	private static Item armor(String name, String pieceName, ArmorType type) {
		return register(name, properties -> new ArsenalArmorItem(pieceName, properties), new Item.Properties().humanoidArmor(PIRATE_GEAR, type));
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
