package com.onepiecearsenal.tool;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.onepiecearsenal.part.PartType;

/**
 * The tools: built from a head and a handle or two at the Shipwright's Anvil, like the hand weapons. Change the numbers
 * here to tune a tool. The head's material decides what it can dig and how fast (mining_level and mining_speed in
 * materials.json); every part adds to its durability and gives its trait, the same as a weapon.
 *
 * damage = head material damage x damage scale + bonus damage (a vanilla iron pickaxe adds 3, an iron axe 8).
 * attack speed = base speed + the handle's speed (a pickaxe is -2.8, an axe -3.1).
 * mining speed = head material mining speed x mining scale.
 * durability = the parts' durability, averaged, x durability scale.
 *
 * Tools are for everyone (no Navy designs), but a Navy material's head still needs a Marine Armory Bench, like any gear.
 */
public enum ToolType {
	//           id              name            parts
	PICKAXE("pickaxe", "Pickaxe", new PartType[] {PartType.PICK_HEAD, PartType.BINDING, PartType.HAFT}, "mineable/pickaxe",
			// damage scale, bonus damage, base speed, mining scale, durability scale
			0.6f, 0.0f, -2.8f, 1.0f, 1.0f,
			"Digs stone, ores and metal."),
	HATCHET("hatchet", "Hatchet", new PartType[] {PartType.HATCHET_HEAD, PartType.BINDING, PartType.HAFT}, "mineable/axe",
			1.0f, 1.0f, -3.1f, 1.0f, 1.0f,
			"Chops wood and strips logs. Lighter than a Boarding Axe, and a worse weapon."),
	SHOVEL("shovel", "Shovel", new PartType[] {PartType.SHOVEL_BLADE, PartType.BINDING, PartType.HAFT}, "mineable/shovel",
			0.7f, 0.0f, -3.0f, 1.0f, 1.0f,
			"Digs dirt, sand and gravel, and makes paths."),
	HOE("hoe", "Hoe", new PartType[] {PartType.HOE_BLADE, PartType.BINDING, PartType.HAFT}, "mineable/hoe",
			0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
			"Tills soil for crops, and cuts hay, leaves and moss."),
	SHEARS("shears", "Shears", new PartType[] {PartType.SHEAR_BLADES, PartType.BINDING, PartType.GRIP}, null,
			0.0f, 0.0f, 0.0f, 1.0f, 1.0f,
			"Shears sheep, and cuts wool, leaves, vines and cobwebs."),
	SLEDGEHAMMER("sledgehammer", "Sledgehammer", new PartType[] {PartType.HAMMER_HEAD, PartType.BINDING, PartType.HAFT}, "mineable/pickaxe",
			0.8f, 1.0f, -3.4f, 0.4f, 2.5f,
			"Smashes a 3x3 square of stone at a time, slowly, wearing a point for each block. Sneak to dig just one."),
	BUCKET("bucket", "Bucket", new PartType[] {PartType.BUCKET_SHELL, PartType.GRIP}, null,
			0.0f, 0.0f, 0.0f, 1.0f, 1.0f,
			"Carries water or lava: more bucketfuls the better its shell (tier 1 holds 1, tier 4 holds 4).");

	private final String id;
	public final String displayName;
	private final PartType[] parts;
	/** The blocks it digs at its head's speed, or null for tools that don't dig like a pickaxe (shears, the bucket). */
	public final TagKey<Block> mineable;
	public final float damageScale;
	public final float bonusDamage;
	public final float baseSpeed;
	public final float miningScale;
	public final float durabilityScale;
	/** What it does, for the tooltip. */
	public final String ability;

	ToolType(String id, String displayName, PartType[] parts, String mineable, float damageScale, float bonusDamage, float baseSpeed,
			float miningScale, float durabilityScale, String ability) {
		this.id = id;
		this.displayName = displayName;
		this.parts = parts;
		this.mineable = mineable == null ? null : TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(mineable));
		this.damageScale = damageScale;
		this.bonusDamage = bonusDamage;
		this.baseSpeed = baseSpeed;
		this.miningScale = miningScale;
		this.durabilityScale = durabilityScale;
		this.ability = ability;
	}

	/** The item id, like "pickaxe". */
	public String id() {
		return id;
	}

	/** The parts in the order they are saved on the tool, head first. */
	public PartType[] parts() {
		return parts.clone();
	}

	public PartType head() {
		return parts[0];
	}

	/** True for the tools that hit things (the hoe, shears and bucket have no attack of their own). */
	public boolean hits() {
		return damageScale > 0 || bonusDamage > 0;
	}

	/** The tool type whose head this part is, or null. */
	public static ToolType withHead(PartType part) {
		for (ToolType type : values()) {
			if (type.parts[0] == part) {
				return type;
			}
		}
		return null;
	}
}
