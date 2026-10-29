package com.onepiecearsenal.tool;

import com.onepiecearsenal.material.Faction;
import com.onepiecearsenal.part.PartType;

/**
 * The hand weapons besides the katana (which has its own files, since only katanas climb the meito grades).
 * Each is built from a head, a binding and a handle. Change the numbers here to tune a weapon type.
 *
 * damage = head material damage x damage scale + bonus damage (an iron sword adds 5, an iron axe 8).
 * speed = base speed + the binding's and handle's speed, averaged (a sword is -2.4, an axe -3.1).
 * durability = the three parts' durability, averaged, x durability scale.
 *
 * Navy weapons (faction NAVY) are the Marines' own designs. Players are pirates, so these are copies
 * of gear taken from Marines.
 */
public enum WeaponType {
	//             id              name            faction          head                    binding              handle
	CUTLASS("cutlass", "Cutlass", Faction.PIRATE, PartType.CUTLASS_BLADE, PartType.HAND_GUARD, PartType.GRIP,
			// damage scale, bonus damage, base speed, durability scale
			0.9f, 0.0f, -2.2f, 0.9f,
			"Boarding: +2 damage on charged hits while falling, like jumping onto a deck."),
	MARINE_SABRE("marine_sabre", "Marine Sabre", Faction.NAVY, PartType.SABRE_BLADE, PartType.HAND_GUARD, PartType.GRIP,
			1.0f, 0.0f, -2.4f, 1.1f,
			"Parry: while sneaking, turns aside 1 in 3 hits from enemies in front of you."),
	BOARDING_AXE("boarding_axe", "Boarding Axe", Faction.PIRATE, PartType.AXE_HEAD, PartType.BINDING, PartType.HAFT,
			1.0f, 2.5f, -3.0f, 1.0f,
			"Breaks through shields and chops wood like an axe."),
	JITTE("jitte", "Jitte", Faction.NAVY, PartType.JITTE_ROD, PartType.BINDING, PartType.GRIP,
			0.75f, 0.0f, -2.0f, 1.3f,
			"Arrest: charged hits briefly drain a Devil Fruit user's power, whatever it's made of."),
	KANABO("kanabo", "Kanabo", Faction.SHARED, PartType.CLUB_HEAD, PartType.STUDS, PartType.HAFT,
			1.1f, 3.0f, -3.2f, 1.2f,
			"Crushing: knocks enemies far back."),
	SPEAR("spear", "Spear", Faction.SHARED, PartType.SPEARHEAD, PartType.BINDING, PartType.HAFT,
			0.9f, 1.0f, -2.6f, 1.0f,
			"Reach: hits enemies 1.5 blocks further away.");

	private final String id;
	public final String displayName;
	public final Faction faction;
	public final PartType head;
	public final PartType binding;
	public final PartType handle;
	public final float damageScale;
	public final float bonusDamage;
	public final float baseSpeed;
	public final float durabilityScale;
	/** What this weapon type does that others don't, for the tooltip. WeaponEffects makes it happen. */
	public final String ability;

	WeaponType(String id, String displayName, Faction faction, PartType head, PartType binding, PartType handle,
			float damageScale, float bonusDamage, float baseSpeed, float durabilityScale, String ability) {
		this.id = id;
		this.displayName = displayName;
		this.faction = faction;
		this.head = head;
		this.binding = binding;
		this.handle = handle;
		this.damageScale = damageScale;
		this.bonusDamage = bonusDamage;
		this.baseSpeed = baseSpeed;
		this.durabilityScale = durabilityScale;
		this.ability = ability;
	}

	/** The item id, like "boarding_axe". */
	public String id() {
		return id;
	}

	/** The parts in the order they are saved on the weapon: head, binding, handle. */
	public PartType[] parts() {
		return new PartType[] {head, binding, handle};
	}

	/** The weapon type whose head this part is, or null. */
	public static WeaponType withHead(PartType part) {
		for (WeaponType type : values()) {
			if (type.head == part) {
				return type;
			}
		}
		return null;
	}
}
