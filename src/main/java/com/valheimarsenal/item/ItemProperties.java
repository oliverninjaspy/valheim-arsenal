package com.valheimarsenal.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;

import com.valheimarsenal.ValheimArsenal;
import com.valheimarsenal.weapon.Family;
import com.valheimarsenal.weapon.WeaponSpec;

/**
 * Builds the {@link Item.Properties} (data components) for each kind of Valheim item.
 */
public final class ItemProperties {
	/** Vanilla's ids for the main-hand damage/speed modifiers, so tooltips show totals like vanilla weapons. */
	private static final Identifier BASE_ATTACK_DAMAGE = Identifier.fromNamespaceAndPath("minecraft", "base_attack_damage");
	private static final Identifier BASE_ATTACK_SPEED = Identifier.fromNamespaceAndPath("minecraft", "base_attack_speed");
	private static final Identifier REACH = ValheimArsenal.id("weapon_reach");
	private static final Identifier KNOCKBACK = ValheimArsenal.id("weapon_knockback");
	private static final Identifier TWO_HANDED = ValheimArsenal.id("two_handed_slow");
	private static final Identifier TOWER_SHIELD = ValheimArsenal.id("tower_shield_slow");

	private ItemProperties() {
	}

	public static Item.Properties melee(WeaponSpec spec) {
		Item.Properties props = new Item.Properties();
		Family family = spec.family();

		if (family == Family.SWORD || family == Family.GREATSWORD) {
			// Sword component: cobweb mining, one durability per hit, sweeping via the #minecraft:swords tag.
			props.sword(spec.tier().toolMaterial(), 0.0F, 0.0F);
		} else if (family == Family.AXE || family == Family.BATTLEAXE) {
			// Valheim axes are the only weapons that chop trees; these mine wood and strip logs like vanilla axes.
			props.axe(spec.tier().toolMaterial(), 0.0F, 0.0F);
			props.delayedComponent(DataComponents.BLOCK_TRANSFORMER, context -> context.getOrThrow(BlockTransformers.AXE));
		} else {
			// Blunt weapons knock shields aside for a moment, like Valheim's stagger-through-block.
			float disableShieldSeconds = (family == Family.CLUB || family == Family.SLEDGE) ? 2.0F : 0.0F;
			props.component(DataComponents.WEAPON, new Weapon(1, disableShieldSeconds));
		}

		applyCommon(props, spec);
		props.component(DataComponents.ATTRIBUTE_MODIFIERS, meleeAttributes(spec));
		return props;
	}

	public static Item.Properties ranged(WeaponSpec spec) {
		Item.Properties props = new Item.Properties();
		applyCommon(props, spec);
		if (spec.twoHanded()) {
			props.component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
					.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(TWO_HANDED, -0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
					.build());
		}
		return props;
	}

	public static Item.Properties staff(WeaponSpec spec) {
		Item.Properties props = new Item.Properties();
		applyCommon(props, spec);
		props.component(DataComponents.ATTRIBUTE_MODIFIERS, meleeAttributes(spec));
		props.component(DataComponents.WEAPON, new Weapon(1, 0.0F));
		return props;
	}

	public static Item.Properties shield(WeaponSpec spec) {
		Item.Properties props = new Item.Properties();
		applyCommon(props, spec);

		WeaponSpec.ShieldKind kind = spec.shield();
		// Bucklers come up instantly, round shields fast, tower shields slowly (vanilla shield: 0.25 s).
		float blockDelay = switch (kind) {
			case BUCKLER -> 0.0F;
			case TOWER -> 0.3F;
			default -> 0.1F;
		};
		float angle = kind == WeaponSpec.ShieldKind.TOWER ? 120.0F : (kind == WeaponSpec.ShieldKind.BUCKLER ? 75.0F : 90.0F);
		float ratio = spec.blockRatio();

		props.equippableUnswappable(EquipmentSlot.OFFHAND);
		props.delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
				blockDelay,
				kind == WeaponSpec.ShieldKind.TOWER ? 0.5F : 1.0F,
				List.of(new BlocksAttacks.DamageReduction(angle, Optional.empty(), 0.0F, ratio)),
				new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
				Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
				Optional.of(SoundEvents.SHIELD_BLOCK),
				Optional.of(SoundEvents.SHIELD_BREAK)));
		props.component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK);

		if (kind == WeaponSpec.ShieldKind.TOWER) {
			props.component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
					.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(TOWER_SHIELD, -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.HAND)
					.build());
		}
		return props;
	}

	private static void applyCommon(Item.Properties props, WeaponSpec spec) {
		props.durability(spec.durability());
		props.repairable(spec.tier().repairItems());
		props.enchantable(spec.tier().enchantability());
		props.rarity(spec.rarity());
		if (spec.fireproof()) {
			props.fireResistant();
		}
	}

	private static ItemAttributeModifiers meleeAttributes(WeaponSpec spec) {
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(BASE_ATTACK_DAMAGE, spec.attackDamage() - 1.0F, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED,
						new AttributeModifier(BASE_ATTACK_SPEED, spec.attackSpeed() - 4.0F, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND);

		if (spec.reach() != 0.0F) {
			builder.add(Attributes.ENTITY_INTERACTION_RANGE,
					new AttributeModifier(REACH, spec.reach(), AttributeModifier.Operation.ADD_VALUE),
					EquipmentSlotGroup.MAINHAND);
		}
		if (spec.knockback() > 0.0F) {
			builder.add(Attributes.ATTACK_KNOCKBACK,
					new AttributeModifier(KNOCKBACK, spec.knockback(), AttributeModifier.Operation.ADD_VALUE),
					EquipmentSlotGroup.MAINHAND);
		}
		if (spec.twoHanded()) {
			builder.add(Attributes.MOVEMENT_SPEED,
					new AttributeModifier(TWO_HANDED, -0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
					EquipmentSlotGroup.MAINHAND);
		}
		return builder.build();
	}
}
