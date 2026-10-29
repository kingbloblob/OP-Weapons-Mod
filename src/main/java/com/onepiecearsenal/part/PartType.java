package com.onepiecearsenal.part;

/**
 * The parts weapons are built from. The head part (blade or barrel) decides the damage and the head trait;
 * the others decide the speed or aim and give their handle trait. All parts share the durability.
 */
public enum PartType {
	KATANA_BLADE("katana_blade", "Katana Blade", true),
	TSUBA("tsuba", "Tsuba", false),
	HILT("hilt", "Hilt", false),
	BARREL("barrel", "Barrel", true),
	LOCK("firing_lock", "Firing Lock", false),
	STOCK("stock", "Stock", false);

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
