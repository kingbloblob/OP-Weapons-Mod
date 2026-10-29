package com.onepiecearsenal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.disguise.MarineDisguise;

/**
 * Before a Marine picks a player as its target (on sight, when sent to hunt them, or when hit), check whether
 * that player's Marine uniform fools it. If it does, the Marine keeps whatever target it had.
 *
 * MarineEntity doesn't override setTarget, so this hooks Mob's and checks that the mob is a Marine.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
	@Inject(method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
	private void arsenal$ignoreDisguisedPlayers(LivingEntity target, CallbackInfo ci) {
		// Runs for every mob in the world, so the cheap checks go first. Targets are only picked on the server.
		LivingEntity self = (LivingEntity) (Object) this;
		if (target instanceof Player player && OnePieceBridge.isMarine(self) && !self.level().isClientSide()
				&& MarineDisguise.fools(self, player)) {
			ci.cancel();
		}
	}
}
