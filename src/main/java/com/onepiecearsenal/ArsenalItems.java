package com.onepiecearsenal;

import java.util.ArrayList;
import java.util.EnumMap;
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
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import com.onepiecearsenal.cannon.CannonStats;
import com.onepiecearsenal.cannon.ShipCannonBlock;
import com.onepiecearsenal.cannon.ShipCannonBlockEntity;
import com.onepiecearsenal.cannon.ShipCannonItem;
import com.onepiecearsenal.fruit.FamousFruitWeapon;
import com.onepiecearsenal.guide.GuideBook;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.station.MarineArmoryBenchBlock;
import com.onepiecearsenal.station.NavyCastingBlock;
import com.onepiecearsenal.station.ShipwrightsAnvilBlock;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.ArsenalArmorItem;
import com.onepiecearsenal.tool.ArsenalGunItem;
import com.onepiecearsenal.tool.ArsenalWeaponItem;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.tool.WeaponType;

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
	/** Copper and the iron of old cannonballs, cast together. */
	public static final Item CANNON_BRONZE_INGOT = register("cannon_bronze_ingot", Item::new, new Item.Properties());
	/** Dropped by Sea Kings. */
	public static final Item SEA_KING_BONE = register("sea_king_bone", Item::new, new Item.Properties());
	/** Dropped by Island Whales. */
	public static final Item ISLAND_WHALE_LEATHER = register("island_whale_leather", Item::new, new Item.Properties());
	/** Salvaged from beaten Pacifistas. Navy science: players can't make it. */
	public static final Item PACIFISTA_ALLOY = register("pacifista_alloy", Item::new, new Item.Properties());
	/** Vegapunk's best work, carried by Seraphim and the Navy's top brass. */
	public static final Item VEGAPUNK_COMPOUND = register("vegapunk_compound", Item::new,
			new Item.Properties().rarity(Rarity.EPIC).fireResistant());

	/** The in-game guide (see GuideBook). */
	public static final Item GUIDE = register("shipwrights_manual", GuideBook::new, new Item.Properties().stacksTo(1));

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
	public static final Item LONG_BARREL = part(PartType.LONG_BARREL);
	public static final Item CANNON_BORE = part(PartType.CANNON_BORE);
	public static final Item FUSE = part(PartType.FUSE);
	public static final Item CUTLASS_BLADE = part(PartType.CUTLASS_BLADE);
	public static final Item SABRE_BLADE = part(PartType.SABRE_BLADE);
	public static final Item AXE_HEAD = part(PartType.AXE_HEAD);
	public static final Item JITTE_ROD = part(PartType.JITTE_ROD);
	public static final Item CLUB_HEAD = part(PartType.CLUB_HEAD);
	public static final Item SPEARHEAD = part(PartType.SPEARHEAD);
	public static final Item HAND_GUARD = part(PartType.HAND_GUARD);
	public static final Item BINDING = part(PartType.BINDING);
	public static final Item STUDS = part(PartType.STUDS);
	public static final Item GRIP = part(PartType.GRIP);
	public static final Item HAFT = part(PartType.HAFT);
	public static final Item GUN_CARRIAGE = part(PartType.GUN_CARRIAGE);
	/** Cannon ammo, one per material (see CannonShells). A part, so the Navy stations can cast it too. */
	public static final Item CANNON_SHELL = part(PartType.CANNON_SHELL);

	// ---- Weapons ----

	/** Starts with iron sword numbers; KatanaStats replaces them with the parts' numbers when it is forged. */
	public static final Item KATANA = register("katana", KatanaItem::new,
			new Item.Properties().sword(ToolMaterial.IRON, 0.0f, -2.4f));
	/** One Piece Mod guns whose stats come from their parts (see ArsenalGunItem). GunStats sets the durability when forged. */
	public static final Map<GunType, Item> GUNS = new EnumMap<>(GunType.class);
	/** Cutlasses, sabres, axes, jittes, kanabo and spears. Iron numbers until WeaponStats puts the parts' numbers on. */
	public static final Map<WeaponType, Item> WEAPONS = new EnumMap<>(WeaponType.class);

	static {
		for (GunType type : GunType.values()) {
			GUNS.put(type, register(type.id(), properties -> new ArsenalGunItem(type, properties),
					new Item.Properties().durability(300).enchantable(10)));
		}
		for (WeaponType type : WeaponType.values()) {
			Item.Properties properties = type == WeaponType.BOARDING_AXE
					? new Item.Properties().axe(ToolMaterial.IRON, 0.0f, type.baseSpeed)
					: new Item.Properties().sword(ToolMaterial.IRON, 0.0f, type.baseSpeed);
			WEAPONS.put(type, register(type.id(), itemProperties -> new ArsenalWeaponItem(type, itemProperties), properties));
		}
	}

	public static final Item FLINTLOCK = GUNS.get(GunType.FLINTLOCK);

	// ---- Armor ----

	/**
	 * Placeholder numbers (iron's) so the pieces equip and render; ArmorStats replaces them with the parts' numbers
	 * when a piece is forged, and gives it the look of its shell material (assets/arsenal/equipment/<material>.json).
	 * equipment/pirate.json is the look of pieces forged before that.
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

	// ---- Ship cannons (see the cannon package) ----

	/** Forged at the Shipwright's Anvil from a Cannon Bore, a Fuse and a Gun Carriage. The parts are saved on its block entity. */
	public static final Block SHIP_CANNON_BLOCK = Registry.register(BuiltInRegistries.BLOCK, ArsenalMod.id("ship_cannon"),
			new ShipCannonBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL).noOcclusion()
					.setId(ResourceKey.create(Registries.BLOCK, ArsenalMod.id("ship_cannon")))));
	public static final Item SHIP_CANNON = register("ship_cannon", properties -> new ShipCannonItem(SHIP_CANNON_BLOCK, properties),
			new Item.Properties().stacksTo(16).useBlockDescriptionPrefix());
	public static final BlockEntityType<ShipCannonBlockEntity> SHIP_CANNON_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ArsenalMod.id("ship_cannon"), FabricBlockEntityTypeBuilder.create(ShipCannonBlockEntity::new, SHIP_CANNON_BLOCK).build());

	// ---- Stations ----

	public static final Block SHIPWRIGHTS_ANVIL = block("shipwrights_anvil", ShipwrightsAnvilBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f).sound(SoundType.ANVIL).requiresCorrectToolForDrops());

	// Navy stations: no recipes, captured from Marine bases (see NavyStationBlock and NavyStations).
	// Blast-proof, so a guarded one can't be blown loose with TNT.

	public static final Block MARINE_ARMORY_BENCH = block("marine_armory_bench", MarineArmoryBenchBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 1200.0f).sound(SoundType.ANVIL)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.IMMOVEABLE));
	public static final Block QUARTERMASTERS_DESK = block("quartermasters_desk", properties -> new NavyCastingBlock("Quartermaster's Desk",
			"Copied", SoundEvents.VILLAGER_WORK_CARTOGRAPHER, List.of(ArsenalMaterial.MARINE_STEEL), properties),
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f, 1200.0f).sound(SoundType.WOOD)
					.pushReaction(PushReaction.IMMOVEABLE));
	public static final Block MARINE_BASE_SMELTERY = block("marine_base_smeltery", properties -> new NavyCastingBlock("Marine Base Smeltery",
			"Cast", SoundEvents.LAVA_EXTINGUISH, List.of(ArsenalMaterial.KAIROSEKI, ArsenalMaterial.PACIFISTA_ALLOY, ArsenalMaterial.VEGAPUNK_COMPOUND), properties),
			BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(5.0f, 1200.0f).sound(SoundType.STONE)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.IMMOVEABLE));

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
			BuiltInRegistries.CREATIVE_MODE_TAB.key(), ArsenalMod.id("arsenal"));

	static void initialize() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
				.icon(() -> KatanaStats.build(ArsenalMaterial.KAIROSEKI, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON))
				.title(Component.literal("One Piece: Arsenal"))
				.displayItems((params, output) -> {
					output.accept(GUIDE);
					output.accept(SHIPWRIGHTS_ANVIL);
					output.accept(MARINE_ARMORY_BENCH);
					output.accept(QUARTERMASTERS_DESK);
					output.accept(MARINE_BASE_SMELTERY);
					output.accept(DRIFTWOOD);
					output.accept(MARINE_STEEL_INGOT);
					output.accept(CANNON_BRONZE_INGOT);
					output.accept(SEA_KING_BONE);
					output.accept(ISLAND_WHALE_LEATHER);
					output.accept(PACIFISTA_ALLOY);
					output.accept(VEGAPUNK_COMPOUND);
					for (Item part : PART_ITEMS) {
						for (ArsenalMaterial material : ArsenalMaterial.values()) {
							if (!material.armorOnly || ((PartItem) part).type().isArmor()) {
								output.accept(PartItem.of(part, material));
							}
						}
					}
					// One of each weapon per material, with Ship Iron for the other parts. Hides and leathers can't make weapons.
					List<ArsenalMaterial> metals = ArsenalMaterial.values().stream().filter(material -> !material.armorOnly).toList();
					for (ArsenalMaterial material : metals) {
						output.accept(KatanaStats.build(material, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON));
					}
					for (WeaponType type : WeaponType.values()) {
						for (ArsenalMaterial material : metals) {
							output.accept(WeaponStats.build(type, material, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON));
						}
					}
					for (GunType type : GunType.values()) {
						for (ArsenalMaterial material : metals) {
							output.accept(GunStats.build(type, material, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON));
						}
					}
					// One Ship Cannon per bore material, with a Ship Iron fuse and carriage.
					for (ArsenalMaterial material : metals) {
						output.accept(CannonStats.build(material, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON));
					}
					// The famous weapons that ate a Devil Fruit.
					for (FamousFruitWeapon famous : FamousFruitWeapon.values()) {
						output.accept(famous.create());
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

	/** The item for a kind of part, like the Katana Blade for PartType.KATANA_BLADE. */
	public static Item partItem(PartType type) {
		for (Item item : PART_ITEMS) {
			if (((PartItem) item).type() == type) {
				return item;
			}
		}
		throw new IllegalArgumentException("No part item for " + type);
	}

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
