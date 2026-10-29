package com.onepiecearsenal.material;

import java.util.Set;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.ArsenalArmorItem;
import com.onepiecearsenal.tool.FlintlockItem;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;

/**
 * Makes the traits happen. Katana hit traits only fire on fully charged hits (wait for the attack bar to fill),
 * the same rule the One Piece Mod's own weapons use; flintlock hit traits fire on every shot that lands.
 * Light and Riveted change the numbers instead (KatanaStats, GunStats).
 */
public class TraitEffects {
	/** How long a seastone blade drains a fruit user. Shorter than the Seastone Jitte's 5 seconds. */
	private static final int SEASTONE_DRAIN_TICKS = 60;
	/** Chance a Rusty blade poisons, when it's raining. */
	private static final float RUSTY_CHANCE = 0.1f;
	/** True while a trait's own extra damage is being dealt, so it can't set the traits off again. */
	private static boolean dealingExtra = false;

	public static void initialize() {
		AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
			if (player instanceof ServerPlayer attacker && level instanceof ServerLevel serverLevel
					&& target instanceof LivingEntity living && !player.isSpectator()
					&& player.getAttackStrengthScale(0.5f) >= 0.9f) {
				ItemStack weapon = player.getMainHandItem();
				if (weapon.getItem() instanceof KatanaItem) {
					onHit(attacker, serverLevel, living, KatanaStats.traits(weapon));
				}
			}
			return InteractionResult.PASS; // PASS lets the normal hit happen too.
		});

		// Flintlock shots are "thrown" damage from the shooter (see the One Piece Mod's Shot.java).
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (!dealingExtra && !blocked && source.is(DamageTypes.THROWN) && source.getEntity() instanceof ServerPlayer shooter
					&& shooter.level() instanceof ServerLevel serverLevel && shooter.getMainHandItem().getItem() instanceof FlintlockItem) {
				onHit(shooter, serverLevel, entity, GunStats.traits(shooter.getMainHandItem()));
			}
		});

		// Seastone armor: a fruit user who hits the wearer up close is drained.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (!dealingExtra && entity instanceof ServerPlayer wearer && source.getDirectEntity() instanceof LivingEntity attacker
					&& attacker == source.getEntity() && OnePieceBridge.isFruitUser(attacker)) {
				for (EquipmentSlot slot : ARMOR_SLOTS) {
					if (hasTrait(wearer.getItemBySlot(slot), Trait.SEASTONE)) {
						OnePieceBridge.drainFruitPower(attacker, SEASTONE_DRAIN_TICKS);
						return;
					}
				}
			}
		});

		// Once a second: fruit users holding or wearing something with seastone in its lining feel weak.
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : level.getPlayers(p -> true)) {
				if (OnePieceBridge.isFruitUser(player) && wearsOrHolds(player, Trait.DRAINING)) {
					player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, true));
				}
			}
		});
	}

	private static void onHit(ServerPlayer player, ServerLevel level, LivingEntity target, Set<Trait> traits) {
		if (traits.contains(Trait.SEASTONE) && OnePieceBridge.isFruitUser(target)) {
			OnePieceBridge.drainFruitPower(target, SEASTONE_DRAIN_TICKS);
		}
		if (traits.contains(Trait.RUSTY) && level.isRaining() && level.getRandom().nextFloat() < RUSTY_CHANCE) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), player);
		}
		if (traits.contains(Trait.REGULATION) && OnePieceBridge.isPirateNpc(target)) {
			extraHit(player, level, target, 1.0f);
		}
	}

	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private static boolean wearsOrHolds(ServerPlayer player, Trait trait) {
		if (hasTrait(player.getMainHandItem(), trait) || hasTrait(player.getOffhandItem(), trait)) {
			return true;
		}
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (hasTrait(player.getItemBySlot(slot), trait)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasTrait(ItemStack stack, Trait trait) {
		if (stack.getItem() instanceof KatanaItem) {
			return KatanaStats.traits(stack).contains(trait);
		}
		if (stack.getItem() instanceof FlintlockItem) {
			return GunStats.traits(stack).contains(trait);
		}
		return stack.getItem() instanceof ArsenalArmorItem && ArmorStats.traits(stack).contains(trait);
	}

	/**
	 * Bonus damage on top of the normal hit, done the same way as the One Piece Mod's weapons: "thrown" damage,
	 * so it can't set off these effects again, then the invulnerable time is reset so the normal hit still lands.
	 */
	private static void extraHit(ServerPlayer player, ServerLevel level, LivingEntity target, float damage) {
		dealingExtra = true;
		try {
			target.hurtServer(level, player.damageSources().thrown(player, player), damage);
		} finally {
			dealingExtra = false;
		}
		target.setInvulnerableTime(0);
	}
}
