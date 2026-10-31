package com.onepiecearsenal.client;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.haki.HakiImbue;

/**
 * Blackens a weapon's blade, head or barrel while it's coated in Armament Haki (see HakiImbue), with a slow purple
 * shimmer like the anime's. In assets/arsenal/items, on the first layer's models:
 * <pre>{"type": "minecraft:model", "model": "...", "tints": [{"type": "arsenal:haki_coat"}]}</pre>
 * Uncoated, it's white, which leaves the texture as drawn.
 */
public record HakiCoatTint() implements ItemTintSource {
	public static final MapCodec<HakiCoatTint> CODEC = MapCodec.unit(new HakiCoatTint());

	private static final int WHITE = 0xFFFFFFFF;

	@Override
	public int calculate(ItemStack stack, ClientLevel level, LivingEntity owner) {
		if (level == null || !HakiImbue.isCoated(stack, level.getGameTime())) {
			return WHITE;
		}
		// Multiplies the texture down to near black: dark purple, glinting a little brighter every two seconds.
		float glint = 0.5f + 0.5f * Mth.sin(level.getGameTime() * (float) Math.PI / 20.0f);
		int red = (int) Mth.lerp(glint, 0x2A, 0x48);
		int green = (int) Mth.lerp(glint, 0x22, 0x34);
		int blue = (int) Mth.lerp(glint, 0x33, 0x5C);
		return 0xFF000000 | red << 16 | green << 8 | blue;
	}

	@Override
	public MapCodec<HakiCoatTint> type() {
		return CODEC;
	}
}
