package com.valheimarsenal.weapon;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

import com.valheimarsenal.ValheimArsenal;

/**
 * Valheim's biome progression, each step tied to a Minecraft tool tier.
 * Block and repair tags live in data/valheim_arsenal/tags.
 */
public enum Tier {
	MEADOWS("meadows", 2.0F, 15),
	BLACK_FOREST("black_forest", 4.0F, 14),
	SWAMP("swamp", 6.0F, 14),
	MOUNTAINS("mountains", 7.0F, 18),
	PLAINS("plains", 8.0F, 12),
	MISTLANDS("mistlands", 9.0F, 16),
	ASHLANDS("ashlands", 9.5F, 16);

	private final String key;
	private final int enchantability;
	private final TagKey<Item> repairItems;
	private final ToolMaterial toolMaterial;

	Tier(String key, float miningSpeed, int enchantability) {
		this.key = key;
		this.enchantability = enchantability;
		this.repairItems = TagKey.create(Registries.ITEM, ValheimArsenal.id("repairs_" + key));
		TagKey<Block> incorrect = TagKey.create(Registries.BLOCK, ValheimArsenal.id("incorrect_for_" + key));
		// Durability and attack damage on the material are placeholders: every item sets
		// its own durability and attribute modifiers from WeaponSpec.
		this.toolMaterial = new ToolMaterial(incorrect, 250, miningSpeed, 0.0F, enchantability, repairItems);
	}

	public String key() {
		return key;
	}

	public int enchantability() {
		return enchantability;
	}

	public TagKey<Item> repairItems() {
		return repairItems;
	}

	public ToolMaterial toolMaterial() {
		return toolMaterial;
	}

	public String translationKey() {
		return "tooltip.valheim_arsenal.tier." + key;
	}
}
