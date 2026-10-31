package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.mastery.Mastery;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.salvage.Salvage;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.tool.WeaponType;
import com.onepiecearsenal.upgrade.Upgrades;

/** Weapon mastery ranks and the mastery slot (Mastery). Run with ./gradlew runGameTest. */
public class MasteryTests {
	private static final ArsenalMaterial IRON = ArsenalMaterial.SHIP_IRON;

	/** A Ship Iron cutlass: Rusty, Riveted, Riveted. */
	private static ItemStack cutlass() {
		return WeaponStats.build(WeaponType.CUTLASS, IRON, IRON, IRON);
	}

	private static ItemStack mastered(ItemStack weapon) {
		Mastery.addXp(weapon, Mastery.xpForRank(Mastery.MAX_RANK));
		return weapon;
	}

	private static ItemStack part(PartType type, ArsenalMaterial material) {
		return PartItem.of(ArsenalItems.partItem(type), material);
	}

	@GameTest
	public void weaponsRankUpWithXp(GameTestHelper helper) {
		ItemStack weapon = cutlass();
		helper.assertTrue(Mastery.canMaster(weapon), "a forged cutlass should have mastery");
		helper.assertTrue(Mastery.canMaster(GunStats.build(GunType.FLINTLOCK, IRON, IRON, IRON)), "guns should have mastery");
		helper.assertFalse(Mastery.canMaster(ArmorStats.build(PartType.COAT_PLATES, IRON, IRON)), "armor shouldn't have mastery");
		helper.assertFalse(Mastery.canMaster(new ItemStack(ArsenalItems.KATANA)), "an unforged katana shouldn't have mastery");

		helper.assertTrue(Mastery.rank(weapon) == 0, "a new weapon should be Untested");
		Mastery.addXp(weapon, Mastery.xpForRank(1));
		helper.assertTrue(Mastery.rank(weapon) == 1, "it should be Blooded at " + Mastery.xpForRank(1) + " XP");
		Mastery.addXp(weapon, 100_000);
		helper.assertTrue(Mastery.isMastered(weapon), "lots of XP should master it");
		helper.assertTrue(Mastery.xp(weapon) == Mastery.xpForRank(Mastery.MAX_RANK), "XP should stop at the top rank");
		helper.succeed();
	}

	@GameTest
	public void armedFoesTeachMoreAndFoldedLearnsFaster(GameTestHelper helper) {
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		helper.assertTrue(Mastery.killXp(zombie) == 1, "an unarmed zombie should be worth 1 XP, got " + Mastery.killXp(zombie));
		zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ArsenalItems.KATANA));
		helper.assertTrue(Mastery.killXp(zombie) == 3, "an armed zombie should be worth 3 XP, got " + Mastery.killXp(zombie));
		helper.assertTrue(Mastery.killXp(helper.spawn(EntityTypes.COW, new BlockPos(1, 1, 1))) == 0, "cows teach nothing");

		RandomSource random = RandomSource.create(1);
		helper.assertTrue(Mastery.withFolded(8, false, random) == 8, "without Folded the XP shouldn't change");
		helper.assertTrue(Mastery.withFolded(8, true, random) == 10, "Folded should add a quarter");
		int total = 0;
		for (int i = 0; i < 4000; i++) {
			total += Mastery.withFolded(1, true, random);
		}
		helper.assertTrue(total > 4800 && total < 5200, "Folded should average 1.25 per 1 XP kill, got " + total / 4000.0);
		helper.assertTrue(Trait.FOLDED.active, "Folded should be switched on");
		helper.succeed();
	}

	@GameTest
	public void aMasteredWeaponBindsAPartsTrait(GameTestHelper helper) {
		ItemStack weapon = cutlass();
		helper.assertTrue(Mastery.whyNot(weapon, Trait.KEEN) != null, "an Untested weapon has no mastery slot yet");
		helper.assertFalse(Mastery.bind(weapon, Trait.KEEN), "binding shouldn't work before it's Mastered");

		mastered(weapon);
		helper.assertTrue(Mastery.traitOf(part(PartType.KATANA_BLADE, ArsenalMaterial.TAMAHAGANE)) == Trait.KEEN,
				"a head part should give its head trait");
		helper.assertTrue(Mastery.traitOf(part(PartType.HILT, ArsenalMaterial.TAMAHAGANE)) == Trait.FOLDED,
				"any other part should give its handle trait");
		helper.assertTrue(Mastery.bind(weapon, Trait.KEEN), "a Mastered weapon should take Keen");
		helper.assertTrue(TraitEffects.traits(weapon).contains(Trait.KEEN), "Keen should work in a fight");
		helper.assertTrue(WeaponStats.traits(weapon).contains(Trait.RUSTY), "the parts' traits should stay");
		helper.assertTrue(Mastery.boundTrait(weapon) == Trait.KEEN, "the slot should hold Keen");

		helper.assertTrue(Mastery.bind(weapon, Trait.SPARKING), "binding again should swap the trait");
		helper.assertTrue(TraitEffects.traits(weapon).contains(Trait.SPARKING) && !TraitEffects.traits(weapon).contains(Trait.KEEN),
				"Sparking should replace Keen");
		helper.succeed();
	}

	@GameTest
	public void onlyFightTraitsCanBeBound(GameTestHelper helper) {
		ItemStack weapon = mastered(cutlass());
		helper.assertTrue(Mastery.whyNot(weapon, Trait.RUSTY) != null, "it already has Rusty from its blade");
		helper.assertTrue(Mastery.whyNot(weapon, Trait.LIGHT) != null, "Light only works when forged in");
		helper.assertTrue(Mastery.whyNot(weapon, Trait.WATERPROOF) != null, "Waterproof only works on armor");
		helper.assertTrue(Mastery.whyNot(weapon, Trait.BLACK_BLADE) != null, "Black Blade only comes from awakening");
		helper.assertTrue(Mastery.whyNot(weapon, Trait.ISSUED) != null, "traits that do nothing yet can't be bound");
		helper.assertTrue(Mastery.whyNot(weapon, Trait.SERRATED) == null, "a cutlass can take Serrated");
		ItemStack gun = mastered(GunStats.build(GunType.FLINTLOCK, IRON, IRON, IRON));
		helper.assertTrue(Mastery.whyNot(gun, Trait.SERRATED) != null, "a gun can't use Serrated");
		helper.assertTrue(Mastery.whyNot(gun, Trait.KEEN) == null, "a gun can take Keen");
		helper.succeed();
	}

	@GameTest
	public void theAnvilBindsAndUsesUpThePart(GameTestHelper helper) {
		ServerPlayer smith = helper.makeMockServerPlayerInLevel();
		BlockPos anvil = helper.absolutePos(new BlockPos(1, 1, 1));
		ItemStack katana = mastered(KatanaStats.build(ArsenalMaterial.KAIROSEKI, IRON, IRON));
		smith.setItemInHand(InteractionHand.MAIN_HAND, katana);
		smith.setItemInHand(InteractionHand.OFF_HAND, part(PartType.CUTLASS_BLADE, ArsenalMaterial.TAMAHAGANE));

		InteractionResult result = Upgrades.work(helper.getLevel(), anvil, smith, smith.getMainHandItem());
		helper.assertTrue(result == InteractionResult.SUCCESS, "the anvil should bind the part's trait, got " + result);
		helper.assertTrue(smith.getOffhandItem().isEmpty(), "the part should be used up");
		helper.assertTrue(KatanaStats.traits(smith.getMainHandItem()).contains(Trait.KEEN), "the katana should be Keen now");

		// Not Mastered and worn: the part is left alone so the anvil can mend it instead.
		ItemStack worn = cutlass();
		worn.setDamageValue(10);
		helper.assertTrue(Mastery.work(helper.getLevel(), anvil, smith, worn, part(PartType.HILT, ArsenalMaterial.TAMAHAGANE)) == null,
				"a worn, unmastered weapon should fall through to mending");
		helper.succeed();
	}

	@GameTest
	public void masteryComesBackWhenASalvagedHeadIsReforged(GameTestHelper helper) {
		ItemStack katana = mastered(KatanaStats.build(ArsenalMaterial.KAIROSEKI, IRON, IRON));
		Mastery.bind(katana, Trait.KEEN);
		List<ItemStack> parts = Salvage.parts(katana);
		ItemStack rebuilt = Salvage.restore(parts.get(0), KatanaStats.build(ArsenalMaterial.KAIROSEKI, IRON, ArsenalMaterial.DRIFTWOOD));
		helper.assertTrue(Mastery.isMastered(rebuilt), "the reforged katana should stay Mastered");
		helper.assertTrue(KatanaStats.traits(rebuilt).contains(Trait.KEEN), "it should keep its bound trait");
		helper.succeed();
	}
}
