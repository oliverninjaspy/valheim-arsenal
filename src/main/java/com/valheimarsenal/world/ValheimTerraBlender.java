package com.valheimarsenal.world;

import terrablender.api.MaterialRuleManager;
import terrablender.api.Regions;
import terrablender.api.TerraBlenderApi;

import com.valheimarsenal.ValheimArsenal;

/**
 * TerraBlender entrypoint (see "terrablender" in fabric.mod.json). TerraBlender is
 * bundled inside the Valheim Arsenal jar, so players don't need to install it.
 */
public class ValheimTerraBlender implements TerraBlenderApi {
	/** Higher = Mistlands regions show up more often. Vanilla's own region has weight 10. */
	public static final int MISTLANDS_WEIGHT = 3;

	@Override
	public void onTerraBlenderInitialized() {
		Regions.register(new MistlandsRegion(ValheimArsenal.id("mistlands"), MISTLANDS_WEIGHT));
		MaterialRuleManager.addRules(MaterialRuleManager.RuleCategory.OVERWORLD, ValheimArsenal.MOD_ID, ModBiomes.MISTLANDS_SURFACE);
	}
}
