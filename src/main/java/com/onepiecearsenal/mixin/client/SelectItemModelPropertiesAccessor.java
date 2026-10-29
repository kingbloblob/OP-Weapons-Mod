package com.onepiecearsenal.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

/** Vanilla's list of item model select properties is private; this lets ArsenalClient add one. */
@Mixin(SelectItemModelProperties.class)
public interface SelectItemModelPropertiesAccessor {
	@Accessor("ID_MAPPER")
	static ExtraCodecs.LateBoundIdMapper<Identifier, SelectItemModelProperty.Type<?, ?>> arsenal$idMapper() {
		throw new AssertionError();
	}
}
