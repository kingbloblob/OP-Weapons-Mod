package com.onepiecearsenal.haki;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.onepiecearsenal.ArsenalMod;

/** The message the player's game sends to the server when they press the Imbue Haki key (see HakiImbue). */
public record ImbueHakiPayload() implements CustomPacketPayload {
	public static final ImbueHakiPayload INSTANCE = new ImbueHakiPayload();
	public static final CustomPacketPayload.Type<ImbueHakiPayload> TYPE = new CustomPacketPayload.Type<>(ArsenalMod.id("imbue_haki"));
	public static final StreamCodec<ByteBuf, ImbueHakiPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
