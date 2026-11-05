package com.onepiecearsenal.town;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.material.Faction;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.station.HighTier;
import com.onepiecearsenal.station.NavyCastingBlock;
import com.onepiecearsenal.station.ShipwrightsAnvilBlock;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * Town blacksmiths: the One Piece Mod's "smith" townsfolk (Shells Town, Loguetown, Mock Town, the Flower Capital and
 * more) work on Arsenal gear if you bring the materials. Talk to one while holding:
 * - a part, with a material in your offhand: they forge a copy of it in that material, for a little less material
 *   than the crafting recipe and a few Berries. Each town's smith has specialty parts that take even less.
 * - a part, with your offhand empty, while you carry a full set: they put the gear together, like a Shipwright's
 *   Anvil you don't have to carry around.
 * - damaged (or broken) forged gear: they mend it with its head material, twice as well as the anvil, for Berries.
 * Talking with anything else in hand (or sneaking to haggle) is left to the main mod: their trades, their chatter.
 *
 * They're honest pirate-town smiths: they won't touch Navy materials or put together Navy gear, but they'll mend it.
 * Their forges and anvils aren't up to the best materials (see HighTier): they won't make, put together or mend those.
 * The fees and specialties are in src/main/resources/arsenal/blacksmiths.json.
 */
public final class Blacksmiths {
	private static Settings settings;

	private Blacksmiths() {
	}

	public static void initialize() {
		settings();
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			// Server only, since only the server knows a townsperson's job. 26.3 calls this once per click, with a hit.
			if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || player.isSecondaryUseActive()
					|| !(player instanceof ServerPlayer customer) || !OnePieceBridge.isTownSmith(entity)
					|| !wantsSmithing(player.getMainHandItem())) {
				return InteractionResult.PASS;
			}
			String busy = OnePieceBridge.townsfolkBusy(entity, customer);
			if (busy != null) {
				OnePieceBridge.townsfolkSay(entity, customer, busy);
			} else {
				serve(level, entity.blockPosition(), customer, OnePieceBridge.townOf(entity),
						line -> OnePieceBridge.townsfolkSay(entity, customer, line));
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** Holding something a blacksmith works on: a part, or worn forged gear. Anything else gets their usual trades and chatter. */
	public static boolean wantsSmithing(ItemStack held) {
		return held.getItem() instanceof PartItem || (held.isDamaged() && Upgrades.gearOf(held) != null);
	}

	/**
	 * The smith of this town (a town structure id, or "" for none) works on what the customer is holding, and says how
	 * it went through say. True if they did any work (and took any Berries).
	 */
	public static boolean serve(Level level, BlockPos pos, ServerPlayer customer, String town, Consumer<String> say) {
		ItemStack held = customer.getMainHandItem();
		if (Upgrades.gearOf(held) != null) {
			return mend(level, pos, customer, held, town, say);
		}
		if (!(held.getItem() instanceof PartItem part)) {
			return false;
		}
		ItemStack offhand = customer.getOffhandItem();
		if (offhand.isEmpty()) {
			return assemble(level, pos, customer, town, say);
		}
		return forgePart(level, pos, customer, part, offhand.getItem(), town, say);
	}

	// ---- Forging a part ----

	private static boolean forgePart(Level level, BlockPos pos, ServerPlayer customer, PartItem part, Item offhand, String town,
			Consumer<String> say) {
		PartType type = part.type();
		if (type.isDial()) {
			say.accept("A " + type.displayName + "? That's Skypiea work. Craft it yourself, around a Dial Shell.");
			return false;
		}
		ArsenalMaterial material = null;
		ArsenalMaterial refused = null;
		for (ArsenalMaterial candidate : ArsenalMaterial.values()) {
			if (candidate.repairItem() != offhand) {
				continue;
			}
			if (candidate.craftable && candidate.faction != Faction.NAVY && !HighTier.is(candidate) && (!candidate.armorOnly || type.isArmor())) {
				material = candidate;
				break;
			}
			refused = refused == null ? candidate : refused;
		}
		if (material == null) {
			if (refused == null) {
				say.accept("I can forge you a copy of that " + type.displayName + ", but what of? Hold the material in your other hand.");
			} else if (refused.faction == Faction.NAVY) {
				say.accept(refused.displayName + "? That's Navy stuff. I'm not working it, they'd hang me. Find their own furnaces.");
			} else if (refused.craftable && HighTier.is(refused)) {
				say.accept(refused.displayName + "? My forge doesn't get near hot enough. You'll want a Shipwright's Forge for that.");
			} else if (refused.armorOnly) {
				say.accept(refused.displayName + " is for armor and linings, not a " + type.displayName + ".");
			} else {
				say.accept("Nobody can forge " + refused.displayName + ". That's a legend, not a material.");
			}
			return false;
		}
		int needed = materialCost(type, town);
		int fee = forgeFee(material, town);
		Inventory inventory = customer.getInventory();
		String materialName = new ItemStack(material.repairItem()).getHoverName().getString();
		if (count(inventory, material.repairItem()) < needed) {
			say.accept("A " + material.displayName + " " + type.displayName + " takes " + needed + " " + materialName + ". Bring me more.");
			return false;
		}
		if (!canPay(customer, fee)) {
			say.accept("That's " + fee + " Berries for the work, and you're short. I don't work for free.");
			return false;
		}
		pay(customer, fee);
		take(inventory, material.repairItem(), needed);
		ItemStack copy = PartItem.of(part, material);
		String name = copy.getHoverName().getString(); // inventory.add empties the stack, so read the name first.
		give(customer, copy);
		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.8f, 1.0f);
		say.accept(specialty(town, type) ? "A " + name + "! My specialty. Took less " + materialName + " than anyone else could, too."
				: "There, a " + name + ". " + needed + " " + materialName + " and " + fee + " Berries.");
		return true;
	}

	/** Material a smith of this town needs for one part: the crafting recipe's, less their savings. At least 1. */
	public static int materialCost(PartType type, String town) {
		Settings settings = settings();
		int saving = settings.materialSaving + (specialty(town, type) ? settings.specialtySaving : 0);
		return Math.max(1, NavyCastingBlock.cost(type) - saving);
	}

	/** Berries a smith of this town charges to forge a part of this material. */
	public static int forgeFee(ArsenalMaterial material, String town) {
		Settings settings = settings();
		return price(settings.forgeFee + settings.forgeFeePerTier * material.tier, town);
	}

	public static boolean specialty(String town, PartType type) {
		Settings.Town shop = settings().towns.get(town);
		return shop != null && shop.specialty.contains(type.id());
	}

	// ---- Putting gear together ----

	private static boolean assemble(Level level, BlockPos pos, ServerPlayer customer, String town, Consumer<String> say) {
		Inventory inventory = customer.getInventory();
		if (!ShipwrightsAnvilBlock.hasSet(inventory, false, false)) {
			say.accept(ShipwrightsAnvilBlock.hasSet(inventory, false, true)
					? "Parts that tough would crack my anvil. That's work for a Grand Line Anvil."
					: ShipwrightsAnvilBlock.hasSet(inventory, true, true)
					? "Navy gear? Not at my anvil. Take that to one of their Armory Benches, if you can get at one."
					: "Bring me a full set of parts and I'll put them together. Or hold a material in your other hand and I'll copy that part in it.");
			return false;
		}
		int fee = price(settings().assembleFee, town);
		if (!canPay(customer, fee)) {
			say.accept("Putting that together is " + fee + " Berries. Come back when you have them.");
			return false;
		}
		if (!ShipwrightsAnvilBlock.forge(level, pos, customer, false, false).consumesAction()) {
			return false;
		}
		pay(customer, fee);
		say.accept("All fitted together, and it'll hold. That's " + fee + " Berries.");
		return true;
	}

	// ---- Mending ----

	private static boolean mend(Level level, BlockPos pos, ServerPlayer customer, ItemStack gear, String town, Consumer<String> say) {
		if (!gear.isDamaged()) {
			say.accept("That " + gear.getHoverName().getString() + " is in fine shape. Bring it back when it's been through a fight.");
			return false;
		}
		ArsenalMaterial strongest = HighTier.strongest(gear);
		if (strongest != null) {
			say.accept(strongest.displayName + "? I can't work that here. It wants a Grand Line Anvil.");
			return false;
		}
		ArsenalMaterial material = Upgrades.headMaterial(gear);
		Item mender = material.repairItem();
		String menderName = new ItemStack(mender).getHoverName().getString();
		int perPiece = Math.max(1, (int) Math.ceil(gear.getMaxDamage() * settings().repairShare));
		int needed = (gear.getDamageValue() + perPiece - 1) / perPiece;
		int feeEach = price(settings().repairFee, town);
		int pieces = Math.min(needed, count(customer.getInventory(), mender));
		if (feeEach > 0) {
			pieces = Math.min(pieces, count(customer.getInventory(), OnePieceBridge.berry()) / feeEach);
		}
		if (pieces <= 0) {
			say.accept(count(customer.getInventory(), mender) == 0
					? "I'll mend it, but I need " + menderName + " to do it, the stuff of its " + material.displayName + "."
					: "Mending's " + feeEach + " Berries a piece of " + menderName + ". You can't pay for even one.");
			return false;
		}
		pay(customer, pieces * feeEach);
		take(customer.getInventory(), mender, pieces);
		gear.setDamageValue(Math.max(0, gear.getDamageValue() - pieces * perPiece));
		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.6f, 1.2f);
		say.accept(gear.isDamaged()
				? "Did what I could with " + pieces + " " + menderName + ". Bring more and I'll finish it."
				: "Good as new! " + pieces + " " + menderName + " and " + pieces * feeEach + " Berries.");
		return true;
	}

	// ---- Berries and materials ----

	/** A fee with the town's prices. */
	private static int price(int fee, String town) {
		Settings.Town shop = settings().towns.get(town);
		return shop == null ? fee : (int) Math.ceil(fee * shop.price);
	}

	private static boolean canPay(ServerPlayer customer, int fee) {
		return count(customer.getInventory(), OnePieceBridge.berry()) >= fee;
	}

	private static void pay(ServerPlayer customer, int fee) {
		take(customer.getInventory(), OnePieceBridge.berry(), fee); // Creative players pay too, like at any town stall.
	}

	/** How many of this item the inventory holds, not counting parts or gear made of it. */
	private static int count(Inventory inventory, Item item) {
		int count = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item) && isLoose(stack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static void take(Inventory inventory, Item item, int count) {
		for (int i = 0; i < inventory.getContainerSize() && count > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item) && isLoose(stack)) {
				int taken = Math.min(count, stack.getCount());
				stack.shrink(taken);
				count -= taken;
			}
		}
	}

	private static boolean isLoose(ItemStack stack) {
		return !(stack.getItem() instanceof PartItem) && Upgrades.gearOf(stack) == null;
	}

	private static void give(ServerPlayer customer, ItemStack stack) {
		if (!customer.getInventory().add(stack)) {
			customer.drop(stack, false, Prediction.SERVER_ONLY); // Inventory full: drop it at their feet.
		}
	}

	// ---- blacksmiths.json ----

	private static Settings settings() {
		if (settings == null) {
			try (InputStream in = Blacksmiths.class.getResourceAsStream("/arsenal/blacksmiths.json")) {
				settings = new Settings(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
			} catch (Exception e) {
				throw new RuntimeException("Could not read arsenal/blacksmiths.json", e);
			}
		}
		return settings;
	}

	private static final class Settings {
		final int forgeFee;
		final int forgeFeePerTier;
		final int materialSaving;
		final int assembleFee;
		final double repairShare;
		final int repairFee;
		final int specialtySaving;
		final Map<String, Town> towns = new java.util.LinkedHashMap<>(); // In blacksmiths.json's order, for the manual.

		Settings(JsonObject json) {
			forgeFee = json.get("forge_fee").getAsInt();
			forgeFeePerTier = json.get("forge_fee_per_tier").getAsInt();
			materialSaving = json.get("material_saving").getAsInt();
			assembleFee = json.get("assemble_fee").getAsInt();
			repairShare = json.get("repair_share").getAsDouble();
			repairFee = json.get("repair_fee").getAsInt();
			specialtySaving = json.get("specialty_saving").getAsInt();
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("towns").entrySet()) {
				JsonObject town = entry.getValue().getAsJsonObject();
				Set<String> specialty = new HashSet<>();
				if (town.has("specialty")) {
					town.getAsJsonArray("specialty").forEach(part -> specialty.add(part.getAsString()));
				}
				towns.put(entry.getKey(), new Town(town.has("price") ? town.get("price").getAsDouble() : 1.0, specialty));
			}
		}

		record Town(double price, Set<String> specialty) {
		}
	}

	/** One line per town for the manual: the town, its prices and its smith's specialty parts. */
	public static java.util.List<String> guideLines() {
		java.util.List<String> lines = new java.util.ArrayList<>();
		for (Map.Entry<String, Settings.Town> entry : settings().towns.entrySet()) {
			Settings.Town shop = entry.getValue();
			java.util.List<String> parts = new java.util.ArrayList<>();
			for (PartType type : PartType.values()) {
				if (shop.specialty().contains(type.id())) {
					parts.add(type.displayName);
				}
			}
			String price = shop.price() == 1.0 ? "" : " (prices x" + shop.price() + ")";
			String town = OnePieceBridge.townName(entry.getKey());
			town = town.isEmpty() ? town : Character.toUpperCase(town.charAt(0)) + town.substring(1); // "the Flower Capital" starts a line.
			lines.add("- " + town + price + ": " + String.join(", ", parts) + ".");
		}
		return lines;
	}

	/** How much of an item one piece of material mends at a town smith (the Shipwright's Anvil mends a quarter). */
	public static double repairShare() {
		return settings().repairShare;
	}
}
