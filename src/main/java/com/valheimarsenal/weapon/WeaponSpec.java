package com.valheimarsenal.weapon;

import net.minecraft.world.item.Rarity;

/**
 * Everything the mod needs to know about one Valheim item.
 * Instances are produced by {@link WeaponCatalog}, which is generated from tools/catalog.py.
 *
 * @param attackDamage    total Minecraft attack damage (including the player's base 1)
 * @param attackSpeed     Minecraft attacks per second
 * @param physical        Valheim's physical damage type, used for skeleton weaknesses
 * @param valheimStats    the original Valheim stat line, shown in the tooltip
 * @param spiritBonus     extra damage dealt to undead
 * @param lightningDamage damage arced to nearby enemies on hit
 * @param backstab        damage multiplier against enemies that have not noticed you
 * @param reach           extra entity reach in blocks (atgeirs reach further, knives less)
 * @param rangedMultiplier arrow damage multiplier for bows and crossbows, or spell power for staves
 * @param blockRatio      fraction of a frontal hit a shield stops
 */
public record WeaponSpec(
		String id,
		Family family,
		Tier tier,
		Rarity rarity,
		float attackDamage,
		float attackSpeed,
		int durability,
		PhysicalType physical,
		String valheimStats,
		float spiritBonus,
		int fireSeconds,
		int frostTicks,
		int frostLevel,
		int poisonTicks,
		int poisonLevel,
		float lightningDamage,
		float backstab,
		boolean twoHanded,
		float reach,
		float knockback,
		float rangedMultiplier,
		StaffKind staff,
		ShieldKind shield,
		float blockRatio,
		boolean fireproof
) {
	public static Builder builder(String id, Family family, Tier tier) {
		return new Builder(id, family, tier);
	}

	public boolean hasElement() {
		return spiritBonus > 0 || fireSeconds > 0 || frostTicks > 0 || poisonTicks > 0 || lightningDamage > 0;
	}

	public enum PhysicalType {
		NONE, BLUNT, SLASH, PIERCE, BLUNT_PIERCE, SLASH_PIERCE;

		/** Valheim skeletons shatter under blunt weapons and shrug off piercing ones. */
		public float skeletonMultiplier() {
			return switch (this) {
				case BLUNT -> 1.5F;
				case BLUNT_PIERCE -> 1.25F;
				case PIERCE -> 0.75F;
				case SLASH_PIERCE -> 0.9F;
				default -> 1.0F;
			};
		}
	}

	public enum StaffKind {
		NONE, EMBERS, FROST, PROTECTION, LIGHTNING, WILD, FRACTURE
	}

	public enum ShieldKind {
		NONE,
		/** Round shields: parry bonus x1.5. */
		ROUND,
		/** Bucklers: small, but a wide parry window and parry bonus x2.5. */
		BUCKLER,
		/** Tower shields: block almost everything but cannot parry, and slow you down. */
		TOWER
	}

	public static final class Builder {
		private final String id;
		private final Family family;
		private final Tier tier;
		private Rarity rarity = Rarity.COMMON;
		private float attackDamage = 1.0F;
		private float attackSpeed = 4.0F;
		private int durability = 250;
		private PhysicalType physical = PhysicalType.NONE;
		private String valheimStats = "";
		private float spiritBonus;
		private int fireSeconds;
		private int frostTicks;
		private int frostLevel;
		private int poisonTicks;
		private int poisonLevel;
		private float lightningDamage;
		private float backstab = 3.0F;
		private boolean twoHanded;
		private float reach;
		private float knockback;
		private float rangedMultiplier = 1.0F;
		private StaffKind staff = StaffKind.NONE;
		private ShieldKind shield = ShieldKind.NONE;
		private float blockRatio;
		private boolean fireproof;

		private Builder(String id, Family family, Tier tier) {
			this.id = id;
			this.family = family;
			this.tier = tier;
		}

		public Builder rarity(Rarity rarity) {
			this.rarity = rarity;
			return this;
		}

		public Builder damage(float attackDamage) {
			this.attackDamage = attackDamage;
			return this;
		}

		public Builder speed(float attackSpeed) {
			this.attackSpeed = attackSpeed;
			return this;
		}

		public Builder durability(int durability) {
			this.durability = durability;
			return this;
		}

		public Builder physical(PhysicalType physical) {
			this.physical = physical;
			return this;
		}

		public Builder stats(String valheimStats) {
			this.valheimStats = valheimStats;
			return this;
		}

		public Builder spirit(float bonus) {
			this.spiritBonus = bonus;
			return this;
		}

		public Builder fire(int seconds) {
			this.fireSeconds = seconds;
			return this;
		}

		public Builder frost(int ticks, int level) {
			this.frostTicks = ticks;
			this.frostLevel = level;
			return this;
		}

		public Builder poison(int ticks, int level) {
			this.poisonTicks = ticks;
			this.poisonLevel = level;
			return this;
		}

		public Builder lightning(float damage) {
			this.lightningDamage = damage;
			return this;
		}

		public Builder backstab(float multiplier) {
			this.backstab = multiplier;
			return this;
		}

		public Builder twoHanded() {
			this.twoHanded = true;
			return this;
		}

		public Builder reach(float reach) {
			this.reach = reach;
			return this;
		}

		public Builder knockback(float knockback) {
			this.knockback = knockback;
			return this;
		}

		public Builder ranged(float multiplier) {
			this.rangedMultiplier = multiplier;
			return this;
		}

		public Builder staff(StaffKind staff) {
			this.staff = staff;
			return this;
		}

		public Builder shield(ShieldKind shield, float blockRatio) {
			this.shield = shield;
			this.blockRatio = blockRatio;
			return this;
		}

		public Builder fireproof() {
			this.fireproof = true;
			return this;
		}

		public WeaponSpec build() {
			return new WeaponSpec(id, family, tier, rarity, attackDamage, attackSpeed, durability, physical,
					valheimStats, spiritBonus, fireSeconds, frostTicks, frostLevel, poisonTicks, poisonLevel,
					lightningDamage, backstab, twoHanded, reach, knockback, rangedMultiplier, staff, shield,
					blockRatio, fireproof);
		}
	}
}
