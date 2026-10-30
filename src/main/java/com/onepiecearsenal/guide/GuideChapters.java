package com.onepiecearsenal.guide;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.armorset.SetBonus;
import com.onepiecearsenal.cannon.CannonShells;
import com.onepiecearsenal.cannon.CannonStats;
import com.onepiecearsenal.fruit.Beast;
import com.onepiecearsenal.fruit.FamousFruitWeapon;
import com.onepiecearsenal.fruit.FruitWeapons;
import com.onepiecearsenal.loot.BossGear;
import com.onepiecearsenal.loot.HunterGear;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Faction;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.meito.Meito;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.skill.WeaponSkill;
import com.onepiecearsenal.station.NavyCastingBlock;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.GunType;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.tool.WeaponType;
import com.onepiecearsenal.upgrade.Upgrade;

/**
 * The chapters of the Shipwright's Manual. The words are in src/main/resources/arsenal/guide.json: edit that file
 * to change what the book says. A chapter with "generated" also gets lines written here from the mod's own data
 * (materials.json, upgrades.json, the traits, weapon and gun types), so those pages stay right when the numbers change.
 */
public final class GuideChapters {
	public static class Chapter {
		public String id;
		public String title;
		/** Item id shown next to the title, like "arsenal:katana". Arsenal gear is shown made of icon_material. */
		public String icon = "arsenal:shipwrights_manual";
		public String icon_material = "ship_iron";
		/** Hex colour for the title and headings, like "FFD75E". */
		public String color = "FFD75E";
		/** One entry per line. "# " = heading, "- " = bullet, "> " = tip, "" = empty line. */
		public List<String> text = new ArrayList<>();
		/** Lines written from the mod's data after the text: materials, traits, weapons, skills, guns, navy_casting, meito, navy_brass, hunter_gear, upgrades or set_bonuses. */
		public String generated;
		/** More lines, shown after the generated ones. */
		public List<String> after = new ArrayList<>();

		public int colorArgb() {
			try {
				return 0xFF000000 | Integer.parseInt(color, 16);
			} catch (NumberFormatException e) {
				return 0xFFFFD75E;
			}
		}

		/** The text plus the generated lines. */
		public List<String> lines() {
			List<String> lines = new ArrayList<>(text);
			if (generated != null) {
				if (!lines.isEmpty()) {
					lines.add("");
				}
				lines.addAll(generate(generated));
			}
			if (!after.isEmpty()) {
				lines.add("");
				lines.addAll(after);
			}
			return lines;
		}

		public ItemStack iconStack() {
			Identifier id = Identifier.tryParse(icon);
			if (id == null) {
				return ItemStack.EMPTY;
			}
			Item item = BuiltInRegistries.ITEM.getValue(id);
			ArsenalMaterial material = ArsenalMaterial.byId(icon_material);
			return material == null ? new ItemStack(item) : forged(item, material);
		}
	}

	private static class File {
		List<Chapter> chapters = new ArrayList<>();
	}

	private static List<Chapter> chapters;

	private GuideChapters() {
	}

	public static List<Chapter> chapters() {
		if (chapters == null) {
			try (InputStream in = GuideChapters.class.getResourceAsStream("/arsenal/guide.json")) {
				chapters = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), File.class).chapters;
			} catch (Exception e) {
				ArsenalMod.LOGGER.error("Could not read arsenal/guide.json", e);
				chapters = new ArrayList<>();
			}
		}
		return chapters;
	}

	/** An Arsenal weapon, gun, armor piece or part made of this material, so icons look like real gear. */
	private static ItemStack forged(Item item, ArsenalMaterial material) {
		ArsenalMaterial iron = ArsenalMaterial.SHIP_IRON;
		if (item == ArsenalItems.KATANA) {
			return KatanaStats.build(material, iron, iron);
		}
		for (Map.Entry<WeaponType, Item> weapon : ArsenalItems.WEAPONS.entrySet()) {
			if (weapon.getValue() == item) {
				return WeaponStats.build(weapon.getKey(), material, iron, iron);
			}
		}
		for (Map.Entry<GunType, Item> gun : ArsenalItems.GUNS.entrySet()) {
			if (gun.getValue() == item) {
				return GunStats.build(gun.getKey(), material, iron, iron);
			}
		}
		PartType shell = item == ArsenalItems.HAT ? PartType.CROWN : item == ArsenalItems.COAT ? PartType.COAT_PLATES
				: item == ArsenalItems.SASH ? PartType.SASH_WEAVE : item == ArsenalItems.BOOTS ? PartType.BOOT_SOLES : null;
		if (shell != null) {
			return ArmorStats.build(shell, material, ArsenalMaterial.DRIFTWOOD);
		}
		if (item == ArsenalItems.SHIP_CANNON) {
			return CannonStats.build(material, iron, iron);
		}
		if (item instanceof PartItem) {
			return PartItem.of(item, material);
		}
		return new ItemStack(item);
	}

	// ---- Lines written from the mod's data ----

	private static List<String> generate(String what) {
		return switch (what) {
			case "materials" -> materials();
			case "traits" -> traits();
			case "weapons" -> weapons();
			case "skills" -> skills();
			case "guns" -> guns();
			case "navy_casting" -> navyCasting();
			case "meito" -> meito();
			case "cannons" -> cannons();
			case "navy_brass" -> navyBrass();
			case "hunter_gear" -> hunterGear();
			case "upgrades" -> upgrades();
			case "set_bonuses" -> setBonuses();
			case "fruit_weapons" -> fruitWeapons();
			default -> List.of("(Nothing is written here yet: \"" + what + "\" isn't a page the manual knows.)");
		};
	}

	private static List<String> setBonuses() {
		List<String> lines = new ArrayList<>();
		for (SetBonus set : SetBonus.values()) {
			lines.add("# " + set.name);
			lines.add("Wear " + set.requirement() + ".");
			lines.add("- " + set.description);
		}
		return lines;
	}

	private static List<String> materials() {
		List<String> lines = new ArrayList<>();
		int tier = 0;
		for (ArsenalMaterial material : ArsenalMaterial.values()) {
			if (material.tier != tier) {
				tier = material.tier;
				lines.add("# Tier " + tier);
			}
			lines.add(material.displayName + " (" + material.faction.displayName + ")");
			lines.add("- Made from: " + itemName(material.repairItem()) + ". " + whereMade(material));
			if (material.armorOnly) {
				lines.add("- Armor only: " + number(material.armor) + " armor as a shell, " + material.durability + " uses.");
				lines.add("- Trait: " + trait(material.headTrait));
			} else {
				lines.add("- Blade: " + number(material.damage) + " damage. Handle: " + signed(material.speed) + " speed. "
						+ material.durability + " uses. Armor shell: " + number(material.armor) + ".");
				lines.add("- Blade trait: " + trait(material.headTrait));
				lines.add("- Handle trait: " + trait(material.handleTrait));
			}
			lines.add("");
		}
		return lines;
	}

	private static String whereMade(ArsenalMaterial material) {
		if (!material.craftable) {
			return "Its parts can't be made at all.";
		}
		for (NavyCastingBlock station : List.of((NavyCastingBlock) ArsenalItems.QUARTERMASTERS_DESK,
				(NavyCastingBlock) ArsenalItems.MARINE_BASE_SMELTERY)) {
			if (station.materials().contains(material)) {
				return "Parts are cast at a " + station.title() + ".";
			}
		}
		return "Parts are made at a crafting table.";
	}

	private static List<String> traits() {
		// Which materials give each trait, as a blade (head) and as a handle.
		Map<Trait, List<String>> heads = new LinkedHashMap<>();
		Map<Trait, List<String>> handles = new LinkedHashMap<>();
		for (ArsenalMaterial material : ArsenalMaterial.values()) {
			heads.computeIfAbsent(material.headTrait, t -> new ArrayList<>()).add(material.displayName);
			if (!material.armorOnly) {
				handles.computeIfAbsent(material.handleTrait, t -> new ArrayList<>()).add(material.displayName);
			}
		}
		List<String> lines = new ArrayList<>();
		lines.add("# Blade traits");
		lines.add("From the blade, head, barrel or armor shell.");
		for (Trait trait : Trait.values()) {
			if (heads.containsKey(trait)) {
				lines.add("- " + trait(trait) + " (" + String.join(", ", heads.get(trait)) + ")");
			}
		}
		lines.add("# Handle traits");
		lines.add("From every other part: guards, hilts, bindings, stocks and linings.");
		for (Trait trait : Trait.values()) {
			if (handles.containsKey(trait) && !heads.containsKey(trait)) {
				lines.add("- " + trait(trait) + " (" + String.join(", ", handles.get(trait)) + ")");
			}
		}
		return lines;
	}

	private static List<String> weapons() {
		List<String> lines = new ArrayList<>();
		lines.add("# Katana (" + Faction.SHARED.displayName + ")");
		lines.add("- Parts: " + parts(PartType.KATANA_BLADE, PartType.TSUBA, PartType.HILT) + ".");
		lines.add("- The only blade that climbs the meito grades. See Meito.");
		for (WeaponType type : WeaponType.values()) {
			lines.add("# " + type.displayName + " (" + type.faction.displayName + ")");
			lines.add("- Parts: " + parts(type.parts()) + ".");
			lines.add("- " + type.ability);
			if (type.faction == Faction.NAVY) {
				lines.add("- Navy design: put together at a Marine Armory Bench.");
			}
		}
		return lines;
	}

	private static List<String> skills() {
		List<String> lines = new ArrayList<>();
		lines.add("# Katana: " + WeaponSkill.IAI_DRAW.displayName);
		lines.add(skillLine(WeaponSkill.IAI_DRAW));
		for (WeaponType type : WeaponType.values()) {
			WeaponSkill skill = WeaponSkill.of(type);
			lines.add("# " + type.displayName + ": " + skill.displayName);
			lines.add(skillLine(skill));
		}
		return lines;
	}

	private static String skillLine(WeaponSkill skill) {
		String damage = skill.damageScale > 0 ? " Each hit does " + Math.round(skill.damageScale * 100) + "% of the weapon's damage." : "";
		return "- " + skill.description + damage + " Ready again after " + skill.cooldownTicks / 20 + " seconds.";
	}

	private static List<String> guns() {
		List<String> lines = new ArrayList<>();
		for (GunType type : GunType.values()) {
			lines.add("# " + type.displayName);
			lines.add("- Parts: " + parts(type.parts()) + ".");
			lines.add("- Shoots " + type.ammo.name().toLowerCase(Locale.ROOT).replace('_', ' ') + "s, held like a "
					+ type.hold.name().toLowerCase(Locale.ROOT) + ".");
			lines.add("- About " + (int) type.baseRange + " blocks of range and " + number(type.baseCooldown / 20.0f)
					+ " seconds to reload, before its parts.");
		}
		return lines;
	}

	private static List<String> cannons() {
		List<String> lines = new ArrayList<>();
		lines.add("# Bores and shells by material");
		lines.add("Bore: damage and range of a Ship Cannon with that bore, compared to a plain Cannon. Shell: hull damage compared to a cannonball, and what its burst does.");
		for (ArsenalMaterial material : ArsenalMaterial.values()) {
			if (material.armorOnly) {
				continue;
			}
			lines.add(material.displayName);
			lines.add("- Bore: " + percent(CannonStats.damage(material)) + " damage, " + percent(CannonStats.range(material)) + " range. As a fuse: "
					+ percent(CannonStats.reload(material)) + " reload time. As a carriage: " + percent(CannonStats.spread(material)) + " spread.");
			StringBuilder shell = new StringBuilder("- Shell: " + percent(CannonShells.hullDamage(material)) + " hull damage.");
			if (CannonShells.burstTrait(material)) {
				shell.append(" Burst: ").append(material.headTrait.displayName).append('.');
			}
			String onShip = CannonShells.shipEffect(material);
			if (onShip != null) {
				shell.append(" On a ship: ").append(onShip);
			}
			lines.add(shell.toString());
		}
		return lines;
	}

	private static String percent(float multiplier) {
		return Math.round(multiplier * 100) + "%";
	}

	private static List<String> navyCasting() {
		List<String> lines = new ArrayList<>();
		for (NavyCastingBlock station : List.of((NavyCastingBlock) ArsenalItems.QUARTERMASTERS_DESK,
				(NavyCastingBlock) ArsenalItems.MARINE_BASE_SMELTERY)) {
			lines.add("- " + station.title() + ": " + station.materials().stream().map(m -> m.displayName)
					.collect(Collectors.joining(", ")) + ".");
		}
		return lines;
	}

	private static List<String> meito() {
		String[] names = {"Ungraded", "Skillful Grade", "Great Grade", "Supreme Grade"};
		List<String> lines = new ArrayList<>();
		for (int grade = 1; grade < names.length; grade++) {
			lines.add("- " + names[grade] + ": " + Meito.wieldRequirement(grade) + ".");
		}
		return lines;
	}

	private static List<String> navyBrass() {
		List<String> lines = new ArrayList<>();
		for (BossGear.Signature signature : BossGear.SIGNATURES) {
			lines.add("- " + signature.name() + " (" + signature.owners() + "): " + signature.description() + ".");
		}
		return lines;
	}

	private static List<String> fruitWeapons() {
		List<String> lines = new ArrayList<>();
		lines.add("# What each animal does");
		for (Beast beast : Beast.values()) {
			lines.add("- " + beast.animal + " (" + beast.signature + "): " + beast.description);
		}
		lines.add("");
		lines.add("# The famous ones");
		lines.add("These come with their fruit already inside. Their owners aren't in the game, so the bosses below carry them instead. Each one may drop it the first time you beat them, and Hard and Expert fights make that likelier.");
		for (FamousFruitWeapon famous : FamousFruitWeapon.values()) {
			lines.add("- " + famous.displayName + ", " + famous.what + ". It ate the " + FruitWeapons.fruitName(famous.fruitId) + ". "
					+ famous.power + " Carried by " + famous.fromName + ".");
		}
		return lines;
	}

	private static List<String> hunterGear() {
		List<String> lines = new ArrayList<>();
		String[] from = {"100,000,000", "300,000,000", "1,000,000,000"};
		for (int threat = 3; threat <= 5; threat++) {
			HunterGear.Loadout loadout = HunterGear.loadout(threat);
			lines.add("# Bounty of " + from[threat - 3] + " or more");
			List<ArsenalMaterial> heads = new ArrayList<>(loadout.weaponHeads());
			if (threat == 5) {
				heads.add(ArsenalMaterial.KAIROSEKI);
			}
			lines.add("- Weapon: " + materialNames(heads) + ".");
			lines.add("- Armor: " + (loadout.armorPieces() == 4 ? "a full set" : loadout.armorPieces() == 1 ? "a coat" : "a coat and hat")
					+ " of " + materialNames(loadout.shells()) + ".");
			lines.add("");
		}
		return lines;
	}

	private static String materialNames(List<ArsenalMaterial> materials) {
		List<String> names = new ArrayList<>();
		for (ArsenalMaterial material : materials) {
			names.add(material.displayName);
		}
		return names.size() < 2 ? String.join("", names)
				: String.join(", ", names.subList(0, names.size() - 1)) + " or " + names.get(names.size() - 1);
	}

	private static List<String> upgrades() {
		List<String> lines = new ArrayList<>();
		lines.add("- Upgrade slots: " + Upgrade.slots(Upgrade.Gear.KATANA) + " on a katana, " + Upgrade.slots(Upgrade.Gear.WEAPON)
				+ " on a hand weapon, " + Upgrade.slots(Upgrade.Gear.GUN) + " on a gun, " + Upgrade.slots(Upgrade.Gear.COAT) + " on each armor piece.");
		lines.add("");
		for (Upgrade upgrade : Upgrade.values()) {
			lines.add(upgrade.displayName + " (up to " + Upgrade.numeral(upgrade.maxLevel) + ")");
			lines.add("- Fits: " + upgrade.fitsName() + ".");
			lines.add("- Each level: " + upgrade.count + " " + upgrade.itemName() + " in your offhand.");
			lines.add("- " + upgrade.description);
			lines.add("");
		}
		return lines;
	}

	// ---- Helpers ----

	private static String trait(Trait trait) {
		return trait.displayName + ": " + trait.description + (trait.active ? "" : " (coming soon)");
	}

	private static String parts(PartType... parts) {
		List<String> names = new ArrayList<>();
		for (PartType part : parts) {
			names.add(part.displayName);
		}
		return String.join(" + ", names);
	}

	private static String itemName(Item item) {
		return new ItemStack(item).getHoverName().getString();
	}

	/** 3.0 as "3", 6.5 as "6.5". */
	private static String number(float value) {
		return value == (int) value ? Integer.toString((int) value) : Float.toString(value);
	}

	private static String signed(float value) {
		return value > 0 ? "+" + number(value) : number(value);
	}
}
