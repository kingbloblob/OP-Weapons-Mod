package com.onepiecearsenal.material;

/**
 * Something special a material does. Each material has one trait when it is the blade (the "head")
 * and one when it is the guard or hilt (the "handle"). TraitEffects.java makes them happen, and
 * TraitStats.java the ones that change a weapon's numbers instead.
 */
public enum Trait {
	// ---- Head traits (from the blade) ----
	BUOYANT("Buoyant", "Floats if dropped at sea.", false),
	RUSTY("Rusty", "In the rain, hits can poison.", true),
	REGULATION("Regulation", "Marine-made: +1 damage against pirates.", true),
	SEASTONE("Seastone", "Drains a Devil Fruit user's power on hit. As armor, drains fruit users who hit you.", true),
	RECOIL("Recoil", "Hits knock enemies back further.", true),
	SERRATED("Serrated", "Critical hits make the target bleed.", true),
	GILDED("Gilded", "Enemies you defeat drop Berries.", true),
	KEEN("Keen", "15% of each hit goes straight through armor.", true),
	LASER_ETCHED("Laser-etched", "Charged hits scorch the target.", true),
	EDIBLE_FORGED("Edible-forged", "Slowly mends itself while you hold or wear it.", true),
	UNBREAKABLE_GRAIN("Unbreakable Grain", "Lasts twice as long.", true),
	BLACK_BLADE("Black Blade", "A permanent coat of Armament Haki: 25% of each hit goes through armor, unless it's seastone.", true),
	FRONTIER_SCIENCE("Frontier Science", "Charged hits burn or freeze the target.", true),
	WATERPROOF("Waterproof", "As armor: swim faster.", true),
	ENDURANCE("Endurance", "As armor: you get hungry more slowly.", true),

	// ---- Handle traits (from the guard and hilt) ----
	LIGHT("Light", "Weighs little: swords swing faster and guns reload quicker.", true),
	RIVETED("Riveted", "+10% durability.", true),
	ISSUED("Issued", "Repaired for free at a Marine base while disguised.", false),
	DRAINING("Draining", "Fruit users who hold or wear it feel weak.", true),
	HEAVY("Heavy", "+1 armor toughness while you hold or wear it.", true),
	PREDATOR("Predator", "+2 damage against sea creatures and Sea Kings.", true),
	GREED("Greed", "Defeating Marines raises your bounty more.", true),
	FOLDED("Folded", "Levels up 25% faster.", false),
	PLATED("Plated", "As armor: each piece has a 10% chance to deflect a bullet or arrow.", true),
	REFORGED("Reforged", "Survives breaking once.", false),
	LEGENDARY_HULL("Legendary Hull", "Hard to knock back while you hold or wear it.", true),
	INHERIT("Inherit", "Keeps its levels when reforged.", false),
	ULTRA_ALLOY("Ultra-alloy", "Can't burn in fire or lava.", true);

	public final String displayName;
	public final String description;
	/** False for traits that are planned but don't do anything yet. Their tooltip says "coming soon". */
	public final boolean active;

	Trait(String displayName, String description, boolean active) {
		this.displayName = displayName;
		this.description = description;
		this.active = active;
	}
}
