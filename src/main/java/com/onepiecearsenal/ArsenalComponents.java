package com.onepiecearsenal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;

import com.onepiecearsenal.haki.HakiCoat;
import com.onepiecearsenal.mastery.WeaponMastery;
import com.onepiecearsenal.meito.BladeRecord;

/**
 * Data saved on Arsenal items (as "data components"), so a part remembers its material and a katana
 * remembers what it was built from when you drop it, trade it or put it in a chest.
 */
public class ArsenalComponents {
	/** The material id of a part, like "ship_iron". */
	public static final DataComponentType<String> PART_MATERIAL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("part_material"),
			DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

	/** The material ids of a katana's blade, tsuba and hilt, in that order. */
	public static final DataComponentType<List<String>> KATANA_PARTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("katana_parts"),
			DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
					.networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

	/** The material ids of a gun's barrel, firing part and stock, in that order (flintlock, musket and hand cannon). */
	public static final DataComponentType<List<String>> GUN_PARTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("gun_parts"),
			DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
					.networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

	/** The material ids of a Ship Cannon's bore, fuse and gun carriage, in that order (on the item and its block entity). */
	public static final DataComponentType<List<String>> CANNON_PARTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("cannon_parts"),
			DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
					.networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

	/** The material ids of a cutlass, sabre, axe, jitte, kanabo or spear's head, binding and handle, in that order. */
	public static final DataComponentType<List<String>> WEAPON_PARTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("weapon_parts"),
			DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
					.networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

	/** The material ids of an armor piece's shell and lining, in that order. */
	public static final DataComponentType<List<String>> ARMOR_PARTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("armor_parts"),
			DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
					.networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

	/** The material ids of a shield's face, rim (a Binding) and strap (a Grip), in that order (see ShieldStats). */
	public static final DataComponentType<List<String>> SHIELD_PARTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("shield_parts"),
			DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
					.networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

	/** The material ids of a pair of gauntlets' plates and lining, in that order (see GauntletStats). */
	public static final DataComponentType<List<String>> GAUNTLET_PARTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("gauntlet_parts"),
			DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
					.networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

	/** A katana's level and deeds, which decide when a whetstone can raise its grade (see Meito). */
	public static final DataComponentType<BladeRecord> BLADE_RECORD = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("blade_record"),
			DataComponentType.<BladeRecord>builder().persistent(BladeRecord.CODEC).networkSynchronized(BladeRecord.STREAM_CODEC).build());

	/**
	 * Seconds a Supreme Grade katana has spent in Haki-coated fights, toward its Black Blade awakening (see Meito).
	 * It ticks up every second of a fight, so it skips the "pull out" animation.
	 */
	public static final DataComponentType<Integer> HAKI_SOAK = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("haki_soak"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).ignoreSwapAnimation().build());

	/** The upgrades added to a piece of gear at the Shipwright's Anvil and their levels, like {"honed_edge": 2} (see Upgrades). */
	public static final DataComponentType<Map<String, Integer>> UPGRADES = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("upgrades"),
			DataComponentType.<Map<String, Integer>>builder().persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
					.networkSynchronized(ByteBufCodecs.<ByteBuf, String, Integer, Map<String, Integer>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT)).build());

	/**
	 * A fingerprint of the numbers a piece of gear was last forged with (see Restat). When the materials or formulas
	 * change, it no longer matches and the piece's stats are worked out again. It skips the "pull out" animation.
	 */
	public static final DataComponentType<Integer> STATS_STAMP = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("stats_stamp"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).ignoreSwapAnimation().build());

	/**
	 * On a head part taken off forged gear at the Shipwright's Anvil (see Salvage): the gear it came from, so forging it
	 * into the same kind of gear again brings back its upgrades, whetstone grade, blade level, enchantments, name and wear.
	 */
	public static final DataComponentType<ItemContainerContents> SALVAGED_FROM = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("salvaged_from"),
			DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC)
					.networkSynchronized(ItemContainerContents.STREAM_CODEC).build());

	/** Set on forged gear the moment it breaks, until its owner has been told (see Breakage). */
	public static final DataComponentType<Boolean> JUST_BROKE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("just_broke"),
			DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).ignoreSwapAnimation().build());

	/**
	 * The Devil Fruit a weapon or gun has eaten, like "inu_inu_no_mi_wolf" (see FruitWeapons). The weapon itself holds
	 * the power, so its wielder doesn't need a fruit, and a fruit user can wield it too.
	 */
	public static final DataComponentType<String> EATEN_FRUIT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("eaten_fruit"),
			DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

	/**
	 * Armament Haki coating a weapon or gun until a game time (see HakiImbue). The client blackens the blade while
	 * it's on, so it's sent to the player's game. It skips the "pull out" animation.
	 */
	public static final DataComponentType<HakiCoat> HAKI_COAT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("haki_coat"),
			DataComponentType.<HakiCoat>builder().persistent(HakiCoat.CODEC).networkSynchronized(HakiCoat.STREAM_CODEC).ignoreSwapAnimation().build());

	/**
	 * The damage an Impact or Reject Palm's Dial has caught and is holding, ready to let out (see DialCharge).
	 * It changes in the middle of fights, so it skips the "pull out" animation.
	 */
	public static final DataComponentType<Float> DIAL_CHARGE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("dial_charge"),
			DataComponentType.<Float>builder().persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).ignoreSwapAnimation().build());

	/**
	 * A weapon's mastery XP and the trait bound into its mastery slot (see Mastery). It changes with kills, so it skips
	 * the "pull out" animation.
	 */
	public static final DataComponentType<WeaponMastery> MASTERY = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("mastery"),
			DataComponentType.<WeaponMastery>builder().persistent(WeaponMastery.CODEC).networkSynchronized(WeaponMastery.STREAM_CODEC)
					.ignoreSwapAnimation().build());

	/** The material ids of a tool's parts, head first, in the order its ToolType lists them (see ToolStats). */
	public static final DataComponentType<List<String>> TOOL_PARTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("tool_parts"),
			DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
					.networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

	/** What an Arsenal Bucket holds ("water" or "lava") and how many bucketfuls of it (see ArsenalBucketItem). */
	public static final DataComponentType<String> BUCKET_FLUID = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("bucket_fluid"),
			DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
	public static final DataComponentType<Integer> BUCKET_AMOUNT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ArsenalMod.id("bucket_amount"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** Loads this class, which registers the components above. */
	static void initialize() {
	}
}
