package com.valheimarsenal.combat;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.valheimarsenal.entity.ThrownSpearEntity;
import com.valheimarsenal.weapon.Family.Secondary;
import com.valheimarsenal.weapon.WeaponSpec;

/**
 * Valheim's secondary (middle mouse) attacks, mapped to right click.
 * Each one costs stamina (food exhaustion) and has a short cooldown.
 */
public final class SecondaryAttacks {
	/** Base melee reach in Minecraft; weapon reach is added on top. */
	private static final double BASE_REACH = 3.0;
	/** Below this food level you are "out of stamina" and cannot do special attacks. */
	public static final int MIN_FOOD_FOR_STAMINA = 6;

	private SecondaryAttacks() {
	}

	public static boolean perform(Level level, Player player, InteractionHand hand, ItemStack stack, WeaponSpec spec) {
		Secondary secondary = spec.family().secondary();
		if (secondary == Secondary.NONE || player.getCooldowns().isOnCooldown(stack)) {
			return false;
		}
		boolean creative = player.getAbilities().instabuild;
		if (!creative && player.getFoodData().getFoodLevel() <= MIN_FOOD_FOR_STAMINA) {
			if (!level.isClientSide()) {
				Fx.message(player, "message.valheim_arsenal.no_stamina");
			}
			return false;
		}

		player.getCooldowns().addCooldown(stack, secondary.cooldown);

		if (level.isClientSide()) {
			// Movement is client-authoritative for players, so dashes happen on the client.
			dash(player, secondary);
			return true;
		}

		ServerLevel server = (ServerLevel) level;
		float damage = spec.attackDamage() * secondary.damageMultiplier;
		double reach = BASE_REACH + spec.reach();

		switch (secondary) {
			case UPPERCUT -> singleTarget(server, player, spec, damage, reach, 40.0, 1.4, 30);
			case HEAVY_CHOP -> singleTarget(server, player, spec, damage, reach, 35.0, 0.6, 12);
			case KICK -> singleTarget(server, player, spec, damage, reach, 45.0, 1.6, 25);
			case LUNGE -> singleTarget(server, player, spec, damage, reach + 2.0, 25.0, 0.4, 0);
			case LEAP -> singleTarget(server, player, spec, damage, reach + 2.5, 30.0, 0.2, 0);
			case HAFT_BASH -> haftBash(server, player, spec, damage, reach);
			case GROUND_SLAM -> groundSlam(server, player, spec, damage);
			case WHIRLWIND, SPIN -> spin(server, player, spec, damage, reach + 0.5);
			case THROW -> throwSpear(server, player, hand, stack, spec);
			default -> {
			}
		}

		if (!creative) {
			player.causeFoodExhaustion(secondary.staminaCost * 1.5F);
		}
		if (secondary != Secondary.THROW) {
			stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		}
		return true;
	}

	private static void dash(Player player, Secondary secondary) {
		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize();
		switch (secondary) {
			case LUNGE -> player.setDeltaMovement(player.getDeltaMovement().add(flat.scale(0.75)).add(0.0, 0.12, 0.0));
			case LEAP -> player.setDeltaMovement(player.getDeltaMovement().add(flat.scale(1.05)).add(0.0, 0.32, 0.0));
			default -> {
			}
		}
	}

	private static DamageSource sourceFor(Player player) {
		return player.damageSources().playerAttack(player);
	}

	private static boolean hit(ServerLevel level, Player player, WeaponSpec spec, LivingEntity target, float damage) {
		boolean hurt = CombatState.extraHit(() -> target.hurtServer(level, sourceFor(player), damage));
		if (hurt) {
			OnHitEffects.applyElements(spec, target, player, level);
		}
		return hurt;
	}

	private static void singleTarget(ServerLevel level, Player player, WeaponSpec spec, float damage, double reach,
			double cone, double knockback, int staggerTicks) {
		@Nullable LivingEntity target = Targeting.firstInFront(level, player, reach, cone);
		if (target == null) {
			Fx.sound(level, player, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F, 0.8F);
			return;
		}
		if (hit(level, player, spec, target, damage)) {
			if (knockback > 0.0) {
				OnHitEffects.knockbackFrom(target, player.position(), knockback);
			}
			if (staggerTicks > 0) {
				OnHitEffects.stagger(level, target, staggerTicks);
			}
			Fx.burstAt(level, ParticleTypes.CRIT, target, 10, 0.25);
			Fx.sound(level, target, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.9F);
		}
	}

	private static void haftBash(ServerLevel level, Player player, WeaponSpec spec, float damage, double reach) {
		List<LivingEntity> targets = Targeting.inFront(level, player, reach, 50.0);
		int hits = 0;
		for (LivingEntity target : targets) {
			if (hits++ >= 3) {
				break;
			}
			if (hit(level, player, spec, target, damage)) {
				OnHitEffects.knockbackFrom(target, player.position(), 1.6);
				OnHitEffects.stagger(level, target, 30);
			}
		}
		Fx.sound(level, player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.9F);
	}

	private static void groundSlam(ServerLevel level, Player player, WeaponSpec spec, float damage) {
		Vec3 look = player.getLookAngle();
		Vec3 center = player.position().add(new Vec3(look.x, 0.0, look.z).normalize().scale(1.5));
		for (LivingEntity target : Targeting.around(level, player, center.add(0.0, 0.5, 0.0), 3.5, null)) {
			if (hit(level, player, spec, target, damage)) {
				OnHitEffects.knockbackFrom(target, center, 0.9);
				OnHitEffects.stagger(level, target, 40);
			}
		}
		Fx.burst(level, ParticleTypes.EXPLOSION, center.add(0.0, 0.2, 0.0), 2, 0.4, 0.0);
		Fx.ring(level, ParticleTypes.CLOUD, center.add(0.0, 0.1, 0.0), 2.5, 20);
		Fx.ring(level, ParticleTypes.CRIT, center.add(0.0, 0.1, 0.0), 3.3, 24);
		Fx.sound(level, center, SoundEvents.ANVIL_LAND, 0.6F, 0.55F);
		Fx.sound(level, center, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.8F, 0.7F);
	}

	private static void spin(ServerLevel level, Player player, WeaponSpec spec, float damage, double radius) {
		Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
		for (LivingEntity target : Targeting.around(level, player, center, radius, null)) {
			if (hit(level, player, spec, target, damage)) {
				OnHitEffects.knockbackFrom(target, player.position(), 0.9);
			}
		}
		Fx.ring(level, ParticleTypes.SWEEP_ATTACK, center, radius * 0.7, 10);
		Fx.sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.75F);
	}

	private static void throwSpear(ServerLevel level, Player player, InteractionHand hand, ItemStack stack, WeaponSpec spec) {
		boolean creative = player.getAbilities().instabuild;
		ThrownSpearEntity spear = new ThrownSpearEntity(level, player, stack.copyWithCount(1));
		spear.setDropOnLanding(!creative);
		spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.2F, 0.5F);
		level.addFreshEntity(spear);
		Fx.sound(level, player, SoundEvents.WITCH_THROW, 1.0F, 0.6F);
		if (!creative) {
			stack.shrink(1);
		}
	}
}
