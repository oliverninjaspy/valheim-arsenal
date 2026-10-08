package com.valheimarsenal.item;

import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.valheimarsenal.combat.SecondaryAttacks;
import com.valheimarsenal.weapon.Family;
import com.valheimarsenal.weapon.WeaponSpec;

/**
 * Clubs, sledges, swords, knives, axes, battleaxes, spears, atgeirs and claws.
 * Left click is the normal attack (extra effects are applied in CombatEvents);
 * right click performs the Valheim secondary attack.
 */
public class ValheimMeleeItem extends Item implements ValheimItem {
	private final WeaponSpec spec;

	public ValheimMeleeItem(WeaponSpec spec, Properties properties) {
		super(properties);
		this.spec = spec;
	}

	@Override
	public WeaponSpec spec() {
		return spec;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (spec.family().secondary() == Family.Secondary.NONE) {
			return super.use(level, player, hand);
		}
		// Valheim blocks on right mouse and does the secondary attack on middle mouse.
		// Here: with a shield in the off hand, right click blocks and sneak + right click
		// does the secondary attack. Without a shield, right click is the secondary attack.
		if (hand == InteractionHand.MAIN_HAND && !player.isShiftKeyDown()
				&& player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		return SecondaryAttacks.perform(level, player, hand, stack, spec) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		Tooltips.append(spec, out);
	}
}
