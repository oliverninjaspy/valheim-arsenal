package com.valheimarsenal.combat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Small helpers for sounds, particles and action-bar messages. */
public final class Fx {
	private Fx() {
	}

	public static void sound(Level level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
	}

	public static void sound(Level level, Entity at, SoundEvent sound, float volume, float pitch) {
		sound(level, at.position(), sound, volume, pitch);
	}

	public static void burst(ServerLevel level, ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
		level.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
	}

	public static void burstAt(ServerLevel level, ParticleOptions particle, Entity entity, int count, double speed) {
		Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.6, 0.0);
		level.sendParticles(particle, center.x, center.y, center.z, count,
				entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, speed);
	}

	/** A flat ring of particles, used for sweeps, slams and spells. */
	public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
		for (int i = 0; i < points; i++) {
			double angle = (Math.PI * 2.0 * i) / points;
			level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius,
					1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	/** A straight line of particles, used for chain lightning arcs. */
	public static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int points) {
		for (int i = 0; i <= points; i++) {
			Vec3 p = from.lerp(to, i / (double) points);
			level.sendParticles(particle, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}

	public static void message(Entity entity, String key, Object... args) {
		if (entity instanceof Player player) {
			player.sendOverlayMessage(Component.translatable(key, args));
		}
	}
}
