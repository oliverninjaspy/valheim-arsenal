package com.valheimarsenal.combat;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;

import com.valheimarsenal.item.ValheimItem;
import com.valheimarsenal.weapon.Family;
import com.valheimarsenal.weapon.WeaponSpec;

/**
 * Valheim's damage rules, applied to every hit dealt with a Valheim weapon:
 * damage-type weaknesses, spirit damage, backstabs, combo finishers and parry ripostes.
 * Called from {@code LivingEntityDamageMixin} at the start of LivingEntity#hurtServer.
 */
public final class DamageRules {
	private DamageRules() {
	}

	public static float modifyIncoming(LivingEntity target, ServerLevel level, DamageSource source, float amount) {
		if (amount <= 0.0F || CombatState.rulesSuppressed()) {
			return amount;
		}
		WeaponSpec spec = ValheimItem.specOf(source.getWeaponItem());
		if (spec == null) {
			return amount;
		}
		LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
		boolean primaryMelee = attacker != null && source.getDirectEntity() == attacker && !CombatState.isExtraHit();
		return apply(spec, target, attacker, amount, level, primaryMelee);
	}

	public static float apply(WeaponSpec spec, LivingEntity target, @Nullable LivingEntity attacker, float amount,
			ServerLevel level, boolean primaryMelee) {
		// Butcher knife: one-hit kills animals, does nothing to anything else.
		if (spec.family() == Family.BUTCHER) {
			return target instanceof Animal ? Math.max(amount, target.getHealth() + 1.0F) : 0.0F;
		}

		float result = amount;

		// Skeletons crumble under blunt weapons and shrug off piercing ones.
		if (target.getType().is(EntityTypeTags.SKELETONS)) {
			result *= spec.physical().skeletonMultiplier();
		}

		// Spirit damage only hurts the undead, but hurts them a lot.
		if (spec.spiritBonus() > 0.0F && target.getType().is(EntityTypeTags.UNDEAD)) {
			result += spec.spiritBonus();
			Fx.burstAt(level, ParticleTypes.SOUL, target, 4, 0.02);
		}

		long now = level.getGameTime();

		// Backstab: enemies that haven't noticed you take x3 (knives x6). Then they're immune for 5 minutes.
		if (attacker != null && isUnaware(target, attacker, now)) {
			result *= spec.backstab();
			CombatState.markBackstabbed(target, now);
			Fx.burstAt(level, ParticleTypes.ENCHANTED_HIT, target, 14, 0.3);
			Fx.sound(level, target, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.7F);
			Fx.message(attacker, "message.valheim_arsenal.backstab", formatMultiplier(spec.backstab()));
		}

		if (primaryMelee && attacker != null) {
			if (CombatState.consumeRiposte(attacker, now)) {
				result *= CombatState.RIPOSTE_MULTIPLIER;
				Fx.burstAt(level, ParticleTypes.CRIT, target, 10, 0.3);
				Fx.message(attacker, "message.valheim_arsenal.riposte");
			}
			if (CombatState.isComboFinisher(attacker, now)) {
				result *= CombatState.COMBO_FINISHER_MULTIPLIER;
				Fx.sound(level, target, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
			}
		}
		return result;
	}

	/** Valheim: unalerted enemies can be backstabbed regardless of which way they face. */
	public static boolean isUnaware(LivingEntity target, LivingEntity attacker, long now) {
		if (!(target instanceof Mob mob) || !mob.isAlive()) {
			return false;
		}
		if (CombatState.isBackstabImmune(mob, now)) {
			return false;
		}
		return mob.getTarget() != attacker && mob.getLastHurtByMob() != attacker;
	}

	private static String formatMultiplier(float value) {
		return value == Math.round(value) ? Integer.toString(Math.round(value)) : Float.toString(value);
	}
}
