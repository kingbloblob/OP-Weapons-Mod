package com.onepiecearsenal.fruit;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.tool.WeaponType;

/**
 * The famous weapons from One Piece that ate a Devil Fruit. Each is ordinary forged Arsenal gear (it repairs,
 * upgrades and salvages like any other) with a fruit already inside, its own name and its own look
 * (custom_model_data, see the item models). They drop from the boss named in "from" (see FruitWeapons).
 *
 * Three of the fruits aren't in the One Piece Mod, because only these weapons ever ate them: their names are here.
 */
public enum FamousFruitWeapon {
	LASSOO("lassoo", "Lassoo", "Mr. 4's dachshund bazooka", "inu_inu_no_mi_dachshund", "Inu Inu no Mi, Model: Dachshund",
			"crocodile", "Crocodile, Mr. 4's old boss", "It sneezes exploding shells at your enemies, no ammo needed.",
			() -> GunStats.build(GunType.HAND_CANNON, ArsenalMaterial.CANNON_BRONZE, ArsenalMaterial.SHIP_IRON, ArsenalMaterial.DRIFTWOOD)),
	FUNKFREED("funkfreed", "Funkfreed", "Spandam's elephant sword", "zou_zou_no_mi", null,
			"lucci", "Rob Lucci of Spandam's CP9", "It turns into an elephant and sweeps your enemies away with its trunk.",
			() -> WeaponStats.build(WeaponType.MARINE_SABRE, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.MARINE_STEEL, ArsenalMaterial.SEA_KING_BONE)),
	CERBERUS("cerberus", "Cerberus", "Shamrock's three-headed sword", "inu_inu_no_mi_cerberus", "Inu Inu no Mi, Model: Cerberus",
			"shanks", "Shanks, Shamrock's twin brother", "Its three heads bite three enemies at once, and their bites wither.",
			() -> WeaponStats.build(WeaponType.CUTLASS, ArsenalMaterial.byId("kokuto"), ArsenalMaterial.byId("treasure_gold"), ArsenalMaterial.SEA_KING_HIDE)),
	RAGNIR("ragnir", "Ragnir", "Loki's hammer, the one people call Mjolnir", "risu_risu_no_mi_ratatoskr", "Risu Risu no Mi, Model: Ratatoskr",
			"big_mom", "Big Mom, who grew up on Elbaph with the giants", "The thunder squirrel calls down lightning on what it hits.",
			() -> WeaponStats.build(WeaponType.KANABO, ArsenalMaterial.WAPOMETAL, ArsenalMaterial.byId("treasure_gold"), ArsenalMaterial.ADAM_WOOD));

	/** Also the custom_model_data string its item model picks. */
	public final String id;
	public final String displayName;
	public final String what;
	public final String fruitId;
	/** The fruit's name, or null when the One Piece Mod already has the fruit (it names it then). */
	public final String fruitName;
	/** The main mod boss (character id) that drops it. */
	public final String from;
	/** Who that is, for the manual. */
	public final String fromName;
	public final String power;
	private final Supplier<ItemStack> gear;

	FamousFruitWeapon(String id, String displayName, String what, String fruitId, String fruitName, String from, String fromName, String power,
			Supplier<ItemStack> gear) {
		this.id = id;
		this.displayName = displayName;
		this.what = what;
		this.fruitId = fruitId;
		this.fruitName = fruitName;
		this.from = from;
		this.fromName = fromName;
		this.power = power;
		this.gear = gear;
	}

	/** A new one, forged, fed and named. */
	public ItemStack create() {
		ItemStack stack = gear.get();
		stack.set(ArsenalComponents.EATEN_FRUIT, fruitId);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(displayName).withStyle(style -> style.withItalic(false).withColor(ChatFormatting.LIGHT_PURPLE)));
		stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(what).withStyle(ChatFormatting.GRAY))));
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(id), List.of()));
		return stack;
	}

	/** The famous weapon whose fruit this is, or null. */
	public static FamousFruitWeapon byFruit(String fruitId) {
		for (FamousFruitWeapon weapon : values()) {
			if (weapon.fruitId.equals(fruitId)) {
				return weapon;
			}
		}
		return null;
	}

	/** The famous weapon a boss drops, or null. */
	public static FamousFruitWeapon droppedBy(String bossId) {
		for (FamousFruitWeapon weapon : values()) {
			if (weapon.from.equals(bossId)) {
				return weapon;
			}
		}
		return null;
	}
}
