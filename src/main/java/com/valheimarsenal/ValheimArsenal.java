package com.valheimarsenal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;

import com.valheimarsenal.combat.CombatEvents;
import com.valheimarsenal.registry.ModEntities;
import com.valheimarsenal.registry.ModItems;

public class ValheimArsenal implements ModInitializer {
	public static final String MOD_ID = "valheim_arsenal";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.initialize();
		ModItems.initialize();
		CombatEvents.register();
		LOGGER.info("Valheim Arsenal loaded {} items. Skål!", ModItems.count());
	}
}
