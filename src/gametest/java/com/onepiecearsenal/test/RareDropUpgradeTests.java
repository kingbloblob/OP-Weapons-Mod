package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.upgrade.Upgrade;
import com.onepiecearsenal.upgrade.Upgrades;
import com.onepiecemod.weapon.GunItem;

/** The upgrades made from the main mod's rare drops (Sea King Fang, Ambergris, Dragon Heartstone, Pacifista Core). */
public class RareDropUpgradeTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;

	@GameTest
	public void rareDropsAreRealItems(GameTestHelper helper) {
		for (String id : List.of("sea_king_fang", "ambergris", "dragon_heartstone", "pacifista_core")) {
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("onepiece", id)),
					"the One Piece Mod should have onepiece:" + id + " (a jar from before its rare drops?)");
		}
		helper.succeed();
	}

	@GameTest
	public void fangEdgeHitsHarderAndBitesSeaCreatures(GameTestHelper helper) {
		ItemStack katana = KatanaStats.build(IRON, IRON, IRON);
		double before = total(katana, Attributes.ATTACK_DAMAGE);
		Upgrade fang = Upgrade.byId("fang_edge");
		helper.assertTrue(Upgrades.add(katana, fang), "Fang Edge should go on a katana");
		helper.assertTrue(Math.abs(total(katana, Attributes.ATTACK_DAMAGE) - before - 1.0) < 0.001, "Fang Edge I should add 1 damage");
		helper.assertTrue(Upgrades.total(katana, "sea_slayer") == 3.0, "Fang Edge I should add 3 against sea creatures");
		helper.assertTrue(Upgrades.whyNot(GunStats.build(GunType.FLINTLOCK, IRON, IRON, IRON), fang) != null, "Fang Edge doesn't fit guns");
		helper.succeed();
	}

	@GameTest
	public void pacifistaCoreWorksDifferentlyOnGunsAndArmor(GameTestHelper helper) {
		Upgrade core = Upgrade.byId("pacifista_core");
		helper.assertTrue(core.fitsName().equals("Guns, armor and gauntlets"), "the manual should say what it fits, got " + core.fitsName());

		ItemStack gun = GunStats.build(GunType.FLINTLOCK, IRON, IRON, IRON);
		GunItem.Stats before = GunStats.stats(gun);
		helper.assertTrue(Upgrades.add(gun, core), "Pacifista Core should go on a gun");
		GunItem.Stats after = GunStats.stats(gun);
		helper.assertTrue(Math.abs(after.damage() - before.damage() - 2.0f) < 0.001, "a cored gun should do 2 more per shot");
		helper.assertTrue(after.cooldown() < before.cooldown(), "a cored gun should reload faster");
		helper.assertTrue(total(gun, Attributes.KNOCKBACK_RESISTANCE) == 0, "a gun shouldn't get the armor's knockback resistance");

		ItemStack coat = ArmorStats.build(PartType.COAT_PLATES, IRON, IRON);
		double knockback = total(coat, Attributes.KNOCKBACK_RESISTANCE);
		helper.assertTrue(Upgrades.add(coat, core), "Pacifista Core should go on a coat");
		helper.assertTrue(Math.abs(total(coat, Attributes.KNOCKBACK_RESISTANCE) - knockback - 0.1) < 0.001, "a cored coat should add 0.1 knockback resistance");
		helper.assertTrue(Upgrades.total(coat, "gun_damage") == 0, "a coat shouldn't count the gun's damage");
		helper.assertFalse(Upgrades.add(coat, core), "Pacifista Core stops at level I");
		helper.succeed();
	}

	@GameTest
	public void pacifistaCoreArmorStopsShots(GameTestHelper helper) {
		Player wearer = helper.makeMockPlayer(GameType.SURVIVAL);
		Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
		List<EquipmentSlot> slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
		List<PartType> shells = List.of(PartType.CROWN, PartType.COAT_PLATES, PartType.SASH_WEAVE, PartType.BOOT_SOLES);
		for (int i = 0; i < slots.size(); i++) {
			ItemStack piece = ArmorStats.build(shells.get(i), IRON, IRON);
			Upgrades.add(piece, Upgrade.byId("pacifista_core"));
			wearer.setItemSlot(slots.get(i), piece);
		}
		// The main mod's bullets are thrown damage, which counts as a projectile.
		float shot = Upgrades.guard(wearer, wearer.damageSources().thrown(shooter, shooter), 10.0f);
		helper.assertTrue(Math.abs(shot - 6.8f) < 0.001, "four cored pieces should take 32% off a shot, got " + shot);
		float punch = Upgrades.guard(wearer, wearer.damageSources().playerAttack(shooter), 10.0f);
		helper.assertTrue(punch == 10.0f, "Pacifista Core armor shouldn't change a punch, got " + punch);
		helper.succeed();
	}

	@GameTest
	public void heartstoneCoreNeedsAGrandLineAnvil(GameTestHelper helper) {
		ServerPlayer smith = helper.makeMockServerPlayerInLevel();
		BlockPos anvil = helper.absolutePos(new BlockPos(1, 1, 1));
		smith.setItemSlot(EquipmentSlot.MAINHAND, KatanaStats.build(IRON, IRON, IRON));
		smith.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(item("dragon_heartstone")));
		Upgrade heart = Upgrade.byId("heartstone_core");

		helper.assertTrue(Upgrades.work(helper.getLevel(), anvil, smith, smith.getMainHandItem(), false) == InteractionResult.FAIL,
				"a Shipwright's Anvil shouldn't add Heartstone Core");
		helper.assertTrue(Upgrades.level(smith.getMainHandItem(), heart) == 0 && smith.getOffhandItem().getCount() == 1,
				"a refused Heartstone Core shouldn't change the katana or take the heartstone");

		helper.assertTrue(Upgrades.work(helper.getLevel(), anvil, smith, smith.getMainHandItem(), true) == InteractionResult.SUCCESS,
				"a Grand Line Anvil should add Heartstone Core");
		ItemStack katana = smith.getMainHandItem();
		helper.assertTrue(Upgrades.level(katana, heart) == 1 && smith.getOffhandItem().isEmpty(), "it should be Heartstone Core I, taking the heartstone");
		helper.assertTrue(katana.has(DataComponents.DAMAGE_RESISTANT), "a Heartstone katana should be fireproof");
		helper.assertTrue(Upgrades.total(katana, "ignite") == 4.0, "a Heartstone katana should set targets on fire for 4 seconds");
		helper.succeed();
	}

	@GameTest
	public void ambergrisPolishMendsOverTime(GameTestHelper helper) {
		ItemStack katana = KatanaStats.build(IRON, IRON, IRON);
		Upgrades.add(katana, Upgrade.byId("ambergris_polish"));
		katana.setDamageValue(10);
		Upgrades.tick(katana, Upgrades.MEND_INTERVAL + 1);
		helper.assertTrue(katana.getDamageValue() == 10, "it should only mend once every five seconds");
		Upgrades.tick(katana, Upgrades.MEND_INTERVAL * 2);
		helper.assertTrue(katana.getDamageValue() == 8, "it should mend 2 durability, damage is " + katana.getDamageValue());

		katana.setDamageValue(katana.getMaxDamage() - 1);
		Upgrades.tick(katana, Upgrades.MEND_INTERVAL * 3);
		helper.assertTrue(katana.getDamageValue() == katana.getMaxDamage() - 1, "broken gear should still need the anvil");

		ItemStack plain = KatanaStats.build(IRON, IRON, IRON);
		plain.setDamageValue(10);
		Upgrades.tick(plain, Upgrades.MEND_INTERVAL);
		helper.assertTrue(plain.getDamageValue() == 10, "gear without the polish shouldn't mend itself");
		helper.succeed();
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("onepiece", id));
	}

	/** The item's total bonus to one attribute from its own modifiers. */
	private static double total(ItemStack stack, Holder<Attribute> attribute) {
		double total = 0;
		for (ItemAttributeModifiers.Entry entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			if (entry.attribute().is(attribute)) {
				total += entry.modifier().amount();
			}
		}
		return total;
	}
}
