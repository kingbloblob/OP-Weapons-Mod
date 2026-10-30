package com.onepiecearsenal.armorset;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Faction;
import com.onepiecearsenal.tool.ArmorStats;

/**
 * One armor set from src/main/resources/arsenal/set_bonuses.json: which pieces belong to it and what wearing
 * enough of them does. SetBonuses makes the effects happen. That file explains every field.
 */
public final class SetBonus {
	private static final List<SetBonus> ALL = load();

	/** What a shell or lining has to be made of. Null fields match anything. */
	public record PartMatch(Faction faction, Set<String> materials) {
		boolean matches(ArsenalMaterial material) {
			return (faction == null || material.faction == faction) && (materials == null || materials.contains(material.id()));
		}

		/** "Navy", "Kairoseki", "Kairoseki or Pacifista Alloy", for the manual. */
		public String describe() {
			List<String> names = new ArrayList<>();
			if (materials != null) {
				for (ArsenalMaterial material : ArsenalMaterial.values()) {
					if (materials.contains(material.id())) {
						names.add(material.displayName);
					}
				}
			}
			String what = String.join(" or ", names);
			if (faction == null) {
				return what;
			}
			return what.isEmpty() ? faction.displayName : faction.displayName + " " + what;
		}
	}

	/**
	 * One effect. type is attribute, mob_effect, damage_taken, damage_dealt, disguise_bounty or suspicion.
	 * id names the attribute or mob effect, against the kind of attacker or target for the damage ones.
	 */
	public record Effect(String type, Identifier id, String against, double amount, String operation, int amplifier) {}

	public final String id;
	public final String name;
	public final String description;
	public final int pieces;
	public final PartMatch shell;
	public final PartMatch lining;
	public final List<Effect> effects;

	private SetBonus(JsonObject json) {
		this.id = json.get("id").getAsString();
		this.name = json.get("name").getAsString();
		this.description = json.get("description").getAsString();
		this.pieces = json.has("pieces") ? json.get("pieces").getAsInt() : 4;
		this.shell = json.has("shell") ? match(json.getAsJsonObject("shell")) : null;
		this.lining = json.has("lining") ? match(json.getAsJsonObject("lining")) : null;
		List<Effect> effects = new ArrayList<>();
		for (JsonElement element : json.getAsJsonArray("effects")) {
			JsonObject effect = element.getAsJsonObject();
			String type = effect.get("type").getAsString();
			String idKey = type.equals("mob_effect") ? "effect" : "attribute";
			String againstKey = type.equals("damage_taken") ? "from" : "against";
			effects.add(new Effect(type,
					effect.has(idKey) ? Identifier.parse(effect.get(idKey).getAsString()) : null,
					effect.has(againstKey) ? effect.get(againstKey).getAsString() : "",
					effect.has("amount") ? effect.get("amount").getAsDouble() : 0.0,
					effect.has("operation") ? effect.get("operation").getAsString() : "add_value",
					effect.has("amplifier") ? effect.get("amplifier").getAsInt() : 0));
		}
		this.effects = List.copyOf(effects);
	}

	/** True if this armor piece counts toward the set: an Arsenal piece whose shell and lining match. */
	public boolean fits(ItemStack piece) {
		List<ArsenalMaterial> parts = ArmorStats.parts(piece);
		return !parts.isEmpty() && (shell == null || shell.matches(parts.get(0))) && (lining == null || lining.matches(parts.get(1)));
	}

	/** "four Kairoseki shells", "four Navy shells and Navy linings", for tooltips and the manual. */
	public String requirement() {
		String count = switch (pieces) {
			case 1 -> "one";
			case 2 -> "two";
			case 3 -> "three";
			default -> "four";
		};
		List<String> parts = new ArrayList<>();
		if (shell != null) {
			parts.add(shell.describe() + " shells");
		}
		if (lining != null) {
			parts.add(lining.describe() + " linings");
		}
		return parts.isEmpty() ? count + " Arsenal pieces" : count + " pieces with " + String.join(" and ", parts);
	}

	/** Every set, in the order of set_bonuses.json. */
	public static List<SetBonus> values() {
		return Collections.unmodifiableList(ALL);
	}

	public static SetBonus byId(String id) {
		for (SetBonus set : ALL) {
			if (set.id.equals(id)) {
				return set;
			}
		}
		return null;
	}

	private static PartMatch match(JsonObject json) {
		Faction faction = json.has("faction") ? Faction.valueOf(json.get("faction").getAsString().toUpperCase(Locale.ROOT)) : null;
		Set<String> materials = null;
		if (json.has("materials")) {
			materials = new HashSet<>();
			for (JsonElement material : json.getAsJsonArray("materials")) {
				if (ArsenalMaterial.byId(material.getAsString()) == null) {
					throw new IllegalStateException("no material \"" + material.getAsString() + "\" in arsenal/materials.json");
				}
				materials.add(material.getAsString());
			}
		}
		return new PartMatch(faction, materials);
	}

	private static List<SetBonus> load() {
		List<SetBonus> sets = new ArrayList<>();
		try (InputStream in = SetBonus.class.getResourceAsStream("/arsenal/set_bonuses.json")) {
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement element : root.getAsJsonArray("sets")) {
				JsonObject json = element.getAsJsonObject();
				try {
					sets.add(new SetBonus(json));
				} catch (RuntimeException e) {
					throw new IllegalStateException("Bad set \"" + json.get("id") + "\" in arsenal/set_bonuses.json: " + e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not read arsenal/set_bonuses.json", e);
		}
		return sets;
	}
}
