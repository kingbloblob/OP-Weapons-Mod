package com.onepiecearsenal.station;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.onepiecearsenal.material.ArsenalMaterial;
import com.onepiecearsenal.part.PartItem;
import com.onepiecearsenal.part.PartType;

/**
 * A Navy station that makes parts out of Navy materials. Those parts have no crafting table recipe: the Navy
 * keeps the patterns and the furnaces, so pirates have to capture the station first.
 *
 * Right-click it holding any part (any material) as the pattern. It's kept, and you get a copy made of the
 * station's material, paid for with that material's ingots from your inventory (as many as the part's crafting
 * recipe would take, see cost()).
 *
 * The Quartermaster's Desk copies Navy patterns in Marine Standard Steel; the Marine Base Smeltery is the only
 * place that can cast seastone (Kairoseki).
 */
public class NavyCastingBlock extends NavyStationBlock {
	/** What the station makes parts of, first choice first. Add a material here to make it station-only too. */
	private final List<ArsenalMaterial> materials;
	private final String verb;
	private final SoundEvent sound;

	public NavyCastingBlock(String title, String verb, SoundEvent sound, List<ArsenalMaterial> materials, Properties properties) {
		super(title, properties);
		this.verb = verb;
		this.sound = sound;
		this.materials = materials;
	}

	public List<ArsenalMaterial> materials() {
		return materials;
	}

	@Override
	protected InteractionResult work(BlockState state, Level level, BlockPos pos, ServerPlayer player) {
		ItemStack pattern = player.getMainHandItem();
		if (!(pattern.getItem() instanceof PartItem part)) {
			player.sendOverlayMessage(Component.literal("Hold a part to use as the pattern. You get a " + names() + " copy of it.")
					.withStyle(ChatFormatting.YELLOW));
			return InteractionResult.FAIL;
		}
		if (part.type().isDial()) {
			player.sendOverlayMessage(Component.literal("Dials are Skypiea work: the Navy can't make a " + part.type().displayName + ".")
					.withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		int cost = cost(part.type());
		Inventory inventory = player.getInventory();
		for (ArsenalMaterial material : materials) {
			Item ingot = material.repairItem();
			if (inventory.countItem(ingot) < cost) {
				continue;
			}
			take(inventory, ingot, cost);
			ItemStack copy = PartItem.of(part, material);
			String name = copy.getHoverName().getString(); // inventory.add empties the stack, so read the name first.
			if (!inventory.add(copy)) {
				player.drop(copy, false, Prediction.SERVER_ONLY);
			}
			level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
			player.sendOverlayMessage(Component.literal(verb + " a " + name + "!").withStyle(ChatFormatting.GREEN));
			return InteractionResult.SUCCESS;
		}
		ArsenalMaterial first = materials.get(0);
		player.sendOverlayMessage(Component.literal("A " + first.displayName + " " + part.type().displayName + " takes " + cost + " "
				+ new ItemStack(first.repairItem()).getHoverName().getString() + ".").withStyle(ChatFormatting.RED));
		return InteractionResult.FAIL;
	}

	/** Ingots per part, the same as the crafting table recipes for the other materials. */
	public static int cost(PartType type) {
		return switch (type) {
			case HILT, LINING, FUSE, BINDING, STUDS, GRIP, HAFT, CANNON_SHELL -> 1;
			case BARREL, LOCK, SABRE_BLADE, JITTE_ROD -> 2;
			case KATANA_BLADE, STOCK, LONG_BARREL, CUTLASS_BLADE, GUN_CARRIAGE, KNUCKLES -> 3;
			case TSUBA, BOOT_SOLES, AXE_HEAD, SPEARHEAD -> 4;
			case CROWN, CANNON_BORE, HAND_GUARD -> 5;
			case CLUB_HEAD -> 6;
			case SASH_WEAVE -> 7;
			case COAT_PLATES -> 8;
			default -> 3; // New part types: give them their own line above.
		};
	}

	private String names() {
		return String.join(" or ", materials.stream().map(material -> material.displayName).toList());
	}

	private static void take(Inventory inventory, Item item, int count) {
		for (int i = 0; i < inventory.getContainerSize() && count > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				int taken = Math.min(count, stack.getCount());
				stack.shrink(taken);
				count -= taken;
			}
		}
	}
}
