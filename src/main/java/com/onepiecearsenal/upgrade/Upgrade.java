package com.onepiecearsenal.upgrade;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One upgrade the Shipwright's Anvil can add to a piece of gear, like Honed Edge or Hull Plating. They're all in
 * src/main/resources/arsenal/upgrades.json, which also explains every field; Upgrades does the work.
 */
public final class Upgrade {
	/** The kinds of gear an upgrade can fit. */
	public enum Gear {
		KATANA("katana", "Katanas"), WEAPON("weapon", "Hand weapons"), GUN("gun", "Guns"),
		HAT("armor", "Hats"), COAT("armor", "Coats"), SASH("armor", "Sashes"), BOOTS("armor", "Boots"),
		SHIELD("shield", "Shields"), GAUNTLETS("gauntlets", "Gauntlets");

		/** The key in upgrades.json's "slots": armor pieces share one. */
		final String slotKey;
		public final String displayName;

		Gear(String slotKey, String displayName) {
			this.slotKey = slotKey;
			this.displayName = displayName;
		}

		/** The four worn pieces (gauntlets are armor too, but held in the offhand: see Upgrades.slotOf). */
		public boolean isArmor() {
			return slotKey.equals("armor");
		}
	}

	/** What one level of an upgrade does (see upgrades.json's _comment). */
	public record Effect(String type, Identifier attribute, double amount, String operation) {
	}

	/** What "armor" fits in upgrades.json: the four pieces, and gauntlets. */
	private static final List<Gear> ARMOR = List.of(Gear.HAT, Gear.COAT, Gear.SASH, Gear.BOOTS, Gear.GAUNTLETS);

	private static final Data DATA = load();

	public final String id;
	public final String displayName;
	public final String description;
	public final int maxLevel;
	public final int count;
	public final List<Gear> fits;
	public final List<Effect> effects;
	/** "minecraft:quartz" or "#minecraft:wool". */
	private final String item;

	private Upgrade(String id, JsonObject json) {
		this.id = id;
		this.displayName = json.get("name").getAsString();
		this.description = json.get("description").getAsString();
		this.maxLevel = json.get("max_level").getAsInt();
		this.count = json.has("count") ? json.get("count").getAsInt() : 1;
		this.item = json.get("item").getAsString();
		this.fits = fits(json.get("fits").getAsString());
		List<Effect> effects = new ArrayList<>();
		for (JsonElement element : json.getAsJsonArray("effects")) {
			JsonObject effect = element.getAsJsonObject();
			effects.add(new Effect(effect.get("type").getAsString(),
					effect.has("attribute") ? Identifier.parse(effect.get("attribute").getAsString()) : null,
					effect.get("amount").getAsDouble(),
					effect.has("operation") ? effect.get("operation").getAsString() : "add_value"));
		}
		this.effects = List.copyOf(effects);
	}

	private static List<Gear> fits(String fits) {
		return switch (fits) {
			case "any" -> List.of(Gear.values());
			case "melee" -> List.of(Gear.KATANA, Gear.WEAPON);
			case "armor" -> ARMOR;
			default -> List.of(Gear.valueOf(fits.toUpperCase(Locale.ROOT)));
		};
	}

	/** True if this stack is what the upgrade takes. */
	public boolean isItem(ItemStack stack) {
		if (item.startsWith("#")) {
			return stack.is(TagKey.create(Registries.ITEM, Identifier.parse(item.substring(1))));
		}
		return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(Identifier.parse(item));
	}

	/** "Quartz", or "any Wool" for a tag. */
	public String itemName() {
		if (item.startsWith("#")) {
			String path = Identifier.parse(item.substring(1)).getPath().replace('_', ' ');
			return "any " + Character.toUpperCase(path.charAt(0)) + path.substring(1);
		}
		Item resolved = BuiltInRegistries.ITEM.getValue(Identifier.parse(item));
		return new ItemStack(resolved).getHoverName().getString();
	}

	/** False if upgrades.json names an item that isn't in the game (a typo, or a main mod item that was renamed). */
	boolean itemExists() {
		return item.startsWith("#") || BuiltInRegistries.ITEM.containsKey(Identifier.parse(item));
	}

	/** "Melee", "Boots", "Any gear" and so on, for the manual. */
	public String fitsName() {
		if (fits.size() == Gear.values().length) {
			return "Any gear";
		}
		if (fits.equals(List.of(Gear.KATANA, Gear.WEAPON))) {
			return "Katanas and hand weapons";
		}
		if (fits.equals(ARMOR)) {
			return "Armor and gauntlets";
		}
		return String.join(", ", fits.stream().map(gear -> gear.displayName).toList());
	}

	/** Roman numerals for levels, like enchantments. */
	public static String numeral(int level) {
		return switch (level) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> Integer.toString(level);
		};
	}

	public static Upgrade byId(String id) {
		return DATA.upgrades.get(id);
	}

	/** Every upgrade, in the order of upgrades.json. */
	public static Collection<Upgrade> values() {
		return Collections.unmodifiableCollection(DATA.upgrades.values());
	}

	/** How many upgrade levels this kind of gear holds. */
	public static int slots(Gear gear) {
		return DATA.slots.getOrDefault(gear, 0);
	}

	/** How much of an item's durability one piece of its head material mends. */
	public static float repairShare() {
		return DATA.repairShare;
	}

	private record Data(Map<String, Upgrade> upgrades, Map<Gear, Integer> slots, float repairShare) {
	}

	private static Data load() {
		Map<String, Upgrade> upgrades = new LinkedHashMap<>();
		Map<Gear, Integer> slots = new EnumMap<>(Gear.class);
		try (InputStream in = Upgrade.class.getResourceAsStream("/arsenal/upgrades.json")) {
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject slotJson = root.getAsJsonObject("slots");
			for (Gear gear : Gear.values()) {
				if (slotJson.has(gear.slotKey)) {
					slots.put(gear, slotJson.get(gear.slotKey).getAsInt());
				}
			}
			for (JsonElement element : root.getAsJsonArray("upgrades")) {
				JsonObject json = element.getAsJsonObject();
				String id = json.get("id").getAsString();
				try {
					upgrades.put(id, new Upgrade(id, json));
				} catch (RuntimeException e) {
					throw new IllegalStateException("Bad upgrade \"" + id + "\" in arsenal/upgrades.json: " + e.getMessage(), e);
				}
			}
			return new Data(upgrades, slots, root.get("repair_share").getAsFloat());
		} catch (Exception e) {
			throw new RuntimeException("Could not read arsenal/upgrades.json", e);
		}
	}
}
