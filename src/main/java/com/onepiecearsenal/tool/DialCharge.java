package com.onepiecearsenal.tool;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.salvage.Breakage;

/**
 * The Skypiea Dials on the Impact and Reject Palms. A Dial catches the force of a blow and keeps it until you let it out.
 * Sneak with one in your main hand and hits from the front (blows, arrows, bullets, blasts) go into the Dial instead of
 * into you, until it is full. The Weapon Skill lets it all out at the enemy in front (WeaponSkills).
 *
 * An Impact Dial lets out what it caught, and the kick hurts your arm a little. A Reject Dial lets it out three times as
 * hard, and the blow goes through you too, as in Skypiea. Neither can take your last half heart.
 * Change the numbers here to tune them.
 */
public final class DialCharge {
	/** The most damage a Dial holds. A blow that finds it nearly full is still caught whole, but the extra is lost. */
	public static final float CAPACITY = 20.0f;
	/** How hard each Dial lets its charge out, and how much of the charge hurts the wielder when it does. */
	private static final float IMPACT_POWER = 1.0f;
	private static final float IMPACT_RECOIL = 0.1f;
	private static final float REJECT_POWER = 3.0f;
	private static final float REJECT_RECOIL = 0.5f;

	private DialCharge() {
	}

	/** True for an Impact or Reject Palm. */
	public static boolean isDial(ItemStack stack) {
		return stack.getItem() instanceof ArsenalWeaponItem weapon && weapon.type().head.isDial();
	}

	private static boolean isReject(ItemStack stack) {
		return stack.getItem() instanceof ArsenalWeaponItem weapon && weapon.type() == WeaponType.REJECT_PALM;
	}

	public static float stored(ItemStack stack) {
		return stack.getOrDefault(ArsenalComponents.DIAL_CHARGE, 0.0f);
	}

	public static void set(ItemStack stack, float charge) {
		if (charge <= 0) {
			stack.remove(ArsenalComponents.DIAL_CHARGE);
		} else {
			stack.set(ArsenalComponents.DIAL_CHARGE, Math.min(CAPACITY, charge));
		}
	}

	/** The damage a release of this charge does (before the weapon's own punch and Haki are added). */
	public static float power(ItemStack stack, float charge) {
		return charge * (isReject(stack) ? REJECT_POWER : IMPACT_POWER);
	}

	/** The damage a release of this charge does to the wielder. */
	public static float recoil(ItemStack stack, float charge) {
		return charge * (isReject(stack) ? REJECT_RECOIL : IMPACT_RECOIL);
	}

	/**
	 * A blow is about to land on this player. If they're sneaking with a forged Dial palm and the blow comes from the
	 * front, the Dial catches it: returns true and the blow does no damage.
	 */
	public static boolean tryCatch(ServerPlayer player, DamageSource source, float amount) {
		ItemStack dial = player.getMainHandItem();
		if (!player.isShiftKeyDown() || !isDial(dial) || WeaponStats.parts(dial).isEmpty() || Breakage.isBroken(dial)
				|| source.is(DamageTypeTags.BYPASSES_SHIELD) || source.getEntity() == null && source.getDirectEntity() == null) {
			return false;
		}
		Vec3 from = source.getSourcePosition();
		if (from == null || !inFront(player, from)) {
			return false;
		}
		float before = stored(dial);
		if (before >= CAPACITY) {
			player.sendOverlayMessage(Component.literal(dial.getHoverName().getString() + " is full! Let it out.").withStyle(ChatFormatting.RED));
			return false;
		}
		set(dial, before + amount);
		dial.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.blockPosition(), SoundEvents.SHULKER_CLOSE, SoundSource.PLAYERS, 0.8f, 1.4f);
		Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(0.6));
		level.sendParticles(ParticleTypes.CLOUD, front.x, front.y - 0.3, front.z, 6, 0.15, 0.15, 0.15, 0.02);
		player.sendOverlayMessage(Component.literal(String.format("%s: %.0f / %.0f caught", dial.getHoverName().getString(), stored(dial), CAPACITY))
				.withStyle(ChatFormatting.AQUA));
		return true;
	}

	/** Hurts the wielder with the release's kick, never past their last half heart. */
	public static void kick(ServerPlayer player, float recoil) {
		float damage = Math.min(recoil, player.getHealth() - 1.0f);
		if (damage > 0) {
			player.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), damage);
		}
	}

	/** The tooltip lines. */
	public static void describe(ItemStack stack, Consumer<Component> textConsumer) {
		if (!isDial(stack)) {
			return;
		}
		float charge = stored(stack);
		textConsumer.accept(Component.literal(String.format("Caught: %.0f / %.0f (lets out %.0f damage)", charge, CAPACITY, power(stack, charge)))
				.withStyle(charge > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY));
	}

	/** True if the point is within about 70 degrees of where the player is looking. */
	private static boolean inFront(ServerPlayer player, Vec3 from) {
		Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
		Vec3 toSource = from.subtract(player.position()).multiply(1, 0, 1);
		return toSource.lengthSqr() < 1.0e-4 || look.dot(toSource.normalize()) > 0.35;
	}
}
