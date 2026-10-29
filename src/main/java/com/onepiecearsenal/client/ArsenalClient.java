package com.onepiecearsenal.client;

import net.fabricmc.api.ClientModInitializer;

import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.mixin.client.SelectItemModelPropertiesAccessor;

/**
 * The client-only start of One Piece Arsenal. Runs before resource packs load, so item models can use
 * the "arsenal:part_material" property.
 */
public class ArsenalClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SelectItemModelPropertiesAccessor.arsenal$idMapper().put(ArsenalMod.id("part_material"), PartMaterialProperty.TYPE);
	}
}
