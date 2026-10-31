package com.onepiecearsenal.salvage;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import com.onepiecearsenal.ArsenalComponents;
import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;
import com.onepiecearsenal.station.ShipwrightsAnvilBlock;
import com.onepiecearsenal.tool.ArsenalGunItem;
import com.onepiecearsenal.tool.ArsenalWeaponItem;
import com.onepiecearsenal.tool.GauntletStats;
import com.onepiecearsenal.tool.Restat;
import com.onepiecearsenal.tool.ShieldStats;
import com.onepiecearsenal.upgrade.Upgrade.Gear;
import com.onepiecearsenal.upgrade.Upgrades;

/**
 * Taking forged gear apart at the Shipwright's Anvil: sneak and right-click it holding the gear.
 * - Gear that still works comes apart into all its parts.
 * - Broken gear (see Breakage) loses the part that held it together: the tsuba, binding, firing part or lining
 *   (a shield's rim, which is a Binding).
 *
 * The head part (blade, head, barrel or shell) carries the gear's history (ArsenalComponents.SALVAGED_FROM): forge
 * it into the same kind of gear again, with any other parts, and it comes back with its upgrades, whetstone grade,
 * blade level, enchantments, Dials, name and wear. Broken gear's head comes back unworn, since the broken part
 * took the wear with it. Swapping parts this way is how you change a katana's tsuba without losing the blade.
 */
public final class Salvage {
	/** Which part a broken piece loses: the second, the one that joins the head to the rest. */
	private static final int SNAPPED_PART = 1;

	private Salvage() {
	}

	public static void initialize() {
		// Sneaking with an item in hand skips the block's own right-click, so this listens before that.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || !player.isSecondaryUseActive() || player.isSpectator()
					|| Upgrades.gearOf(player.getMainHandItem()) == null
					|| !(level.getBlockState(hit.getBlockPos()).getBlock() instanceof ShipwrightsAnvilBlock)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer smith) {
				takeApart(level, hit.getBlockPos(), smith);
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** Takes the gear in the smith's main hand apart and gives them the parts. */
	public static void takeApart(Level level, BlockPos pos, ServerPlayer smith) {
		ItemStack gear = smith.getMainHandItem();
		boolean broken = Breakage.isBroken(gear);
		String name = gear.getHoverName().getString();
		List<ItemStack> parts = parts(gear);
		smith.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		for (ItemStack part : parts) {
			if (!smith.getInventory().add(part)) {
				smith.drop(part, false, Prediction.SERVER_ONLY); // Inventory full: drop it at their feet.
			}
		}
		level.playSound(null, pos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 0.6f, 1.2f);
		smith.sendOverlayMessage(Component.literal(broken
				? "Took the " + name + " apart. The part holding it together snapped."
				: "Took the " + name + " apart.").withStyle(ChatFormatting.GREEN));
	}

	/** What the gear comes apart into, head first. Broken gear's snapped part is left out. Doesn't change the gear. */
	public static List<ItemStack> parts(ItemStack gear) {
		Gear kind = Upgrades.gearOf(gear);
		if (kind == null) {
			return List.of();
		}
		boolean broken = Breakage.isBroken(gear);
		List<ArsenalMaterial> materials = Restat.parts(gear, kind);
		PartType[] types = partTypes(gear, kind);
		List<ItemStack> parts = new ArrayList<>();
		for (int i = 0; i < types.length; i++) {
			if (broken && i == SNAPPED_PART) {
				continue;
			}
			parts.add(PartItem.of(ArsenalItems.partItem(types[i]), materials.get(i)));
		}

		ItemStack history = gear.copyWithCount(1);
		history.remove(ArsenalComponents.JUST_BROKE);
		if (broken) {
			history.setDamageValue(0);
		}
		parts.get(0).set(ArsenalComponents.SALVAGED_FROM, ItemContainerContents.fromItems(List.of(history)));
		return parts;
	}

	/** The gear's part types, in the same order as its materials. */
	private static PartType[] partTypes(ItemStack gear, Gear kind) {
		return switch (kind) {
			case KATANA -> new PartType[] {PartType.KATANA_BLADE, PartType.TSUBA, PartType.HILT};
			case WEAPON -> ((ArsenalWeaponItem) gear.getItem()).type().parts();
			case GUN -> ((ArsenalGunItem) gear.getItem()).type().parts();
			case HAT -> new PartType[] {PartType.CROWN, PartType.LINING};
			case COAT -> new PartType[] {PartType.COAT_PLATES, PartType.LINING};
			case SASH -> new PartType[] {PartType.SASH_WEAVE, PartType.LINING};
			case BOOTS -> new PartType[] {PartType.BOOT_SOLES, PartType.LINING};
			case SHIELD -> ShieldStats.parts();
			case GAUNTLETS -> GauntletStats.parts();
		};
	}

	/** The gear a salvaged head part came from, or an empty stack for a fresh part. */
	public static ItemStack history(ItemStack part) {
		ItemContainerContents saved = part.get(ArsenalComponents.SALVAGED_FROM);
		return saved == null ? ItemStack.EMPTY : saved.copyOne();
	}

	/**
	 * Gear just forged with this head part: if the head was salvaged from the same kind of gear, it's that gear again,
	 * with the new parts' numbers worked out by Restat. Otherwise the freshly built gear, unchanged.
	 */
	public static ItemStack restore(ItemStack head, ItemStack built) {
		ItemStack gear = history(head);
		if (gear.isEmpty() || gear.getItem() != built.getItem()) {
			return built; // A hand cannon's bore made into a Ship Cannon starts fresh.
		}
		copyParts(built, gear);
		gear.remove(ArsenalComponents.STATS_STAMP);
		Restat.refresh(gear);
		return gear;
	}

	private static void copyParts(ItemStack from, ItemStack to) {
		for (var parts : List.of(ArsenalComponents.KATANA_PARTS, ArsenalComponents.WEAPON_PARTS, ArsenalComponents.GUN_PARTS,
				ArsenalComponents.ARMOR_PARTS, ArsenalComponents.SHIELD_PARTS, ArsenalComponents.GAUNTLET_PARTS)) {
			List<String> materials = from.get(parts);
			if (materials != null) {
				to.set(parts, materials);
			}
		}
	}
}
