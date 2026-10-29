package com.onepiecearsenal.cannon;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Trait;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.part.PartItem;
import com.onepiecemod.ship.ShipEntity;
import com.onepiecemod.shipcombat.CannonAddons;
import com.onepiecemod.shipcombat.CannonballEntity;
import com.onepiecemod.shipcombat.ShipHull;

/**
 * Cannon Shells: cannon ammo made of an Arsenal material. Hold a stack when you fire (M aboard a ship, or right-click
 * a cannon on land) and every cannon loads that shell instead of a cannonball.
 *
 *  - Hull damage: 0.6 + 0.08 x the material's damage, of a cannonball's (Ship Iron = the same as a cannonball).
 *  - Everyone caught in the burst (not your own crew) gets the material's blade trait, as if your gun shot them:
 *    a Kairoseki shell drains Devil Fruit users, Sea King Bone ones bleed, Pacifista Alloy ones burn, and so on.
 *  - Some shells also do something to the ship they hit (shipEffect()).
 * The One Piece Mod's own special shot (Chain Shot, Fire Shot, Grapeshot) still works as before.
 */
public final class CannonShells implements CannonAddons.Shell {
	/** How far from the burst people get the shell's trait. */
	private static final double BURST_RADIUS = 3.0;
	/** The "shot damage" the traits that scale with it (Keen) work from: about what the blast does. */
	private static final float TRAIT_HIT_DAMAGE = 8.0f;
	/** How long a Kairoseki shell drains the fruit users aboard the ship it hits. */
	private static final int SEASTONE_SHIP_DRAIN_TICKS = 100;
	private static final int BURN_TICKS = 100;
	private static final int TANGLE_TICKS = 100;

	private CannonShells() {
	}

	/** Makes Ship Cannons and Cannon Shells work with the One Piece Mod's cannons. */
	public static void initialize() {
		CannonAddons.registerCannon(ArsenalItems.SHIP_CANNON_BLOCK, (level, state, blockEntity) ->
				blockEntity instanceof ShipCannonBlockEntity cannon ? CannonStats.profile(cannon.parts()) : CannonAddons.Profile.PLAIN);
		CannonAddons.registerShell(ArsenalItems.CANNON_SHELL, new CannonShells());
	}

	/** Share of a cannonball's hull damage a shell of this material does. */
	public static float hullDamage(ArsenalMaterial material) {
		return 0.6f + 0.08f * material.damage;
	}

	/** What a shell of this material does to the ship it hits, for tooltips and the guide. Null if nothing extra. */
	public static String shipEffect(ArsenalMaterial material) {
		return switch (material.headTrait) {
			case SEASTONE -> "Drains every Devil Fruit user aboard.";
			case LASER_ETCHED -> "Sets her burning.";
			case FRONTIER_SCIENCE -> "Sets her burning or freezes her rigging.";
			default -> null;
		};
	}

	@Override
	public float hullDamage(ItemStack shell) {
		ArsenalMaterial material = PartItem.material(shell);
		return material == null ? 1.0f : hullDamage(material);
	}

	@Override
	public void onHitShip(ServerLevel level, CannonballEntity ball, ShipEntity ship, Vec3 at) {
		ArsenalMaterial material = PartItem.material(ball.getItem());
		if (material == null) {
			return;
		}
		switch (material.headTrait) {
			case SEASTONE -> {
				for (LivingEntity crew : aboard(ship)) {
					if (OnePieceBridge.isFruitUser(crew)) {
						OnePieceBridge.drainFruitPower(crew, SEASTONE_SHIP_DRAIN_TICKS);
					}
				}
			}
			case LASER_ETCHED -> ShipHull.ignite(ship, BURN_TICKS);
			case FRONTIER_SCIENCE -> {
				if (level.getRandom().nextBoolean()) {
					ShipHull.ignite(ship, BURN_TICKS);
				} else {
					ShipHull.chain(ship, TANGLE_TICKS);
					level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 30, 1.0, 1.0, 1.0, 0.05);
				}
			}
			default -> {
			}
		}
	}

	@Override
	public void onBurst(ServerLevel level, CannonballEntity ball, Vec3 at) {
		ArsenalMaterial material = PartItem.material(ball.getItem());
		ServerPlayer shooter = ball.shooter();
		if (material == null || shooter == null) {
			return; // The traits work like the shooter's own gun, so they need a shooter.
		}
		if (!burstTrait(material)) {
			return;
		}
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(BURST_RADIUS),
				victim -> victim != shooter && victim.isAlive() && !ball.onFiringShip(victim))) {
			TraitEffects.onHit(shooter, level, victim, EnumSet.of(material.headTrait), TRAIT_HIT_DAMAGE, false);
			if (material.headTrait == Trait.SERRATED) {
				victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), shooter); // Bone splinters: bleeding.
			}
		}
	}

	/** The players and riders aboard a ship. */
	private static List<LivingEntity> aboard(ShipEntity ship) {
		List<LivingEntity> crew = new ArrayList<>(ShipEntity.playersAboard(ship));
		for (Entity passenger : ship.getPassengers()) {
			if (passenger instanceof LivingEntity living && !crew.contains(living)) {
				crew.add(living);
			}
		}
		return crew;
	}

	/** The blade traits a shell's burst passes on (the rest only work on a weapon or armor). */
	private static final Set<Trait> BURST_TRAITS = EnumSet.of(Trait.SEASTONE, Trait.RUSTY, Trait.REGULATION, Trait.KEEN,
			Trait.BLACK_BLADE, Trait.RECOIL, Trait.SERRATED, Trait.LASER_ETCHED, Trait.FRONTIER_SCIENCE);

	/** Does a shell of this material pass its trait on to the people in its burst? */
	public static boolean burstTrait(ArsenalMaterial material) {
		return BURST_TRAITS.contains(material.headTrait);
	}
}
