package com.valheimarsenal.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.valheimarsenal.combat.CombatState;
import com.valheimarsenal.combat.Fx;
import com.valheimarsenal.combat.Targeting;
import com.valheimarsenal.registry.ModEntities;

/**
 * Staff projectiles. The item it carries decides what it is:
 * a fire charge is a Staff of Embers fireball, a snowball is a Staff of Frost shard.
 */
public class MagicBoltEntity extends ThrowableItemProjectile {
	private static final int MAX_AGE = 80;
	private float damage = 6.0F;

	public MagicBoltEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
		super(type, level);
	}

	public MagicBoltEntity(Level level, LivingEntity owner, ItemStack visual, float damage) {
		super(ModEntities.MAGIC_BOLT, owner, level, visual);
		this.damage = damage;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIRE_CHARGE;
	}

	private boolean isEmber() {
		return getItem().is(Items.FIRE_CHARGE);
	}

	@Override
	protected double getDefaultGravity() {
		return isEmber() ? 0.0 : 0.005;
	}

	@Override
	public void tick() {
		super.tick();
		Level level = level();
		if (level.isClientSide()) {
			if (isEmber()) {
				level.addParticle(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 0.0, 0.0, 0.0);
				level.addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0.0, 0.01, 0.0);
			} else {
				level.addParticle(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.1, getZ(), 0.0, 0.0, 0.0);
			}
		} else if (tickCount > MAX_AGE) {
			discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (isEmber() || !(level() instanceof ServerLevel level) || !(hitResult.getEntity() instanceof LivingEntity target)) {
			return;
		}
		// Frost shard
		Entity owner = getOwner();
		DamageSource source = damageSources().indirectMagic(this, owner);
		boolean hurt = CombatState.withRulesSuppressed(() -> target.hurtServer(level, source, damage));
		if (hurt) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
			target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 40));
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 at = hitResult.getLocation();
		if (isEmber()) {
			explode(level, at);
		} else {
			Fx.burst(level, ParticleTypes.SNOWFLAKE, at, 10, 0.2, 0.05);
			Fx.sound(level, at, SoundEvents.GLASS_BREAK, 0.4F, 1.8F);
		}
		discard();
	}

	/** Staff of Embers: a fiery burst that hurts and ignites everything nearby, without breaking blocks. */
	private void explode(ServerLevel level, Vec3 at) {
		Entity owner = getOwner();
		LivingEntity attacker = owner instanceof LivingEntity living ? living : null;
		DamageSource source = damageSources().indirectMagic(this, owner);
		double radius = 3.0;
		for (LivingEntity target : Targeting.around(level, attacker, at, radius, null)) {
			double falloff = 1.0 - 0.5 * (target.getBoundingBox().getCenter().distanceTo(at) / radius);
			float dealt = (float) (damage * Math.max(0.5, falloff));
			CombatState.withRulesSuppressed(() -> target.hurtServer(level, source, dealt));
			target.igniteForSeconds(5.0F);
		}
		Fx.burst(level, ParticleTypes.EXPLOSION, at, 1, 0.0, 0.0);
		Fx.burst(level, ParticleTypes.FLAME, at, 40, 0.8, 0.08);
		Fx.burst(level, ParticleTypes.LAVA, at, 8, 0.5, 0.0);
		Fx.sound(level, at, SoundEvents.FIRECHARGE_USE, 1.2F, 0.7F);
		Fx.sound(level, at, SoundEvents.BLAZE_SHOOT, 0.8F, 0.5F);
	}
}
