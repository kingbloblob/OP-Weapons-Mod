package com.onepiecearsenal.material;

/**
 * Something special a material does. Each material has one trait when it is the blade (the "head")
 * and one when it is the guard or hilt (the "handle"). TraitEffects.java makes them happen.
 */
public enum Trait {
	// ---- Head traits (from the blade) ----
	BUOYANT("Buoyant", "Floats if dropped at sea.", false),
	RUSTY("Rusty", "In the rain, hits can poison.", true),
	REGULATION("Regulation", "Marine-made: +1 damage against pirates.", true),
	SEASTONE("Seastone", "Drains a Devil Fruit user's power on hit. As armor, drains fruit users who hit you.", true),

	// ---- Handle traits (from the guard and hilt) ----
	LIGHT("Light", "Weighs little: swords swing faster and guns reload quicker.", true),
	RIVETED("Riveted", "+10% durability.", true),
	ISSUED("Issued", "Repaired for free at a Marine base while disguised.", false),
	DRAINING("Draining", "Fruit users who hold or wear it feel weak.", true);

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
