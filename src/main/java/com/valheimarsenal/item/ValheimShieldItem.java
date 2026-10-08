package com.valheimarsenal.item;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.valheimarsenal.combat.CombatState;

import com.valheimarsenal.weapon.WeaponSpec;

/** Valheim shields. Blocking uses the BLOCKS_ATTACKS component; parrying is handled in Parry. */
public class ValheimShieldItem extends ShieldItem implements ValheimItem {
	private final WeaponSpec spec;

	public ValheimShieldItem(WeaponSpec spec, Properties properties) {
		super(properties);
		this.spec = spec;
	}

	@Override
	public WeaponSpec spec() {
		return spec;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		InteractionResult result = super.use(level, player, hand);
		if (!level.isClientSide()) {
			// Remember when the shield went up: a parry is a block that lands right away.
			CombatState.markBlockStarted(player, level.getGameTime());
		}
		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		Tooltips.append(spec, out);
	}
}
