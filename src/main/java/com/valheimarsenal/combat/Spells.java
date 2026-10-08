package com.valheimarsenal.combat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.valheimarsenal.entity.MagicBoltEntity;
import com.valheimarsenal.weapon.WeaponSpec;

/**
 * Valheim's staves. Elemental magic costs "eitr" (food exhaustion here);
 * blood magic (Staff of Protection) costs a share of your current health instead.
 */
public final class Spells {
	private static final double CAST_RANGE = 32.0;

	private Spells() {
	}

	public static boolean cast(Level level, Player player, InteractionHand hand, ItemStack stack, WeaponSpec spec) {
		if (player.getCooldowns().isOnCooldown(stack)) {
			return false;
		}
		boolean creative = player.getAbilities().instabuild;
		boolean bloodMagic = spec.staff() == WeaponSpec.StaffKind.PROTECTION;
		if (!creative) {
			if (bloodMagic && player.getHealth() <= 4.0F) {
				if (!level.isClientSide()) {
					Fx.message(player, "message.valheim_arsenal.no_health");
				}
				return false;
			}
			if (!bloodMagic && player.getFoodData().getFoodLevel() <= SecondaryAttacks.MIN_FOOD_FOR_STAMINA) {
				if (!level.isClientSide()) {
					Fx.message(player, "message.valheim_arsenal.no_eitr");
				}
				return false;
			}
		}

		int cooldown = switch (spec.staff()) {
			case EMBERS -> 30;
			case FROST -> 6;
			case PROTECTION -> 200;
			case LIGHTNING -> 25;
			case WILD -> 40;
			case FRACTURE -> 30;
			default -> 20;
		};
		player.getCooldowns().addCooldown(stack, cooldown);
		if (level.isClientSide()) {
			return true;
		}

		ServerLevel server = (ServerLevel) level;
		float power = spec.attackDamage();
		float eitr = switch (spec.staff()) {
			case EMBERS -> embers(server, player, power);
			case FROST -> frost(server, player, power);
			case PROTECTION -> protection(server, player);
			case LIGHTNING -> lightning(server, player, power);
			case WILD -> wild(server, player, power);
			case FRACTURE -> fracture(server, player, power);
			default -> 0.0F;
		};
		if (!creative && eitr > 0.0F) {
			player.causeFoodExhaustion(eitr);
		}
		stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		return true;
	}

	// ---- Staff of Embers: a slow fireball that bursts into flame ---------------------------

	private static float embers(ServerLevel level, Player player, float power) {
		MagicBoltEntity bolt = new MagicBoltEntity(level, player, new ItemStack(Items.FIRE_CHARGE), power);
		bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.4F, 0.5F);
		level.addFreshEntity(bolt);
		Fx.sound(level, player, SoundEvents.BLAZE_SHOOT, 1.0F, 0.8F);
		return 4.0F;
	}

	// ---- Staff of Frost: a rapid spray of ice shards ---------------------------------------

	private static float frost(ServerLevel level, Player player, float power) {
		for (float spread : new float[] {-5.0F, 0.0F, 5.0F}) {
			MagicBoltEntity shard = new MagicBoltEntity(level, player, new ItemStack(Items.SNOWBALL), power);
			shard.shootFromRotation(player, player.getXRot(), player.getYRot() + spread, 0.0F, 2.0F, 2.0F);
			level.addFreshEntity(shard);
		}
		Fx.sound(level, player, SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 1.2F);
		return 0.6F;
	}

	// ---- Staff of Protection: blood magic shield for you and your allies -------------------

	private static float protection(ServerLevel level, Player player) {
		player.setHealth(Math.max(1.0F, player.getHealth() * 0.6F));
		Vec3 center = player.position();
		List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10.0),
				e -> e.isAlive() && (e == player || e instanceof Player || e.isAlliedTo(player)));
		for (LivingEntity ally : allies) {
			ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 90 * 20, 4));
			Fx.burstAt(level, ParticleTypes.HEART, ally, 4, 0.1);
		}
		Fx.ring(level, ParticleTypes.TOTEM_OF_UNDYING, center.add(0.0, 1.0, 0.0), 1.2, 24);
		Fx.sound(level, player, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.2F);
		return 0.0F;
	}

	// ---- Dundr: call down lightning where you point ----------------------------------------

	private static float lightning(ServerLevel level, Player player, float power) {
		Vec3 point = aimPoint(level, player);
		LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
		bolt.setPos(point);
		bolt.setVisualOnly(true);
		level.addFreshEntity(bolt);

		DamageSource source = level.damageSources().indirectMagic(player, player);
		LivingEntity first = null;
		for (LivingEntity target : Targeting.around(level, player, point.add(0.0, 1.0, 0.0), 2.5, null)) {
			if (first == null) {
				first = target;
			}
			CombatState.withRulesSuppressed(() -> target.hurtServer(level, source, power));
			target.igniteForSeconds(2.0F);
		}
		if (first != null) {
			OnHitEffects.chainLightning(level, first, player, power * 0.5F, 2);
		}
		return 2.0F;
	}

	// ---- Staff of the Wild: roots burst from the ground and hold enemies -------------------

	private static float wild(ServerLevel level, Player player, float power) {
		Vec3 point = aimPoint(level, player);
		DamageSource source = level.damageSources().indirectMagic(player, player);
		for (LivingEntity target : Targeting.around(level, player, point.add(0.0, 0.5, 0.0), 4.0, null)) {
			CombatState.withRulesSuppressed(() -> target.hurtServer(level, source, power));
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 6));
			Fx.burstAt(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), target, 20, 0.1);
		}
		Fx.ring(level, ParticleTypes.HAPPY_VILLAGER, point.add(0.0, 0.3, 0.0), 3.0, 24);
		Fx.ring(level, ParticleTypes.COMPOSTER, point.add(0.0, 0.3, 0.0), 2.0, 16);
		Fx.sound(level, point, SoundEvents.GRASS_BREAK, 1.0F, 0.6F);
		Fx.sound(level, point, SoundEvents.EVOKER_CAST_SPELL, 0.7F, 0.6F);
		return 2.5F;
	}

	// ---- Staff of Fracturing: a line of erupting stone ------------------------------------

	private static float fracture(ServerLevel level, Player player, float power) {
		Vec3 look = player.getLookAngle();
		Vec3 step = new Vec3(look.x, 0.0, look.z).normalize().scale(1.25);
		Vec3 at = player.position();
		DamageSource source = level.damageSources().indirectMagic(player, player);
		BlockParticleOption rock = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACKSTONE.defaultBlockState());
		Set<LivingEntity> struck = new HashSet<>();
		for (int i = 1; i <= 9; i++) {
			at = at.add(step);
			Fx.burst(level, rock, at.add(0.0, 0.3, 0.0), 14, 0.3, 0.15);
			Fx.burst(level, ParticleTypes.CRIT, at.add(0.0, 0.5, 0.0), 3, 0.2, 0.1);
			for (LivingEntity target : Targeting.around(level, player, at.add(0.0, 0.6, 0.0), 1.4, null)) {
				if (struck.add(target)) {
					CombatState.withRulesSuppressed(() -> target.hurtServer(level, source, power));
					target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.45, 0.0));
				}
			}
		}
		Fx.sound(level, player, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1.0F, 0.5F);
		Fx.sound(level, player, SoundEvents.ANVIL_LAND, 0.4F, 0.4F);
		return 2.0F;
	}

	/** Where the player is aiming: the first enemy in a narrow cone, else the block they look at. */
	private static Vec3 aimPoint(ServerLevel level, Player player) {
		@Nullable LivingEntity target = Targeting.firstInFront(level, player, CAST_RANGE, 4.0);
		if (target != null) {
			return target.position();
		}
		HitResult hit = player.pick(CAST_RANGE, 1.0F, false);
		return hit.getLocation();
	}
}
