package com.onepiecearsenal.client;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.ArsenalComponents;

/**
 * Lets an item model pick a texture by material, Tinkers' Construct style. In assets/arsenal/items:
 * <pre>{"type": "minecraft:select", "property": "arsenal:part_material", "index": 0, "cases": [...]}</pre>
 * Without an index it reads a part's own material. With one it reads that part of a forged weapon or armor piece
 * (0 = blade, barrel, head, shell, bore, face or plates, in the order KatanaStats, GunStats, WeaponStats, ArmorStats, CannonStats,
 * ShieldStats and GauntletStats save them).
 */
public record PartMaterialProperty(int index) implements SelectItemModelProperty<String> {
	public static final SelectItemModelProperty.Type<PartMaterialProperty, String> TYPE = SelectItemModelProperty.Type.create(
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					Codec.INT.optionalFieldOf("index", -1).forGetter(PartMaterialProperty::index)
			).apply(instance, PartMaterialProperty::new)),
			Codec.STRING);

	@Override
	public String get(ItemStack stack, ClientLevel level, LivingEntity owner, int seed, ItemDisplayContext context) {
		if (index < 0) {
			return stack.get(ArsenalComponents.PART_MATERIAL);
		}
		List<String> parts = stack.get(ArsenalComponents.KATANA_PARTS);
		if (parts == null) {
			parts = stack.get(ArsenalComponents.GUN_PARTS);
		}
		if (parts == null) {
			parts = stack.get(ArsenalComponents.WEAPON_PARTS);
		}
		if (parts == null) {
			parts = stack.get(ArsenalComponents.ARMOR_PARTS);
		}
		if (parts == null) {
			parts = stack.get(ArsenalComponents.CANNON_PARTS);
		}
		if (parts == null) {
			parts = stack.get(ArsenalComponents.SHIELD_PARTS);
		}
		if (parts == null) {
			parts = stack.get(ArsenalComponents.GAUNTLET_PARTS);
		}
		return parts != null && index < parts.size() ? parts.get(index) : null;
	}

	@Override
	public Codec<String> valueCodec() {
		return Codec.STRING;
	}

	@Override
	public SelectItemModelProperty.Type<PartMaterialProperty, String> type() {
		return TYPE;
	}
}
