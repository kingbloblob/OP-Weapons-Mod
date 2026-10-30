package com.onepiecearsenal.skill;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.tool.ArsenalWeaponItem;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.WeaponType;

/**
 * The move each kind of Arsenal weapon does when you press the Weapon Skill key (X by default).
 * Damage is a share of the weapon's own attack damage, so better parts, grades and upgrades make the skill hit
 * harder too. Cooldowns are in ticks (20 ticks = 1 second). What each skill actually does is in WeaponSkills.
 * Guns have no skill: reloading is their key already.
 */
public enum WeaponSkill {
	//          name               cooldown  damage x  description
	IAI_DRAW("Iai Draw", 200, 1.75f,
			"Draw and cut in one motion: a heavy strike on the enemy you're looking at, up to 5 blocks away."),
	BOARDING_LEAP("Boarding Leap", 120, 0.0f,
			"Leap forward like you're jumping onto an enemy deck. No fall damage for 3 seconds, and hits on the way down are boarding hits."),
	RIPOSTE("Riposte", 200, 1.5f,
			"Stand on guard for 1.5 seconds. The first blow that comes at you is turned aside and answered with a counter-cut."),
	CLEAVE("Cleave", 120, 1.0f,
			"A wide swing that hits up to 6 enemies in front of you and knocks their shields aside."),
	ARREST_HOOK("Arrest Hook", 240, 0.75f,
			"Hook the enemy you're looking at (up to 6 blocks) and drag them to you. They're slowed for 3 seconds, and fruit users are drained."),
	GROUND_SMASH("Ground Smash", 200, 1.25f,
			"Slam the ground: every enemy within 4 blocks is hurt, thrown into the air and slowed."),
	SPEAR_CHARGE("Spear Charge", 160, 1.25f,
			"Charge 6 blocks forward, running through every enemy in your way and driving them back.");

	public final String displayName;
	public final int cooldownTicks;
	/** The share of the weapon's attack damage each hit of the skill does. */
	public final float damageScale;
	public final String description;

	WeaponSkill(String displayName, int cooldownTicks, float damageScale, String description) {
		this.displayName = displayName;
		this.cooldownTicks = cooldownTicks;
		this.damageScale = damageScale;
		this.description = description;
	}

	/** The skill of a hand weapon type. */
	public static WeaponSkill of(WeaponType type) {
		return switch (type) {
			case CUTLASS -> BOARDING_LEAP;
			case MARINE_SABRE -> RIPOSTE;
			case BOARDING_AXE -> CLEAVE;
			case JITTE -> ARREST_HOOK;
			case KANABO -> GROUND_SMASH;
			case SPEAR -> SPEAR_CHARGE;
		};
	}

	/** The skill of the weapon in this stack, or null for anything that isn't a forged Arsenal hand weapon. */
	public static WeaponSkill of(ItemStack stack) {
		if (stack.getItem() instanceof KatanaItem) {
			return IAI_DRAW;
		}
		return stack.getItem() instanceof ArsenalWeaponItem weapon ? of(weapon.type()) : null;
	}

	/** The tooltip line, like "[X] Cleave: A wide swing...". The key shown is whatever the player bound it to. */
	public static void describe(ItemStack stack, Consumer<Component> textConsumer) {
		WeaponSkill skill = of(stack);
		if (skill != null) {
			textConsumer.accept(Component.literal("[").append(Component.keybind("key.arsenal.weapon_skill")).append("] " + skill.displayName
					+ " (" + skill.cooldownTicks / 20 + "s): " + skill.description).withStyle(ChatFormatting.YELLOW));
		}
	}
}
