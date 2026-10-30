package com.onepiecearsenal.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;

/** Parts made from plain Minecraft materials, and part recipes that don't clash with vanilla ones. Run with ./gradlew runGameTest. */
public class VanillaMaterialTests {
	private static final List<String> VANILLA_MATERIALS = List.of("flint", "stone", "copper", "amethyst", "emerald", "prismarine",
			"obsidian", "diamond", "netherite");
	private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final List<PartType> SHELLS = List.of(PartType.CROWN, PartType.COAT_PLATES, PartType.SASH_WEAVE, PartType.BOOT_SOLES);

	@GameTest
	public void vanillaMaterialsComeFromVanillaItems(GameTestHelper helper) {
		for (String id : VANILLA_MATERIALS) {
			ArsenalMaterial material = ArsenalMaterial.byId(id);
			helper.assertTrue(material != null, id + " should be in materials.json");
			helper.assertTrue(material.repairItem() != Items.AIR, id + "'s item should exist");
			helper.assertTrue(BuiltInRegistries.ITEM.getKey(material.repairItem()).getNamespace().equals("minecraft"),
					id + " should be made from a vanilla item");
			helper.assertTrue(material.craftable && !material.armorOnly, id + " should make every part at a crafting table");
		}
		helper.succeed();
	}

	/** Every Arsenal part recipe, filled in with its own ingredients, must make only that part. */
	@GameTest
	public void partRecipesDontClashWithOtherRecipes(GameTestHelper helper) {
		List<RecipeHolder<?>> recipes = List.copyOf(helper.getLevel().getServer().getRecipeManager().getRecipes());
		for (RecipeHolder<?> holder : recipes) {
			if (!holder.id().identifier().getNamespace().equals("arsenal") || !(holder.value() instanceof ShapedRecipe shaped)) {
				continue;
			}
			List<ItemStack> grid = new ArrayList<>();
			for (Optional<Ingredient> slot : shaped.getIngredients()) {
				grid.add(slot.map(ingredient -> new ItemStack(ingredient.items().findFirst().orElseThrow())).orElse(ItemStack.EMPTY));
			}
			CraftingInput input = CraftingInput.of(shaped.getWidth(), shaped.getHeight(), grid);
			for (RecipeHolder<?> other : recipes) {
				if (other != holder && other.value() instanceof CraftingRecipe crafting && crafting.matches(input, helper.getLevel())) {
					helper.fail(holder.id().identifier() + " is the same shape as " + other.id().identifier());
				}
			}
		}
		helper.succeed();
	}

	@GameTest
	public void wornVanillaTraitsAddUp(GameTestHelper helper) {
		ArmorStand diamond = fullSet(helper, "diamond", 1);
		ArmorStand netherite = fullSet(helper, "netherite", 3);
		ArmorStand obsidian = fullSet(helper, "obsidian", 5);
		helper.succeedWhen(() -> {
			// Tier toughness (1 a piece at tier 3, 2 at tier 4) plus 1 a piece from the trait, like vanilla diamond and netherite.
			check(helper, diamond, Attributes.ARMOR_TOUGHNESS, 4 * (ArmorStats.toughness(material("diamond")) + 1.0), "a full diamond set's toughness");
			check(helper, netherite, Attributes.ARMOR_TOUGHNESS, 4 * (ArmorStats.toughness(material("netherite")) + 1.0), "a full netherite set's toughness");
			check(helper, netherite, Attributes.KNOCKBACK_RESISTANCE, 0.4, "a full netherite set should resist knockback like netherite armor");
			check(helper, obsidian, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0, "a full obsidian set shouldn't be blown back");
		});
	}

	private static ArmorStand fullSet(GameTestHelper helper, String id, int x) {
		ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(x, 1, 1));
		for (int i = 0; i < SLOTS.size(); i++) {
			// A Driftwood lining, so only the shell's trait counts.
			stand.setItemSlot(SLOTS.get(i), ArmorStats.build(SHELLS.get(i), material(id), ArsenalMaterial.DRIFTWOOD));
		}
		return stand;
	}

	private static void check(GameTestHelper helper, ArmorStand stand, Holder<Attribute> attribute, double expected, String message) {
		double value = stand.getAttributeValue(attribute);
		helper.assertTrue(Math.abs(value - expected) < 1e-6, message + " (got " + value + ", expected " + expected + ")");
	}

	private static ArsenalMaterial material(String id) {
		return ArsenalMaterial.byId(id);
	}
}
