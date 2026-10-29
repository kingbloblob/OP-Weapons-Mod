package com.onepiecearsenal.loot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.compat.OnePieceBridge;

/**
 * Materials that come from beating the One Piece Mod's creatures and the Navy's machines, when a player wins:
 * Sea King Bone from Sea Kings, Island Whale Leather from Island Whales, Pacifista Alloy from Pacifistas,
 * and Vegapunk Compound from Seraphim and (sometimes) Vice Admirals.
 */
public class MaterialLoot {
	private static final Identifier SEA_KING = Identifier.fromNamespaceAndPath("onepiece", "sea_king");
	private static final Identifier ISLAND_WHALE = Identifier.fromNamespaceAndPath("onepiece", "island_whale");
	private static final Identifier PACIFISTA = Identifier.fromNamespaceAndPath("onepiece", "pacifista");

	public static void initialize() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof Player && entity.level() instanceof ServerLevel level) {
				drop(entity, level, entity.getRandom());
			}
		});
	}

	private static void drop(LivingEntity entity, ServerLevel level, RandomSource random) {
		Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		if (type.equals(SEA_KING)) {
			give(entity, level, ArsenalItems.SEA_KING_BONE, 1 + random.nextInt(3));
		} else if (type.equals(ISLAND_WHALE)) {
			give(entity, level, ArsenalItems.ISLAND_WHALE_LEATHER, 2 + random.nextInt(3));
		} else if (type.equals(PACIFISTA)) {
			give(entity, level, ArsenalItems.PACIFISTA_ALLOY, 1 + random.nextInt(2));
		}
		// Marines spawned from a kit (see the main mod's npc_kits.json).
		switch (OnePieceBridge.marineKit(entity)) {
			case "pacifista" -> give(entity, level, ArsenalItems.PACIFISTA_ALLOY, 1 + random.nextInt(2));
			case "seraphim" -> give(entity, level, ArsenalItems.VEGAPUNK_COMPOUND, 1);
			case "vice_admiral" -> give(entity, level, ArsenalItems.VEGAPUNK_COMPOUND, random.nextInt(4) == 0 ? 1 : 0);
			default -> {
			}
		}
	}

	private static void give(LivingEntity entity, ServerLevel level, Item item, int count) {
		if (count > 0) {
			entity.spawnAtLocation(level, new ItemStack(item, count));
		}
	}
}
