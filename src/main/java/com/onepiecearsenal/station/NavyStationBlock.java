package com.onepiecearsenal.station;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.onepiecearsenal.compat.OnePieceBridge;
import com.onepiecearsenal.disguise.MarineDisguise;

/**
 * A Navy crafting station. Players can't craft these: they stand in Marine bases (see NavyStations) and have
 * to be captured.
 *
 * While the base's Marines are around (any Marine within GUARD_RANGE), the station is guarded: trying to use it
 * or break it brings them down on you. An officer's uniform (Kairoseki shells) gets you in to use it without
 * a fight, but you can't carry it off. Once no Marines are left nearby, right-click it to capture it, or mine
 * it and take it home.
 *
 * A captured station works anywhere. But if a Marine spots it, the Navy sends a squad after whoever is nearby,
 * and any Marine who reaches it takes it back.
 */
public abstract class NavyStationBlock extends HorizontalDirectionalBlock {
	/** False for a station still held by the Navy. Placed by a player = captured. */
	public static final BooleanProperty CAPTURED = BooleanProperty.create("captured");

	/** Marines this close guard an uncaptured station. */
	public static final double GUARD_RANGE = 24.0;
	/** Marines this close who can see a captured station report it. */
	private static final double SPOT_RANGE = 16.0;
	/** A Marine this close to a captured station takes it back. */
	private static final double RETAKE_RANGE = 2.0;
	/** Who gets hunted when the station is spotted: the nearest player this close to it. */
	private static final double OWNER_RANGE = 64.0;
	/** How often a captured station looks around. 20 ticks = 1 second. */
	private static final int CHECK_TICKS = 40;
	/** After a station is reported, the Navy doesn't send another squad for it for this long. */
	private static final long REPORT_COOLDOWN_TICKS = 20 * 60 * 10;

	/** When each captured station was last reported (dimension + position, game time). Forgotten on restart. */
	private static final Map<String, Long> LAST_REPORT = new HashMap<>();

	private final String title;

	protected NavyStationBlock(String title, Properties properties) {
		super(properties);
		this.title = title;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CAPTURED, true));
	}

	/** The station's name, like "Marine Armory Bench". */
	public String title() {
		return title;
	}

	/** What the station does when a player is allowed to use it. */
	protected abstract InteractionResult work(BlockState state, Level level, BlockPos pos, ServerPlayer player);

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, CAPTURED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (state.getValue(CAPTURED)) {
			level.scheduleTick(pos, this, CHECK_TICKS);
		}
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer pirate)) {
			return InteractionResult.SUCCESS;
		}
		if (state.getValue(CAPTURED) || pirate.isCreative()) {
			return work(state, level, pos, pirate);
		}
		List<Mob> guards = guards(level, pos);
		if (guards.isEmpty()) {
			// Nobody left to stop you.
			level.setBlock(pos, state.setValue(CAPTURED, true), Block.UPDATE_ALL);
			level.scheduleTick(pos, this, CHECK_TICKS);
			level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8f, 1.2f);
			pirate.sendSystemMessage(Component.literal("You captured the " + title + "! Mine it to take it with you, but don't let the Marines see it.")
					.withStyle(ChatFormatting.GOLD));
			return work(state.setValue(CAPTURED, true), level, pos, pirate);
		}
		if (guards.stream().allMatch(guard -> MarineDisguise.fools(guard, pirate))) {
			if (MarineDisguise.quality(pirate).ordinal() >= MarineDisguise.Quality.OFFICER.ordinal()) {
				return work(state, level, pos, pirate); // Officers are allowed at the Navy's stations.
			}
			pirate.sendOverlayMessage(Component.literal("Recruits aren't allowed at the " + title + ". You'd need an officer's uniform.")
					.withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		warn(pirate, guards);
		return InteractionResult.FAIL;
	}

	/**
	 * Called before a player breaks a Navy station. A guarded station can't be carried off.
	 * Returns false (and calls the guards) if it's still guarded.
	 */
	public boolean canBreak(BlockState state, Level level, BlockPos pos, ServerPlayer player) {
		if (state.getValue(CAPTURED) || player.isCreative()) {
			return true;
		}
		List<Mob> guards = guards(level, pos);
		if (guards.isEmpty()) {
			return true;
		}
		warn(player, guards);
		return false;
	}

	/** Tells the player the station is guarded, and the guards who aren't fooled come for them. */
	private void warn(ServerPlayer pirate, List<Mob> guards) {
		pirate.sendOverlayMessage(Component.literal("The Marines are guarding the " + title + "! Beat the " + guards.size()
				+ " still nearby to capture it.").withStyle(ChatFormatting.RED));
		for (Mob guard : guards) {
			if (!MarineDisguise.fools(guard, pirate)) {
				guard.setTarget(pirate);
			}
		}
	}

	/** Living Marines close enough to guard this spot. */
	public static List<Mob> guards(Level level, BlockPos pos) {
		return level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(GUARD_RANGE),
				mob -> mob.isAlive() && OnePieceBridge.isMarine(mob));
	}

	/** A captured station looks around every couple of seconds for Marines. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(CAPTURED)) {
			return;
		}
		List<Mob> marines = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(SPOT_RANGE),
				mob -> mob.isAlive() && OnePieceBridge.isMarine(mob));
		for (Mob marine : marines) {
			if (marine.distanceToSqr(Vec3.atCenterOf(pos)) <= (RETAKE_RANGE + 0.5) * (RETAKE_RANGE + 0.5)) {
				retake(level, pos);
				return;
			}
		}
		for (Mob marine : marines) {
			if (canSee(level, marine, pos)) {
				report(level, pos);
				break;
			}
		}
		level.scheduleTick(pos, this, CHECK_TICKS);
	}

	private static boolean canSee(ServerLevel level, Mob marine, BlockPos pos) {
		BlockHitResult hit = level.clip(new ClipContext(marine.getEyePosition(), Vec3.atCenterOf(pos),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, marine));
		return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
	}

	/** A Marine saw the stolen station: the Navy sends a squad after the nearest pirate. */
	private void report(ServerLevel level, BlockPos pos) {
		String key = level.dimension().identifier() + "@" + pos.asLong();
		long now = level.getGameTime();
		Long last = LAST_REPORT.get(key);
		if (last != null && now - last < REPORT_COOLDOWN_TICKS) {
			return;
		}
		Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, OWNER_RANGE,
				entity -> entity instanceof Player player && !player.isSpectator() && !player.isCreative());
		if (!(nearest instanceof ServerPlayer pirate)) {
			return;
		}
		LAST_REPORT.put(key, now);
		OnePieceBridge.sendMarineSquad(pirate);
		pirate.sendSystemMessage(Component.literal("A Marine spotted your stolen " + title + "! The Navy is sending a squad to take it back.")
				.withStyle(ChatFormatting.RED));
	}

	/** A Marine reached the station and takes it back to the Navy. */
	private void retake(ServerLevel level, BlockPos pos) {
		level.removeBlock(pos, false);
		level.playSound(null, pos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0f, 0.8f);
		level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.02);
		LAST_REPORT.remove(level.dimension().identifier() + "@" + pos.asLong());
		for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(Vec3.atCenterOf(pos)) < OWNER_RANGE * OWNER_RANGE)) {
			player.sendSystemMessage(Component.literal("The Marines took back the " + title + "!").withStyle(ChatFormatting.RED));
		}
	}
}
