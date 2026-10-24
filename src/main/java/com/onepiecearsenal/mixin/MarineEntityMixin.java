package com.onepiecearsenal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.onepiecearsenal.disguise.MarineDisguise;
import com.onepiecemod.bounty.MarineEntity;

/**
 * Before a Marine picks a player as its target (on sight, when sent to hunt them, or when hit), check whether
 * that player's Marine uniform fools it. If it does, the Marine keeps whatever target it had.
 */
@Mixin(MarineEntity.class)
public abstract class MarineEntityMixin {
	@Inject(method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
	private void arsenal$ignoreDisguisedPlayers(LivingEntity target, CallbackInfo ci) {
		if (target instanceof Player player && MarineDisguise.fools((MarineEntity) (Object) this, player)) {
			ci.cancel();
		}
	}
}
