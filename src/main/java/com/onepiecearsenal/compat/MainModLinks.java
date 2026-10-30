package com.onepiecearsenal.compat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import com.onepiecearsenal.disguise.MarineDisguise;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.tool.ArmorStats;
import com.onepiecearsenal.tool.ArsenalArmorItem;

/**
 * Where Arsenal gear plugs into the One Piece Mod's own systems, beyond the seastone and bounty traits in
 * TraitEffects:
 *  - Black Leg kicks land with the boots: Arsenal boots add their head trait (Seastone boots drain fruit users,
 *    Laser-etched ones scorch) and hit harder the more armor they give.
 *  - The News Coo and the Marines hear about Arsenal deeds: capturing a Navy station, raising a blade to a new
 *    grade, and striking down Marines while wearing their uniform all raise your bounty and make the paper.
 *
 * Change the numbers here to tune it.
 */
public final class MainModLinks {
	/** Extra kick damage for each armor point the boots give (1 to 3), on top of Black Leg's own +5. */
	private static final float KICK_DAMAGE_PER_ARMOR = 1.0f;
	/** Bounty for capturing a Navy station. */
	private static final long STATION_BOUNTY = 5_000_000L;
	/** Bounty for raising a blade to Skillful, Great and Supreme Grade (index = the new grade). */
	private static final long[] GRADE_BOUNTY = {0, 2_000_000L, 20_000_000L, 100_000_000L};
	/** Bounty for beating a Marine while dressed as one, at most once a day. */
	private static final long IMPOSTOR_BOUNTY = 3_000_000L;
	private static final long TICKS_PER_DAY = 24_000L;

	/** Players who just used a whetstone the gates allowed, and their sword's grade before it. */
	private static final Map<ServerPlayer, Integer> WHETTING = new WeakHashMap<>();
	/** The day each player (by name) last made the paper as a Marine impostor. Forgotten on restart. */
	private static final Map<String, Long> IMPOSTOR_DAY = new HashMap<>();

	private MainModLinks() {
	}

	/** Call after Meito.initialize(), so a whetstone the Meito gates refuse never gets here. */
	public static void initialize() {
		// Black Leg Style: a charged kick in Arsenal boots.
		AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
			if (player instanceof ServerPlayer kicker && level instanceof ServerLevel serverLevel
					&& target instanceof LivingEntity living && !player.isSpectator()
					&& player.getAttackStrengthScale(0.5f) >= 0.9f && OnePieceBridge.isKicking(kicker)) {
				kick(kicker, serverLevel, living, kicker.getItemBySlot(EquipmentSlot.FEET));
			}
			return InteractionResult.PASS; // PASS lets the normal hit happen too.
		});

		// A whetstone on a sword in the off hand. The main mod raises the grade right after this, so look next tick.
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer smith
					&& OnePieceBridge.whetstoneGrade(player.getMainHandItem()) > 0 && OnePieceBridge.isSword(player.getOffhandItem())) {
				WHETTING.put(smith, OnePieceBridge.swordGrade(player.getOffhandItem()));
			}
			return InteractionResult.PASS;
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (WHETTING.isEmpty()) {
				return;
			}
			for (Map.Entry<ServerPlayer, Integer> entry : WHETTING.entrySet()) {
				ItemStack sword = entry.getKey().getOffhandItem();
				int grade = OnePieceBridge.isSword(sword) ? OnePieceBridge.swordGrade(sword) : 0;
				if (grade > entry.getValue()) {
					promoted(entry.getKey(), sword, grade);
				}
			}
			WHETTING.clear();
		});

		// A Marine beaten by someone in a Navy uniform. The main mod already pays its usual bounty for the Marine.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer killer && OnePieceBridge.isMarine(entity)
					&& MarineDisguise.quality(killer) != MarineDisguise.Quality.NONE) {
				impostor(killer);
			}
		});
	}

	private static void kick(ServerPlayer player, ServerLevel level, LivingEntity target, ItemStack boots) {
		if (!(boots.getItem() instanceof ArsenalArmorItem) || Breakage.isBroken(boots)) {
			return;
		}
		List<ArsenalMaterial> parts = ArmorStats.parts(boots);
		if (parts.isEmpty()) {
			return;
		}
		TraitEffects.onHit(player, level, target, ArmorStats.traits(boots), (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE), true);
		TraitEffects.extraHit(player, level, target, KICK_DAMAGE_PER_ARMOR * ArmorStats.defense(parts.get(0), ArmorType.BOOTS));
	}

	/** Called by NavyStationBlock when a player takes a station from the Navy. */
	public static void capturedStation(ServerPlayer pirate, String station) {
		String name = pirate.getName().getString();
		OnePieceBridge.raiseBounty(pirate, STATION_BOUNTY, "You stole the Navy's " + station + ".");
		OnePieceBridge.reportNews(pirate.level().getServer(), "NAVY STATION SEIZED",
				"Pirate " + name + " has made off with a Marine base's " + station + ".", false);
	}

	private static void promoted(ServerPlayer smith, ItemStack sword, int grade) {
		String name = smith.getName().getString();
		String blade = sword.getHoverName().getString();
		OnePieceBridge.raiseBounty(smith, GRADE_BOUNTY[grade], "Word spread that you carry a " + OnePieceBridge.gradeName(grade) + " blade.");
		if (grade >= 2) {
			// Supreme Grade blades are rare enough that everyone hears at once.
			OnePieceBridge.reportNews(smith.level().getServer(), grade == 3 ? "A SUPREME GRADE BLADE!" : "A NEW MEITO",
					name + "'s " + blade + " is now a " + OnePieceBridge.gradeName(grade) + " sword.", grade == 3);
		}
	}

	private static void impostor(ServerPlayer killer) {
		String name = killer.getName().getString();
		long day = killer.level().getGameTime() / TICKS_PER_DAY;
		Long last = IMPOSTOR_DAY.get(name);
		if (last != null && last == day) {
			return;
		}
		IMPOSTOR_DAY.put(name, day);
		OnePieceBridge.raiseBounty(killer, IMPOSTOR_BOUNTY, "You struck down a Marine while wearing the Navy's uniform.");
		OnePieceBridge.reportNews(killer.level().getServer(), "IMPOSTOR IN THE RANKS",
				"A pirate in Marine uniform turned on the Navy. HQ names " + name + ".", false);
	}
}
