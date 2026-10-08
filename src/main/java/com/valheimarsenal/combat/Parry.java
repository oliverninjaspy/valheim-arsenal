package com.valheimarsenal.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.valheimarsenal.item.ValheimShieldItem;
import com.valheimarsenal.weapon.WeaponSpec;

/**
 * Valheim's parry: raise your shield just as a hit lands and you take no damage,
 * the attacker staggers, and your next hit deals double damage.
 * Tower shields cannot parry. Bucklers have the most forgiving timing.
 */
public final class Parry {
	private Parry() {
	}

	/** Fabric ALLOW_DAMAGE callback. Returning false cancels the damage. */
	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof Player player) || !player.isUsingItem() || !(player.level() instanceof ServerLevel level)) {
			return true;
		}
		ItemStack shield = player.getUseItem();
		if (!(shield.getItem() instanceof ValheimShieldItem shieldItem)) {
			return true;
		}
		WeaponSpec spec = shieldItem.spec();
		int window = switch (spec.shield()) {
			case BUCKLER -> 10;
			case ROUND -> 6;
			default -> 0;
		};
		if (window == 0 || CombatState.ticksSinceBlockStarted(player, level.getGameTime()) > window) {
			return true;
		}
		if (source.is(DamageTypeTags.BYPASSES_SHIELD)) {
			return true;
		}
		Entity incoming = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
		if (incoming == null) {
			return true;
		}
		Vec3 toAttacker = incoming.position().subtract(player.position());
		Vec3 look = player.getLookAngle();
		if (toAttacker.x * look.x + toAttacker.z * look.z <= 0.0) {
			return true; // hit from behind
		}

		long now = level.getGameTime();
		CombatState.grantRiposte(player, now);

		if (source.getEntity() instanceof LivingEntity attacker && attacker.distanceTo(player) < 6.0F) {
			OnHitEffects.stagger(level, attacker, spec.shield() == WeaponSpec.ShieldKind.BUCKLER ? 50 : 35);
			OnHitEffects.knockbackFrom(attacker, player.position(), 0.6);
		}
		if (incoming instanceof Projectile) {
			incoming.discard(); // parried projectiles shatter
		}

		Vec3 sparks = player.getEyePosition().add(look.scale(0.8));
		Fx.burst(level, ParticleTypes.CRIT, sparks, 16, 0.2, 0.4);
		Fx.burst(level, ParticleTypes.ELECTRIC_SPARK, sparks, 6, 0.15, 0.2);
		Fx.sound(level, player, SoundEvents.ANVIL_LAND, 0.5F, 1.8F);
		Fx.message(player, "message.valheim_arsenal.parry");
		shield.hurtAndBreak(1, player, player.getUsedItemHand() == InteractionHand.MAIN_HAND
				? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		return false;
	}
}
