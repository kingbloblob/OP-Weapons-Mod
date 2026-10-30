package com.onepiecearsenal.tool;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
					default -> {
					}
				}
			}
			return InteractionResult.PASS;
		});

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

	/** True if the attacker is within about 70 degrees of where the defender is looking. */
	private static boolean inFront(LivingEntity defender, LivingEntity attacker) {
		Vec3 look = defender.getLookAngle().multiply(1, 0, 1).normalize();
		Vec3 toAttacker = attacker.position().subtract(defender.position()).multiply(1, 0, 1).normalize();
		return look.dot(toAttacker) > 0.35;
	}
}
