package com.onepiecearsenal.cannon;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.material.ArsenalMaterial;

/**
 * Remembers what a placed Ship Cannon is made of. The materials move from the item to the block when it's placed
 * and back when it's broken, and a ship keeps them while she sails (the One Piece Mod saves block entities aboard).
 */
public class ShipCannonBlockEntity extends BlockEntity {
	private List<String> parts = List.of();

	public ShipCannonBlockEntity(BlockPos pos, BlockState state) {
		super(ArsenalItems.SHIP_CANNON_ENTITY, pos, state);
	}

	/** The materials as bore, fuse, carriage. Empty for a cannon that wasn't built from parts. */
	public List<ArsenalMaterial> parts() {
		return CannonStats.parts(parts);
	}

	@Override
	public void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("Parts", Codec.STRING.listOf(), parts);
	}

	@Override
	public void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		parts = input.read("Parts", Codec.STRING.listOf()).orElse(List.of());
	}

	@Override
	public void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		List<String> fromItem = components.get(ArsenalComponents.CANNON_PARTS);
		if (fromItem != null) {
			parts = List.copyOf(fromItem);
		}
	}

	@Override
	public void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!parts.isEmpty()) {
			components.set(ArsenalComponents.CANNON_PARTS, parts);
		}
	}
}
