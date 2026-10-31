package com.onepiecearsenal.tool;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.salvage.Breakage;

/**
 * What makes each weapon type different, whatever it's made of (the material traits are in TraitEffects).
 * The axe's shield breaking, the kanabo's knockback and the spear's reach come from the item and its numbers
 * instead (ArsenalItems, WeaponStats).
 */
public final class WeaponEffects {
	/** Extra damage of a cutlass's charged hit while falling. */
	private static final float BOARDING_DAMAGE = 2.0f;
	/** How long a jitte's charged hit drains a fruit user. Shorter than a seastone blade's 3 seconds. */
	private static final int ARREST_TICKS = 30;
	/** Chance a sneaking sabre wielder turns aside a hit from the front. */
	private static final float PARRY_CHANCE = 1.0f / 3.0f;
	/** Fishman Gloves' Water Shock: a share of the punch's damage that goes through armor, more when it's wet. */
	private static final float WATER_SHOCK = 0.25f;
	private static final float WET_SHOCK = 0.5f;

	private WeaponEffects() {
	}

	public static void initialize() {
		AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
			if (player instanceof ServerPlayer attacker && level instanceof ServerLevel serverLevel
					&& target instanceof LivingEntity living && !player.isSpectator()
					&& player.getAttackStrengthScale(0.5f) >= 0.9f
					&& player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon && !Breakage.isBroken(player.getMainHandItem())) {
				switch (weapon.type()) {
					case CUTLASS -> {
						if (TraitEffects.isCritical(attacker)) {
							TraitEffects.extraHit(attacker, serverLevel, living, BOARDING_DAMAGE);
						}
					}
					case JITTE -> {
						if (OnePieceBridge.isFruitUser(living)) {
							OnePieceBridge.drainFruitPower(living, ARREST_TICKS);
						}
					}
					case FISHMAN_GLOVES -> { // The shock goes through the water inside them, so armor doesn't stop it.
						float share = wet(attacker) || wet(living) ? WET_SHOCK : WATER_SHOCK;
						float shock = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * share;
						TraitEffects.extraHit(attacker, serverLevel, living, shock, attacker.damageSources().indirectMagic(attacker, attacker));
						serverLevel.sendParticles(ParticleTypes.SPLASH, living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(),
								12, 0.3, 0.3, 0.3, 0.1);
					}
					default -> {
					}
				}
			}
			return InteractionResult.PASS;
		});

		// Impact and Reject Palms: sneak and the Dial catches blows from the front (see DialCharge).
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(entity instanceof ServerPlayer defender && DialCharge.tryCatch(defender, source, amount)));

		// Sabre parry: sneak to stand on guard, and some hits from the front glance off the blade.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer defender && defender.isShiftKeyDown()
					&& source.getDirectEntity() instanceof LivingEntity attacker && attacker == source.getEntity()
					&& defender.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon && weapon.type() == WeaponType.MARINE_SABRE
					&& !Breakage.isBroken(defender.getMainHandItem())
					&& inFront(defender, attacker) && defender.getRandom().nextFloat() < PARRY_CHANCE) {
				ItemStack sabre = defender.getMainHandItem();
				sabre.hurtAndBreak(1, defender, EquipmentSlot.MAINHAND);
				defender.level().playSound(null, defender.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 1.6f);
				return false;
			}
			return true;
		});
	}

	/** In water or out in the rain: Fish-Man Karate is strongest with water all around. */
	public static boolean wet(LivingEntity entity) {
		return entity.isInWaterOrRain();
	}

	/** True if the attacker is within about 70 degrees of where the defender is looking. */
	private static boolean inFront(LivingEntity defender, LivingEntity attacker) {
		Vec3 look = defender.getLookAngle().multiply(1, 0, 1).normalize();
		Vec3 toAttacker = attacker.position().subtract(defender.position()).multiply(1, 0, 1).normalize();
		return look.dot(toAttacker) > 0.35;
	}
}
