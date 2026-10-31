package com.onepiecearsenal.ammo;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.onepiecearsenal.cannon.CannonShells;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecemod.weapon.GunAddons;

/**
 * Musket Balls: bullets cast from an Arsenal material, eight to a craft.
 *
 *  - Damage: 0.5 x (the material's damage - 5) on top of the gun's (Ship Iron = a plain Bullet, Black Blade Steel +1.5,
 *    Driftwood -1).
 *  - The creature it hits gets the material's blade trait, the same traits a Cannon Shell's burst passes on: Kairoseki
 *    drains Devil Fruit users, Sea King Bone makes them bleed, Pacifista Alloy burns, and so on. A trait the gun's own
 *    barrel already gives isn't given twice.
 */
public final class MusketBalls implements GunAddons.Round {
	/** The "shot damage" the traits that scale with it (Keen) work from: about a flintlock's shot. */
	private static final float TRAIT_HIT_DAMAGE = 8.0f;

	/** Extra damage a ball of this material does, on top of the gun's. */
	public static float bonusDamage(ArsenalMaterial material) {
		return Math.max(-1.5f, 0.5f * (material.damage - 5.0f));
	}

	/** Does a ball of this material pass its trait on to what it hits? */
	public static boolean hasTrait(ArsenalMaterial material) {
		return CannonShells.burstTrait(material);
	}

	@Override
	public float bonusDamage(ServerPlayer shooter, ServerLevel level, LivingEntity target, ItemStack round) {
		ArsenalMaterial material = PartItem.material(round);
		return material == null ? 0.0f : bonusDamage(material);
	}

	@Override
	public void onHit(ServerPlayer shooter, ServerLevel level, LivingEntity target, ItemStack round, ItemStack gun) {
		ArsenalMaterial material = PartItem.material(round);
		if (material == null || !hasTrait(material) || !target.isAlive()) {
			return;
		}
		Set<Trait> traits = EnumSet.of(material.headTrait);
		traits.removeAll(GunStats.traits(gun));
		if (traits.isEmpty()) {
			return;
		}
		target.damageCooldownTime = 0; // The shot just hit, so without this the trait's own extra hit would bounce off.
		TraitEffects.onHit(shooter, level, target, traits, TRAIT_HIT_DAMAGE, false);
		if (material.headTrait == Trait.SERRATED) {
			target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), shooter); // Bone splinters: bleeding.
		}
	}

	@Override
	public @Nullable ParticleOptions trail(ItemStack round) {
		ArsenalMaterial material = PartItem.material(round);
		if (material == null) {
			return null;
		}
		return switch (material.headTrait) {
			case SEASTONE -> ParticleTypes.BUBBLE_POP;
			case LASER_ETCHED, SPARKING -> ParticleTypes.SMALL_FLAME;
			case FRONTIER_SCIENCE -> ParticleTypes.ELECTRIC_SPARK;
			default -> null;
		};
	}
}
