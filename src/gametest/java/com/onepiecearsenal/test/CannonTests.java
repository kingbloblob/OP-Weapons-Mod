package com.onepiecearsenal.test;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.cannon.CannonShells;
import com.onepiecearsenal.cannon.CannonStats;
import com.onepiecearsenal.cannon.ShipCannonBlockEntity;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecemod.ship.ShipLayout;
import com.onepiecemod.shipcombat.CannonAddons;

/** Ship Cannons and Cannon Shells, hooked into the One Piece Mod's cannons. Run with ./gradlew runGameTest. */
public class CannonTests {
	private static final BlockPos POS = new BlockPos(1, 1, 1);

	@GameTest
	public void shipIronCannonShootsLikeAPlainOne(GameTestHelper helper) {
		ArsenalMaterial iron = ArsenalMaterial.SHIP_IRON;
		helper.assertTrue(CannonStats.profile(List.of(iron, iron, iron)).equals(CannonAddons.Profile.PLAIN),
				"a Ship Iron cannon should match the plain Cannon, got " + CannonStats.profile(List.of(iron, iron, iron)));
		CannonAddons.Profile best = CannonStats.profile(List.of(ArsenalMaterial.VEGAPUNK_COMPOUND, ArsenalMaterial.DRIFTWOOD, ArsenalMaterial.VEGAPUNK_COMPOUND));
		helper.assertTrue(best.damage() > 1 && best.speed() > 1 && best.reload() < 1 && best.spread() < 1,
				"a Vegapunk bore and carriage with a Driftwood fuse should beat the plain Cannon everywhere, got " + best);
		helper.succeed();
	}

	@GameTest
	public void placedCannonFiresWithItsParts(GameTestHelper helper) {
		ItemStack item = CannonStats.build(ArsenalMaterial.CANNON_BRONZE, ArsenalMaterial.DRIFTWOOD, ArsenalMaterial.KAIROSEKI);
		BlockState state = ArsenalItems.SHIP_CANNON_BLOCK.defaultBlockState();
		helper.setBlock(POS, state);
		BlockPos at = helper.absolutePos(POS);
		ShipCannonBlockEntity cannon = (ShipCannonBlockEntity) helper.getLevel().getBlockEntity(at);
		cannon.applyComponentsFromItemStack(item);
		CannonAddons.Profile expected = CannonStats.profile(CannonStats.parts(item));

		helper.assertTrue(CannonAddons.isCannon(state), "the Ship Cannon should be in the One Piece Mod's cannons tag");
		helper.assertTrue(CannonAddons.profile(helper.getLevel(), state, at).equals(expected), "a placed cannon should shoot with its parts' numbers");
		helper.assertTrue(item.get(ArsenalComponents.CANNON_PARTS).equals(cannon.collectComponents().get(ArsenalComponents.CANNON_PARTS)),
				"breaking it should give back a cannon with the same parts");

		// What a ship does when she sets sail: save the block entity, and read it back from the save at sea.
		CompoundTag saved = cannon.saveWithFullMetadata(helper.getLevel().registryAccess());
		ShipLayout.ShipBlock aboard = new ShipLayout.ShipBlock(BlockPos.ZERO, state, Optional.of(saved));
		helper.assertTrue(CannonAddons.profile(helper.getLevel(), aboard).equals(expected), "aboard a ship it should keep its parts");
		helper.succeed();
	}

	@GameTest
	public void shellsLoadIntoCannons(GameTestHelper helper) {
		ItemStack seastone = PartItem.of(ArsenalItems.CANNON_SHELL, ArsenalMaterial.KAIROSEKI);
		CannonAddons.Shell shell = CannonAddons.shell(seastone);
		helper.assertTrue(shell != null && CannonAddons.isAmmo(seastone), "a Cannon Shell should be cannon ammo");
		helper.assertTrue(Math.abs(shell.hullDamage(PartItem.of(ArsenalItems.CANNON_SHELL, ArsenalMaterial.SHIP_IRON)) - 1.0f) < 0.001f,
				"a Ship Iron shell should hit a hull like a cannonball");
		helper.assertTrue(shell.hullDamage(seastone) > 1.0f, "a Kairoseki shell should hit harder than a cannonball");
		helper.assertTrue(CannonShells.burstTrait(ArsenalMaterial.KAIROSEKI) && CannonShells.shipEffect(ArsenalMaterial.KAIROSEKI) != null,
				"a Kairoseki shell should drain fruit users");
		helper.succeed();
	}
}
