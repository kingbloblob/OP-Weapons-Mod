package com.onepiecearsenal;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

import com.onepiecearsenal.compat.MainModLinks;
import com.onepiecearsenal.disguise.MarineDisguise;
import com.onepiecearsenal.disguise.PirateGrudge;
import com.onepiecearsenal.disguise.Suspicion;
import com.onepiecearsenal.guide.GuideBook;
import com.onepiecearsenal.loot.MarineGear;
import com.onepiecearsenal.loot.MarineSteelLoot;
import com.onepiecearsenal.loot.MaterialLoot;
import com.onepiecearsenal.material.TraitEffects;
import com.onepiecearsenal.material.TraitStats;
import com.onepiecearsenal.meito.Meito;
import com.onepiecearsenal.station.NavyStations;
import com.onepiecearsenal.tool.WeaponEffects;

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
		TraitStats.initialize();
		TraitEffects.initialize();
		WeaponEffects.initialize();
		MaterialLoot.initialize();
		MarineDisguise.initialize();
		MarineSteelLoot.initialize();
		MarineGear.initialize();
		Suspicion.initialize();
		PirateGrudge.initialize();
		Meito.initialize();
		MainModLinks.initialize(); // After Meito, so whetstones its gates refuse don't count.
		NavyStations.initialize();
		GuideBook.initialize();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
