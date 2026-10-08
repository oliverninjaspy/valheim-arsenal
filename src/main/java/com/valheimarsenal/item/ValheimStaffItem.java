package com.valheimarsenal.item;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.valheimarsenal.combat.Spells;
import com.valheimarsenal.weapon.WeaponSpec;

/** Valheim's magic staves. Right click casts; see {@link Spells}. */
public class ValheimStaffItem extends Item implements ValheimItem {
	private final WeaponSpec spec;

	public ValheimStaffItem(WeaponSpec spec, Properties properties) {
		super(properties);
		this.spec = spec;
	}

	@Override
	public WeaponSpec spec() {
		return spec;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		return Spells.cast(level, player, hand, stack, spec) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		Tooltips.append(spec, out);
	}
}
