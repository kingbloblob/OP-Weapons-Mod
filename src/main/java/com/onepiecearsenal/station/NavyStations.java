package com.onepiecearsenal.station;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.ArsenalMod;

/**
 * Puts the Navy's crafting stations into the One Piece Mod's Marine bases, and keeps players from carrying off
 * one that is still guarded (see NavyStationBlock).
 *
 * Which bases get which station is set by structure tags, so a datapack can change it:
 * arsenal:has_armory_bench, arsenal:has_quartermasters_desk and arsenal:has_base_smeltery
 * (data/arsenal/tags/worldgen/structure). The first time a player walks into a base, its stations are set
 * down on a floor indoors, the same way the main mod pins up its Wanted Posters.
 */
public class NavyStations {
	private static final Map<String, TagKey<Structure>> TAGS = Map.of(
			"armory_bench", tag("has_armory_bench"),
			"quartermasters_desk", tag("has_quartermasters_desk"),
			"base_smeltery", tag("has_base_smeltery"));

	/** Bases that already got each station ("station@start chunk"). Saved per dimension. */
	public static final AttachmentType<List<String>> DONE = AttachmentRegistry.create(
			ArsenalMod.id("navy_stations_placed"),
			builder -> builder.persistent(Codec.STRING.listOf())
	);

	private static final int TRIES = 300;

	public static void initialize() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
				!(state.getBlock() instanceof NavyStationBlock station) || !(player instanceof ServerPlayer pirate)
						|| station.canBreak(state, level, pos, pirate));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 100 != 70) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.level() instanceof ServerLevel level && !player.isSpectator()) {
					check(level, player.blockPosition());
				}
			}
		});
	}

	/** Sets down any stations the base at this spot should have and doesn't yet. */
	public static void check(ServerLevel level, BlockPos at) {
		for (Map.Entry<String, TagKey<Structure>> entry : TAGS.entrySet()) {
			StructureStart start = level.structureManager().getStructureWithPieceAt(at, entry.getValue());
			if (start == null || !start.isValid()) {
				continue;
			}
			String key = entry.getKey() + "@" + start.getChunkPos().pack();
			List<String> done = level.getAttachedOrElse(DONE, List.of());
			if (done.contains(key)) {
				continue;
			}
			BlockPos placed = place(level, start, block(entry.getKey()), level.getRandom());
			if (placed == null) {
				continue; // Try again next time, when more of the base may be loaded.
			}
			ArsenalMod.LOGGER.info("Put a Navy station ({}) in a Marine base at {}", entry.getKey(), placed);
			List<String> copy = new ArrayList<>(done);
			copy.add(key);
			level.setAttached(DONE, copy);
		}
	}

	private static Block block(String station) {
		return switch (station) {
			case "armory_bench" -> ArsenalItems.MARINE_ARMORY_BENCH;
			case "quartermasters_desk" -> ArsenalItems.QUARTERMASTERS_DESK;
			default -> ArsenalItems.MARINE_BASE_SMELTERY;
		};
	}

	/** Puts the station on a floor in one of the base's buildings, indoors if it can. Returns where, or null. */
	private static BlockPos place(ServerLevel level, StructureStart start, Block station, RandomSource random) {
		List<StructurePiece> pieces = start.getPieces();
		if (pieces.isEmpty()) {
			return null;
		}
		for (int tries = 0; tries < TRIES; tries++) {
			boolean indoors = tries < TRIES * 2 / 3;
			BoundingBox box = pieces.get(random.nextInt(pieces.size())).getBoundingBox();
			int x = box.minX() + random.nextInt(box.getXSpan());
			int z = box.minZ() + random.nextInt(box.getZSpan());
			BlockPos spot = findFloor(level, x, z, box.minY(), box.maxY(), indoors);
			if (spot == null) {
				continue;
			}
			BlockState state = station.defaultBlockState()
					.setValue(NavyStationBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random))
					.setValue(NavyStationBlock.CAPTURED, false);
			if (level.setBlock(spot, state, Block.UPDATE_ALL)) {
				return spot;
			}
		}
		return null;
	}

	/** An air block with a solid floor and headroom, top down. Indoors = under a roof. */
	private static BlockPos findFloor(ServerLevel level, int x, int z, int minY, int maxY, boolean indoors) {
		for (int y = maxY; y > minY; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!level.isLoaded(pos)) {
				return null;
			}
			if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
				continue;
			}
			BlockPos floor = pos.below();
			if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
				continue;
			}
			if (indoors && level.canSeeSky(pos)) {
				continue;
			}
			return pos;
		}
		return null;
	}

	private static TagKey<Structure> tag(String name) {
		return TagKey.create(Registries.STRUCTURE, ArsenalMod.id(name));
	}
}
