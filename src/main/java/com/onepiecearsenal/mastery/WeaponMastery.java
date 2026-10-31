package com.onepiecearsenal.mastery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How well a weapon has been used, saved on the weapon itself, so it goes with the weapon when it changes hands.
 *
 * @param xp    mastery experience from foes beaten with it (see Mastery)
 * @param trait the trait bound into its mastery slot, by its enum name in lower case ("keen"), or "" for none
 */
public record WeaponMastery(int xp, String trait) {
	public static final WeaponMastery EMPTY = new WeaponMastery(0, "");

	public static final Codec<WeaponMastery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("xp", 0).forGetter(WeaponMastery::xp),
			Codec.STRING.optionalFieldOf("trait", "").forGetter(WeaponMastery::trait)
	).apply(instance, WeaponMastery::new));

	public static final StreamCodec<ByteBuf, WeaponMastery> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, WeaponMastery::xp,
			ByteBufCodecs.STRING_UTF8, WeaponMastery::trait,
			WeaponMastery::new);

	public WeaponMastery withXp(int xp) {
		return new WeaponMastery(xp, trait);
	}

	public WeaponMastery withTrait(String trait) {
		return new WeaponMastery(xp, trait);
	}
}
