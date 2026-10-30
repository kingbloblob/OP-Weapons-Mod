package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.meito.Meito;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.KatanaStats;

/** The materials from the One Piece Mod's islands. Run with ./gradlew runGameTest. */
public class IslandMaterialTests {
	private static final List<String> ISLAND_MATERIALS = List.of("dial_shell", "sea_pearl", "sabaody_resin", "shandora_gold", "kokuto", "dragon_scale");
	private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final List<PartType> SHELLS = List.of(PartType.CROWN, PartType.COAT_PLATES, PartType.SASH_WEAVE, PartType.BOOT_SOLES);

	@GameTest
	public void islandMaterialsComeFromTheMainMod(GameTestHelper helper) {
		for (String id : ISLAND_MATERIALS) {
			ArsenalMaterial material = ArsenalMaterial.byId(id);
			helper.assertTrue(material != null, id + " should be in materials.json");
			helper.assertTrue(material.repairItem() != Items.AIR, id + "'s item should exist in the One Piece Mod");
			helper.assertTrue(BuiltInRegistries.ITEM.getKey(material.repairItem()).getNamespace().equals("onepiece"),
					id + " should be made from a One Piece Mod item");
		}
		helper.succeed();
	}

	@GameTest
	public void kokutoAwakensInHalfTheTime(GameTestHelper helper) {
		ItemStack kokuto = KatanaStats.build(material("kokuto"), ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON);
		ItemStack tamahagane = KatanaStats.build(ArsenalMaterial.TAMAHAGANE, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SHIP_IRON);
		helper.assertTrue(Meito.awakenSeconds(kokuto) == Meito.AWAKEN_SECONDS / 2, "a Kokuto blade should awaken in half the time");
		helper.assertTrue(Meito.awakenSeconds(tamahagane) == Meito.AWAKEN_SECONDS, "other blades should take the full time");
		helper.succeed();
	}

	@GameTest
	public void wornIslandTraitsAddUp(GameTestHelper helper) {
		ArmorStand dragon = fullSet(helper, "dragon_scale", 1);
		ArmorStand resin = fullSet(helper, "sabaody_resin", 3);
		ArmorStand pearl = fullSet(helper, "sea_pearl", 5);
		helper.succeedWhen(() -> {
			check(helper, dragon, Attributes.BURNING_TIME, 0.0, "a full Dragon Scale set shouldn't burn");
			check(helper, resin, Attributes.FALL_DAMAGE_MULTIPLIER, 0.2, "a full Sabaody Resin set should take a fifth of the fall damage");
			check(helper, pearl, Attributes.OXYGEN_BONUS, 3.0, "a full pearl set should breathe like Respiration III");
		});
	}

	private static ArmorStand fullSet(GameTestHelper helper, String id, int x) {
		ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(x, 1, 1));
		for (int i = 0; i < SLOTS.size(); i++) {
			stand.setItemSlot(SLOTS.get(i), ArmorStats.build(SHELLS.get(i), material(id), material(id)));
		}
		return stand;
	}

	private static void check(GameTestHelper helper, ArmorStand stand, Holder<Attribute> attribute, double expected, String message) {
		double value = stand.getAttributeValue(attribute);
		helper.assertTrue(Math.abs(value - expected) < 1e-6, message + " (got " + value + ")");
	}

	private static ArsenalMaterial material(String id) {
		return ArsenalMaterial.byId(id);
	}
}
