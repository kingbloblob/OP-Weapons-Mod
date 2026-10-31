package com.onepiecearsenal.upgrade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

import com.onepiecearsenal.haki.HakiImbue;
import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.fruit.FruitWeapons;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.ArsenalGunItem;
import com.onepiecearsenal.tool.ArsenalWeaponItem;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.upgrade.Upgrade.Effect;
import com.onepiecearsenal.upgrade.Upgrade.Gear;

/**
 * Repairs and upgrades at the Shipwright's Anvil, the Tinkers' Construct way: right-click the anvil holding a forged
 * weapon, gun or armor piece.
 * - With an upgrade's item in your offhand (Quartz for Honed Edge, Hull Plating for Hull Plating...), it adds a level
 *   of that upgrade, if the gear has a free upgrade slot.
 * - Otherwise, if it's damaged, it's mended with the material of its blade, head, barrel or shell, taken from
 *   anywhere in your inventory: each piece mends a quarter (upgrades.json's repair_share).
 *
 * The upgrades and their levels are saved on the item (ArsenalComponents.UPGRADES). Attribute upgrades go straight
 * on the item's attribute modifiers, Reinforced on its durability; gun upgrades are read by GunStats and Kokuto
 * Inlay by the hit hook below. The One Piece Mod's whetstones, gun parts, Dials and enchantments are separate and
 * all still work on top.
 */
public final class Upgrades {
	private Upgrades() {
	}

	public static void initialize() {
		for (Upgrade upgrade : Upgrade.values()) {
			if (!upgrade.itemExists()) {
				ArsenalMod.LOGGER.warn("Arsenal upgrade {} takes an item that isn't in the game; it can't be added until upgrades.json is fixed",
						upgrade.id);
			}
		}

		// Kokuto Inlay: with Armament Haki on, part of a fully charged hit goes through armor, like the Keen trait.
		AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
			if (player instanceof ServerPlayer attacker && level instanceof ServerLevel serverLevel
					&& target instanceof LivingEntity living && !player.isSpectator()
					&& player.getAttackStrengthScale(0.5f) >= 0.9f) {
				ItemStack weapon = player.getMainHandItem();
				double share = total(weapon, "haki_pierce");
				if (share > 0 && !Breakage.isBroken(weapon) && HakiImbue.hakiBehind(attacker, weapon) > 0) {
					float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * share);
					TraitEffects.extraHit(attacker, serverLevel, living, damage, attacker.damageSources().indirectMagic(attacker, attacker));
				}
			}
			return InteractionResult.PASS;
		});
	}

	// ---- Reading what's on an item ----

	/** What kind of gear this is, or null if it isn't forged Arsenal gear. */
	public static Gear gearOf(ItemStack stack) {
		Item item = stack.getItem();
		if (item instanceof KatanaItem) {
			return KatanaStats.parts(stack).isEmpty() ? null : Gear.KATANA;
		}
		if (item instanceof ArsenalWeaponItem) {
			return WeaponStats.parts(stack).isEmpty() ? null : Gear.WEAPON;
		}
		if (item instanceof ArsenalGunItem) {
			return GunStats.parts(stack).isEmpty() ? null : Gear.GUN;
		}
		if (ArmorStats.parts(stack).isEmpty()) {
			return null;
		}
		if (item == ArsenalItems.HAT) {
			return Gear.HAT;
		}
		if (item == ArsenalItems.COAT) {
			return Gear.COAT;
		}
		if (item == ArsenalItems.SASH) {
			return Gear.SASH;
		}
		return item == ArsenalItems.BOOTS ? Gear.BOOTS : null;
	}

	/** The material that mends this gear: its blade, head, barrel or shell. Null if it isn't forged gear. */
	public static ArsenalMaterial headMaterial(ItemStack stack) {
		Gear gear = gearOf(stack);
		if (gear == null) {
			return null;
		}
		return switch (gear) {
			case KATANA -> KatanaStats.parts(stack).get(0);
			case WEAPON -> WeaponStats.parts(stack).get(0);
			case GUN -> GunStats.parts(stack).get(0);
			default -> ArmorStats.parts(stack).get(0);
		};
	}

	/** Each upgrade on the item and its level, in the order they were added. */
	public static Map<Upgrade, Integer> upgrades(ItemStack stack) {
		Map<Upgrade, Integer> upgrades = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : stack.getOrDefault(ArsenalComponents.UPGRADES, Map.<String, Integer>of()).entrySet()) {
			Upgrade upgrade = Upgrade.byId(entry.getKey());
			if (upgrade != null && entry.getValue() > 0) { // An upgrade removed from upgrades.json just stops counting.
				upgrades.put(upgrade, entry.getValue());
			}
		}
		return upgrades;
	}

	public static int level(ItemStack stack, Upgrade upgrade) {
		return upgrades(stack).getOrDefault(upgrade, 0);
	}

	/** Upgrade slots in use: one per level. */
	public static int usedSlots(ItemStack stack) {
		return upgrades(stack).values().stream().mapToInt(Integer::intValue).sum();
	}

	/** The sum of one kind of effect over all the item's upgrades, like the extra shot damage of "gun_damage". */
	public static double total(ItemStack stack, String effectType) {
		double total = 0;
		for (Map.Entry<Upgrade, Integer> entry : upgrades(stack).entrySet()) {
			for (Effect effect : entry.getKey().effects) {
				if (effect.type().equals(effectType)) {
					total += effect.amount() * entry.getValue();
				}
			}
		}
		return total;
	}

	/** Durability with the item's Reinforced levels on top of what its parts give. */
	public static int durability(ItemStack stack, int base) {
		return Math.round(base * (1.0f + (float) total(stack, "durability")));
	}

	/** Why this upgrade can't go on this item right now, or null if it can. */
	public static String whyNot(ItemStack stack, Upgrade upgrade) {
		Gear gear = gearOf(stack);
		if (gear == null || !upgrade.fits.contains(gear)) {
			return upgrade.displayName + " doesn't fit " + stack.getHoverName().getString() + ". It fits: " + upgrade.fitsName() + ".";
		}
		if (level(stack, upgrade) >= upgrade.maxLevel) {
			return upgrade.displayName + " is already at its highest level, " + Upgrade.numeral(upgrade.maxLevel) + ".";
		}
		if (usedSlots(stack) >= Upgrade.slots(gear)) {
			return "All " + Upgrade.slots(gear) + " upgrade slots are used.";
		}
		return null;
	}

	// ---- Changing an item ----

	/** Adds one level of the upgrade. Returns false (and changes nothing) if it can't go on. */
	public static boolean add(ItemStack stack, Upgrade upgrade) {
		if (whyNot(stack, upgrade) != null) {
			return false;
		}
		int baseDurability = Math.round(stack.getMaxDamage() / (1.0f + (float) total(stack, "durability")));
		Map<String, Integer> levels = new LinkedHashMap<>(stack.getOrDefault(ArsenalComponents.UPGRADES, Map.<String, Integer>of()));
		levels.merge(upgrade.id, 1, Integer::sum);
		stack.set(ArsenalComponents.UPGRADES, levels);
		applyAttributes(stack, upgrade);
		stack.set(DataComponents.MAX_DAMAGE, durability(stack, baseDurability));
		return true;
	}

	/**
	 * Puts the upgrades of old gear on gear rebuilt from it (re-forged, or awakened): the levels, their attribute
	 * modifiers and Reinforced's durability. The new stack's durability should be fresh from its parts.
	 */
	public static void carryOver(ItemStack from, ItemStack to) {
		Map<String, Integer> levels = from.get(ArsenalComponents.UPGRADES);
		if (levels == null || levels.isEmpty()) {
			return;
		}
		to.set(ArsenalComponents.UPGRADES, levels);
		for (Upgrade upgrade : upgrades(to).keySet()) {
			applyAttributes(to, upgrade);
		}
		to.set(DataComponents.MAX_DAMAGE, durability(to, to.getMaxDamage()));
	}

	/** Swaps the upgrade's attribute modifiers on the item for ones at its current level. */
	private static void applyAttributes(ItemStack stack, Upgrade upgrade) {
		EquipmentSlotGroup slot = slotOf(stack);
		int level = level(stack, upgrade);
		ItemAttributeModifiers old = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		String prefix = "upgrade." + upgrade.id + ".";
		List<ItemAttributeModifiers.Entry> entries = new ArrayList<>();
		for (ItemAttributeModifiers.Entry entry : old.modifiers()) {
			Identifier id = entry.modifier().id();
			if (!(id.getNamespace().equals(ArsenalMod.MOD_ID) && id.getPath().startsWith(prefix))) {
				entries.add(entry);
			}
		}
		ItemAttributeModifiers modifiers = new ItemAttributeModifiers(entries);
		for (int i = 0; i < upgrade.effects.size(); i++) {
			Effect effect = upgrade.effects.get(i);
			if (!effect.type().equals("attribute") || level <= 0) {
				continue;
			}
			Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.get(effect.attribute()).orElse(null);
			if (attribute == null) {
				ArsenalMod.LOGGER.warn("Arsenal upgrade {} names an unknown attribute {}", upgrade.id, effect.attribute());
				continue;
			}
			// One id per upgrade, effect and slot, so a set of Hull Plated armor counts every piece.
			Identifier id = ArsenalMod.id(prefix + i + "." + slot.getSerializedName());
			modifiers = modifiers.withModifierAdded(attribute, new AttributeModifier(id, effect.amount() * level,
					AttributeModifier.Operation.valueOf(effect.operation().toUpperCase(Locale.ROOT))), slot);
		}
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
	}

	/** Armor works in its own slot, weapons and guns in the main hand. */
	private static EquipmentSlotGroup slotOf(ItemStack stack) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		Gear gear = gearOf(stack);
		return equippable != null && gear != null && gear.isArmor() ? EquipmentSlotGroup.bySlot(equippable.slot()) : EquipmentSlotGroup.MAINHAND;
	}

	// ---- At the anvil ----

	/**
	 * The smith right-clicked the Shipwright's Anvil holding forged gear. Upgrades it with the item in their offhand,
	 * or mends it. Returns null if there was nothing to do (or nothing to mend it with), so the anvil can forge instead.
	 */
	public static InteractionResult work(Level level, BlockPos pos, ServerPlayer smith, ItemStack gear) {
		ItemStack offhand = smith.getOffhandItem();
		if (OnePieceBridge.fruitId(offhand) != null) {
			return FruitWeapons.feed(level, pos, smith, gear, offhand);
		}
		for (Upgrade upgrade : Upgrade.values()) {
			if (upgrade.isItem(offhand)) {
				return upgrade(level, pos, smith, gear, upgrade, offhand);
			}
		}
		if (gear.isDamaged()) {
			return repair(level, pos, smith, gear);
		}
		return null;
	}

	private static InteractionResult upgrade(Level level, BlockPos pos, ServerPlayer smith, ItemStack gear, Upgrade upgrade, ItemStack offhand) {
		String problem = whyNot(gear, upgrade);
		if (problem == null && offhand.getCount() < upgrade.count) {
			problem = upgrade.displayName + " needs " + upgrade.count + " " + upgrade.itemName() + " in your offhand.";
		}
		if (problem != null) {
			smith.sendOverlayMessage(Component.literal(problem).withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		add(gear, upgrade);
		offhand.shrink(upgrade.count);
		level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
		Gear kind = gearOf(gear);
		smith.sendOverlayMessage(Component.literal(upgrade.displayName + " " + Upgrade.numeral(level(gear, upgrade)) + " added ("
				+ usedSlots(gear) + "/" + Upgrade.slots(kind) + " upgrade slots).").withStyle(ChatFormatting.GREEN));
		return InteractionResult.SUCCESS;
	}

	private static InteractionResult repair(Level level, BlockPos pos, ServerPlayer smith, ItemStack gear) {
		ArsenalMaterial material = headMaterial(gear);
		Item mender = material.repairItem();
		int perPiece = Math.max(1, (int) Math.ceil(gear.getMaxDamage() * Upgrade.repairShare()));
		int needed = (gear.getDamageValue() + perPiece - 1) / perPiece;
		int used = take(smith.getInventory(), mender, needed);
		String name = new ItemStack(mender).getHoverName().getString();
		if (used == 0) {
			return null; // Nothing to mend it with: the anvil forges instead, or says what it needs (help).
		}
		gear.setDamageValue(Math.max(0, gear.getDamageValue() - used * perPiece));
		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
		smith.sendOverlayMessage(Component.literal("Mended with " + used + " " + name + (gear.isDamaged() ? "." : ": good as new!"))
				.withStyle(ChatFormatting.GREEN));
		return InteractionResult.SUCCESS;
	}

	/** Takes up to count of the item from anywhere in the inventory, and says how many it took. */
	private static int take(Inventory inventory, Item item, int count) {
		int taken = 0;
		for (int i = 0; i < inventory.getContainerSize() && taken < count; i++) {
			ItemStack stack = inventory.getItem(i);
			// Parts and gear are never a material, even when they're made of the same item.
			if (stack.is(item) && gearOf(stack) == null) {
				int take = Math.min(count - taken, stack.getCount());
				stack.shrink(take);
				taken += take;
			}
		}
		return taken;
	}

	/** What to tell a smith who held gear up to the anvil with nothing to do to it. */
	public static Component help(ItemStack gear) {
		if (gear.isDamaged()) {
			ArsenalMaterial material = headMaterial(gear);
			return Component.literal("Mend it with " + new ItemStack(material.repairItem()).getHoverName().getString() + ", the stuff of its "
					+ material.displayName + (Breakage.isBroken(gear) ? ". Or sneak and right-click to take it apart for its parts."
					: ". Or hold an upgrade's item in your offhand to upgrade it.")).withStyle(ChatFormatting.YELLOW);
		}
		Gear kind = gearOf(gear);
		return Component.literal(gear.getHoverName().getString() + ": " + usedSlots(gear) + "/" + Upgrade.slots(kind)
				+ " upgrade slots used. Hold an upgrade's item in your offhand to add it (see the Shipwright's Manual).")
				.withStyle(ChatFormatting.YELLOW);
	}

	// ---- Tooltips ----

	/** The upgrade lines on an item's tooltip. Nothing for gear without upgrades. */
	public static void describe(ItemStack stack, Consumer<Component> lines) {
		Map<Upgrade, Integer> upgrades = upgrades(stack);
		Gear gear = gearOf(stack);
		if (upgrades.isEmpty() || gear == null) {
			return;
		}
		lines.accept(Component.literal("Upgrades (" + usedSlots(stack) + "/" + Upgrade.slots(gear) + " slots):").withStyle(ChatFormatting.GRAY));
		for (Map.Entry<Upgrade, Integer> entry : upgrades.entrySet()) {
			lines.accept(Component.literal(" " + entry.getKey().displayName + " " + Upgrade.numeral(entry.getValue()))
					.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}
}
