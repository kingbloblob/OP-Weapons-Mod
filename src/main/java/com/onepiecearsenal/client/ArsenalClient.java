package com.onepiecearsenal.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLScancode;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.onepiecearsenal.ArsenalMod;
import com.onepiecearsenal.guide.GuideBook;
import com.onepiecearsenal.haki.ImbueHakiPayload;
import com.onepiecearsenal.mixin.client.ItemTintSourcesAccessor;
import com.onepiecearsenal.mixin.client.SelectItemModelPropertiesAccessor;
import com.onepiecearsenal.skill.WeaponSkillPayload;

/**
 * The client-only start of One Piece Arsenal. Runs before resource packs load, so item models can use
 * the "arsenal:part_material" property.
 *
 * X uses the weapon skill of the Arsenal weapon in your hand (see WeaponSkill). The One Piece Mod already
 * uses Z, C, V, B, G, R, Y, H, J, K, U, M, N and I, so X is the free key next to its techniques. You can change
 * it in Options > Controls > Key Binds, under "One Piece Arsenal".
 *
 * Left Alt coats the weapon in your hand in Armament Haki (see HakiImbue); the blackened blade is the
 * "arsenal:haki_coat" tint (HakiCoatTint). Every letter is taken: vanilla 26.3 opens the Friends List on O and Social
 * Interactions on P, and the One Piece Mod's quest journal is on O too.
 */
public class ArsenalClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ArsenalMod.id("arsenal"));

	@Override
	public void onInitializeClient() {
		SelectItemModelPropertiesAccessor.arsenal$idMapper().put(ArsenalMod.id("part_material"), PartMaterialProperty.TYPE);
		ItemTintSourcesAccessor.arsenal$idMapper().put(ArsenalMod.id("haki_coat"), HakiCoatTint.CODEC);
		GuideBook.openBook = () -> Minecraft.getInstance().gui.setScreen(new GuideScreen());

		KeyMapping skillKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.arsenal.weapon_skill", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_X, CATEGORY));
		KeyMapping imbueKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.arsenal.imbue_haki", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_LALT, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (skillKey.consumeClick()) {
				if (client.player != null && ClientPlayNetworking.canSend(WeaponSkillPayload.TYPE)) {
					ClientPlayNetworking.send(WeaponSkillPayload.INSTANCE);
				}
			}
			while (imbueKey.consumeClick()) {
				if (client.player != null && ClientPlayNetworking.canSend(ImbueHakiPayload.TYPE)) {
					ClientPlayNetworking.send(ImbueHakiPayload.INSTANCE);
				}
			}
		});
	}
}
