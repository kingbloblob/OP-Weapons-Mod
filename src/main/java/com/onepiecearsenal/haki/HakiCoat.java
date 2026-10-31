package com.onepiecearsenal.haki;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Armament Haki coating a weapon (see HakiImbue).
 *
 * @param until the game time (in ticks) the coat wears off
 * @param level the wielder's Armament Haki level when they coated it, 2 to 5
 */
public record HakiCoat(long until, int level) {
	public static final Codec<HakiCoat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("until").forGetter(HakiCoat::until),
			Codec.INT.optionalFieldOf("level", 2).forGetter(HakiCoat::level)
	).apply(instance, HakiCoat::new));

	public static final StreamCodec<ByteBuf, HakiCoat> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, HakiCoat::until,
			ByteBufCodecs.VAR_INT, HakiCoat::level,
			HakiCoat::new);

	/** True while the coat is still on at this game time. */
	public boolean activeAt(long gameTime) {
		return gameTime < until;
	}
}
