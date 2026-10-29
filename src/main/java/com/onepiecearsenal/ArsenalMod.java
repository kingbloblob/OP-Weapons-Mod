package com.onepiecearsenal;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

import com.onepiecearsenal.disguise.MarineDisguise;
import com.onepiecearsenal.loot.MarineSteelLoot;
import com.onepiecearsenal.material.TraitEffects;

/**
 * The starting point of One Piece Arsenal. Minecraft calls onInitialize() once while the game is loading,
 * after the One Piece Mod (fabric.mod.json makes Arsenal depend on it).
 */
public class ArsenalMod implements ModInitializer {
	public static final String MOD_ID = "arsenal";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ArsenalComponents.initialize();
		ArsenalItems.initialize();
		TraitEffects.initialize();
		MarineDisguise.initialize();
		MarineSteelLoot.initialize();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
