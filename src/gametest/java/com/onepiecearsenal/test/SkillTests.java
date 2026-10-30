package com.onepiecearsenal.test;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.skill.WeaponSkill;
import com.onepiecearsenal.skill.WeaponSkills;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.tool.WeaponType;

/** Weapon skills on the X key (WeaponSkill, WeaponSkills). Run with ./gradlew runGameTest. */
public class SkillTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;

	@GameTest
	public void everyHandWeaponHasItsOwnSkill(GameTestHelper helper) {
		Set<WeaponSkill> seen = EnumSet.of(WeaponSkill.of(KatanaStats.build(IRON, IRON, IRON)));
		for (WeaponType type : WeaponType.values()) {
			WeaponSkill skill = WeaponSkill.of(WeaponStats.build(type, IRON, IRON, IRON));
			helper.assertTrue(skill != null, type.displayName + " should have a skill");
			helper.assertTrue(seen.add(skill), type.displayName + " shouldn't share its skill with another weapon");
		}
		helper.assertTrue(seen.size() == WeaponSkill.values().length, "every skill should belong to a weapon");
		helper.assertTrue(WeaponSkill.of(GunStats.build(GunType.FLINTLOCK, IRON, IRON, IRON)) == null, "guns have no skill");
		helper.succeed();
	}

	@GameTest
	public void cleaveHitsTheEnemyInFrontAndCostsItsPrice(GameTestHelper helper) {
		ServerPlayer player = facingSouth(helper);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
		ItemStack axe = WeaponStats.build(WeaponType.BOARDING_AXE, IRON, IRON, IRON);
		player.setItemSlot(EquipmentSlot.MAINHAND, axe);

		WeaponSkills.use(player);
		helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "Cleave should hurt the zombie in front");
		helper.assertTrue(player.getCooldowns().isOnCooldown(axe), "Cleave should start the axe's cooldown");
		helper.assertTrue(axe.getDamageValue() == 2, "a skill should cost 2 uses, cost " + axe.getDamageValue());

		float health = zombie.getHealth();
		zombie.damageCooldownTime = 0;
		WeaponSkills.use(player);
		helper.assertTrue(zombie.getHealth() == health, "Cleave shouldn't work again while it's cooling down");
		helper.succeed();
	}

	@GameTest
	public void theCooldownDoesNotStopNormalHits(GameTestHelper helper) {
		ServerPlayer player = facingSouth(helper);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
		ItemStack katana = KatanaStats.build(IRON, IRON, IRON);
		player.setItemSlot(EquipmentSlot.MAINHAND, katana);
		player.getCooldowns().addCooldown(katana, WeaponSkill.IAI_DRAW.cooldownTicks);
		player.attack(zombie);
		helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "a katana whose skill is cooling down should still cut");
		helper.succeed();
	}

	@GameTest
	public void aTargetedSkillWithNothingInReachIsFree(GameTestHelper helper) {
		ServerPlayer player = facingSouth(helper);
		ItemStack jitte = WeaponStats.build(WeaponType.JITTE, IRON, IRON, IRON);
		player.setItemSlot(EquipmentSlot.MAINHAND, jitte);
		WeaponSkills.use(player);
		helper.assertFalse(player.getCooldowns().isOnCooldown(jitte), "Arrest Hook with no one to hook shouldn't start a cooldown");
		helper.assertTrue(jitte.getDamageValue() == 0, "Arrest Hook with no one to hook shouldn't wear the jitte");
		helper.succeed();
	}

	@GameTest
	public void arrestHookDragsTheEnemyIn(GameTestHelper helper) {
		ServerPlayer player = facingSouth(helper);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 5));
		player.setItemSlot(EquipmentSlot.MAINHAND, WeaponStats.build(WeaponType.JITTE, IRON, IRON, IRON));
		WeaponSkills.use(player);
		helper.assertTrue(zombie.getDeltaMovement().z < 0, "the hooked zombie should be pulled north, toward the player");
		helper.assertTrue(zombie.hasEffect(MobEffects.SLOWNESS), "the hooked zombie should be slowed");
		helper.succeed();
	}

	/** A player standing at (1, 1, 1) of the test, looking straight south (toward +z). */
	private static ServerPlayer facingSouth(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
		player.teleportTo(at.x, at.y, at.z);
		player.setYRot(0.0f);
		player.setXRot(0.0f);
		return player;
	}
}
