package com.valheimarsenal.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import com.valheimarsenal.combat.CombatState;
import com.valheimarsenal.combat.DamageRules;
import com.valheimarsenal.combat.Fx;
import com.valheimarsenal.combat.OnHitEffects;
import com.valheimarsenal.item.ValheimItem;
import com.valheimarsenal.registry.ModEntities;
import com.valheimarsenal.registry.ModItems;
import com.valheimarsenal.weapon.Family;
import com.valheimarsenal.weapon.WeaponSpec;

/**
 * A thrown Valheim spear. Like in Valheim, it lands where it hits and you have to
 * go and pick it up again.
 */
public class ThrownSpearEntity extends ThrowableItemProjectile {
	private boolean dropOnLanding = true;

	public ThrownSpearEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
		super(type, level);
	}

	public ThrownSpearEntity(Level level, LivingEntity owner, ItemStack spear) {
		super(ModEntities.THROWN_SPEAR, owner, level, spear);
	}

	public void setDropOnLanding(boolean dropOnLanding) {
		this.dropOnLanding = dropOnLanding;
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.get("flint_spear");
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!(level() instanceof ServerLevel level) || !(hitResult.getEntity() instanceof LivingEntity target)) {
			return;
		}
		WeaponSpec spec = ValheimItem.specOf(getItem());
		if (spec == null) {
			return;
		}
		Entity owner = getOwner();
		LivingEntity attacker = owner instanceof LivingEntity living ? living : null;
		float base = spec.attackDamage() * Family.Secondary.THROW.damageMultiplier;
		float damage = DamageRules.apply(spec, target, attacker, base, level, false);
		DamageSource source = damageSources().thrown(this, owner);
		boolean hurt = CombatState.withRulesSuppressed(() -> target.hurtServer(level, source, damage));
		if (hurt) {
			OnHitEffects.applyElements(spec, target, attacker, level);
			Fx.burstAt(level, ParticleTypes.CRIT, target, 8, 0.2);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (level() instanceof ServerLevel level) {
			Fx.sound(level, this, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.35F, 1.6F);
			dropSpear(level);
			discard();
		}
	}

	private void dropSpear(ServerLevel level) {
		if (!dropOnLanding) {
			return;
		}
		ItemStack stack = getItem().copy();
		if (stack.isEmpty()) {
			return;
		}
		if (stack.isDamageableItem()) {
			stack.setDamageValue(stack.getDamageValue() + 1);
			if (stack.getDamageValue() >= stack.getMaxDamage()) {
				Fx.burst(level, ParticleTypes.POOF, position(), 6, 0.1, 0.02);
				return; // the throw broke it
			}
		}
		ItemEntity drop = new ItemEntity(level, getX(), getY() + 0.1, getZ(), stack);
		drop.setDefaultPickUpDelay();
		level.addFreshEntity(drop);
	}
}
