package com.onepiecearsenal.test;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.meito.BladeRecord;
import com.onepiecearsenal.meito.Meito;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.Restat;
import com.onepiecearsenal.upgrade.Upgrade;
import com.onepiecearsenal.upgrade.Upgrades;
import com.onepiecemod.weapon.WeaponUpgrades;

/** Gear forged before a balance change catching up with the new numbers (Restat). Run with ./gradlew runGameTest. */
public class RestatTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;

	@GameTest
	public void oldKatanaCatchesUpAndKeepsTheRest(GameTestHelper helper) {
		ItemStack today = KatanaStats.build(ArsenalMaterial.byId("tamahagane"), IRON, ArsenalMaterial.DRIFTWOOD);
		WeaponUpgrades.setGrade(today, 2);
		Upgrades.add(today, Upgrade.byId("honed_edge"));
		Upgrades.add(today, Upgrade.byId("reinforced"));
		today.set(ArsenalComponents.BLADE_RECORD, new BladeRecord(900, 30, 1));
		today.set(DataComponents.CUSTOM_NAME, Component.literal("Sea Cutter"));

		// The same blade as it was before the balance pass: 2 more damage, 600 uses, half worn.
		ItemStack old = today.copy();
		old.remove(ArsenalComponents.STATS_STAMP);
		bump(old, Attributes.ATTACK_DAMAGE, ArsenalMod.id("katana_damage"), 2.0);
		old.set(DataComponents.MAX_DAMAGE, 600);
		old.setDamageValue(300);

		helper.assertTrue(Restat.refresh(old), "an old katana should need a refresh");
		helper.assertFalse(Restat.refresh(old), "a refreshed katana shouldn't need another");
		helper.assertTrue(Math.abs(total(old, Attributes.ATTACK_DAMAGE) - total(today, Attributes.ATTACK_DAMAGE)) < 0.001,
				"it should hit like one forged today (expected " + total(today, Attributes.ATTACK_DAMAGE) + ", got " + total(old, Attributes.ATTACK_DAMAGE) + ")");
		helper.assertTrue(old.getMaxDamage() == today.getMaxDamage(),
				"it should last like one forged today, Reinforced included (expected " + today.getMaxDamage() + ", got " + old.getMaxDamage() + ")");
		helper.assertTrue(Math.abs(old.getDamageValue() - today.getMaxDamage() / 2) <= 1, "it should stay half worn, got " + old.getDamageValue());
		helper.assertTrue(WeaponUpgrades.grade(old) == 2, "it should keep its grade");
		helper.assertTrue(Upgrades.level(old, Upgrade.byId("honed_edge")) == 1, "it should keep its upgrades");
		helper.assertTrue(Meito.record(old).xp() == 900, "it should keep its blade level");
		helper.assertTrue(old.getHoverName().getString().equals("Sea Cutter"), "it should keep its name");
		helper.succeed();
	}

	@GameTest
	public void oldCoatCatchesUp(GameTestHelper helper) {
		ArsenalMaterial kairoseki = ArsenalMaterial.KAIROSEKI;
		ItemStack coat = ArmorStats.build(PartType.COAT_PLATES, kairoseki, IRON);
		Upgrades.add(coat, Upgrade.byId("hull_plating"));
		coat.remove(ArsenalComponents.STATS_STAMP);
		bump(coat, Attributes.ARMOR, ArsenalMod.id("armor_plates.chest"), 3.0);
		coat.set(DataComponents.MAX_DAMAGE, 999);

		helper.assertTrue(Restat.refresh(coat), "an old coat should need a refresh");
		double expected = ArmorStats.defense(kairoseki, ArmorStats.typeOf(PartType.COAT_PLATES)) + 1;
		helper.assertTrue(total(coat, Attributes.ARMOR) == expected,
				"it should give today's defense plus its Hull Plating (expected " + expected + ", got " + total(coat, Attributes.ARMOR) + ")");
		int durability = Upgrades.durability(coat, ArmorStats.durability(kairoseki, IRON, ArmorStats.typeOf(PartType.COAT_PLATES)));
		helper.assertTrue(coat.getMaxDamage() == durability, "it should last like one forged today (expected " + durability + ", got " + coat.getMaxDamage() + ")");
		helper.succeed();
	}

	/** Adds to one modifier's amount, as if the item had been forged with bigger numbers. */
	private static void bump(ItemStack stack, Holder<Attribute> attribute, Identifier id, double extra) {
		ItemAttributeModifiers modifiers = ItemAttributeModifiers.EMPTY;
		for (ItemAttributeModifiers.Entry entry : stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers()) {
			AttributeModifier modifier = entry.modifier();
			if (entry.attribute().is(attribute) && modifier.is(id)) {
				modifier = new AttributeModifier(id, modifier.amount() + extra, modifier.operation());
			}
			modifiers = modifiers.withModifierAdded(entry.attribute(), modifier, entry.slot());
		}
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
	}

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
