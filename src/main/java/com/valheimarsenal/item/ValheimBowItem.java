package com.valheimarsenal.item;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.valheimarsenal.weapon.WeaponSpec;

/** Valheim bows. Arrow damage is scaled by the bow (see CombatEvents), and elemental bows apply their element on hit. */
public class ValheimBowItem extends BowItem implements ValheimItem {
	private final WeaponSpec spec;

	public ValheimBowItem(WeaponSpec spec, Properties properties) {
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
