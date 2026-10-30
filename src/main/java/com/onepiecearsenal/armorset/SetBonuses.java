package com.onepiecearsenal.armorset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.tool.ArsenalArmorItem;

/**
 * Makes armor set bonuses happen (the sets themselves are in arsenal/set_bonuses.json, read by SetBonus).
 * Attributes and mob effects are put on players once a second while the set is worn. Damage changes work for
 * anyone wearing the set, Marines in Arsenal gear included, through LivingEntityMixin. The disguise ones are read
 * by MarineDisguise and Suspicion.
 */
public final class SetBonuses {
	/** All damage_taken cuts together never take off more than this share of a hit. */
	private static final float MAX_DAMAGE_CUT = 0.6f;
	/**
	 * How long a set's mob effect lasts each time it's topped up (every second). Over 10 seconds so its icon doesn't
	 * blink; when the set comes off, effects this short that the set gave are taken away.
	 */
	private static final int EFFECT_TICKS = 260;
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	/** The sets someone wears, as worked out on one game tick. */
	private record Worn(long tick, List<SetBonus> sets) {}
	/** Every hit asks, so it's worked out once per tick per wearer. Forgets entities that unload. */
	private static final Map<LivingEntity, Worn> WORN = new WeakHashMap<>();
	/** The sets each player was told about last, so the message only shows when one switches on. */
	private static final Map<UUID, Set<String>> LAST_SETS = new HashMap<>();

	private SetBonuses() {
	}

	public static void initialize() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_SETS.remove(handler.player.getUUID()));

		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : level.getPlayers(p -> true)) {
				apply(player);
			}
		});
	}

	/**
	 * Puts the attributes and mob effects of the sets the player wears on them, and takes off those of sets they
	 * don't. Runs once a second; tests call it directly.
	 */
	public static void apply(ServerPlayer player) {
		List<SetBonus> worn = active(player);
		Set<String> ids = new HashSet<>();
		for (SetBonus set : SetBonus.values()) {
			boolean on = worn.contains(set);
			if (on) {
				ids.add(set.id);
			}
			for (int i = 0; i < set.effects.size(); i++) {
				SetBonus.Effect effect = set.effects.get(i);
				if (effect.type().equals("attribute")) {
					attribute(player, set, i, effect, on);
				} else if (effect.type().equals("mob_effect")) {
					mobEffect(player, set, effect, on);
				}
			}
		}

		Set<String> last = LAST_SETS.put(player.getUUID(), ids);
		List<String> started = worn.stream().filter(set -> last == null || !last.contains(set.id)).map(set -> set.name).toList();
		if (last != null && !started.isEmpty()) {
			player.sendOverlayMessage(Component.literal("Set bonus: " + String.join(", ", started)).withStyle(ChatFormatting.GOLD));
		}
	}

	private static void attribute(ServerPlayer player, SetBonus set, int index, SetBonus.Effect effect, boolean on) {
		Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.get(effect.id()).orElse(null);
		AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		Identifier id = ArsenalMod.id("set." + set.id + "." + index);
		if (on) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, effect.amount(),
					AttributeModifier.Operation.valueOf(effect.operation().toUpperCase(Locale.ROOT))));
		} else {
			instance.removeModifier(id);
		}
	}

	private static void mobEffect(ServerPlayer player, SetBonus set, SetBonus.Effect effect, boolean on) {
		Holder<MobEffect> mobEffect = BuiltInRegistries.MOB_EFFECT.get(effect.id()).orElse(null);
		if (mobEffect == null) {
			return;
		}
		if (on) {
			// Ambient, like a beacon's, so it shows no particles.
			player.addEffect(new MobEffectInstance(mobEffect, EFFECT_TICKS, effect.amplifier(), true, false, true));
			return;
		}
		MobEffectInstance current = player.getEffect(mobEffect);
		if (current != null && current.isAmbient() && current.getAmplifier() == effect.amplifier() && current.getDuration() <= EFFECT_TICKS) {
			player.removeEffect(mobEffect);
		}
	}

	/** The sets this entity wears enough pieces of, in set_bonuses.json order. Empty for anyone in no Arsenal armor. */
	public static List<SetBonus> active(LivingEntity wearer) {
		long now = wearer.level().getGameTime();
		Worn cached = WORN.get(wearer);
		if (cached != null && cached.tick() == now) {
			return cached.sets();
		}
		List<ItemStack> pieces = new ArrayList<>(4);
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack piece = wearer.getItemBySlot(slot);
			if (piece.getItem() instanceof ArsenalArmorItem && !Breakage.isBroken(piece)) {
				pieces.add(piece); // Broken armor stops nothing, so it doesn't count toward a set either.
			}
		}
		List<SetBonus> sets = new ArrayList<>();
		if (!pieces.isEmpty()) {
			for (SetBonus set : SetBonus.values()) {
				if (pieces.stream().filter(set::fits).count() >= set.pieces) {
					sets.add(set);
				}
			}
		}
		List<SetBonus> result = List.copyOf(sets);
		WORN.put(wearer, new Worn(now, result));
		return result;
	}

	/** The sets an armor piece belongs to, for its tooltip. */
	public static List<SetBonus> setsOf(ItemStack piece) {
		return SetBonus.values().stream().filter(set -> set.fits(piece)).collect(Collectors.toList());
	}

	/** How many times the bounty the wearer's Marine uniform can hide: 1 without a set that helps. */
	public static double disguiseBounty(LivingEntity wearer) {
		return product(wearer, "disguise_bounty");
	}

	/** How fast watching Marines get suspicious of the wearer: 1 normally, less with a set that helps. */
	public static double suspicion(LivingEntity wearer) {
		return product(wearer, "suspicion");
	}

	private static double product(LivingEntity wearer, String type) {
		double result = 1.0;
		for (SetBonus set : active(wearer)) {
			for (SetBonus.Effect effect : set.effects) {
				if (effect.type().equals(type)) {
					result *= effect.amount();
				}
			}
		}
		return result;
	}

	/**
	 * A hit's damage after the victim's damage_taken and the attacker's damage_dealt set bonuses. Called by
	 * LivingEntityMixin as every hit starts, so the cheap checks come first.
	 */
	public static float adjustDamage(LivingEntity victim, DamageSource source, float amount) {
		if (amount <= 0 || victim.level().isClientSide()) {
			return amount;
		}
		LivingEntity attacker = source.getEntity() instanceof LivingEntity living && living != victim ? living : null;
		float multiplier = 1.0f;
		for (SetBonus set : active(victim)) {
			for (SetBonus.Effect effect : set.effects) {
				if (effect.type().equals("damage_taken") && comesFrom(effect.against(), source, attacker)) {
					multiplier *= 1.0f - (float) effect.amount();
				}
			}
		}
		multiplier = Math.max(1.0f - MAX_DAMAGE_CUT, multiplier);
		if (attacker != null) {
			for (SetBonus set : active(attacker)) {
				for (SetBonus.Effect effect : set.effects) {
					if (effect.type().equals("damage_dealt") && isKind(effect.against(), victim)) {
						multiplier *= 1.0f + (float) effect.amount();
					}
				}
			}
		}
		return amount * multiplier;
	}

	private static boolean comesFrom(String from, DamageSource source, LivingEntity attacker) {
		return switch (from) {
			case "projectiles" -> source.is(DamageTypeTags.IS_PROJECTILE);
			case "fire" -> source.is(DamageTypeTags.IS_FIRE);
			default -> attacker != null && isKind(from, attacker);
		};
	}

	private static boolean isKind(String kind, LivingEntity entity) {
		return switch (kind) {
			case "devil_fruit_users" -> OnePieceBridge.isFruitUser(entity);
			case "marines" -> OnePieceBridge.isMarine(entity);
			case "pirates" -> OnePieceBridge.isPirateNpc(entity);
			case "sea_creatures" -> entity.getType().builtInRegistryHolder().is(TraitEffects.SEA_CREATURES);
			default -> false;
		};
	}
}
