package com.onepiecearsenal.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.fruit.Beast;
import com.onepiecearsenal.fruit.FamousFruitWeapon;
import com.onepiecearsenal.fruit.FruitWeapons;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.skill.WeaponSkills;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.tool.WeaponType;

/** Devil Fruit weapons (fruit/FruitWeapons). Run with ./gradlew runGameTest. */
public class FruitWeaponTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;

	@GameTest
	public void aWeaponEatsAZoanFruitAtTheAnvil(GameTestHelper helper) {
		ServerPlayer player = facingSouth(helper);
		ItemStack cutlass = WeaponStats.build(WeaponType.CUTLASS, IRON, IRON, IRON);
		player.setItemSlot(EquipmentSlot.MAINHAND, cutlass);
		player.setItemInHand(InteractionHand.OFF_HAND, fruit("inu_inu_no_mi_wolf"));

		FruitWeapons.feed(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), player, cutlass, player.getOffhandItem());
		helper.assertTrue("inu_inu_no_mi_wolf".equals(FruitWeapons.fruitOf(cutlass)), "the cutlass should have eaten the wolf fruit");
		helper.assertTrue(player.getOffhandItem().isEmpty(), "the fruit should be eaten");
		helper.assertTrue(Beast.of("inu_inu_no_mi_wolf") == Beast.DOG, "a wolf fruit makes a dog");
		helper.succeed();
	}

	@GameTest
	public void onlyOneZoanAndNeverKairoseki(GameTestHelper helper) {
		ItemStack cutlass = WeaponStats.build(WeaponType.CUTLASS, IRON, IRON, IRON);
		helper.assertTrue(FruitWeapons.whyNot(cutlass, "mera_mera_no_mi") != null, "a Logia fruit shouldn't go in a weapon");
		helper.assertTrue(FruitWeapons.whyNot(cutlass, "gomu_gomu_no_mi") != null, "a Paramecia fruit shouldn't go in a weapon");
		helper.assertTrue(FruitWeapons.whyNot(cutlass, "zou_zou_no_mi") == null, "a Zoan fruit should go in a weapon");
		helper.assertTrue(FruitWeapons.whyNot(GunStats.build(GunType.FLINTLOCK, IRON, IRON, IRON), "zou_zou_no_mi") == null, "guns eat fruits too");

		ItemStack seastone = WeaponStats.build(WeaponType.JITTE, ArsenalMaterial.KAIROSEKI, IRON, IRON);
		helper.assertTrue(FruitWeapons.whyNot(seastone, "zou_zou_no_mi") != null, "a Kairoseki weapon should refuse a fruit");
		ItemStack fed = FamousFruitWeapon.FUNKFREED.create();
		helper.assertTrue(FruitWeapons.whyNot(fed, "inu_inu_no_mi_wolf") != null, "a weapon should only eat one fruit");
		helper.succeed();
	}

	@GameTest
	public void famousWeaponsComeWithTheirFruitAndLook(GameTestHelper helper) {
		for (FamousFruitWeapon famous : FamousFruitWeapon.values()) {
			ItemStack stack = famous.create();
			helper.assertTrue(famous.fruitId.equals(FruitWeapons.fruitOf(stack)), famous.displayName + " should hold its fruit");
			helper.assertTrue(FruitWeapons.famousOf(stack) == famous, famous.displayName + " should be recognised");
			helper.assertFalse(FruitWeapons.fruitName(famous.fruitId).equals(famous.fruitId), famous.displayName + "'s fruit should have a name");
		}
		ItemStack playerFed = WeaponStats.build(WeaponType.MARINE_SABRE, IRON, IRON, IRON);
		playerFed.set(com.onepiecearsenal.ArsenalComponents.EATEN_FRUIT, "zou_zou_no_mi");
		helper.assertTrue(FruitWeapons.famousOf(playerFed) == null, "a sabre a player fed the Zou Zou no Mi isn't Funkfreed");
		helper.succeed();
	}

	@GameTest
	public void sneakAndSkillWakesItAndItBites(GameTestHelper helper) {
		ServerPlayer player = facingSouth(helper);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
		ItemStack funkfreed = FamousFruitWeapon.FUNKFREED.create();
		player.setItemSlot(EquipmentSlot.MAINHAND, funkfreed);
		player.setShiftKeyDown(true);

		WeaponSkills.use(player);
		helper.assertTrue(FruitWeapons.isAwake(player), "Sneak + X should wake Funkfreed");
		helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "the elephant's Trunk Sweep should hit the zombie in front");
		helper.assertTrue(funkfreed.getDamageValue() == 4, "waking it should cost 4 uses, cost " + funkfreed.getDamageValue());
		helper.assertFalse(player.getCooldowns().isOnCooldown(funkfreed), "waking it shouldn't use up the sabre's own skill");

		// Put it away and it falls asleep on the next tick.
		player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(FruitWeapons.isAwake(player), "it should sleep once it's put away");
			helper.succeed();
		});
	}

	private static ItemStack fruit(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("onepiece", id)));
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
