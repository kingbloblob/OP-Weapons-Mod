package com.onepiecearsenal.part;

/**
 * The parts weapons and armor are built from. The head part (blade, barrel, or an armor shell) decides the
 * damage or defense and the head trait; the others decide speed, aim or comfort and give their handle trait.
 * All parts share the durability.
 */
public enum PartType {
	KATANA_BLADE("katana_blade", "Katana Blade", true),
	TSUBA("tsuba", "Tsuba", false),
	HILT("hilt", "Hilt", false),
	BARREL("barrel", "Barrel", true),
	LOCK("firing_lock", "Firing Lock", false),
	STOCK("stock", "Stock", false),
	LONG_BARREL("long_barrel", "Long Barrel", true),
	CANNON_BORE("cannon_bore", "Cannon Bore", true),
	FUSE("fuse", "Fuse", false),
	// The ship cannon's carriage (its bore and fuse are the hand cannon's), and cannon ammo (see cannon/).
	GUN_CARRIAGE("gun_carriage", "Gun Carriage", false),
	CANNON_SHELL("cannon_shell", "Cannon Shell", true),
	// Bullets of any material for flintlocks and muskets (see ammo/MusketBalls).
	MUSKET_BALL("musket_ball", "Musket Ball", true),
	// Heads of the other hand weapons (see WeaponType), and the bindings and handles they share.
	CUTLASS_BLADE("cutlass_blade", "Cutlass Blade", true),
	SABRE_BLADE("sabre_blade", "Sabre Blade", true),
	AXE_HEAD("axe_head", "Axe Head", true),
	JITTE_ROD("jitte_rod", "Jitte Rod", true),
	CLUB_HEAD("club_head", "Kanabo Head", true),
	SPEARHEAD("spearhead", "Spearhead", true),
	// Fishman Gloves' knuckles, and the Skypiea Dials the Impact and Reject Palms are built around (their recipes take a
	// Dial Shell as well as the casing's material).
	KNUCKLES("knuckles", "Knuckle Plates", true),
	IMPACT_CORE("impact_core", "Impact Dial Core", true),
	REJECT_CORE("reject_core", "Reject Dial Core", true),
	HAND_GUARD("hand_guard", "Hand Guard", false),
	BINDING("binding", "Binding", false),
	STUDS("studs", "Studs", false),
	GRIP("grip", "Grip", false),
	HAFT("haft", "Haft", false),
	// Armor shells (one per piece) and the lining that goes inside any of them.
	CROWN("crown", "Hat Crown", true),
	COAT_PLATES("coat_plates", "Coat Plates", true),
	SASH_WEAVE("sash_weave", "Sash Weave", true),
	BOOT_SOLES("boot_soles", "Boot Soles", true),
	LINING("lining", "Lining", false),
	// A shield's face (its rim is a Binding and its strap a Grip, see ShieldStats), and the plates of a pair of
	// gauntlets (lined with a Lining, see GauntletStats).
	SHIELD_FACE("shield_face", "Shield Face", true),
	GAUNTLET_PLATES("gauntlet_plates", "Gauntlet Plates", true),
	// Tool heads (see tool/ToolType). They share the weapons' Bindings, Hafts and Grips.
	PICK_HEAD("pick_head", "Pick Head", true),
	HATCHET_HEAD("hatchet_head", "Hatchet Head", true),
	SHOVEL_BLADE("shovel_blade", "Shovel Blade", true),
	HOE_BLADE("hoe_blade", "Hoe Blade", true),
	SHEAR_BLADES("shear_blades", "Shear Blades", true),
	HAMMER_HEAD("hammer_head", "Sledgehammer Head", true),
	BUCKET_SHELL("bucket_shell", "Bucket Shell", true);

	private final String id;
	public final String displayName;
	/** True for the part that does the cutting or shooting (uses the material's head trait). */
	public final boolean head;

	PartType(String id, String displayName, boolean head) {
		this.id = id;
		this.displayName = displayName;
		this.head = head;
	}

	public String id() {
		return id;
	}

	/** The Skypiea Dial cores. Only crafted with Dial Shells: the Navy stations don't know how to make them. */
	public boolean isDial() {
		return this == IMPACT_CORE || this == REJECT_CORE;
	}

	/** Armor shells, gauntlet plates and linings: the only parts hides and leathers (armor-only materials) can make. */
	public boolean isArmor() {
		return this == CROWN || this == COAT_PLATES || this == SASH_WEAVE || this == BOOT_SOLES || this == LINING || this == GAUNTLET_PLATES;
	}
}
