package com.onepiecearsenal.fruit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.tool.ArsenalGunItem;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * Devil Fruit weapons, like Lassoo, Funkfreed and Cerberus in One Piece: a weapon that eats a Zoan fruit comes alive.
 *
 * Feeding: right-click the Shipwright's Anvil holding forged gear with a Zoan fruit in your offhand. The weapon eats
 * it (see Upgrades.work). Only Zoan fruits work, the same as in the story, a weapon only eats one fruit, and Kairoseki
 * parts refuse a fruit outright: the sea turned to stone.
 *
 * Living form: Sneak + Weapon Skill key (X). The weapon turns into its animal (Beast) and strikes with the animal's
 * signature move, then fights by itself for a while, biting the nearest enemy every second and a half. It sleeps
 * again when the time runs out or you stop holding it. The power is the weapon's, so anyone can use it, fruit or not.
 */
public final class FruitWeapons {
	/** How long the living form lasts and the wait before it can wake again, in ticks. */
	static final int FORM_TICKS = 20 * 12;
	static final int COOLDOWN_TICKS = 20 * 60;
	/** Ticks between the living form's bites, and how far it reaches. */
	private static final int BITE_TICKS = 30;
	private static final double BITE_RANGE = 5.0;
	private static final double LASSOO_RANGE = 14.0;
	/** Waking the form costs the same as a weapon skill: 4 uses and 1 hunger point. */
	private static final int DURABILITY_COST = 4;
	private static final float EXHAUSTION = 4.0f;
	/** Each living-form bite does this share of the weapon's damage; the signature move's hits do more. */
	private static final float BITE_SCALE = 0.5f;
	private static final float SIGNATURE_SCALE = 1.25f;
	/** The chance a boss drops its famous fruit weapon on a first win, by difficulty (Normal, Hard, Expert). */
	private static final float[] DROP_CHANCE = {0.25f, 0.5f, 0.85f};

	/** A weapon awake in someone's hands: its fruit, and the game times its form ends and its next bite comes. */
	private record Form(ServerPlayer player, String fruitId, long until, long nextBite) {
	}

	private static final Map<UUID, Form> AWAKE = new HashMap<>();
	private static final Map<UUID, Long> READY_AT = new HashMap<>();

	private FruitWeapons() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(FruitWeapons::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			AWAKE.remove(handler.player.getUUID());
			READY_AT.remove(handler.player.getUUID());
		});

		// The bosses who carry a famous fruit weapon drop it, sometimes, the first time you beat them.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer player && entity.level() instanceof ServerLevel level) {
				ItemStack drop = dropFor(entity, entity.getRandom().nextFloat());
				if (!drop.isEmpty()) {
					entity.spawnAtLocation(level, drop);
					player.sendSystemMessage(Component.literal(entity.getDisplayName().getString() + " dropped " + drop.getHoverName().getString() + "!")
							.withStyle(ChatFormatting.LIGHT_PURPLE));
				}
			}
		});
	}

	// ---- Reading a weapon ----

	/** The fruit this weapon ate, or null. */
	public static String fruitOf(ItemStack stack) {
		return stack.get(ArsenalComponents.EATEN_FRUIT);
	}

	public static boolean hasFruit(ItemStack stack) {
		return fruitOf(stack) != null;
	}

	/** A fruit's name, from the One Piece Mod or, for fruits only famous weapons ate, from FamousFruitWeapon. */
	public static String fruitName(String fruitId) {
		String name = OnePieceBridge.fruitName(fruitId);
		if (name == null) {
			FamousFruitWeapon famous = FamousFruitWeapon.byFruit(fruitId);
			name = famous != null && famous.fruitName != null ? famous.fruitName : fruitId;
		}
		return name;
	}

	/** The parts of forged gear that can eat a fruit (katanas, hand weapons and guns), or an empty list. */
	private static List<ArsenalMaterial> parts(ItemStack stack) {
		if (stack.getItem() instanceof KatanaItem) {
			return KatanaStats.parts(stack);
		}
		if (stack.getItem() instanceof ArsenalGunItem) {
			return GunStats.parts(stack);
		}
		return WeaponStats.parts(stack);
	}

	/** Why this weapon can't eat this fruit, or null if it can. */
	public static String whyNot(ItemStack weapon, String fruitId) {
		List<ArsenalMaterial> parts = parts(weapon);
		if (parts.isEmpty()) {
			return "Only forged weapons and guns can eat a Devil Fruit.";
		}
		if (hasFruit(weapon)) {
			return weapon.getHoverName().getString() + " already ate the " + fruitName(fruitOf(weapon)) + ". A weapon only holds one fruit.";
		}
		if (!OnePieceBridge.isZoan(fruitId)) {
			return "Only Zoan fruits can live in a weapon. The " + fruitName(fruitId) + " isn't a Zoan.";
		}
		if (parts.contains(ArsenalMaterial.KAIROSEKI)) {
			return "Kairoseki is the sea turned to stone. It won't hold a Devil Fruit.";
		}
		return null;
	}

	/** The tooltip lines of a weapon that ate a fruit. */
	public static void describe(ItemStack stack, Consumer<Component> lines) {
		String fruit = fruitOf(stack);
		if (fruit == null) {
			return;
		}
		Beast beast = Beast.of(fruit);
		lines.accept(Component.literal("Ate the " + fruitName(fruit)).withStyle(ChatFormatting.LIGHT_PURPLE));
		FamousFruitWeapon famous = famousOf(stack);
		String power = famous != null ? famous.power : beast.description;
		lines.accept(Component.literal("[Sneak + ").append(Component.keybind("key.arsenal.weapon_skill")).append("] " + beast.animal + " Form ("
				+ COOLDOWN_TICKS / 20 + "s): " + power).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	/** The famous weapon this is (it has the fruit and the look), or null for a weapon a player fed. */
	public static FamousFruitWeapon famousOf(ItemStack stack) {
		FamousFruitWeapon famous = FamousFruitWeapon.byFruit(fruitOf(stack) == null ? "" : fruitOf(stack));
		CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
		return famous != null && model != null && model.strings().contains(famous.id) ? famous : null;
	}

	// ---- Feeding, at the Shipwright's Anvil ----

	/** The smith holds a fruit in their offhand: the weapon in their main hand eats it, if it can. */
	public static InteractionResult feed(Level level, BlockPos pos, ServerPlayer smith, ItemStack weapon, ItemStack fruitStack) {
		String fruitId = OnePieceBridge.fruitId(fruitStack);
		String why = whyNot(weapon, fruitId);
		if (why != null) {
			smith.sendOverlayMessage(Component.literal(why).withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		weapon.set(ArsenalComponents.EATEN_FRUIT, fruitId);
		fruitStack.consume(1, smith);
		Beast beast = Beast.of(fruitId);
		level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
		level.playSound(null, pos, voice(fruitId, beast), SoundSource.PLAYERS, 1.0f, 1.0f);
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.3, 0.4, 0.05);
		}
		smith.sendSystemMessage(Component.literal(weapon.getHoverName().getString() + " ate the " + fruitName(fruitId) + " and came alive as a "
				+ beast.animal.toLowerCase() + "! Sneak + Weapon Skill wakes it.").withStyle(ChatFormatting.LIGHT_PURPLE));
		return InteractionResult.SUCCESS;
	}

	// ---- The living form ----

	/** Sneak + Weapon Skill with a fruit weapon in hand. Returns false (and does nothing) if the main hand holds none. */
	public static boolean awaken(ServerPlayer player) {
		ItemStack weapon = player.getMainHandItem();
		String fruit = fruitOf(weapon);
		if (fruit == null || Upgrades.gearOf(weapon) == null) {
			return false;
		}
		if (Breakage.isBroken(weapon)) {
			Breakage.tellBroken(player, weapon);
			return true;
		}
		ServerLevel level = (ServerLevel) player.level();
		long now = level.getGameTime();
		Beast beast = Beast.of(fruit);
		if (AWAKE.containsKey(player.getUUID())) {
			tell(player, "It's already awake!", ChatFormatting.GRAY);
			return true;
		}
		long ready = READY_AT.getOrDefault(player.getUUID(), 0L);
		if (now < ready && !player.isCreative()) {
			tell(player, beast.animal + " Form is ready in " + Math.max(1, (ready - now + 19) / 20) + "s", ChatFormatting.GRAY);
			return true;
		}

		FamousFruitWeapon famous = famousOf(weapon);
		List<LivingEntity> targets = famous == FamousFruitWeapon.LASSOO
				? OnePieceBridge.enemiesInCone(player, level, LASSOO_RANGE, 0.85, 3)
				: beast == Beast.ELEPHANT || beast == Beast.DINOSAUR
						? OnePieceBridge.enemiesInCone(player, level, 5.0, 0.3, 6)
						: OnePieceBridge.enemiesInCone(player, level, BITE_RANGE, 0.5, 3);
		for (LivingEntity target : targets) {
			bite(player, level, fruit, famous, target, SIGNATURE_SCALE);
		}

		AWAKE.put(player.getUUID(), new Form(player, fruit, now + FORM_TICKS, now + BITE_TICKS));
		READY_AT.put(player.getUUID(), now + FORM_TICKS + COOLDOWN_TICKS);
		player.causeFoodExhaustion(EXHAUSTION);
		weapon.hurtAndBreak(DURABILITY_COST, player, EquipmentSlot.MAINHAND);
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.5, 0.6, 0.5, 0.05);
		sound(level, player, voice(fruit, beast), 1.0f);
		String name = famous != null ? famous.displayName : beast.animal + " Form";
		tell(player, name + ": " + beast.signature + "!", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
		return true;
	}

	/** True while this player's weapon is awake. */
	public static boolean isAwake(ServerPlayer player) {
		return AWAKE.containsKey(player.getUUID());
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Form>> it = AWAKE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Form> entry = it.next();
			Form form = entry.getValue();
			ServerPlayer player = form.player();
			if (player.isRemoved() || !player.isAlive()) {
				it.remove();
				continue;
			}
			ServerLevel level = (ServerLevel) player.level();
			long now = level.getGameTime();
			ItemStack weapon = player.getMainHandItem();
			if (now >= form.until() || !form.fruitId().equals(fruitOf(weapon)) || Breakage.isBroken(weapon)) {
				it.remove();
				tell(player, "Your weapon goes back to sleep.", ChatFormatting.GRAY);
				continue;
			}

			Beast beast = Beast.of(form.fruitId());
			FamousFruitWeapon famous = famousOf(weapon);
			if (now % 20 == 0) {
				level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.4, 0.5, 0.4, 0.02);
				buffWielder(player, beast, form.fruitId());
			}
			if (now < form.nextBite()) {
				continue;
			}
			entry.setValue(new Form(player, form.fruitId(), form.until(), now + BITE_TICKS));
			double range = famous == FamousFruitWeapon.LASSOO ? LASSOO_RANGE : BITE_RANGE;
			// Cerberus has three heads, so it bites three enemies; everything else bites the nearest one.
			int heads = famous == FamousFruitWeapon.CERBERUS ? 3 : 1;
			List<LivingEntity> near = new ArrayList<>(OnePieceBridge.enemiesAround(player, level, range));
			near.sort(Comparator.comparingDouble(player::distanceToSqr));
			for (LivingEntity target : near.subList(0, Math.min(heads, near.size()))) {
				bite(player, level, form.fruitId(), famous, target, BITE_SCALE);
			}
			if (!near.isEmpty()) {
				sound(level, player, voice(form.fruitId(), beast), 1.2f);
			}
		}
	}

	/** While the weapon is awake, some animals help their wielder too. Runs once a second. */
	private static void buffWielder(ServerPlayer player, Beast beast, String fruitId) {
		switch (beast) {
			case ELEPHANT -> player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0, true, false, true));
			case HORSE -> player.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 1, true, false, true));
			case HUMAN -> player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30, 0, true, false, true));
			default -> {
			}
		}
		if (fruitId.equals("tori_tori_no_mi_phoenix")) { // The phoenix's blue flames heal.
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, true, false, true));
		}
	}

	/** One bite (or shot, or strike) of the living weapon. */
	private static void bite(ServerPlayer player, ServerLevel level, String fruitId, FamousFruitWeapon famous, LivingEntity target, float scale) {
		float damage = weaponDamage(player) * scale;
		Beast beast = Beast.of(fruitId);
		if (famous == FamousFruitWeapon.LASSOO) {
			damage *= 1.5f; // Its sneezes are cannon shells.
		}
		if (beast == Beast.CAT) {
			damage *= 1.5f;
		}
		if (!target.hurtServer(level, player.damageSources().playerAttack(player), damage)) {
			return;
		}
		Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
		switch (beast) {
			case DOG -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0), player);
			case DINOSAUR, OX, HORSE -> push(target, away, 1.0, 0.3);
			case ELEPHANT -> push(target, away, 1.6, 0.4);
			case BIRD -> push(target, away, 0.3, 0.8);
			case SNAKE -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), player);
			case DRAGON -> target.igniteForSeconds(4);
			case HUMAN -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), player);
			case MOLE -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), player);
			case AXOLOTL -> player.heal(1.5f);
			case SPIDER -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2), player);
			case SQUIRREL -> lightning(level, target);
			default -> {
			}
		}
		// A few fruits do more than their animal: the ice wolf freezes, the fox and phoenix burn, Cerberus withers.
		switch (fruitId) {
			case "inu_inu_no_mi_okuchi_no_makami" -> target.setTicksFrozen(target.getTicksRequiredToFreeze() + 60);
			case "inu_inu_no_mi_kyubi_no_kitsune", "tori_tori_no_mi_phoenix" -> target.igniteForSeconds(3);
			case "inu_inu_no_mi_cerberus" -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), player);
			default -> {
			}
		}
		if (famous == FamousFruitWeapon.LASSOO) {
			level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 1, 0, 0, 0, 0);
			level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
			push(target, away, 0.8, 0.4);
		} else {
			particles(level, target, beast == Beast.DRAGON ? ParticleTypes.FLAME : ParticleTypes.CRIT, 8);
		}
	}

	/** The damage a bite starts from: a gun's shot damage, or the attack damage of the weapon in hand. */
	private static float weaponDamage(ServerPlayer player) {
		ItemStack weapon = player.getMainHandItem();
		if (weapon.getItem() instanceof ArsenalGunItem) {
			return GunStats.stats(weapon).damage();
		}
		return (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
	}

	/** A lightning strike that only looks and sounds like one: it never hurts you or sets blocks on fire. */
	private static void lightning(ServerLevel level, LivingEntity target) {
		LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
		bolt.setPos(target.position());
		bolt.setVisualOnly(true);
		level.addFreshEntity(bolt);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.5f, 1.3f);
	}

	private static SoundEvent voice(String fruitId, Beast beast) {
		return switch (beast) {
			case DOG, CAT, ELEPHANT, OX, MOLE, BEAST -> SoundEvents.RAVAGER_ROAR;
			case DINOSAUR, DRAGON, SNAKE -> SoundEvents.ENDER_DRAGON_GROWL;
			case BIRD -> SoundEvents.PARROT_FLY;
			case HORSE -> SoundEvents.HORSE_GALLOP;
			case SPIDER -> SoundEvents.SPIDER_AMBIENT;
			case SQUIRREL -> SoundEvents.LIGHTNING_BOLT_IMPACT;
			default -> SoundEvents.PLAYER_ATTACK_STRONG;
		};
	}

	// ---- Famous weapons from bosses ----

	/** What this beaten boss drops for a roll of 0 to 1: its famous fruit weapon on a lucky first win, or nothing. */
	public static ItemStack dropFor(LivingEntity entity, float roll) {
		FamousFruitWeapon famous = FamousFruitWeapon.droppedBy(OnePieceBridge.bossId(entity));
		if (famous == null || OnePieceBridge.isBossMinion(entity) || !OnePieceBridge.isFirstBossWin(entity)
				|| roll >= DROP_CHANCE[Math.clamp(OnePieceBridge.bossDifficulty(entity), 0, 2)]) {
			return ItemStack.EMPTY;
		}
		return famous.create();
	}

	// ---- Small helpers ----

	private static void push(LivingEntity target, Vec3 away, double strength, double up) {
		target.setDeltaMovement(away.x * strength, up, away.z * strength);
		target.needsSync = true;
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
