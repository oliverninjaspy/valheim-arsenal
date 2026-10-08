package com.valheimarsenal.item;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.valheimarsenal.weapon.WeaponSpec;

/** Arbalest and Ripper. Bolts hit much harder and fly flatter (see CombatEvents). */
public class ValheimCrossbowItem extends CrossbowItem implements ValheimItem {
	private final WeaponSpec spec;

	public ValheimCrossbowItem(WeaponSpec spec, Properties properties) {
		super(properties);
		this.spec = spec;
	}

	@Override
	public WeaponSpec spec() {
		return spec;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		Tooltips.append(spec, out);
	}
}
