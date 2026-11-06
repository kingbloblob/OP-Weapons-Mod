package com.onepiecearsenal.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.salvage.Salvage;
import com.onepiecearsenal.station.ShipwrightsAnvilBlock;
import com.onepiecearsenal.tool.GauntletStats;
import com.onepiecearsenal.tool.ShieldStats;
import com.onepiecearsenal.upgrade.Upgrade;
import com.onepiecearsenal.upgrade.Upgrades;

/** Shields and gauntlets (ShieldStats, GauntletStats). Run with ./gradlew runGameTest. */
public class ShieldGauntletTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;
	private static final ArsenalMaterial KAIROSEKI = ArsenalMaterial.KAIROSEKI;

	@GameTest
	public void shieldBlocksWithItsPartsNumbers(GameTestHelper helper) {
		ItemStack shield = ShieldStats.build(KAIROSEKI, ArsenalMaterial.DRIFTWOOD, IRON);
		BlocksAttacks blocks = shield.get(DataComponents.BLOCKS_ATTACKS);
		helper.assertTrue(blocks != null, "a forged shield should block");
		helper.assertTrue(blocks.damageReductions().get(0).horizontalBlockingAngle() == 110.0f,
				"a tier 3 face should cover 110 degrees, got " + blocks.damageReductions().get(0).horizontalBlockingAngle());
		helper.assertTrue(blocks.blockDelaySeconds() < 0.25f, "a light Driftwood rim should raise it faster than vanilla, got " + blocks.blockDelaySeconds());
		helper.assertTrue(blocks.disableCooldownScale() < 1.0f, "axes should knock a Kairoseki face down for less time than vanilla");
		int expected = Math.round((KAIROSEKI.durability + ArsenalMaterial.DRIFTWOOD.durability + IRON.durability) / 3.0f * ShieldStats.DURABILITY_SCALE);
		helper.assertTrue(shield.getMaxDamage() >= expected, "durability should be " + expected + " or more, got " + shield.getMaxDamage());
		helper.assertFalse(new ItemStack(ArsenalItems.SHIELD).has(DataComponents.BLOCKS_ATTACKS), "an unforged shield shouldn't block");
		helper.succeed();
	}

	@GameTest
	public void anvilForgesShieldsAndGauntlets(GameTestHelper helper) {
		ServerPlayer smith = helper.makeMockServerPlayerInLevel();
		smith.getInventory().add(PartItem.of(ArsenalItems.SHIELD_FACE, IRON));
		smith.getInventory().add(PartItem.of(ArsenalItems.BINDING, IRON));
		smith.getInventory().add(PartItem.of(ArsenalItems.GRIP, ArsenalMaterial.DRIFTWOOD));
		smith.getInventory().add(PartItem.of(ArsenalItems.GAUNTLET_PLATES, ArsenalMaterial.SEA_KING_HIDE));
		smith.getInventory().add(PartItem.of(ArsenalItems.LINING, IRON));
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		ShipwrightsAnvilBlock.forge(helper.getLevel(), pos, smith, false, false);
		ShipwrightsAnvilBlock.forge(helper.getLevel(), pos, smith, false, false);
		helper.assertTrue(smith.getInventory().countItem(ArsenalItems.SHIELD) == 1, "it should forge a shield");
		helper.assertTrue(smith.getInventory().countItem(ArsenalItems.GAUNTLETS) == 1, "it should forge gauntlets, even from a hide");
		helper.assertTrue(smith.getInventory().countItem(ArsenalItems.SHIELD_FACE) == 0, "the parts should be used up");
		helper.succeed();
	}

	@GameTest
	public void gauntletsAreArmorInTheOffhand(GameTestHelper helper) {
		ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 1, 1));
		stand.setItemSlot(EquipmentSlot.OFFHAND, GauntletStats.build(KAIROSEKI, IRON));
		double armor = GauntletStats.defense(KAIROSEKI);
		helper.succeedWhen(() -> {
			helper.assertTrue(stand.getAttributeValue(Attributes.ARMOR) == armor,
					"Kairoseki gauntlets should give " + armor + " armor, got " + stand.getAttributeValue(Attributes.ARMOR));
			helper.assertTrue(stand.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 1.0,
					"tier 3 plates should give 1 toughness, got " + stand.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		});
	}

	@GameTest
	public void guardOnlyCoversTheFrontWhileSneaking(GameTestHelper helper) {
		ServerPlayer fighter = helper.makeMockServerPlayerInLevel();
		fighter.setItemSlot(EquipmentSlot.OFFHAND, GauntletStats.build(IRON, IRON));
		Vec3 ahead = fighter.position().add(fighter.getViewVector(1.0f).multiply(2, 0, 2));
		Vec3 behind = fighter.position().subtract(fighter.getViewVector(1.0f).multiply(2, 0, 2));
		Zombie front = helper.spawn(EntityTypes.ZOMBIE, BlockPos.containing(helper.relativeVec(ahead)));
		Zombie back = helper.spawn(EntityTypes.ZOMBIE, BlockPos.containing(helper.relativeVec(behind)));
		front.setPos(ahead);
		back.setPos(behind);

		helper.assertTrue(GauntletStats.guard(fighter, fighter.damageSources().mobAttack(front), 8.0f) == 8.0f,
				"no guard without sneaking");
		fighter.setShiftKeyDown(true);
		float guarded = GauntletStats.guard(fighter, fighter.damageSources().mobAttack(front), 8.0f);
		helper.assertTrue(guarded == 6.0f, "a sneaking guard should take a quarter off a hit from the front, got " + guarded);
		helper.assertTrue(GauntletStats.guard(fighter, fighter.damageSources().mobAttack(back), 8.0f) == 8.0f,
				"the guard shouldn't cover hits from behind");
		helper.succeed();
	}

	@GameTest
	public void spikedShieldPricksWhoeverHitsIt(GameTestHelper helper) {
		ServerPlayer blocker = helper.makeMockServerPlayerInLevel();
		ItemStack shield = ShieldStats.build(ArsenalMaterial.byId("prismarine"), IRON, IRON);
		helper.assertTrue(Upgrades.add(shield, Upgrade.byId("spiked_boss")), "Spiked Boss should fit a shield");
		Zombie attacker = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		float before = attacker.getHealth();
		ShieldStats.counter(blocker, helper.getLevel(), attacker, shield);
		// Spined (2) + Spiked Boss I (1.5).
		helper.assertTrue(attacker.getHealth() <= before - 3.0f, "the counter should prick for 3.5, health went " + before + " -> " + attacker.getHealth());
		helper.succeed();
	}

	@GameTest
	public void offhandGearTakesUpgradesAndComesApart(GameTestHelper helper) {
		ItemStack gauntlets = GauntletStats.build(IRON, IRON);
		helper.assertTrue(Upgrades.add(gauntlets, Upgrade.byId("hull_plating")), "Hull Plating should fit gauntlets");
		helper.assertTrue(Upgrades.whyNot(GauntletStats.build(IRON, IRON), Upgrade.byId("spiked_boss")) != null, "Spiked Boss shouldn't fit gauntlets");
		ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 1, 1));
		stand.setItemSlot(EquipmentSlot.OFFHAND, gauntlets);
		double armor = GauntletStats.defense(IRON) + 1.0;

		var parts = Salvage.parts(ShieldStats.build(IRON, ArsenalMaterial.DRIFTWOOD, IRON));
		helper.assertTrue(parts.size() == 3, "a shield should come apart into 3 parts, got " + parts.size());
		helper.assertTrue(parts.get(0).is(ArsenalItems.SHIELD_FACE) && parts.get(1).is(ArsenalItems.BINDING) && parts.get(2).is(ArsenalItems.GRIP),
				"a shield should come apart into its face, rim and strap");
		helper.succeedWhen(() -> helper.assertTrue(stand.getAttributeValue(Attributes.ARMOR) == armor,
				"upgraded gauntlets should give " + armor + " armor in the offhand, got " + stand.getAttributeValue(Attributes.ARMOR)));
	}
}
