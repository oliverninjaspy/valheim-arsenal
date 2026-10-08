package com.valheimarsenal.world;

import java.util.function.Consumer;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import terrablender.api.Region;
import terrablender.api.RegionType;

/**
 * A TerraBlender region where the vanilla overworld layout is kept, but its dark,
 * hilly old forests become the Mistlands.
 */
public class MistlandsRegion extends Region {
	public MistlandsRegion(Identifier name, int weight) {
		super(name, RegionType.OVERWORLD, weight);
	}

	@Override
	public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
		this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
			builder.replaceBiome(Biomes.DARK_FOREST, ModBiomes.MISTLANDS);
			builder.replaceBiome(Biomes.OLD_GROWTH_PINE_TAIGA, ModBiomes.MISTLANDS);
			builder.replaceBiome(Biomes.OLD_GROWTH_SPRUCE_TAIGA, ModBiomes.MISTLANDS);
			builder.replaceBiome(Biomes.WINDSWEPT_FOREST, ModBiomes.MISTLANDS);
		});
	}
}
