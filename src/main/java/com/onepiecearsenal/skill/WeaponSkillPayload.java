package com.onepiecearsenal.skill;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.onepiecearsenal.ArsenalMod;

/**
 * The message the player's game sends to the server when they press the Weapon Skill key.
 * Keys only exist on the player's computer, so the server has to be told about them.
 */
public record WeaponSkillPayload() implements CustomPacketPayload {
	public static final WeaponSkillPayload INSTANCE = new WeaponSkillPayload();
	public static final CustomPacketPayload.Type<WeaponSkillPayload> TYPE = new CustomPacketPayload.Type<>(ArsenalMod.id("weapon_skill"));
	public static final StreamCodec<ByteBuf, WeaponSkillPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
