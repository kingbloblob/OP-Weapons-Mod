package com.onepiecearsenal.meito;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a katana has done, saved on the katana itself, so its history goes with it when it changes hands.
 *
 * @param xp         blade experience from kills, capped at its grade's max level (see Meito)
 * @param armedKills enemies it beat that were holding a weapon
 * @param bossTier   the highest tier of boss it has beaten (1 to 5), 0 for none
 */
public record BladeRecord(int xp, int armedKills, int bossTier) {
	public static final BladeRecord EMPTY = new BladeRecord(0, 0, 0);

	public static final Codec<BladeRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("xp", 0).forGetter(BladeRecord::xp),
			Codec.INT.optionalFieldOf("armed_kills", 0).forGetter(BladeRecord::armedKills),
			Codec.INT.optionalFieldOf("boss_tier", 0).forGetter(BladeRecord::bossTier)
	).apply(instance, BladeRecord::new));

	public static final StreamCodec<ByteBuf, BladeRecord> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, BladeRecord::xp,
			ByteBufCodecs.VAR_INT, BladeRecord::armedKills,
			ByteBufCodecs.VAR_INT, BladeRecord::bossTier,
			BladeRecord::new);
}
