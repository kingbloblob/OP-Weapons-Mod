package com.onepiecearsenal.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.skill.WeaponSkill;
import com.onepiecearsenal.skill.WeaponSkills;
import com.onepiecearsenal.tool.DialCharge;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.tool.WeaponType;

/** Fishman Gloves and the Skypiea Dial palms (DialCharge). Run with ./gradlew runGameTest. */
public class DialGearTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;
	private static final TagKey<Item> FIST_WEAPONS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("onepiece", "fist_weapons"));
	private static final TagKey<Item> HOLDS_DIAL = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("onepiece", "holds_dial"));

	@GameTest
	public void glovesCountAsFistsForFishManKarate(GameTestHelper helper) {
		ItemStack gloves = WeaponStats.build(WeaponType.FISHMAN_GLOVES, IRON, IRON, IRON);
		helper.assertTrue(gloves.is(FIST_WEAPONS), "Fishman Gloves should be in #onepiece:fist_weapons");
		helper.assertTrue(gloves.is(HOLDS_DIAL), "Fishman Gloves should take the main mod's Dials");
		helper.assertFalse(WeaponStats.build(WeaponType.IMPACT_PALM, IRON, IRON, IRON).is(FIST_WEAPONS), "a Dial palm isn't a fist weapon");
		helper.assertTrue(WeaponSkill.of(gloves) == WeaponSkill.SEA_DRAGON_THROW, "gloves should throw");
		helper.succeed();
	}

	@GameTest
	public void seaDragonThrowSendsTheEnemyOverYourShoulder(GameTestHelper helper) {
		ServerPlayer player = facingSouth(helper);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
		player.setItemSlot(EquipmentSlot.MAINHAND, WeaponStats.build(WeaponType.FISHMAN_GLOVES, IRON, IRON, IRON));
		WeaponSkills.use(player);
		helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "the throw should hurt");
		helper.assertTrue(zombie.getDeltaMovement().z < 0 && zombie.getDeltaMovement().y > 0, "the zombie should fly up and back over the player");
		helper.assertTrue(zombie.hasEffect(MobEffects.SLOWNESS), "a thrown enemy should be left reeling");
		helper.succeed();
	}

	@GameTest
	public void aSneakingDialCatchesBlowsFromTheFront(GameTestHelper helper) {
		ServerPlayer player = facingSouth(helper);
		Zombie front = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
		ItemStack dial = WeaponStats.build(WeaponType.IMPACT_PALM, IRON, IRON, IRON);
		player.setItemSlot(EquipmentSlot.MAINHAND, dial);

		helper.assertFalse(DialCharge.tryCatch(player, player.damageSources().mobAttack(front), 5.0f), "only a sneaking wielder braces the Dial");
		player.setShiftKeyDown(true);
		helper.assertTrue(DialCharge.tryCatch(player, player.damageSources().mobAttack(front), 5.0f), "the Dial should catch a blow from the front");
		helper.assertTrue(DialCharge.stored(dial) == 5.0f, "the Dial should hold 5, holds " + DialCharge.stored(dial));
		helper.assertTrue(dial.getDamageValue() == 1, "catching a blow should wear the Dial by 1");

		Zombie behind = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 0));
		helper.assertFalse(DialCharge.tryCatch(player, player.damageSources().mobAttack(behind), 5.0f), "a blow from behind isn't caught");

		DialCharge.set(dial, DialCharge.CAPACITY);
		helper.assertFalse(DialCharge.tryCatch(player, player.damageSources().mobAttack(front), 5.0f), "a full Dial catches nothing");
		helper.assertFalse(DialCharge.tryCatch(player, player.damageSources().fall(), 5.0f), "falls aren't blows");
		helper.succeed();
	}

	@GameTest
	public void impactLetsOutWhatItCaught(GameTestHelper helper) {
		ServerPlayer player = facingSouth(helper);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
		ItemStack dial = WeaponStats.build(WeaponType.IMPACT_PALM, IRON, IRON, IRON);
		player.setItemSlot(EquipmentSlot.MAINHAND, dial);

		WeaponSkills.use(player);
		helper.assertFalse(player.getCooldowns().isOnCooldown(dial), "an empty Dial shouldn't fire");
		helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "an empty Dial shouldn't hurt");

		DialCharge.set(dial, 12.0f);
		WeaponSkills.use(player);
		helper.assertTrue(zombie.getMaxHealth() - zombie.getHealth() >= 12.0f || !zombie.isAlive(),
				"Impact should let out at least the 12 it caught, did " + (zombie.getMaxHealth() - zombie.getHealth()));
		helper.assertTrue(DialCharge.stored(dial) == 0.0f, "the Dial should be empty after Impact");
		helper.assertTrue(player.getCooldowns().isOnCooldown(dial), "Impact should start its cooldown");
		helper.succeed();
	}

	@GameTest
	public void rejectHitsThreeTimesAsHardAndKicksBack(GameTestHelper helper) {
		ItemStack impact = WeaponStats.build(WeaponType.IMPACT_PALM, IRON, IRON, IRON);
		ItemStack reject = WeaponStats.build(WeaponType.REJECT_PALM, IRON, IRON, IRON);
		helper.assertTrue(DialCharge.power(reject, 10.0f) == 3 * DialCharge.power(impact, 10.0f), "Reject should let out three times as much");
		helper.assertTrue(DialCharge.recoil(reject, 10.0f) > DialCharge.recoil(impact, 10.0f), "Reject should kick back harder");

		ServerPlayer player = facingSouth(helper);
		helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
		player.setItemSlot(EquipmentSlot.MAINHAND, reject);
		player.setHealth(2.0f);
		DialCharge.set(reject, DialCharge.CAPACITY);
		WeaponSkills.use(player);
		helper.assertTrue(player.isAlive() && player.getHealth() >= 1.0f, "Reject's kick should never take the last half heart");
		helper.succeed();
	}

	/** A player standing at (1, 1, 1) of the test, looking straight south (toward +z). */
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
