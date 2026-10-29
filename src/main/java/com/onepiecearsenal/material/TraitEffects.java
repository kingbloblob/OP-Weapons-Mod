package com.onepiecearsenal.material;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.mixin.FoodDataAccessor;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.ArsenalArmorItem;
import com.onepiecearsenal.tool.ArsenalGunItem;
import com.onepiecearsenal.tool.ArsenalWeaponItem;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;

/**
 * Makes the traits happen. Hand weapon hit traits only fire on fully charged hits (wait for the attack bar to fill),
 * the same rule the One Piece Mod's own weapons use; gun hit traits fire on every shot that lands.
 * Light, Riveted and the other number traits change the numbers instead (TraitStats).
 */
public class TraitEffects {
	/** How long a seastone blade drains a fruit user. Shorter than the Seastone Jitte's 5 seconds. */
	private static final int SEASTONE_DRAIN_TICKS = 60;
	/** Chance a Rusty blade poisons, when it's raining. */
	private static final float RUSTY_CHANCE = 0.1f;
	/** Chance each Plated armor piece deflects a projectile. */
	private static final float PLATED_CHANCE = 0.15f;
	/** Share of a Keen weapon's hit that ignores armor. */
	private static final float KEEN_SHARE = 0.15f;
	/** Share of a Black Blade's hit that its Haki coat drives through armor. */
	private static final float BLACK_BLADE_SHARE = 0.25f;
	/** Berries of bounty a Greed weapon adds for each Marine you beat, on top of the main mod's own. */
	private static final long GREED_BOUNTY = 50_000L;
	/** Hunger exhaustion Endurance armor takes away each second (4 exhaustion = 1 hunger point). */
	private static final float ENDURANCE_PER_SECOND = 0.05f;
	/** How often Edible-forged gear mends 1 durability: every 5 seconds. */
	private static final int MEND_TICKS = 100;
	/** Sea creatures and Sea Kings, for Predator. */
	private static final TagKey<EntityType<?>> SEA_CREATURES = TagKey.create(Registries.ENTITY_TYPE, ArsenalMod.id("sea_creatures"));
	/** True while a trait's own extra damage is being dealt, so it can't set the traits off again. */
	private static boolean dealingExtra = false;

	public static void initialize() {
		AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
			if (player instanceof ServerPlayer attacker && level instanceof ServerLevel serverLevel
					&& target instanceof LivingEntity living && !player.isSpectator()
					&& player.getAttackStrengthScale(0.5f) >= 0.9f) {
				ItemStack weapon = player.getMainHandItem();
				if (weapon.getItem() instanceof KatanaItem || weapon.getItem() instanceof ArsenalWeaponItem) {
					onHit(attacker, serverLevel, living, traits(weapon), (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE), true);
				}
			}
			return InteractionResult.PASS; // PASS lets the normal hit happen too.
		});

		// Gun shots are "thrown" damage from the shooter (see the One Piece Mod's Shot.java). The traits come
		// from the gun that fired (ArsenalGunItem.firing), so an offhand gun works and a thrown knife
		// doesn't pick up the traits of a gun you happen to hold.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			ItemStack gun = ArsenalGunItem.firing();
			if (!dealingExtra && !blocked && !gun.isEmpty() && source.is(DamageTypes.THROWN) && source.getEntity() instanceof ServerPlayer shooter
					&& shooter.level() instanceof ServerLevel serverLevel) {
				entity.damageCooldownTime = 0; // The shot just hit, so without this its extra trait hits would bounce off.
				onHit(shooter, serverLevel, entity, GunStats.traits(gun), baseDamageTaken, false);
			}
		});

		// Plated armor: each Plated piece has a chance to deflect a bullet, arrow or anything else thrown.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer wearer && source.is(DamageTypeTags.IS_PROJECTILE)) {
				for (EquipmentSlot slot : ARMOR_SLOTS) {
					if (hasTrait(wearer.getItemBySlot(slot), Trait.PLATED) && wearer.getRandom().nextFloat() < PLATED_CHANCE) {
						wearer.level().playSound(null, wearer.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.4f, 1.8f);
						return false;
					}
				}
			}
			return true;
		});

		// Gilded and Greed: what the weapon in your hand earns you when you win a fight.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer killer && entity.level() instanceof ServerLevel level && entity != killer) {
				Set<Trait> traits = traits(killer.getMainHandItem());
				if (traits.contains(Trait.GILDED) && (entity instanceof Enemy || OnePieceBridge.isNpc(entity))) {
					entity.spawnAtLocation(level, new ItemStack(OnePieceBridge.berry(), 1 + level.getRandom().nextInt(3)));
				}
				if (traits.contains(Trait.GREED) && OnePieceBridge.isMarine(entity)) {
					OnePieceBridge.raiseBounty(killer, GREED_BOUNTY, "The Navy noticed your Greed weapon.");
				}
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

		// Once a second: fruit users holding or wearing something with seastone in its lining feel weak,
		// Endurance armor takes the edge off hunger, and Edible-forged gear mends a little.
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 20 != 0) {
				return;
			}
			boolean mend = level.getGameTime() % MEND_TICKS == 0;
			for (ServerPlayer player : level.getPlayers(p -> true)) {
				if (OnePieceBridge.isFruitUser(player) && wearsOrHolds(player, Trait.DRAINING)) {
					player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, true));
				}
				if (wearsOrHolds(player, Trait.ENDURANCE)) {
					FoodDataAccessor exhaustion = (FoodDataAccessor) player.getFoodData();
					exhaustion.arsenal$setExhaustionLevel(Math.max(0.0f, exhaustion.arsenal$getExhaustionLevel() - ENDURANCE_PER_SECOND));
				}
				if (mend) {
					for (EquipmentSlot slot : EquipmentSlot.values()) {
						ItemStack stack = player.getItemBySlot(slot);
						if (stack.isDamaged() && hasTrait(stack, Trait.EDIBLE_FORGED)) {
							stack.setDamageValue(stack.getDamageValue() - 1);
						}
					}
				}
			}
		});
	}

	/**
	 * A hit by an Arsenal weapon, or a Black Leg kick in Arsenal boots (MainModLinks). hitDamage is the weapon's
	 * attack damage (melee) or the shot's damage (guns); melee is false for gun shots, which can't crit.
	 */
	public static void onHit(ServerPlayer player, ServerLevel level, LivingEntity target, Set<Trait> traits, float hitDamage, boolean melee) {
		RandomSource random = level.getRandom();
		if (traits.contains(Trait.SEASTONE) && OnePieceBridge.isFruitUser(target)) {
			OnePieceBridge.drainFruitPower(target, SEASTONE_DRAIN_TICKS);
		}
		if (traits.contains(Trait.RUSTY) && level.isRaining() && random.nextFloat() < RUSTY_CHANCE) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), player);
		}
		if (traits.contains(Trait.REGULATION) && OnePieceBridge.isPirateNpc(target)) {
			extraHit(player, level, target, 1.0f);
		}
		if (traits.contains(Trait.PREDATOR) && target.getType().builtInRegistryHolder().is(SEA_CREATURES)) {
			extraHit(player, level, target, 2.0f);
		}
		if (traits.contains(Trait.KEEN)) {
			// Magic damage skips armor, so this part of the hit goes straight through.
			extraHit(player, level, target, hitDamage * KEEN_SHARE, player.damageSources().indirectMagic(player, player));
		}
		if (traits.contains(Trait.BLACK_BLADE) && !wears(target, Trait.SEASTONE)) {
			// The Haki coat hits past armor, the same way Keen does. Seastone armor stops it.
			extraHit(player, level, target, hitDamage * BLACK_BLADE_SHARE, player.damageSources().indirectMagic(player, player));
			level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
					6, 0.3, 0.4, 0.3, 0.05);
		}
		if (traits.contains(Trait.RECOIL)) {
			target.knockback(0.6, player.getX() - target.getX(), player.getZ() - target.getZ(), player.damageSources().playerAttack(player), 0.0f);
		}
		if (traits.contains(Trait.SERRATED) && melee && isCritical(player)) {
			target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), player); // Bleeding.
		}
		if (traits.contains(Trait.LASER_ETCHED)) {
			target.igniteForSeconds(2.0f);
		}
		if (traits.contains(Trait.FRONTIER_SCIENCE)) {
			if (random.nextBoolean()) {
				target.igniteForSeconds(3.0f);
			} else {
				target.setTicksFrozen(target.getTicksRequiredToFreeze() + 60);
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), player);
			}
		}
	}

	/** The same rule as vanilla critical hits: falling, not on a ladder, not in water, not riding, not sprinting. */
	public static boolean isCritical(ServerPlayer player) {
		return player.fallDistance > 0 && !player.onGround() && !player.onClimbable() && !player.isInWater()
				&& !player.isPassenger() && !player.isSprinting();
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

	private static boolean wears(LivingEntity entity, Trait trait) {
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (hasTrait(entity.getItemBySlot(slot), trait)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasTrait(ItemStack stack, Trait trait) {
		return traits(stack).contains(trait);
	}

	/** The traits of any Arsenal weapon or armor piece. Empty for anything else. */
	public static Set<Trait> traits(ItemStack stack) {
		if (stack.getItem() instanceof KatanaItem) {
			return KatanaStats.traits(stack);
		}
		if (stack.getItem() instanceof ArsenalWeaponItem) {
			return WeaponStats.traits(stack);
		}
		if (stack.getItem() instanceof ArsenalGunItem) {
			return GunStats.traits(stack);
		}
		return stack.getItem() instanceof ArsenalArmorItem ? ArmorStats.traits(stack) : EnumSet.noneOf(Trait.class);
	}

	/**
	 * Bonus damage on top of the normal hit, done the same way as the One Piece Mod's weapons: "thrown" damage,
	 * so it can't set off these effects again, then the invulnerable time is reset so the normal hit still lands.
	 */
	public static void extraHit(ServerPlayer player, ServerLevel level, LivingEntity target, float damage) {
		extraHit(player, level, target, damage, player.damageSources().thrown(player, player));
	}

	private static void extraHit(ServerPlayer player, ServerLevel level, LivingEntity target, float damage, DamageSource source) {
		dealingExtra = true;
		try {
			target.hurtServer(level, source, damage);
		} finally {
			dealingExtra = false;
		}
		target.damageCooldownTime = 0; // 26.3: the hit-immunity timer (setInvulnerableTime no longer resets it).
	}
}
