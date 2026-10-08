package com.valheimarsenal.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import com.valheimarsenal.ValheimArsenal;
import com.valheimarsenal.entity.MagicBoltEntity;
import com.valheimarsenal.entity.ThrownSpearEntity;

public final class ModEntities {
	public static final EntityType<ThrownSpearEntity> THROWN_SPEAR = register("thrown_spear",
			EntityType.Builder.<ThrownSpearEntity>of(ThrownSpearEntity::new, MobCategory.MISC)
					.sized(0.4F, 0.4F)
					.clientTrackingRange(6)
					.updateInterval(10));

	public static final EntityType<MagicBoltEntity> MAGIC_BOLT = register("magic_bolt",
			EntityType.Builder.<MagicBoltEntity>of(MagicBoltEntity::new, MobCategory.MISC)
					.sized(0.35F, 0.35F)
					.clientTrackingRange(6)
					.updateInterval(5));

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ValheimArsenal.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void initialize() {
		// Loading the class registers the entity types.
	}
}
