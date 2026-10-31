package com.onepiecearsenal.mixin.client;

import com.mojang.serialization.MapCodec;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

/** Vanilla's list of item model tint sources; this lets ArsenalClient add one. */
@Mixin(ItemTintSources.class)
public interface ItemTintSourcesAccessor {
	@Accessor("ID_MAPPER")
	static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>> arsenal$idMapper() {
		throw new AssertionError();
	}
}
