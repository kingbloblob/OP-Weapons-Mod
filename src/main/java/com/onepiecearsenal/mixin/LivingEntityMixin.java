package com.onepiecearsenal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import com.onepiecearsenal.armorset.SetBonuses;
import com.onepiecearsenal.upgrade.Upgrades;
import com.onepiecearsenal.tool.GauntletStats;
import com.onepiecearsenal.haki.HakiImbue;

/**
 * As a hit starts, armor set bonuses change its damage: a full Kairoseki set takes less from Devil Fruit users,
 * a Jolly Roger set deals more to Marines, and so on (SetBonuses.adjustDamage). Gauntlets' Guard works the same way. Fabric has no event that can change
 * a hit's damage, so this changes the amount hurtServer was given before anything else reads it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float arsenal$setBonusDamage(float amount, ServerLevel level, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		// Gauntlets' Guard first (a sneaking wearer's arms take a quarter of a hit from the front), then Pacifista Core
		// armor against shots, then set bonuses.
		return SetBonuses.adjustDamage(self, source, Upgrades.guard(self, source, GauntletStats.guard(self, source, amount)));
	}

	/** A Haki-coated weapon or bullet hits harder (HakiImbue.adjustDamage). */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float arsenal$hakiCoatDamage(float amount, ServerLevel level, DamageSource source) {
		return HakiImbue.adjustDamage((LivingEntity) (Object) this, source, amount);
	}
}
