package com.valheimarsenal.weapon;

/**
 * Valheim weapon families. Each family carries its Valheim secondary attack and
 * the special behaviour of its primary attack.
 */
public enum Family {
	CLUB(Secondary.UPPERCUT, Primary.NORMAL),
	SLEDGE(Secondary.GROUND_SLAM, Primary.SHOCKWAVE),
	SWORD(Secondary.LUNGE, Primary.NORMAL),
	GREATSWORD(Secondary.WHIRLWIND, Primary.NORMAL),
	KNIFE(Secondary.LEAP, Primary.NORMAL),
	AXE(Secondary.HEAVY_CHOP, Primary.NORMAL),
	BATTLEAXE(Secondary.HAFT_BASH, Primary.CLEAVE),
	SPEAR(Secondary.THROW, Primary.NORMAL),
	ATGEIR(Secondary.SPIN, Primary.NORMAL),
	FIST(Secondary.KICK, Primary.NORMAL),
	BUTCHER(Secondary.NONE, Primary.NORMAL),
	BOW(Secondary.NONE, Primary.RANGED),
	CROSSBOW(Secondary.NONE, Primary.RANGED),
	STAFF(Secondary.NONE, Primary.MAGIC),
	SHIELD(Secondary.NONE, Primary.BLOCK);

	private final Secondary secondary;
	private final Primary primary;

	Family(Secondary secondary, Primary primary) {
		this.secondary = secondary;
		this.primary = primary;
	}

	public Secondary secondary() {
		return secondary;
	}

	public Primary primary() {
		return primary;
	}

	public boolean isMelee() {
		return primary == Primary.NORMAL || primary == Primary.SHOCKWAVE || primary == Primary.CLEAVE;
	}

	/** What happens on a normal left-click hit, on top of vanilla damage. */
	public enum Primary {
		NORMAL,
		/** Sledges: the impact sends a shockwave that hits everything around the target. */
		SHOCKWAVE,
		/** Battleaxes: the swing carries through to enemies beside the target. (Greatswords sweep like swords.) */
		CLEAVE,
		RANGED,
		MAGIC,
		BLOCK
	}

	/** Valheim's middle-mouse attack, mapped to right-click. */
	public enum Secondary {
		NONE(0, 0, 0f),
		/** Clubs and maces: slow upward swing, double damage, big knockback, staggers. */
		UPPERCUT(20, 2, 2.0f),
		/** Sledges: overhead ground slam that staggers everything nearby. */
		GROUND_SLAM(40, 3, 1.0f),
		/** Swords: long wind-up stab with a short dash, triple damage. */
		LUNGE(30, 2, 3.0f),
		/** Greatswords: a full spin that hits everything around you. */
		WHIRLWIND(35, 3, 1.5f),
		/** Knives: jump forward and stab. */
		LEAP(25, 2, 2.0f),
		/** Axes: a committed overhead chop. */
		HEAVY_CHOP(25, 2, 2.0f),
		/** Battleaxes: a quick bash with the haft that shoves enemies back and staggers them. */
		HAFT_BASH(15, 1, 0.5f),
		/** Spears: throw the spear. You have to go and pick it up again. */
		THROW(10, 1, 1.25f),
		/** Atgeirs: a 360-degree sweep that knocks back everything around you. */
		SPIN(30, 3, 1.0f),
		/** Fists and claws: a heavy blow that staggers. */
		KICK(20, 2, 2.0f);

		/** Cooldown in ticks before the secondary can be used again. */
		public final int cooldown;
		/** Stamina cost, applied as food exhaustion (Minecraft's closest equivalent). */
		public final int staminaCost;
		/** Damage multiplier applied to the weapon's attack damage. */
		public final float damageMultiplier;

		Secondary(int cooldown, int staminaCost, float damageMultiplier) {
			this.cooldown = cooldown;
			this.staminaCost = staminaCost;
			this.damageMultiplier = damageMultiplier;
		}
	}
}
