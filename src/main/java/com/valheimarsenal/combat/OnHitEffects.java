package com.valheimarsenal.combat;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.valheimarsenal.weapon.WeaponSpec;

/** Elemental effects, staggers and area spill-over. */
public final class OnHitEffects {
	private OnHitEffects() {
	}

	/** Fire, frost, poison and lightning, applied after a hit lands. */
	public static void applyElements(WeaponSpec spec, LivingEntity target, @Nullable LivingEntity attacker, ServerLevel level) {
		if (!target.isAlive()) {
			return;
		}
		if (spec.fireSeconds() > 0) {
			target.igniteForSeconds(spec.fireSeconds());
			Fx.burstAt(level, ParticleTypes.FLAME, target, 6, 0.02);
		}
		if (spec.frostTicks() > 0) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, spec.frostTicks(), spec.frostLevel()));
			int chill = target.getTicksRequiredToFreeze() + spec.frostTicks() / 2;
			target.setTicksFrozen(Math.max(target.getTicksFrozen(), chill));
			Fx.burstAt(level, ParticleTypes.SNOWFLAKE, target, 8, 0.03);
		}
		if (spec.poisonTicks() > 0) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, spec.poisonTicks(), spec.poisonLevel()));
			Fx.burstAt(level, ParticleTypes.ITEM_SLIME, target, 6, 0.05);
		}
		if (spec.lightningDamage() > 0.0F) {
			chainLightning(level, target, attacker, spec.lightningDamage(), 3);
		}
	}

	/** Lightning arcs from the struck enemy to up to {@code maxArcs} others nearby. */
	public static void chainLightning(ServerLevel level, LivingEntity origin, @Nullable LivingEntity attacker, float damage, int maxArcs) {
		Vec3 from = origin.getBoundingBox().getCenter();
		Fx.burstAt(level, ParticleTypes.ELECTRIC_SPARK, origin, 12, 0.15);
		List<LivingEntity> nearby = Targeting.around(level, attacker, from, 5.0, origin);
		DamageSource source = attacker != null
				? level.damageSources().indirectMagic(attacker, attacker)
				: level.damageSources().magic();
		int arcs = 0;
		for (LivingEntity next : nearby) {
			if (arcs++ >= maxArcs) {
				break;
			}
			Fx.line(level, ParticleTypes.ELECTRIC_SPARK, from, next.getBoundingBox().getCenter(), 10);
			CombatState.withRulesSuppressed(() -> next.hurtServer(level, source, damage));
		}
		Fx.sound(level, origin, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.6F);
	}

	/**
	 * Valheim's stagger: the enemy reels, can barely move and hits weakly for a moment.
	 */
	public static void stagger(ServerLevel level, LivingEntity target, int ticks) {
		if (!target.isAlive()) {
			return;
		}
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 4));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
		if (target instanceof Mob mob) {
			mob.getNavigation().stop();
		}
		Fx.burst(level, ParticleTypes.CRIT, target.position().add(0.0, target.getBbHeight() + 0.2, 0.0), 8, 0.25, 0.1);
		if (target instanceof Player player) {
			Fx.message(player, "message.valheim_arsenal.staggered");
		}
	}

	/** Shoves the target directly away from {@code from}, respecting knockback resistance. */
	public static void knockbackFrom(LivingEntity target, Vec3 from, double strength) {
		double scaled = strength * Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		Vec3 away = new Vec3(target.getX() - from.x, 0.0, target.getZ() - from.z);
		if (scaled <= 0.0 || away.lengthSqr() < 1.0E-4) {
			return;
		}
		away = away.normalize().scale(scaled);
		target.push(away.x, target.onGround() ? Math.min(0.4, scaled * 0.4) : 0.0, away.z);
	}
}
