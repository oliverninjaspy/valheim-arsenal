package com.valheimarsenal.item;

import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.valheimarsenal.weapon.Family;
import com.valheimarsenal.weapon.WeaponSpec;

/** Valheim-style tooltip lines shared by every item class. */
public final class Tooltips {
	private static final String PREFIX = "tooltip.valheim_arsenal.";

	private Tooltips() {
	}

	public static void append(WeaponSpec spec, Consumer<Component> out) {
		out.accept(Component.translatable(spec.tier().translationKey()).withStyle(ChatFormatting.DARK_GRAY));
		if (!spec.valheimStats().isEmpty()) {
			out.accept(Component.literal(spec.valheimStats()).withStyle(ChatFormatting.GRAY));
		}

		if (spec.spiritBonus() > 0) {
			out.accept(Component.translatable(PREFIX + "spirit", format(spec.spiritBonus())).withStyle(ChatFormatting.WHITE));
		}
		if (spec.fireSeconds() > 0) {
			out.accept(Component.translatable(PREFIX + "fire", spec.fireSeconds()).withStyle(ChatFormatting.GOLD));
		}
		if (spec.frostTicks() > 0) {
			out.accept(Component.translatable(PREFIX + "frost").withStyle(ChatFormatting.AQUA));
		}
		if (spec.poisonTicks() > 0) {
			out.accept(Component.translatable(PREFIX + "poison", spec.poisonTicks() / 20).withStyle(ChatFormatting.DARK_GREEN));
		}
		if (spec.lightningDamage() > 0) {
			out.accept(Component.translatable(PREFIX + "lightning", format(spec.lightningDamage())).withStyle(ChatFormatting.YELLOW));
		}

		Family family = spec.family();
		switch (spec.physical()) {
			case BLUNT, BLUNT_PIERCE -> out.accept(Component.translatable(PREFIX + "blunt").withStyle(ChatFormatting.DARK_AQUA));
			case PIERCE -> out.accept(Component.translatable(PREFIX + "pierce").withStyle(ChatFormatting.DARK_AQUA));
			default -> {
			}
		}

		if (family.primary() == Family.Primary.SHOCKWAVE) {
			out.accept(Component.translatable(PREFIX + "primary.shockwave").withStyle(ChatFormatting.BLUE));
		} else if (family.primary() == Family.Primary.CLEAVE) {
			out.accept(Component.translatable(PREFIX + "primary.cleave").withStyle(ChatFormatting.BLUE));
		}

		if (family.secondary() != Family.Secondary.NONE) {
			out.accept(Component.translatable(PREFIX + "secondary." + family.secondary().name().toLowerCase(Locale.ROOT))
					.withStyle(ChatFormatting.GOLD));
		}

		switch (family) {
			case BUTCHER -> out.accept(Component.translatable(PREFIX + "butcher").withStyle(ChatFormatting.RED));
			case BOW, CROSSBOW -> out.accept(Component.translatable(PREFIX + "ranged", format(spec.rangedMultiplier()))
					.withStyle(ChatFormatting.BLUE));
			case STAFF -> out.accept(Component.translatable(PREFIX + "staff." + spec.staff().name().toLowerCase(Locale.ROOT))
					.withStyle(ChatFormatting.LIGHT_PURPLE));
			case SHIELD -> {
				out.accept(Component.translatable(PREFIX + "block", Math.round(spec.blockRatio() * 100)).withStyle(ChatFormatting.BLUE));
				out.accept(Component.translatable(PREFIX + "shield." + spec.shield().name().toLowerCase(Locale.ROOT))
						.withStyle(ChatFormatting.GOLD));
			}
			default -> {
			}
		}

		if (family != Family.SHIELD && family != Family.STAFF && family != Family.BUTCHER) {
			out.accept(Component.translatable(PREFIX + "backstab", format(spec.backstab())).withStyle(ChatFormatting.DARK_PURPLE));
		}
		if (spec.twoHanded()) {
			out.accept(Component.translatable(PREFIX + "two_handed").withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	private static String format(float value) {
		if (value == Math.round(value)) {
			return Integer.toString(Math.round(value));
		}
		return String.format(Locale.ROOT, "%.1f", value);
	}
}
