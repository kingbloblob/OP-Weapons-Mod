package com.onepiecearsenal.material;

/**
 * Whose gear a material belongs to. Players are always pirates: Navy materials are looted,
 * stolen or bought from Marines, never handed out.
 */
public enum Faction {
	PIRATE("Pirate"),
	NAVY("Navy"),
	SHARED("Common");

	public final String displayName;

	Faction(String displayName) {
		this.displayName = displayName;
	}
}
