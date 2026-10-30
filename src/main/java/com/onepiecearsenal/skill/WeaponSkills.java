package com.onepiecearsenal.skill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.meito.Meito;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;

/**
 * What happens when you press the Weapon Skill key (X by default) with an Arsenal weapon in your main hand.
 * Each weapon type has one skill (WeaponSkill). It is on a separate key from the main mod's fighting style
 * techniques (Z), so a cutlass or katana can use both: the style's moves and the weapon's own skill.
 *
 * A skill costs 1 hunger point and 2 uses of the weapon. The cooldown shows on the weapon in your hotbar,
 * and is shared by every weapon of that kind (two cutlasses don't give two leaps).
 */
public final class WeaponSkills {
	/** Uses of the weapon a skill costs. */
	private static final int DURABILITY_COST = 2;
	/** Hunger a skill costs (4 exhaustion = 1 hunger point), and the hunger below which you're too tired. */
	private static final float EXHAUSTION = 4.0f;
	private static final int MIN_FOOD = 2;
	/** Armament Haki makes skill hits 25% stronger, the same as the main mod's techniques. */
	private static final float ARMAMENT_BONUS = 1.25f;
	private static final int LEAP_SAFE_TICKS = 60;
	private static final int RIPOSTE_TICKS = 30;
	private static final int HOOK_SLOW_TICKS = 60;
	private static final int SHIELD_DISABLE_TICKS = 100;

	/** Game time until which a player who used Boarding Leap takes no fall damage. */
	private static final Map<UUID, Long> SAFE_FALL_UNTIL = new HashMap<>();
	/** Game time until which a player who used Riposte is on guard. */
	private static final Map<UUID, Long> GUARD_UNTIL = new HashMap<>();

	private WeaponSkills() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(WeaponSkillPayload.TYPE, WeaponSkillPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(WeaponSkillPayload.TYPE, (payload, context) -> use(context.player()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			SAFE_FALL_UNTIL.remove(handler.player.getUUID());
			GUARD_UNTIL.remove(handler.player.getUUID());
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
				return true;
			}
			long now = level.getGameTime();
			// Boarding Leap: you land on the deck on your feet.
			if (source.is(DamageTypeTags.IS_FALL) && now <= SAFE_FALL_UNTIL.getOrDefault(player.getUUID(), 0L)) {
				SAFE_FALL_UNTIL.remove(player.getUUID());
				return false;
			}
			// Riposte: the first blow while on guard is turned aside and answered, if you still hold the sabre.
			if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
					&& now <= GUARD_UNTIL.getOrDefault(player.getUUID(), 0L)
					&& WeaponSkill.of(player.getMainHandItem()) == WeaponSkill.RIPOSTE) {
				GUARD_UNTIL.remove(player.getUUID());
				level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6f, 1.8f);
				particles(level, attacker, ParticleTypes.SWEEP_ATTACK, 3);
				hit(player, level, attacker, WeaponSkill.RIPOSTE);
				return false;
			}
			return true;
		});
	}

	/** The Weapon Skill key was pressed. */
	public static void use(ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) {
			return;
		}
		ItemStack weapon = player.getMainHandItem();
		WeaponSkill skill = WeaponSkill.of(weapon);
		if (skill == null) {
			tell(player, "Hold an Arsenal katana, cutlass, sabre, axe, jitte, kanabo or spear to use a weapon skill.", ChatFormatting.RED);
			return;
		}
		boolean katana = weapon.getItem() instanceof KatanaItem;
		if (katana ? KatanaStats.parts(weapon).isEmpty() : WeaponStats.parts(weapon).isEmpty()) {
			tell(player, "Forge it at a Shipwright's Anvil first.", ChatFormatting.RED);
			return;
		}
		if (Breakage.isBroken(weapon)) {
			Breakage.tellBroken(player, weapon);
			return;
		}
		if (katana && !Meito.worthy(player, OnePieceBridge.swordGrade(weapon))) {
			tell(player, weapon.getHoverName().getString() + " refuses you.", ChatFormatting.RED);
			return;
		}
		if (player.getCooldowns().isOnCooldown(weapon)) {
			int seconds = Mth.ceil(player.getCooldowns().getCooldownPercent(weapon, 0.0f) * skill.cooldownTicks / 20.0f);
			tell(player, skill.displayName + " is ready in " + Math.max(1, seconds) + "s", ChatFormatting.GRAY);
			return;
		}
		if (!player.isCreative() && player.getFoodData().getFoodLevel() <= MIN_FOOD) {
			tell(player, "You're too hungry to fight like that. Eat something!", ChatFormatting.RED);
			return;
		}

		if (!perform(skill, player, (ServerLevel) player.level())) {
			tell(player, "No enemy in reach for " + skill.displayName + ".", ChatFormatting.GRAY);
			return; // A skill that needs a target costs nothing when there isn't one.
		}
		player.getCooldowns().addCooldown(weapon, skill.cooldownTicks);
		player.causeFoodExhaustion(EXHAUSTION);
		weapon.hurtAndBreak(DURABILITY_COST, player, EquipmentSlot.MAINHAND);
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		tell(player, skill.displayName + "!", ChatFormatting.GOLD, ChatFormatting.BOLD);
	}

	/** Does the skill. False if it needed an enemy and there was none, so nothing happened. */
	static boolean perform(WeaponSkill skill, ServerPlayer player, ServerLevel level) {
		Vec3 look = player.getLookAngle();
		Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();
		long now = level.getGameTime();

		switch (skill) {
			case IAI_DRAW -> { // One heavy cut on the enemy you're looking at.
				List<LivingEntity> target = OnePieceBridge.enemiesInLine(player, level, 5.0, 0.8, 1);
				if (target.isEmpty()) {
					return false;
				}
				hit(player, level, target.get(0), skill);
				particles(level, target.get(0), ParticleTypes.SWEEP_ATTACK, 4);
				particles(level, target.get(0), ParticleTypes.CRIT, 12);
				sound(level, player, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f);
			}
			case BOARDING_LEAP -> { // Leap forward and up. Hits on the way down are cutlass boarding hits (WeaponEffects).
				player.setDeltaMovement(flatLook.x * 1.3, 0.75, flatLook.z * 1.3);
				sendMotion(player);
				player.resetFallDistance();
				SAFE_FALL_UNTIL.put(player.getUUID(), now + LEAP_SAFE_TICKS);
				level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 8, 0.3, 0.05, 0.3, 0.02);
				sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f);
			}
			case RIPOSTE -> { // On guard: the next blow is answered (ALLOW_DAMAGE in initialize).
				GUARD_UNTIL.put(player.getUUID(), now + RIPOSTE_TICKS);
				particles(level, player, ParticleTypes.ENCHANTED_HIT, 10);
				sound(level, player, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.4f);
			}
			case CLEAVE -> { // A wide swing in front that knocks shields aside.
				for (LivingEntity target : OnePieceBridge.enemiesInCone(player, level, 3.5, 0.3, 6)) {
					breakGuard(target);
					hit(player, level, target, skill);
				}
				Vec3 front = player.getEyePosition().add(flatLook.scale(1.5));
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y - 0.4, front.z, 5, 0.9, 0.1, 0.9, 0);
				sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f);
			}
			case ARREST_HOOK -> { // Drag the enemy you're looking at to you and hold them.
				List<LivingEntity> target = OnePieceBridge.enemiesInLine(player, level, 6.0, 0.8, 1);
				if (target.isEmpty()) {
					return false;
				}
				LivingEntity caught = target.get(0);
				Vec3 toPlayer = player.position().subtract(caught.position());
				Vec3 pull = toPlayer.normalize().scale(Math.min(1.2, toPlayer.length() * 0.3));
				caught.setDeltaMovement(pull.x, 0.3, pull.z);
				caught.needsSync = true;
				caught.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, HOOK_SLOW_TICKS, 2), player);
				if (OnePieceBridge.isFruitUser(caught)) {
					OnePieceBridge.drainFruitPower(caught, HOOK_SLOW_TICKS);
				}
				hit(player, level, caught, skill);
				particles(level, caught, ParticleTypes.CRIT, 8);
				sound(level, player, SoundEvents.FISHING_BOBBER_RETRIEVE, 0.8f);
			}
			case GROUND_SMASH -> { // Everyone around is hurt, thrown up and slowed.
				for (LivingEntity target : OnePieceBridge.enemiesAround(player, level, 4.0)) {
					hit(player, level, target, skill);
					Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
					target.setDeltaMovement(away.x, 0.6, away.z);
					target.needsSync = true;
					target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), player);
				}
				level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(), 3, 1.2, 0.1, 1.2, 0);
				level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 30, 2.0, 0.1, 2.0, 0.05);
				sound(level, player, SoundEvents.ANVIL_LAND, 0.6f);
			}
			case SPEAR_CHARGE -> { // Dash forward through everyone in the way and drive them back.
				for (LivingEntity target : OnePieceBridge.enemiesInLine(player, level, 6.0, 1.0, 8)) {
					hit(player, level, target, skill);
					target.setDeltaMovement(flatLook.x * 0.9, 0.25, flatLook.z * 0.9);
					target.needsSync = true;
				}
				player.setDeltaMovement(flatLook.x * 1.8, 0.1, flatLook.z * 1.8);
				sendMotion(player);
				level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 10, 0.3, 0.05, 0.3, 0.02);
				sound(level, player, SoundEvents.PLAYER_ATTACK_STRONG, 0.9f);
			}
		}
		return true;
	}

	/**
	 * One hit of a skill: a share of the weapon's attack damage (grades, levels, upgrades and Strength all count),
	 * then the weapon's material traits, like a normal swing.
	 */
	private static void hit(ServerPlayer player, ServerLevel level, LivingEntity target, WeaponSkill skill) {
		float attack = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float damage = attack * skill.damageScale * (OnePieceBridge.armamentActive(player) ? ARMAMENT_BONUS : 1.0f);
		if (target.hurtServer(level, player.damageSources().playerAttack(player), damage)) {
			TraitEffects.onHit(player, level, target, TraitEffects.traits(player.getMainHandItem()), attack, true);
		}
	}

	/** A target holding up a shield drops it, and a player's shield can't be raised again for 5 seconds. */
	private static void breakGuard(LivingEntity target) {
		if (target.isBlocking()) {
			ItemStack shield = target.getUseItem();
			target.stopUsingItem();
			if (target instanceof Player player) {
				player.getCooldowns().addCooldown(shield, SHIELD_DISABLE_TICKS);
			}
			target.level().playSound(null, target.blockPosition(), SoundEvents.SHIELD_BREAK.value(), SoundSource.PLAYERS, 0.8f, 1.0f);
		}
	}

	/** True while the player is on guard from Riposte. */
	static boolean onGuard(ServerPlayer player) {
		return player.level().getGameTime() <= GUARD_UNTIL.getOrDefault(player.getUUID(), 0L);
	}

	/** Tells the player's game about a push. needsSync doesn't reach the player's own game, so the push would be lost. */
	private static void sendMotion(ServerPlayer player) {
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	private static void particles(ServerLevel level, LivingEntity at, ParticleOptions particle, int count) {
		level.sendParticles(particle, at.getX(), at.getY() + at.getBbHeight() / 2, at.getZ(), count, 0.4, 0.4, 0.4, 0.05);
	}

	private static void sound(ServerLevel level, ServerPlayer player, SoundEvent sound, float pitch) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0f, pitch);
	}

	private static void tell(ServerPlayer player, String message, ChatFormatting... style) {
		player.sendOverlayMessage(Component.literal(message).withStyle(style));
	}
}
