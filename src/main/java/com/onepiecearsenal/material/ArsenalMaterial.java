package com.onepiecearsenal.material;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecemod.world.WorldBlocks;

/**
 * The materials parts can be made of. Change the numbers here to make a material stronger or weaker.
 * For comparison, a vanilla iron sword does 6 damage and lasts 250 uses.
 *
 * damage = attack damage the blade adds (a katana with a 5-damage blade hits as hard as an iron sword).
 * durability = uses, averaged over all the parts.
 * speed = attack speed the guard and hilt add (0 = a normal sword).
 */
public enum ArsenalMaterial {
	//          id                   name                     tier faction          damage durability speed  head trait          handle trait     repaired with
	DRIFTWOOD("driftwood", "Driftwood", 1, Faction.PIRATE, 3.0f, 80, 0.2f, Trait.BUOYANT, Trait.LIGHT, () -> ArsenalItems.DRIFTWOOD),
	SHIP_IRON("ship_iron", "Ship Iron", 1, Faction.PIRATE, 5.0f, 220, 0.0f, Trait.RUSTY, Trait.RIVETED, () -> Items.IRON_INGOT),
	MARINE_STEEL("marine_steel", "Marine Standard Steel", 1, Faction.NAVY, 5.0f, 260, 0.0f, Trait.REGULATION, Trait.ISSUED, () -> ArsenalItems.MARINE_STEEL_INGOT),
	KAIROSEKI("kairoseki", "Kairoseki", 3, Faction.NAVY, 7.0f, 700, -0.1f, Trait.SEASTONE, Trait.DRAINING, () -> WorldBlocks.SEASTONE_INGOT);

	private final String id;
	public final String displayName;
	public final int tier;
	public final Faction faction;
	public final float damage;
	public final int durability;
	public final float speed;
	public final Trait headTrait;
	public final Trait handleTrait;
	private final Supplier<Item> repairItem;

	ArsenalMaterial(String id, String displayName, int tier, Faction faction, float damage, int durability, float speed,
			Trait headTrait, Trait handleTrait, Supplier<Item> repairItem) {
		this.id = id;
		this.displayName = displayName;
		this.tier = tier;
		this.faction = faction;
		this.damage = damage;
		this.durability = durability;
		this.speed = speed;
		this.headTrait = headTrait;
		this.handleTrait = handleTrait;
		this.repairItem = repairItem;
	}

	/** The name saved on items, like "ship_iron". Also used in the recipe files. */
	public String id() {
		return id;
	}

	/** The item this material is made from, which also repairs it. */
	public Item repairItem() {
		return repairItem.get();
	}

	/** Returns null for an unknown id, for example a material removed in an update. */
	public static ArsenalMaterial byId(String id) {
		for (ArsenalMaterial material : values()) {
			if (material.id.equals(id)) {
				return material;
			}
		}
		return null;
	}
}
