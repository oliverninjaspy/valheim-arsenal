package com.valheimarsenal.item;

import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import com.valheimarsenal.weapon.WeaponSpec;

/** Implemented by every Valheim weapon, bow, staff and shield. */
public interface ValheimItem {
	WeaponSpec spec();

	static @Nullable WeaponSpec specOf(@Nullable ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		return stack.getItem() instanceof ValheimItem valheimItem ? valheimItem.spec() : null;
	}
}
