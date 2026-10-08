package com.valheimarsenal.combat;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import com.valheimarsenal.item.ValheimItem;
import com.valheimarsenal.weapon.Family;
import com.valheimarsenal.weapon.WeaponSpec;

/** Hooks Valheim combat into Fabric's damage and entity events. */
public final class CombatEvents {
	/** Vanilla arrows start with 2.0 base damage; Valheim bows scale it. */
	private static final double VANILLA_ARROW_DAMAGE = 2.0;

	private CombatEvents() {
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(Parry::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(CombatEvents::afterDamage);
		ServerEntityEvents.ENTITY_LOAD.register(CombatEvents::onEntityLoad);
	}

	/**
	 * Scale arrows fired from Valheim bows and crossbows. Base damage is set (not multiplied),
	 * so arrows reloaded from disk are unaffected. Crossbow bolts also leave the string faster
	 * and fly flatter.
	 */
	private static void onEntityLoad(Entity entity, ServerLevel level) {
		if (!(entity instanceof AbstractArrow arrow)) {
			return;
		}
		WeaponSpec spec = ValheimItem.specOf(arrow.getWeaponItem());
		if (spec == null || (spec.family() != Family.BOW && spec.family() != Family.CROSSBOW)) {
			return;
		}
		arrow.setBaseDamage(VANILLA_ARROW_DAMAGE * spec.rangedMultiplier());
		if (spec.family() == Family.CROSSBOW && arrow.tickCount == 0) {
			arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.35));
		}
	}

	private static void afterDamage(LivingEntity target, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
		if (blocked || damageTaken <= 0.0F || CombatState.isExtraHit() || !(target.level() instanceof ServerLevel level)) {
			return;
		}
		WeaponSpec spec = ValheimItem.specOf(source.getWeaponItem());
		if (spec == null) {
			return;
		}
		@Nullable LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;

		OnHitEffects.applyElements(spec, target, attacker, level);

		boolean primaryMelee = attacker != null && source.getDirectEntity() == attacker;
		if (!primaryMelee) {
			return;
		}

		CombatState.registerHit(attacker, level.getGameTime());

		switch (spec.family().primary()) {
			case SHOCKWAVE -> shockwave(level, attacker, target, spec);
			case CLEAVE -> cleave(level, attacker, target, spec);
			default -> {
			}
		}
	}

	private static DamageSource meleeSource(LivingEntity attacker) {
		return attacker instanceof Player player
				? attacker.damageSources().playerAttack(player)
				: attacker.damageSources().mobAttack(attacker);
	}

	/** Sledges: the impact hits everything around the target for 60% damage and shoves it away. */
	private static void shockwave(ServerLevel level, LivingEntity attacker, LivingEntity target, WeaponSpec spec) {
		Vec3 center = target.position();
		float damage = spec.attackDamage() * 0.6F;
		DamageSource source = meleeSource(attacker);
		for (LivingEntity other : Targeting.around(level, attacker, center.add(0.0, 0.5, 0.0), 2.75, target)) {
			boolean hurt = CombatState.extraHit(() -> other.hurtServer(level, source, damage));
			if (hurt) {
				OnHitEffects.applyElements(spec, other, attacker, level);
				OnHitEffects.knockbackFrom(other, center, 0.7);
			}
		}
		Fx.ring(level, ParticleTypes.CLOUD, center.add(0.0, 0.1, 0.0), 1.8, 14);
		Fx.burst(level, ParticleTypes.CRIT, center.add(0.0, 0.2, 0.0), 10, 0.8, 0.2);
		Fx.sound(level, center, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.5F, 0.8F);
	}

	/** Battleaxes and greatswords: the swing carries into enemies beside the target for 70% damage. */
	private static void cleave(ServerLevel level, LivingEntity attacker, LivingEntity target, WeaponSpec spec) {
		float damage = spec.attackDamage() * 0.7F;
		DamageSource source = meleeSource(attacker);
		double reach = 3.0 + spec.reach();
		for (LivingEntity other : Targeting.inFront(level, attacker, reach, 65.0)) {
			if (other == target || other instanceof Player) {
				continue;
			}
			boolean hurt = CombatState.extraHit(() -> other.hurtServer(level, source, damage));
			if (hurt) {
				OnHitEffects.applyElements(spec, other, attacker, level);
				Fx.burstAt(level, ParticleTypes.SWEEP_ATTACK, other, 1, 0.0);
			}
		}
	}
}
