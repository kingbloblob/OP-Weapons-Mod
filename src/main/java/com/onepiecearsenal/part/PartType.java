package com.onepiecearsenal.part;

/**
 * The parts a katana is built from. The blade decides the damage and the head trait;
 * the guard and hilt decide the speed and the handle trait. All three share the durability.
 */
public enum PartType {
	KATANA_BLADE("katana_blade", "Katana Blade", true),
	TSUBA("tsuba", "Tsuba", false),
	HILT("hilt", "Hilt", false);

	private final String id;
	public final String displayName;
	/** True for the part that does the cutting (uses the material's head trait). */
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
