package com.onepiecearsenal.ammo;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.onepiecemod.weapon.GunAddons;
import com.onepiecemod.weapon.Shot;

/**
 * Dial Rounds: bullets with a Sky Island Dial packed behind the ball. One Dial and eight Bullets make eight.
 * The Dial lets out what it stored when the ball lands.
 */
public enum DialRound implements GunAddons.Round {
	/** Sets what it hits on fire. */
	FLAME("flame_dial_round", "Flame Dial Round", "onepiece:flame_dial", "Sets what it hits on fire."),
	/** Lets out a stored blow where it lands: hurts and throws back everyone close by. Never breaks blocks. */
	IMPACT("impact_dial_round", "Impact Dial Round", "onepiece:impact_dial", "Bursts where it lands, hurting and throwing back everyone close by."),
	/** A blast of wind that hurls what it hits away and up. */
	BREATH("breath_dial_round", "Breath Dial Round", "onepiece:breath_dial", "A gust that hurls what it hits away and into the air."),
	/** A flash of stored light that blinds everyone close to where it lands, and makes them glow. */
	FLASH("flash_dial_round", "Flash Dial Round", "onepiece:flash_dial", "A flash that blinds everyone close to where it lands.");

	private static final int FLAME_SECONDS = 5;
	private static final double IMPACT_RADIUS = 2.5;
	private static final float IMPACT_DAMAGE = 4.0f;
	private static final double BREATH_PUSH = 1.6;
	private static final double FLASH_RADIUS = 4.0;
	private static final int FLASH_TICKS = 80;

	private final String id;
	public final String displayName;
	/** The One Piece Mod Dial it's made with. */
	public final String dialItem;
	public final String description;

	DialRound(String id, String displayName, String dialItem, String description) {
		this.id = id;
		this.displayName = displayName;
		this.dialItem = dialItem;
		this.description = description;
	}

	public String id() {
		return id;
	}

	@Override
	public void onHit(ServerPlayer shooter, ServerLevel level, LivingEntity target, ItemStack round, ItemStack gun) {
		switch (this) {
			case FLAME -> target.igniteForSeconds(FLAME_SECONDS);
			case BREATH -> {
				Vec3 away = target.position().subtract(shooter.position()).multiply(1, 0, 1);
				away = away.lengthSqr() < 1.0E-4 ? shooter.getLookAngle().multiply(1, 0, 1) : away.normalize();
				target.push(away.x * BREATH_PUSH, 0.6, away.z * BREATH_PUSH);
				target.needsSync = true;
			}
			default -> {
			}
		}
	}

	@Override
	public void onLand(ServerPlayer shooter, ServerLevel level, Vec3 at, @Nullable LivingEntity target, ItemStack round) {
		switch (this) {
			case FLAME -> level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.03);
			case IMPACT -> {
				level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
				level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
				for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(IMPACT_RADIUS),
						victim -> Shot.canHit(shooter, victim))) {
					victim.damageCooldownTime = 0;
					victim.hurtServer(level, shooter.damageSources().playerAttack(shooter), IMPACT_DAMAGE);
					Vec3 away = victim.position().subtract(at).multiply(1, 0, 1);
					away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
					victim.push(away.x * 1.0, 0.4, away.z * 1.0);
					victim.needsSync = true;
				}
			}
			case BREATH -> level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 16, 0.3, 0.3, 0.3, 0.08);
			case FLASH -> {
				level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 30, 0.4, 0.4, 0.4, 0.15);
				level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.0f, 1.2f);
				for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(FLASH_RADIUS),
						victim -> Shot.canHit(shooter, victim))) {
					victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, FLASH_TICKS, 0), shooter);
					victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, FLASH_TICKS + 40, 0), shooter);
				}
			}
		}
	}

	@Override
	public @Nullable ParticleOptions trail(ItemStack round) {
		return switch (this) {
			case FLAME -> ParticleTypes.FLAME;
			case IMPACT -> ParticleTypes.CRIT;
			case BREATH -> ParticleTypes.CLOUD;
			case FLASH -> ParticleTypes.END_ROD;
		};
	}
}
