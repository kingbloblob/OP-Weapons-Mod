package com.onepiecearsenal.loot;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.compat.OnePieceBridge;

/**
 * Marine Standard Steel can't be crafted: it's taken from the Navy. Marines drop it when a player beats them
 * (more from officers and admirals), and the main mod's Marine base and bunker chests hold some.
 */
public class MarineSteelLoot {
	private static final Identifier MARINE_CHEST = Identifier.fromNamespaceAndPath("onepiece", "chests/marine");
	private static final Identifier BUNKER_CHEST = Identifier.fromNamespaceAndPath("onepiece", "chests/marine_bunker");

	public static void initialize() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (OnePieceBridge.isMarine(entity) && source.getEntity() instanceof Player && entity.level() instanceof ServerLevel level) {
				int count = ingotsFor(OnePieceBridge.marineKit(entity), entity.getRandom());
				if (count > 0) {
					entity.spawnAtLocation(level, new ItemStack(ArsenalItems.MARINE_STEEL_INGOT, count));
				}
			}
		});

		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (key.identifier().equals(MARINE_CHEST)) {
				tableBuilder.withPool(steel(0.6f, 1, 3));
			} else if (key.identifier().equals(BUNKER_CHEST)) {
				tableBuilder.withPool(steel(1.0f, 2, 5));
			}
		});
	}

	/** Ingots dropped by a beaten Marine: one in four of the rank and file, every officer and up. */
	private static int ingotsFor(String kit, RandomSource random) {
		return switch (kit) {
			case "vice_admiral", "cp0_agent", "pacifista", "seraphim" -> 2 + random.nextInt(3);
			case "elite_marine" -> 1 + random.nextInt(2);
			case "marine_officer" -> random.nextInt(2) == 0 ? 1 + random.nextInt(2) : 0;
			default -> random.nextInt(4) == 0 ? 1 : 0;
		};
	}

	private static LootPool.Builder steel(float chance, int min, int max) {
		return LootPool.lootPool()
				.when(LootItemRandomChanceCondition.randomChance(chance))
				.add(LootItem.lootTableItem(ArsenalItems.MARINE_STEEL_INGOT)
						.apply(SetItemCountFunction.setCount(Holder.direct(new UniformGenerator(
								Holder.direct(new ConstantValue(min)), Holder.direct(new ConstantValue(max)))))));
	}
}
