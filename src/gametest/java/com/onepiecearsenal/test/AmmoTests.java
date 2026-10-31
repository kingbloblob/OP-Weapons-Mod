package com.onepiecearsenal.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.ammo.DialRound;
import com.onepiecearsenal.ammo.MusketBalls;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.station.NavyCastingBlock;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecemod.weapon.Ammo;
import com.onepiecemod.weapon.GunAddons;

/** Musket Balls and Dial Rounds, fired from guns through the One Piece Mod's GunAddons. Run with ./gradlew runGameTest. */
public class AmmoTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;

	@GameTest
	public void roundsFitPistolsAndRifles(GameTestHelper helper) {
		helper.assertTrue(GunAddons.firesAs(PartItem.of(ArsenalItems.MUSKET_BALL, IRON)) == Ammo.BULLET, "a Musket Ball should be a bullet");
		for (DialRound round : DialRound.values()) {
			helper.assertTrue(GunAddons.firesAs(new ItemStack(ArsenalItems.DIAL_ROUNDS.get(round))) == Ammo.BULLET, round.displayName + " should be a bullet");
		}
		helper.assertTrue(MusketBalls.bonusDamage(IRON) == 0.0f, "a Ship Iron ball should hit like a plain Bullet");
		helper.assertTrue(MusketBalls.bonusDamage(ArsenalMaterial.byId("black_blade")) > 0.0f, "a Black Blade ball should hit harder");
		helper.assertTrue(NavyCastingBlock.yield(PartType.MUSKET_BALL) == 8, "a Navy station should cast eight balls, like the recipe");
		helper.succeed();
	}

	@GameTest
	public void offhandMusketBallIsFiredAndPassesItsTrait(GameTestHelper helper) {
		ServerPlayer player = facingSouth(helper);
		Mob target = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 1, 4));
		player.setItemSlot(EquipmentSlot.MAINHAND, GunStats.build(GunType.FLINTLOCK, IRON, IRON, IRON));
		ItemStack balls = PartItem.of(ArsenalItems.MUSKET_BALL, ArsenalMaterial.byId("sea_king_bone"));
		balls.setCount(8);
		player.setItemSlot(EquipmentSlot.OFFHAND, balls);
		player.getInventory().add(new ItemStack(com.onepiecemod.weapon.WeaponItems.BULLET, 8)); // Off hand goes first.

		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getOffhandItem().getCount() == 7, "the shot should use one ball from the off hand, has " + player.getOffhandItem().getCount());
		helper.assertTrue(target.getHealth() < target.getMaxHealth(), "the shot should hit the villager");
		helper.assertTrue(target.hasEffect(MobEffects.WITHER), "a Sea King Bone ball should make it bleed");
		helper.succeed();
	}

	@GameTest
	public void flameDialRoundSetsTargetsAlight(GameTestHelper helper) {
		ServerPlayer player = facingSouth(helper);
		Mob target = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 1, 4));
		player.setItemSlot(EquipmentSlot.MAINHAND, GunStats.build(GunType.MUSKET, IRON, IRON, IRON));
		player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ArsenalItems.DIAL_ROUNDS.get(DialRound.FLAME), 4));

		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getOffhandItem().getCount() == 3, "the musket should fire the Flame Dial Round");
		helper.assertTrue(target.getRemainingFireTicks() > 0, "a Flame Dial Round should set the villager alight");
		helper.succeed();
	}

	private static ServerPlayer facingSouth(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
		player.teleportTo(at.x, at.y, at.z);
		player.setYRot(0.0f);
		player.setXRot(0.0f);
		return player;
	}
}
