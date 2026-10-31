package com.onepiecearsenal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import com.onepiecearsenal.armorset.SetBonuses;
import com.onepiecearsenal.haki.HakiImbue;

/**
 * As a hit starts, armor set bonuses change its damage: a full Kairoseki set takes less from Devil Fruit users,
 * a Jolly Roger set deals more to Marines, and so on (SetBonuses.adjustDamage). Fabric has no event that can change
 * a hit's damage, so this changes the amount hurtServer was given before anything else reads it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float arsenal$setBonusDamage(float amount, ServerLevel level, DamageSource source) {
		return SetBonuses.adjustDamage((LivingEntity) (Object) this, source, amount);
	}

	/** A Haki-coated weapon or bullet hits harder (HakiImbue.adjustDamage). */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float arsenal$hakiCoatDamage(float amount, ServerLevel level, DamageSource source) {
		return HakiImbue.adjustDamage((LivingEntity) (Object) this, source, amount);
	}
}
