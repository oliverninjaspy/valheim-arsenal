package com.valheimarsenal.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.valheimarsenal.ValheimArsenal;
import com.valheimarsenal.item.ItemProperties;
import com.valheimarsenal.item.ValheimBowItem;
import com.valheimarsenal.item.ValheimCrossbowItem;
import com.valheimarsenal.item.ValheimMeleeItem;
import com.valheimarsenal.item.ValheimShieldItem;
import com.valheimarsenal.item.ValheimStaffItem;
import com.valheimarsenal.weapon.WeaponCatalog;
import com.valheimarsenal.weapon.WeaponSpec;

public final class ModItems {
	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	private static final List<Item> MATERIALS = new ArrayList<>();

	public static final ResourceKey<CreativeModeTab> TAB_KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, ValheimArsenal.id("arsenal"));

	private ModItems() {
	}

	public static void initialize() {
		for (String material : WeaponCatalog.MATERIALS) {
			MATERIALS.add(register(material, Item::new, new Item.Properties()));
		}

		for (WeaponSpec spec : WeaponCatalog.ALL) {
			Item item = switch (spec.family()) {
				case BOW -> register(spec.id(), props -> new ValheimBowItem(spec, props), ItemProperties.ranged(spec));
				case CROSSBOW -> register(spec.id(), props -> new ValheimCrossbowItem(spec, props), ItemProperties.ranged(spec));
				case STAFF -> register(spec.id(), props -> new ValheimStaffItem(spec, props), ItemProperties.staff(spec));
				case SHIELD -> register(spec.id(), props -> new ValheimShieldItem(spec, props), ItemProperties.shield(spec));
				default -> register(spec.id(), props -> new ValheimMeleeItem(spec, props), ItemProperties.melee(spec));
			};
			ITEMS.put(spec.id(), item);
		}

		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.icon(() -> new ItemStack(get("frostner")))
				.title(Component.translatable("itemGroup.valheim_arsenal.arsenal"))
				.displayItems((params, output) -> {
					for (Item material : MATERIALS) {
						output.accept(material);
					}
					for (Item item : ITEMS.values()) {
						output.accept(item);
					}
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, tab);
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ValheimArsenal.id(name));
		Item item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static Item get(String id) {
		Item item = ITEMS.get(id);
		if (item == null) {
			throw new IllegalArgumentException("Unknown Valheim Arsenal item: " + id);
		}
		return item;
	}

	public static Map<String, Item> all() {
		return Collections.unmodifiableMap(ITEMS);
	}

	public static int count() {
		return ITEMS.size() + MATERIALS.size();
	}
}
