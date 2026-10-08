package com.valheimarsenal.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Finding who a swing, slam or spell hits. */
public final class Targeting {
	private Targeting() {
	}

	/**
	 * Whether an area attack from {@code attacker} should hurt {@code target}.
	 * Area attacks skip the attacker, their pets and allies, and other players.
	 */
	public static boolean canAreaHit(@Nullable LivingEntity attacker, LivingEntity target) {
		if (!target.isAlive() || target.isSpectator() || target == attacker) {
			return false;
		}
		if (attacker == null) {
			return !(target instanceof Player);
		}
		if (target.isAlliedTo(attacker) || attacker.isAlliedTo(target)) {
			return false;
		}
		return !(target instanceof Player);
	}

	/** Everything within {@code radius} of {@code center}, nearest first. */
	public static List<LivingEntity> around(ServerLevel level, @Nullable LivingEntity attacker, Vec3 center, double radius,
			@Nullable Entity exclude) {
		AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
				center.x + radius, center.y + radius, center.z + radius);
		double radiusSq = radius * radius;
		List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != exclude && canAreaHit(attacker, e) && e.getBoundingBox().getCenter().distanceToSqr(center) <= radiusSq));
		found.sort(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(center)));
		return found;
	}

	/** Everything in a cone in front of {@code attacker}, nearest first. */
	public static List<LivingEntity> inFront(ServerLevel level, LivingEntity attacker, double range, double coneDegrees) {
		Vec3 eye = attacker.getEyePosition();
		Vec3 look = attacker.getLookAngle();
		double minDot = Math.cos(Math.toRadians(coneDegrees));
		AABB box = attacker.getBoundingBox().inflate(range + 1.0);
		List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, box, e -> {
			if (!canAreaHit(attacker, e) && !(e instanceof Player && e != attacker && e.isAlive())) {
				return false;
			}
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
			double distance = to.length() - e.getBbWidth() * 0.5;
			if (distance > range) {
				return false;
			}
			return to.lengthSqr() < 0.25 || to.normalize().dot(look) >= minDot;
		}));
		found.sort(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(eye)));
		return found;
	}

	/** The single nearest enemy in front of the attacker, or {@code null}. */
	public static @Nullable LivingEntity firstInFront(ServerLevel level, LivingEntity attacker, double range, double coneDegrees) {
		List<LivingEntity> found = inFront(level, attacker, range, coneDegrees);
		return found.isEmpty() ? null : found.get(0);
	}
}
