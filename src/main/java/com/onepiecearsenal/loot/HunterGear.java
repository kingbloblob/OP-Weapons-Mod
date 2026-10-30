package com.onepiecearsenal.loot;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.tool.WeaponType;

/**
 * The main mod's bounty hunters (who come after pirates worth 100,000,000 or more) carry Arsenal gear instead of a
 * plain vanilla sword: a forged pirate weapon and some armor, better the higher the bounty they were sent for.
 * The very best hunters sometimes carry Kairoseki, the Devil Fruit hunter's favourite, and it drains a fruit user's
 * power when it hits (see TraitEffects).
 *
 * The weapon doesn't change how hard a hunter hits: the hunter's own damage is evened out so they hit as hard as with
 * the iron, diamond or netherite sword they used to carry. Kanabo still knock you far back and spears still reach further.
 *
 * Beat a hunter and their gear sometimes drops, worn from use. Each hunter is kitted out once, the first time it's
 * added to a world. Change the tables and chances here to tune it.
 */
public final class HunterGear {
	/** True once a hunter has been kitted out, so reloading its chunk doesn't roll again. Saved with the hunter. */
	public static final AttachmentType<Boolean> OUTFITTED = AttachmentRegistry.create(
			ArsenalMod.id("hunter_outfitted"),
			builder -> builder.persistent(Codec.BOOL)
	);

	/** How often the weapon drops. */
	private static final float WEAPON_DROP_CHANCE = 0.10f;
	/** How often each armor piece drops. */
	private static final float ARMOR_DROP_CHANCE = 0.05f;
	/** Chance a threat level 5 hunter's weapon is Kairoseki. */
	private static final float KAIROSEKI_CHANCE = 0.3f;
	/** Removes or adds the hunter's own damage so the Arsenal weapon hits as hard as the sword it replaced. */
	private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;

	/** What a hunter sent at each threat level (3, 4, 5) carries. */
	public record Loadout(List<ArsenalMaterial> weaponHeads, List<ArsenalMaterial> handles, int armorPieces,
			List<ArsenalMaterial> shells, List<ArsenalMaterial> linings, float swordDamage) {
	}

	public static final List<Loadout> LOADOUTS = List.of(
			// Threat 3 (from 100,000,000): rough pirate iron and bone, one piece of armor. Used to carry an iron sword (+5).
			new Loadout(List.of(ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SEA_KING_BONE),
					List.of(ArsenalMaterial.DRIFTWOOD, ArsenalMaterial.SHIP_IRON),
					1, List.of(ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SEA_KING_HIDE), List.of(ArsenalMaterial.SEA_KING_HIDE), 5.0f),
			// Threat 4 (from 300,000,000): Wano steel and bronze, two pieces. Used to carry a diamond sword (+6).
			new Loadout(List.of(ArsenalMaterial.SEA_KING_BONE, ArsenalMaterial.CANNON_BRONZE, ArsenalMaterial.TAMAHAGANE),
					List.of(ArsenalMaterial.SHIP_IRON, ArsenalMaterial.SEA_KING_BONE),
					2, List.of(ArsenalMaterial.SEA_KING_HIDE, ArsenalMaterial.ISLAND_WHALE_LEATHER), List.of(ArsenalMaterial.SEA_KING_HIDE), 6.0f),
			// Threat 5 (from 1,000,000,000): the best a pirate can forge, and a full set. Used to carry a netherite sword (+7).
			new Loadout(List.of(ArsenalMaterial.TAMAHAGANE, ArsenalMaterial.WAPOMETAL, ArsenalMaterial.ADAM_WOOD),
					List.of(ArsenalMaterial.SEA_KING_BONE, ArsenalMaterial.ADAM_WOOD),
					4, List.of(ArsenalMaterial.ISLAND_WHALE_LEATHER, ArsenalMaterial.WAPOMETAL), List.of(ArsenalMaterial.ISLAND_WHALE_LEATHER), 7.0f)
	);

	/** The hand weapons hunters pick from. Navy designs (sabre, jitte) are left to the Marines. */
	private static final List<WeaponType> WEAPONS = List.of(WeaponType.CUTLASS, WeaponType.BOARDING_AXE, WeaponType.KANABO, WeaponType.SPEAR);
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final PartType[] SHELLS = {PartType.COAT_PLATES, PartType.CROWN, PartType.SASH_WEAVE, PartType.BOOT_SOLES};

	private HunterGear() {
	}

	public static void initialize() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob hunter && OnePieceBridge.isBountyHunter(hunter) && !hunter.hasAttached(OUTFITTED)) {
				hunter.setAttached(OUTFITTED, true);
				outfit(hunter, hunter.getRandom());
			}
		});
	}

	/** What a hunter sent at this threat level (3 to 5) carries. */
	public static Loadout loadout(int threat) {
		return LOADOUTS.get(Math.clamp(threat, 3, 5) - 3);
	}

	/** Gives the hunter an Arsenal weapon and armor for the threat level they were sent for. */
	public static void outfit(Mob hunter, RandomSource random) {
		int threat = Math.max(3, OnePieceBridge.hunterStrength(hunter));
		Loadout loadout = loadout(threat);
		String carrier = "Carried by " + hunterName(hunter);

		ArsenalMaterial head = threat >= 5 && random.nextFloat() < KAIROSEKI_CHANCE ? ArsenalMaterial.KAIROSEKI : pick(loadout.weaponHeads(), random);
		ArsenalMaterial handle = pick(loadout.handles(), random);
		// One in five carry a katana, like Wano's ronin turned bounty hunter.
		int choice = random.nextInt(WEAPONS.size() + 1);
		ItemStack weapon = choice == WEAPONS.size()
				? KatanaStats.build(head, handle, handle)
				: WeaponStats.build(WEAPONS.get(choice), head, handle, handle);
		hunter.setItemSlot(EquipmentSlot.MAINHAND, signed(weapon, carrier));
		hunter.setDropChance(EquipmentSlot.MAINHAND, WEAPON_DROP_CHANCE);
		evenOutDamage(hunter, loadout.swordDamage() - (float) weaponDamage(weapon));

		// The coat first, then hat, sash and boots.
		for (int i = 0; i < loadout.armorPieces(); i++) {
			ItemStack piece = ArmorStats.build(SHELLS[i], pick(loadout.shells(), random), pick(loadout.linings(), random));
			hunter.setItemSlot(ARMOR_SLOTS[i], signed(piece, carrier));
			hunter.setDropChance(ARMOR_SLOTS[i], ARMOR_DROP_CHANCE);
		}
	}

	/** The damage the weapon adds in the main hand. */
	public static double weaponDamage(ItemStack weapon) {
		double total = 0;
		for (ItemAttributeModifiers.Entry entry : weapon.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && entry.modifier().operation() == ADD && entry.slot().test(EquipmentSlot.MAINHAND)) {
				total += entry.modifier().amount();
			}
		}
		return total;
	}

	private static void evenOutDamage(Mob hunter, float difference) {
		AttributeInstance damage = hunter.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null) {
			damage.addOrReplacePermanentModifier(new AttributeModifier(ArsenalMod.id("hunter_weapon_balance"), difference, ADD));
		}
	}

	/** "Iron Fang Rook" from "Iron Fang Rook (Gomu Gomu)". */
	private static String hunterName(Mob hunter) {
		String name = hunter.getCustomName() != null ? hunter.getCustomName().getString() : "a bounty hunter";
		int fruit = name.indexOf(" (");
		return fruit > 0 ? name.substring(0, fruit) + ", bounty hunter" : name;
	}

	/** Notes on the gear who carried it, so a dropped piece tells its story. It keeps its parts and works like any other. */
	private static ItemStack signed(ItemStack gear, String carrier) {
		List<Component> lore = new ArrayList<>(gear.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
		lore.add(Component.literal(carrier).withStyle(ChatFormatting.DARK_PURPLE));
		gear.set(DataComponents.LORE, new ItemLore(lore));
		return gear;
	}

	private static <T> T pick(List<T> options, RandomSource random) {
		return options.get(random.nextInt(options.size()));
	}
}
