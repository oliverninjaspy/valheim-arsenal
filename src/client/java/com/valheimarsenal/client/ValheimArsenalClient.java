package com.valheimarsenal.client;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

import net.fabricmc.api.ClientModInitializer;

import com.valheimarsenal.registry.ModEntities;

public class ValheimArsenalClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Thrown spears and staff bolts render as their item sprite.
		EntityRenderers.register(ModEntities.THROWN_SPEAR, ThrownItemRenderer::new);
		EntityRenderers.register(ModEntities.MAGIC_BOLT, ThrownItemRenderer::new);
	}
}
