package com.onepiecearsenal.material;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * The materials parts can be made of. The numbers live in src/main/resources/arsenal/materials.json:
 * change them there to make a material stronger or weaker, or add a new one (and run tools/gen_resources.py
 * to write its part recipes). That file also explains what each number means.
 */
public final class ArsenalMaterial {
	private static final Map<String, ArsenalMaterial> BY_ID = load();

	// The materials the code itself needs by name (creative tab icons, examples, the Navy stations and Black Blade awakening).
	public static final ArsenalMaterial DRIFTWOOD = require("driftwood");
	public static final ArsenalMaterial SHIP_IRON = require("ship_iron");
	public static final ArsenalMaterial MARINE_STEEL = require("marine_steel");
	public static final ArsenalMaterial KAIROSEKI = require("kairoseki");
	public static final ArsenalMaterial PACIFISTA_ALLOY = require("pacifista_alloy");
	public static final ArsenalMaterial VEGAPUNK_COMPOUND = require("vegapunk_compound");
	public static final ArsenalMaterial BLACK_BLADE = require("black_blade");
	// Used by the famous Marines' named gear (loot/BossGear).
	public static final ArsenalMaterial TAMAHAGANE = require("tamahagane");
	public static final ArsenalMaterial ADAM_WOOD = require("adam_wood");
	public static final ArsenalMaterial SEA_KING_HIDE = require("sea_king_hide");
	public static final ArsenalMaterial ISLAND_WHALE_LEATHER = require("island_whale_leather");

	private final String id;
	public final String displayName;
	public final int tier;
	public final Faction faction;
	public final float damage;
	public final int durability;
	public final float speed;
	public final float armor;
	public final Trait headTrait;
	public final Trait handleTrait;
	/** Hides and leathers: only armor shells and linings can be made of it. */
	public final boolean armorOnly;
	/** False for materials with no part recipes or station (Black Blade Steel). */
	public final boolean craftable;
	private final Identifier repairItem;

	private ArsenalMaterial(String id, JsonObject json) {
		this.id = id;
		this.displayName = json.get("name").getAsString();
		this.tier = json.get("tier").getAsInt();
		this.faction = Faction.valueOf(json.get("faction").getAsString().toUpperCase(Locale.ROOT));
		this.damage = json.get("damage").getAsFloat();
		this.durability = json.get("durability").getAsInt();
		this.speed = json.get("speed").getAsFloat();
		this.armor = json.get("armor").getAsFloat();
		this.headTrait = Trait.valueOf(json.get("head_trait").getAsString().toUpperCase(Locale.ROOT));
		this.handleTrait = Trait.valueOf(json.get("handle_trait").getAsString().toUpperCase(Locale.ROOT));
		this.armorOnly = json.has("armor_only") && json.get("armor_only").getAsBoolean();
		this.craftable = !json.has("craftable") || json.get("craftable").getAsBoolean();
		this.repairItem = Identifier.parse(json.get("item").getAsString());
	}

	/** The name saved on items, like "ship_iron". Also used in the recipe files. */
	public String id() {
		return id;
	}

	/** A fingerprint of the numbers and traits gear is built from. When it changes, Restat brings old gear up to date. */
	public int statsHash() {
		return Objects.hash(id, tier, damage, durability, speed, armor, headTrait, handleTrait);
	}

	/** The item this material is made from, which also repairs it. */
	public Item repairItem() {
		return BuiltInRegistries.ITEM.getValue(repairItem);
	}

	/** Returns null for an unknown id, for example a material removed in an update. */
	public static ArsenalMaterial byId(String id) {
		return BY_ID.get(id);
	}

	/** Every material, in the order of materials.json (weakest tier first). */
	public static Collection<ArsenalMaterial> values() {
		return Collections.unmodifiableCollection(BY_ID.values());
	}

	private static ArsenalMaterial require(String id) {
		ArsenalMaterial material = BY_ID.get(id);
		if (material == null) {
			throw new IllegalStateException("arsenal/materials.json has no \"" + id + "\" material, and Arsenal needs it");
		}
		return material;
	}

	private static Map<String, ArsenalMaterial> load() {
		Map<String, ArsenalMaterial> materials = new LinkedHashMap<>();
		try (InputStream in = ArsenalMaterial.class.getResourceAsStream("/arsenal/materials.json")) {
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement element : root.getAsJsonArray("materials")) {
				JsonObject json = element.getAsJsonObject();
				String id = json.get("id").getAsString();
				try {
					materials.put(id, new ArsenalMaterial(id, json));
				} catch (RuntimeException e) {
					throw new IllegalStateException("Bad material \"" + id + "\" in arsenal/materials.json: " + e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not read arsenal/materials.json", e);
		}
		return materials;
	}
}
