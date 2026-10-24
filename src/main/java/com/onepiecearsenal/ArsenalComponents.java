package com.onepiecearsenal;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

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

	/** Loads this class, which registers the components above. */
	static void initialize() {
	}
}
