package com.valheimarsenal.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;

import com.valheimarsenal.ValheimArsenal;

/**
 * Keys for the Mistlands. The biome itself is data-driven:
 * data/valheim_arsenal/worldgen/biome/mistlands.json (fog, colours, spawns, features) and
 * data/valheim_arsenal/worldgen/material_rule/mistlands_surface.json (rocky, mossy ground).
 */
public final class ModBiomes {
	public static final ResourceKey<Biome> MISTLANDS = ResourceKey.create(Registries.BIOME, ValheimArsenal.id("mistlands"));
	public static final ResourceKey<MaterialRule> MISTLANDS_SURFACE =
			ResourceKey.create(Registries.MATERIAL_RULE, ValheimArsenal.id("mistlands_surface"));

	private ModBiomes() {
	}
}
