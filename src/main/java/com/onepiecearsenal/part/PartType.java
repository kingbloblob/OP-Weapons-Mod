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
	// Armor shells (one per piece) and the lining that goes inside any of them.
	CROWN("crown", "Hat Crown", true),
	COAT_PLATES("coat_plates", "Coat Plates", true),
	SASH_WEAVE("sash_weave", "Sash Weave", true),
	BOOT_SOLES("boot_soles", "Boot Soles", true),
	LINING("lining", "Lining", false);

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
}
