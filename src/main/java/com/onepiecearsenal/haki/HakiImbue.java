package com.onepiecearsenal.haki;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.meito.Meito;
import com.onepiecearsenal.salvage.Breakage;
import com.onepiecearsenal.tool.ArsenalGunItem;
import com.onepiecearsenal.tool.ArsenalWeaponItem;
import com.onepiecearsenal.tool.GunStats;
import com.onepiecearsenal.tool.KatanaItem;
import com.onepiecearsenal.tool.KatanaStats;
import com.onepiecearsenal.tool.WeaponStats;

/**
 * Haki imbuing: press the Imbue Haki key (O by default) with a forged Arsenal weapon or gun in hand to coat it in
 * Armament Haki for a while, the way Zoro coats his swords. The main mod's own Armament (B) coats your body; this
 * reaches past it into the weapon, so it needs Armament Haki level 2 or higher, but not switched on.
 *
 * While the coat is on, the blade, head or barrel turns black (client/HakiCoatTint), and the weapon:
 * - lands hits on Logia users instead of passing through them (bullets from a coated gun too),
 * - hits harder, +1 damage per Armament level,
 * - takes no wear,
 * - counts as Haki-coated for a katana's Black Blade awakening and the Kokuto Inlay upgrade.
 * It costs 2 hunger to coat and a little more each second while you hold it. Press the key again to let it go.
 *
 * Black Blades are always coated as far as Logia users are concerned: their hits land too.
 */
public final class HakiImbue {
	private HakiImbue() {
	}

	/** The Armament Haki level needed to reach past your body into a weapon. */
	public static final int MIN_LEVEL = 2;
	/** How long a coat lasts: BASE_SECONDS plus SECONDS_PER_LEVEL for every Armament level (30s at level 2, 60s at 5). */
	public static final int BASE_SECONDS = 10;
	public static final int SECONDS_PER_LEVEL = 10;
	/** Seconds after a coat ends (or is let go) before you can coat again. */
	public static final int COOLDOWN_SECONDS = 20;
	/** Hunger it costs to coat (4 exhaustion = 1 hunger point), and each second while held. */
	private static final float COAT_EXHAUSTION = 8.0f;
	private static final float UPKEEP_EXHAUSTION = 0.15f;
	/** Below this food level the coat fades, like the main mod's Armament does when you're starving. */
	private static final int MIN_FOOD = 4;

	private static final Map<UUID, Long> READY_AT = new HashMap<>();
	/** The server's game time, for the places that see an item but not the world it's in (like its wear). */
	private static volatile long serverTime;

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(ImbueHakiPayload.TYPE, ImbueHakiPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ImbueHakiPayload.TYPE, (payload, context) -> press(context.player()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> READY_AT.remove(handler.player.getUUID()));

		if (!OnePieceBridge.registerCoatedHit(HakiImbue::isCoatedHit)) {
			ArsenalMod.LOGGER.warn("The One Piece Mod is too old to let Haki-imbued weapons hit Logia users. Update it.");
		}

		// A coated weapon lands its hits with the main mod's black flash.
		AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
			if (player instanceof ServerPlayer attacker && level instanceof ServerLevel serverLevel && target instanceof LivingEntity
					&& !player.isSpectator() && coatOf(player.getMainHandItem(), serverLevel.getGameTime()) != null) {
				serverLevel.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + target.getBbHeight() / 2,
						target.getZ(), 8, 0.25, 0.25, 0.25, 0.02);
				serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), OnePieceBridge.armamentHitSound(),
						SoundSource.PLAYERS, 0.4f, 1.3f);
			}
			return InteractionResult.PASS;
		});

		// Once a second: the coat costs hunger while held, fades when you're starving, and tells you when it wears off.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			serverTime = server.overworld().getGameTime();
			if (serverTime % 20 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				upkeep(player, player.getMainHandItem());
			}
		});
	}

	// ---- Reading the coat ----

	/** True for gear that can be coated: forged Arsenal katanas, weapons and guns. */
	public static boolean canCoat(ItemStack stack) {
		if (stack.getItem() instanceof KatanaItem) {
			return !KatanaStats.parts(stack).isEmpty();
		}
		if (stack.getItem() instanceof ArsenalWeaponItem) {
			return !WeaponStats.parts(stack).isEmpty();
		}
		return stack.getItem() instanceof ArsenalGunItem && !GunStats.parts(stack).isEmpty();
	}

	/** The weapon's coat if it's still on at this game time, else null. */
	public static HakiCoat coatOf(ItemStack stack, long gameTime) {
		HakiCoat coat = stack.get(ArsenalComponents.HAKI_COAT);
		return coat != null && coat.activeAt(gameTime) && !Breakage.isBroken(stack) ? coat : null;
	}

	/** True while the weapon is coated in Haki. */
	public static boolean isCoated(ItemStack stack, long gameTime) {
		return coatOf(stack, gameTime) != null;
	}

	/** The Armament level of the weapon's coat, or 0 if it isn't coated. */
	public static int coatLevel(ItemStack stack, long gameTime) {
		HakiCoat coat = coatOf(stack, gameTime);
		return coat == null ? 0 : coat.level();
	}

	/**
	 * The Armament Haki behind this weapon's hits: the wielder's own if it's switched on, or the weapon's coat,
	 * whichever is stronger. 0 if neither.
	 */
	public static int hakiBehind(ServerPlayer player, ItemStack weapon) {
		return Math.max(OnePieceBridge.activeArmamentLevel(player), coatLevel(weapon, player.level().getGameTime()));
	}

	/** The coated weapon or gun this hit came from, or null if it wasn't one. Projectiles count for guns. */
	private static HakiCoat coatedHit(Player attacker, DamageSource source) {
		long now = attacker.level().getGameTime();
		if (source.getDirectEntity() == attacker) {
			return coatOf(attacker.getMainHandItem(), now);
		}
		if (source.getDirectEntity() instanceof Projectile) {
			for (InteractionHand hand : InteractionHand.values()) {
				ItemStack held = attacker.getItemInHand(hand);
				if (held.getItem() instanceof ArsenalGunItem && isCoated(held, now)) {
					return coatOf(held, now);
				}
			}
		}
		return null;
	}

	/** For the main mod's Logia dodge: a coated weapon's hits, and a Black Blade's, land on Logia users. */
	private static boolean isCoatedHit(Entity attacker, DamageSource source) {
		if (!(attacker instanceof Player player)) {
			return false;
		}
		if (coatedHit(player, source) != null) {
			return true;
		}
		ItemStack held = player.getMainHandItem();
		return source.getDirectEntity() == player && held.getItem() instanceof KatanaItem
				&& KatanaStats.isBlackBlade(held) && !Breakage.isBroken(held);
	}

	/** A coated weapon's hit (or bullet) deals +1 damage per Armament level. Called as any hit starts (LivingEntityMixin). */
	public static float adjustDamage(LivingEntity target, DamageSource source, float amount) {
		if (amount <= 0 || !(source.getEntity() instanceof Player attacker) || attacker == target) {
			return amount;
		}
		boolean melee = source.getDirectEntity() == attacker && source.is(DamageTypes.PLAYER_ATTACK);
		if (!melee && !(source.getDirectEntity() instanceof Projectile)) {
			return amount; // Thorns, trait extra hits and so on don't get it twice.
		}
		HakiCoat coat = coatedHit(attacker, source);
		return coat == null ? amount : amount + coat.level();
	}

	/** Haki keeps the blade from wear while it's coated (ItemStackMixin). Repairs still go through. */
	public static int keepUnworn(ItemStack stack, int damage) {
		return damage > stack.getDamageValue() && isCoated(stack, serverTime) ? stack.getDamageValue() : damage;
	}

	/** The tooltip line of a coated weapon. */
	public static void describe(ItemStack stack, Consumer<Component> lines) {
		if (canCoat(stack)) {
			HakiCoat coat = stack.get(ArsenalComponents.HAKI_COAT);
			if (coat != null) {
				lines.accept(Component.literal("Coated in Armament Haki (level " + coat.level() + ")").withStyle(ChatFormatting.DARK_PURPLE));
			} else {
				lines.accept(Component.literal("[").append(Component.keybind("key.arsenal.imbue_haki"))
						.append("] Imbue with Armament Haki").withStyle(ChatFormatting.DARK_GRAY));
			}
		}
	}

	// ---- Coating and letting go ----

	/** The Imbue Haki key: coats the weapon in hand, or lets the coat go if it's already on. */
	public static void press(ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) {
			return;
		}
		ItemStack weapon = player.getMainHandItem();
		long now = player.level().getGameTime();
		if (!canCoat(weapon)) {
			tell(player, "Hold a forged Arsenal katana, weapon or gun to imbue it with Haki.", ChatFormatting.RED);
			return;
		}
		if (isCoated(weapon, now)) {
			release(player, weapon, now);
			return;
		}
		String reason = whyNot(player, weapon, now);
		if (reason != null) {
			tell(player, reason, ChatFormatting.RED);
			return;
		}
		coat(player, weapon, now);
	}

	/** Why this player can't coat this weapon right now, or null if they can. */
	public static String whyNot(ServerPlayer player, ItemStack weapon, long now) {
		if (Breakage.isBroken(weapon)) {
			return weapon.getHoverName().getString() + " is broken.";
		}
		int level = OnePieceBridge.armamentLevel(player);
		if (level < MIN_LEVEL) {
			return level == 0 ? "You haven't awakened Armament Haki yet."
					: "Your Armament Haki can't reach past your skin yet. Train it to level " + MIN_LEVEL + ".";
		}
		if (weapon.getItem() instanceof KatanaItem && !Meito.worthy(player, OnePieceBridge.swordGrade(weapon))) {
			return weapon.getHoverName().getString() + " refuses you.";
		}
		long readyAt = READY_AT.getOrDefault(player.getUUID(), 0L);
		if (now < readyAt) {
			return "Your Haki needs " + Math.max(1, (readyAt - now + 19) / 20) + "s to gather again.";
		}
		if (!player.isCreative() && player.getFoodData().getFoodLevel() <= MIN_FOOD) {
			return "You're too hungry to imbue Haki. Eat something!";
		}
		return null;
	}

	/** Coats the weapon for as long as the player's Armament level allows. */
	public static void coat(ServerPlayer player, ItemStack weapon, long now) {
		int level = Math.max(MIN_LEVEL, OnePieceBridge.armamentLevel(player));
		long until = now + (BASE_SECONDS + SECONDS_PER_LEVEL * (long) level) * 20;
		weapon.set(ArsenalComponents.HAKI_COAT, new HakiCoat(until, level));
		READY_AT.put(player.getUUID(), until + COOLDOWN_SECONDS * 20L);
		if (!player.isCreative()) {
			player.causeFoodExhaustion(COAT_EXHAUSTION);
		}
		ServerLevel level0 = (ServerLevel) player.level();
		level0.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.5, 0.4, 0.03);
		level0.playSound(null, player.getX(), player.getY(), player.getZ(), OnePieceBridge.armamentOnSound(), SoundSource.PLAYERS, 0.8f, 0.8f);
		tell(player, weapon.getHoverName().getString() + " turns black with your Haki ("
				+ (until - now) / 20 + "s).", ChatFormatting.DARK_PURPLE);
	}

	private static void release(ServerPlayer player, ItemStack weapon, long now) {
		weapon.remove(ArsenalComponents.HAKI_COAT);
		READY_AT.put(player.getUUID(), now + COOLDOWN_SECONDS * 20L);
		tell(player, "You let the Haki fade from " + weapon.getHoverName().getString() + ".", ChatFormatting.GRAY);
	}

	/** Once a second for the weapon in the player's main hand. */
	private static void upkeep(ServerPlayer player, ItemStack weapon) {
		HakiCoat coat = weapon.get(ArsenalComponents.HAKI_COAT);
		if (coat == null) {
			return;
		}
		long now = player.level().getGameTime();
		if (!coat.activeAt(now) || Breakage.isBroken(weapon)) {
			weapon.remove(ArsenalComponents.HAKI_COAT);
			tell(player, "The Haki fades from " + weapon.getHoverName().getString() + ".", ChatFormatting.GRAY);
			return;
		}
		if (player.isCreative()) {
			return;
		}
		if (player.getFoodData().getFoodLevel() <= MIN_FOOD) {
			weapon.remove(ArsenalComponents.HAKI_COAT);
			tell(player, "You're too hungry to keep up the Haki. It fades from " + weapon.getHoverName().getString() + ".", ChatFormatting.RED);
			return;
		}
		player.causeFoodExhaustion(UPKEEP_EXHAUSTION);
	}

	/** Wipes a coat that wore off while the weapon sat in the inventory (from the item's inventoryTick). */
	public static void tidy(ItemStack stack, long gameTime, net.minecraft.world.entity.EquipmentSlot slot) {
		if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND) {
			return; // upkeep() handles the weapon in hand, and says so when its coat wears off.
		}
		HakiCoat coat = stack.get(ArsenalComponents.HAKI_COAT);
		if (coat != null && !coat.activeAt(gameTime)) {
			stack.remove(ArsenalComponents.HAKI_COAT);
		}
	}

	private static void tell(ServerPlayer player, String message, ChatFormatting style) {
		player.sendOverlayMessage(Component.literal(message).withStyle(style));
	}
}
