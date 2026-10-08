package com.valheimarsenal.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import net.minecraft.world.entity.LivingEntity;

/**
 * Short-lived, server-side combat bookkeeping: combo chains, parry ripostes,
 * backstab immunity, and guards that stop our own extra hits from re-triggering
 * weapon effects.
 */
public final class CombatState {
	/** Valheim: a 3-hit chain, the third hit lands hardest. */
	public static final int COMBO_WINDOW_TICKS = 30;
	public static final float COMBO_FINISHER_MULTIPLIER = 1.5F;
	/** Valheim: backstabbed enemies cannot be backstabbed again for 5 minutes. */
	public static final long BACKSTAB_IMMUNITY_TICKS = 5L * 60L * 20L;
	/** Valheim: after a perfect parry, your next hit within a short window deals double damage. */
	public static final long RIPOSTE_WINDOW_TICKS = 40L;
	public static final float RIPOSTE_MULTIPLIER = 2.0F;

	private static final ThreadLocal<Integer> EXTRA_HIT_DEPTH = ThreadLocal.withInitial(() -> 0);
	private static final ThreadLocal<Integer> RULES_SUPPRESSED_DEPTH = ThreadLocal.withInitial(() -> 0);

	private static final Map<UUID, Combo> COMBOS = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> RIPOSTE_UNTIL = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> BACKSTAB_IMMUNE_UNTIL = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> BLOCK_STARTED = new ConcurrentHashMap<>();

	private CombatState() {
	}

	private record Combo(long lastHitTick, int count) {
	}

	// ---- Guards ---------------------------------------------------------------------------

	/** True while we are applying a secondary attack, AoE spill-over, or spell damage. */
	public static boolean isExtraHit() {
		return EXTRA_HIT_DEPTH.get() > 0;
	}

	public static <T> T extraHit(Supplier<T> action) {
		EXTRA_HIT_DEPTH.set(EXTRA_HIT_DEPTH.get() + 1);
		try {
			return action.get();
		} finally {
			EXTRA_HIT_DEPTH.set(EXTRA_HIT_DEPTH.get() - 1);
		}
	}

	/** True when the caller has already applied the weapon damage rules (thrown spears, spells, chain lightning). */
	public static boolean rulesSuppressed() {
		return RULES_SUPPRESSED_DEPTH.get() > 0;
	}

	public static <T> T withRulesSuppressed(Supplier<T> action) {
		RULES_SUPPRESSED_DEPTH.set(RULES_SUPPRESSED_DEPTH.get() + 1);
		try {
			return extraHit(action);
		} finally {
			RULES_SUPPRESSED_DEPTH.set(RULES_SUPPRESSED_DEPTH.get() - 1);
		}
	}

	// ---- Combos ---------------------------------------------------------------------------

	/** Whether the next primary hit from this attacker is the third hit of a chain. */
	public static boolean isComboFinisher(LivingEntity attacker, long now) {
		Combo combo = COMBOS.get(attacker.getUUID());
		return combo != null && combo.count() == 2 && now - combo.lastHitTick() <= COMBO_WINDOW_TICKS;
	}

	/** Records a landed primary hit and returns its position in the chain (1, 2 or 3). */
	public static int registerHit(LivingEntity attacker, long now) {
		Combo previous = COMBOS.get(attacker.getUUID());
		if (previous != null && previous.lastHitTick() == now) {
			return previous.count(); // sweep hits in the same swing don't advance the chain
		}
		int count = (previous == null || now - previous.lastHitTick() > COMBO_WINDOW_TICKS || previous.count() >= 3)
				? 1 : previous.count() + 1;
		COMBOS.put(attacker.getUUID(), new Combo(now, count));
		if (COMBOS.size() > 256) {
			COMBOS.values().removeIf(c -> now - c.lastHitTick() > COMBO_WINDOW_TICKS);
		}
		return count;
	}

	// ---- Parry riposte ----------------------------------------------------------------------

	public static void grantRiposte(LivingEntity defender, long now) {
		RIPOSTE_UNTIL.put(defender.getUUID(), now + RIPOSTE_WINDOW_TICKS);
	}

	public static boolean consumeRiposte(LivingEntity attacker, long now) {
		Long until = RIPOSTE_UNTIL.remove(attacker.getUUID());
		return until != null && now <= until;
	}

	// ---- Shield timing (for parries) ------------------------------------------------------

	public static void markBlockStarted(LivingEntity defender, long now) {
		BLOCK_STARTED.put(defender.getUUID(), now);
	}

	/** Ticks since the defender last raised a Valheim shield, or Long.MAX_VALUE if unknown. */
	public static long ticksSinceBlockStarted(LivingEntity defender, long now) {
		Long started = BLOCK_STARTED.get(defender.getUUID());
		return started == null ? Long.MAX_VALUE : now - started;
	}

	// ---- Backstab immunity ----------------------------------------------------------------

	public static boolean isBackstabImmune(LivingEntity target, long now) {
		Long until = BACKSTAB_IMMUNE_UNTIL.get(target.getUUID());
		return until != null && now <= until;
	}

	public static void markBackstabbed(LivingEntity target, long now) {
		BACKSTAB_IMMUNE_UNTIL.put(target.getUUID(), now + BACKSTAB_IMMUNITY_TICKS);
		if (BACKSTAB_IMMUNE_UNTIL.size() > 1024) {
			BACKSTAB_IMMUNE_UNTIL.values().removeIf(until -> until < now);
		}
	}
}
