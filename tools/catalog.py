"""
Single source of truth for every item in Valheim Arsenal.

Edit this file, then run `python3 tools/generate.py` to regenerate:
  * src/main/java/com/valheimarsenal/weapon/WeaponCatalog.java
  * item models, item definitions, textures, lang, recipes and tags

Valheim stats are the in-game base values (no quality upgrades) taken from the
Valheim wiki. They are shown in each tooltip and converted to Minecraft-scale
numbers by generate.py (see `mc_damage`).
"""

MOD_ID = "valheim_arsenal"
NS = MOD_ID + ":"

# ---------------------------------------------------------------------------
# Tiers follow Valheim's biome progression, each mapped to a Minecraft tier.
# ---------------------------------------------------------------------------
TIERS = {
    #  key            biome label      material label  durability  enchantability  repair items
    "MEADOWS":      ("Meadows",       "Wood & Flint",  130, 15, ["#minecraft:planks", "minecraft:flint"]),
    "BLACK_FOREST": ("Black Forest",  "Bronze",        220, 14, [NS + "bronze_ingot"]),
    "SWAMP":        ("Swamp",         "Iron",          320, 14, ["minecraft:iron_ingot"]),
    "MOUNTAINS":    ("Mountains",     "Silver",        480, 18, [NS + "silver_ingot"]),
    "PLAINS":       ("Plains",        "Black Metal",   900, 12, [NS + "black_metal_ingot"]),
    "MISTLANDS":    ("Mistlands",     "Eitr & Carapace", 1400, 16, [NS + "refined_eitr", "minecraft:armadillo_scute"]),
    "ASHLANDS":     ("Ashlands",      "Flametal",      2000, 16, [NS + "flametal_ingot"]),
}

# ---------------------------------------------------------------------------
# Weapon families. speed = Minecraft attack speed (attacks/second).
# ---------------------------------------------------------------------------
FAMILIES = {
    # family       speed  reach  knockback two_handed backstab  durability x  physical label
    "CLUB":       (1.2,   0.0,   0.6,      False,     3.0,      1.0),
    "SLEDGE":     (0.7,   0.0,   1.2,      True,      3.0,      1.2),
    "SWORD":      (1.6,   0.0,   0.0,      False,     3.0,      1.0),
    "GREATSWORD": (1.0,   0.5,   0.3,      True,      3.0,      1.2),
    "KNIFE":      (2.2,  -0.5,   0.0,      False,     6.0,      0.8),
    "AXE":        (1.0,   0.0,   0.2,      False,     3.0,      1.0),
    "BATTLEAXE":  (0.8,   0.5,   0.5,      True,      3.0,      1.2),
    "SPEAR":      (1.3,   0.5,   0.0,      False,     3.0,      0.9),
    "ATGEIR":     (1.1,   1.0,   0.2,      True,      3.0,      1.1),
    "FIST":       (2.0,  -0.5,   0.3,      False,     3.0,      1.0),
    "BUTCHER":    (2.0,  -0.5,   0.0,      False,     1.0,      1.0),
    "BOW":        (0.0,   0.0,   0.0,      True,      3.0,      1.0),
    "CROSSBOW":   (0.0,   0.0,   0.0,      True,      3.0,      1.0),
    "STAFF":      (1.0,   0.0,   0.0,      True,      1.0,      0.6),
    "SHIELD":     (0.0,   0.0,   0.0,      False,     1.0,      1.0),
}

TWO_HANDED_FAMILIES = {k for k, v in FAMILIES.items() if v[3]}


def W(id, name, family, tier, phys=None, elem=None, recipe=None, rarity="COMMON",
      sprite=None, **extra):
    return dict(id=id, name=name, family=family, tier=tier, phys=phys or {}, elem=elem or {},
                recipe=recipe or [], rarity=rarity, sprite=sprite or {}, **extra)


L = "minecraft:leather"
S = "minecraft:stick"
BR = NS + "bronze_ingot"
IR = "minecraft:iron_ingot"
SI = NS + "silver_ingot"
BM = NS + "black_metal_ingot"
EI = NS + "refined_eitr"
FL = NS + "flametal_ingot"
CA = "minecraft:armadillo_scute"   # carapace

# Sprite hints: "metal" (palette key for the business end), "handle", "accent", "glow".
WEAPONS = [
    # ---------------- Clubs & maces ----------------
    W("club", "Club", "CLUB", "MEADOWS", {"blunt": 12}, recipe=["#minecraft:logs", "#minecraft:logs", L],
      sprite=dict(template="club", metal="oak", handle="oak")),
    W("bronze_mace", "Bronze Mace", "CLUB", "BLACK_FOREST", {"blunt": 35}, recipe=[BR] * 4 + [S, L],
      sprite=dict(template="mace", metal="bronze", handle="oak")),
    W("iron_mace", "Iron Mace", "CLUB", "SWAMP", {"blunt": 55}, recipe=[IR] * 4 + [S, L],
      sprite=dict(template="mace", metal="iron", handle="dark_oak")),
    W("frostner", "Frostner", "CLUB", "MOUNTAINS", {"blunt": 35}, {"frost": 40, "spirit": 20},
      recipe=[SI, SI, SI, "minecraft:blue_ice", "minecraft:dark_oak_log", "minecraft:packed_ice", L], rarity="RARE",
      sprite=dict(template="frostner", metal="frost", handle="dark_oak", accent="ice")),
    W("porcupine", "Porcupine", "CLUB", "PLAINS", {"blunt": 50, "pierce": 45},
      recipe=[BM, BM, BM, "minecraft:pointed_dripstone", "minecraft:pointed_dripstone", S, L], rarity="UNCOMMON",
      sprite=dict(template="porcupine", metal="blackmetal", handle="dark_oak", accent="bone")),
    W("flametal_mace", "Flametal Mace", "CLUB", "ASHLANDS", {"blunt": 135},
      recipe=[FL] * 4 + ["minecraft:crimson_stem", L],
      sprite=dict(template="mace", metal="flametal", handle="ashwood", accent="ember")),

    # ---------------- Sledges ----------------
    W("stagbreaker", "Stagbreaker", "SLEDGE", "MEADOWS", {"blunt": 20, "pierce": 5},
      recipe=["minecraft:spruce_log"] * 3 + ["minecraft:bone", "minecraft:bone", L], speed=0.85,
      sprite=dict(template="stagbreaker", metal="spruce", handle="oak", accent="antler")),
    W("iron_sledge", "Iron Sledge", "SLEDGE", "SWAMP", {"blunt": 55},
      recipe=[IR] * 6 + [S, S, L],
      sprite=dict(template="sledge", metal="iron", handle="dark_oak")),
    W("demolisher", "Demolisher", "SLEDGE", "MISTLANDS", {"blunt": 145},
      recipe=[BM, BM, BM, BM, EI, "minecraft:lightning_rod", "minecraft:cherry_log", L], rarity="RARE",
      sprite=dict(template="demolisher", metal="blackmetal", handle="yggdrasil", accent="eitr")),

    # ---------------- Swords ----------------
    W("bronze_sword", "Bronze Sword", "SWORD", "BLACK_FOREST", {"slash": 35}, recipe=[BR, BR, S, L],
      sprite=dict(template="sword", metal="bronze", handle="leather")),
    W("iron_sword", "Iron Sword", "SWORD", "SWAMP", {"slash": 55}, recipe=[IR, IR, S, L],
      sprite=dict(template="sword", metal="iron", handle="leather", accent="iron_guard")),
    W("silver_sword", "Silver Sword", "SWORD", "MOUNTAINS", {"slash": 75}, {"spirit": 30},
      recipe=[SI, SI, IR, S, L], rarity="UNCOMMON",
      sprite=dict(template="sword_fancy", metal="silver", handle="leather", accent="silver_gem")),
    W("blackmetal_sword", "Blackmetal Sword", "SWORD", "PLAINS", {"slash": 95}, recipe=[BM, BM, S, L, "minecraft:string"],
      sprite=dict(template="sword", metal="blackmetal", handle="linen")),
    W("mistwalker", "Mistwalker", "SWORD", "MOUNTAINS", {"slash": 75}, {"frost": 40, "spirit": 15},
      recipe=[SI, SI, IR, "minecraft:glow_ink_sac", "minecraft:blue_ice", L], rarity="RARE",
      sprite=dict(template="mistwalker", metal="mist", handle="leather", accent="wisp")),
    W("nidhogg", "Nidhögg", "SWORD", "ASHLANDS", {"slash": 135},
      recipe=[FL, FL, "minecraft:crimson_stem", "minecraft:bone", L], rarity="UNCOMMON",
      sprite=dict(template="nidhogg", metal="nidhogg", handle="ashwood", accent="ember")),
    W("dyrnwyn", "Dyrnwyn", "SWORD", "ASHLANDS", {"slash": 145}, {"fire": 10},
      recipe=[FL, FL, FL, "minecraft:blaze_rod", "minecraft:magma_cream", L], rarity="EPIC",
      sprite=dict(template="dyrnwyn", metal="flametal", handle="ashwood", accent="fire")),
    W("krom", "Krom", "GREATSWORD", "MISTLANDS", {"slash": 150},
      recipe=[BM, BM, BM, BM, EI, CA, L], rarity="RARE",
      sprite=dict(template="greatsword", metal="blackmetal", handle="leather", accent="eitr")),
    W("slayer", "Slayer", "GREATSWORD", "ASHLANDS", {"slash": 170},
      recipe=[FL, FL, FL, FL, "minecraft:crimson_stem", "minecraft:blackstone", L], rarity="RARE",
      sprite=dict(template="greatsword", metal="flametal", handle="ashwood", accent="ember")),

    # ---------------- Knives ----------------
    W("flint_knife", "Flint Knife", "KNIFE", "MEADOWS", {"slash": 5, "pierce": 5},
      recipe=["minecraft:flint", "minecraft:flint", S, L],
      sprite=dict(template="knife", metal="flint", handle="oak")),
    W("copper_knife", "Copper Knife", "KNIFE", "BLACK_FOREST", {"slash": 12, "pierce": 12},
      recipe=["minecraft:copper_ingot", "minecraft:copper_ingot", S, L],
      sprite=dict(template="knife", metal="copper", handle="oak")),
    W("butcher_knife", "Butcher Knife", "BUTCHER", "SWAMP", {}, {"pure": 1000},
      recipe=[IR, "minecraft:birch_planks", L],
      sprite=dict(template="cleaver", metal="iron", handle="birch")),
    W("abyssal_razor", "Abyssal Razor", "KNIFE", "SWAMP", {"slash": 20, "pierce": 20},
      recipe=["minecraft:nautilus_shell", "minecraft:prismarine_shard", S, L], rarity="UNCOMMON",
      sprite=dict(template="razor", metal="chitin", handle="leather")),
    W("silver_knife", "Silver Knife", "KNIFE", "MOUNTAINS", {"slash": 25, "pierce": 25}, {"spirit": 12},
      recipe=[SI, SI, S, L],
      sprite=dict(template="knife", metal="silver", handle="leather")),
    W("blackmetal_knife", "Blackmetal Knife", "KNIFE", "PLAINS", {"slash": 34, "pierce": 34},
      recipe=[BM, BM, S, L],
      sprite=dict(template="knife", metal="blackmetal", handle="linen")),
    W("skoll_and_hati", "Skoll and Hati", "KNIFE", "MISTLANDS", {"slash": 45, "pierce": 45},
      recipe=[BM, BM, BM, EI, CA, L], rarity="RARE", speed=2.6,
      sprite=dict(template="dual_knives", metal="blackmetal", handle="leather", accent="eitr")),

    # ---------------- Axes ----------------
    W("stone_axe", "Stone Axe", "AXE", "MEADOWS", {"slash": 15}, recipe=["minecraft:cobblestone", "minecraft:cobblestone", S, L],
      sprite=dict(template="axe_small", metal="stone", handle="oak")),
    W("flint_axe", "Flint Axe", "AXE", "MEADOWS", {"slash": 20}, recipe=["minecraft:flint", "minecraft:flint", "minecraft:flint", S, L],
      sprite=dict(template="axe_small", metal="flint", handle="oak")),
    W("bronze_axe", "Bronze Axe", "AXE", "BLACK_FOREST", {"slash": 40}, recipe=[BR, BR, BR, S, L],
      sprite=dict(template="axe", metal="bronze", handle="oak")),
    W("iron_axe", "Iron Axe", "AXE", "SWAMP", {"slash": 60}, recipe=[IR, IR, IR, S, L],
      sprite=dict(template="axe", metal="iron", handle="dark_oak")),
    W("blackmetal_axe", "Blackmetal Axe", "AXE", "PLAINS", {"slash": 100}, recipe=[BM, BM, BM, S, L],
      sprite=dict(template="axe", metal="blackmetal", handle="linen")),
    W("jotun_bane", "Jotun Bane", "AXE", "MISTLANDS", {"slash": 80}, {"poison": 40},
      recipe=[IR, IR, EI, "minecraft:fermented_spider_eye", "minecraft:cherry_log", L], rarity="RARE",
      sprite=dict(template="jotun_bane", metal="iron", handle="yggdrasil", accent="poison")),
    W("berserkir_axes", "Berserkir Axes", "AXE", "ASHLANDS", {"slash": 140},
      recipe=[FL, FL, FL, FL, "minecraft:crimson_stem", "minecraft:crimson_stem", L], rarity="RARE", speed=1.4,
      sprite=dict(template="dual_axes", metal="flametal", handle="ashwood", accent="ember")),

    # ---------------- Battleaxes ----------------
    W("battleaxe", "Battleaxe", "BATTLEAXE", "MOUNTAINS", {"slash": 70},
      recipe=[IR, IR, IR, SI, SI, S, S, L],
      sprite=dict(template="battleaxe", metal="iron", handle="dark_oak")),
    W("crystal_battleaxe", "Crystal Battleaxe", "BATTLEAXE", "MOUNTAINS", {"slash": 90}, {"spirit": 30},
      recipe=["minecraft:amethyst_shard"] * 4 + [SI, SI, S, S, L], rarity="RARE",
      sprite=dict(template="crystal_battleaxe", metal="crystal", handle="dark_oak", accent="crystal")),

    # ---------------- Spears ----------------
    W("flint_spear", "Flint Spear", "SPEAR", "MEADOWS", {"pierce": 20}, recipe=["minecraft:flint", S, S, L],
      sprite=dict(template="spear", metal="flint", handle="oak")),
    W("bronze_spear", "Bronze Spear", "SPEAR", "BLACK_FOREST", {"pierce": 35}, recipe=[BR, S, S, L],
      sprite=dict(template="spear", metal="bronze", handle="birch")),
    W("ancient_bark_spear", "Ancient Bark Spear", "SPEAR", "SWAMP", {"pierce": 55},
      recipe=[IR, "minecraft:dark_oak_log", "minecraft:dark_oak_log", L],
      sprite=dict(template="spear_long", metal="iron", handle="ancient_bark")),
    W("fang_spear", "Fang Spear", "SPEAR", "MOUNTAINS", {"pierce": 75},
      recipe=["minecraft:bone", "minecraft:bone", "minecraft:bone", SI, S, S, L],
      sprite=dict(template="fang_spear", metal="fang", handle="dark_oak", accent="silver_bind")),
    W("carapace_spear", "Carapace Spear", "SPEAR", "MISTLANDS", {"pierce": 115},
      recipe=[CA, CA, CA, EI, "minecraft:cherry_log", L], rarity="UNCOMMON",
      sprite=dict(template="carapace_spear", metal="carapace", handle="yggdrasil", accent="eitr")),
    W("splitnir", "Splitnir", "SPEAR", "ASHLANDS", {"pierce": 135},
      recipe=[FL, FL, "minecraft:crimson_stem", "minecraft:crimson_stem", L], rarity="RARE",
      sprite=dict(template="splitnir", metal="flametal", handle="ashwood", accent="ember")),

    # ---------------- Atgeirs ----------------
    W("bronze_atgeir", "Bronze Atgeir", "ATGEIR", "BLACK_FOREST", {"pierce": 45},
      recipe=[BR, BR, BR, BR, S, S, L],
      sprite=dict(template="atgeir", metal="bronze", handle="oak")),
    W("iron_atgeir", "Iron Atgeir", "ATGEIR", "SWAMP", {"pierce": 65},
      recipe=[IR, IR, IR, IR, S, S, L],
      sprite=dict(template="atgeir", metal="iron", handle="dark_oak")),
    W("blackmetal_atgeir", "Blackmetal Atgeir", "ATGEIR", "PLAINS", {"pierce": 105},
      recipe=[BM, BM, BM, BM, S, S, L],
      sprite=dict(template="atgeir", metal="blackmetal", handle="linen")),
    W("himminafl", "Himminafl", "ATGEIR", "MISTLANDS", {"pierce": 85}, {"lightning": 40},
      recipe=[BM, BM, BM, EI, "minecraft:lightning_rod", "minecraft:cherry_log", L], rarity="EPIC",
      sprite=dict(template="himminafl", metal="silver", handle="yggdrasil", accent="lightning")),

    # ---------------- Fists ----------------
    W("flesh_rippers", "Flesh Rippers", "FIST", "MISTLANDS", {"slash": 60},
      recipe=[BM, BM, EI, CA, CA, L], rarity="UNCOMMON",
      sprite=dict(template="claws", metal="blackmetal", handle="leather", accent="eitr")),

    # ---------------- Bows ----------------
    W("crude_bow", "Crude Bow", "BOW", "MEADOWS", {"pierce": 22},
      recipe=[S, S, S, "minecraft:string", "minecraft:string", L],
      sprite=dict(template="bow", metal="oak", handle="leather")),
    W("finewood_bow", "Finewood Bow", "BOW", "BLACK_FOREST", {"pierce": 32},
      recipe=["minecraft:birch_log", "minecraft:birch_log", "minecraft:copper_ingot", "minecraft:string", "minecraft:string", L],
      sprite=dict(template="bow", metal="birch", handle="leather", accent="copper_band")),
    W("huntsman_bow", "Huntsman Bow", "BOW", "SWAMP", {"pierce": 42},
      recipe=["minecraft:birch_log", "minecraft:birch_log", IR, IR, "minecraft:string", "minecraft:string", L],
      sprite=dict(template="bow", metal="dark_oak", handle="leather", accent="iron_band")),
    W("draugr_fang", "Draugr Fang", "BOW", "MOUNTAINS", {"pierce": 47}, {"poison": 20},
      recipe=["minecraft:dark_oak_log", SI, SI, "minecraft:slime_ball", "minecraft:string", "minecraft:string", L], rarity="RARE",
      sprite=dict(template="bow_recurve", metal="ancient_bark", handle="leather", accent="poison")),
    W("spinesnap", "Spinesnap", "BOW", "MISTLANDS", {"pierce": 72}, {"spirit": 20},
      recipe=["minecraft:cherry_log", "minecraft:cherry_log", EI, CA, "minecraft:string", "minecraft:string", L], rarity="RARE",
      sprite=dict(template="bow_recurve", metal="yggdrasil", handle="leather", accent="eitr")),
    W("ash_fang", "Ash Fang", "BOW", "ASHLANDS", {"pierce": 82}, {"spirit": 15},
      recipe=[FL, "minecraft:crimson_stem", "minecraft:crimson_stem", "minecraft:bone", "minecraft:string", "minecraft:string", L], rarity="RARE",
      sprite=dict(template="bow_recurve", metal="ashwood", handle="leather", accent="ember")),

    # ---------------- Crossbows ----------------
    W("arbalest", "Arbalest", "CROSSBOW", "MISTLANDS", {"pierce": 200},
      recipe=[BM, BM, IR, EI, "minecraft:tripwire_hook", "minecraft:string", "minecraft:cherry_log"], rarity="RARE",
      sprite=dict(template="crossbow", metal="blackmetal", handle="yggdrasil", accent="eitr")),
    W("ripper", "Ripper", "CROSSBOW", "ASHLANDS", {"pierce": 220},
      recipe=[FL, FL, IR, "minecraft:tripwire_hook", "minecraft:string", "minecraft:crimson_stem"], rarity="RARE",
      sprite=dict(template="crossbow", metal="flametal", handle="ashwood", accent="ember")),

    # ---------------- Magic staves ----------------
    W("staff_of_embers", "Staff of Embers", "STAFF", "MISTLANDS", {"blunt": 120}, {"fire": 120},
      recipe=["minecraft:cherry_log", "minecraft:cherry_log", EI, EI, "minecraft:blaze_rod", "minecraft:fire_charge"], rarity="RARE",
      staff="EMBERS", sprite=dict(template="staff_orb", metal="yggdrasil", handle="yggdrasil", accent="fire")),
    W("staff_of_frost", "Staff of Frost", "STAFF", "MISTLANDS", {}, {"frost": 30},
      recipe=["minecraft:cherry_log", "minecraft:cherry_log", EI, EI, "minecraft:blue_ice", "minecraft:snowball"], rarity="RARE",
      staff="FROST", sprite=dict(template="staff_orb", metal="yggdrasil", handle="yggdrasil", accent="ice")),
    W("staff_of_protection", "Staff of Protection", "STAFF", "MISTLANDS", {}, {},
      recipe=["minecraft:cherry_log", "minecraft:cherry_log", EI, EI, "minecraft:golden_apple"], rarity="RARE",
      staff="PROTECTION", sprite=dict(template="staff_ring", metal="yggdrasil", handle="yggdrasil", accent="blood")),
    W("dundr", "Dundr", "STAFF", "ASHLANDS", {}, {"lightning": 25},
      recipe=[FL, FL, "minecraft:crimson_stem", "minecraft:lightning_rod", "minecraft:copper_ingot", EI], rarity="EPIC",
      staff="LIGHTNING", sprite=dict(template="staff_fork", metal="ashwood", handle="ashwood", accent="lightning")),
    W("staff_of_the_wild", "Staff of the Wild", "STAFF", "ASHLANDS", {"blunt": 55}, {},
      recipe=["minecraft:crimson_stem", "minecraft:crimson_stem", FL, EI, "minecraft:moss_block", "minecraft:hanging_roots"], rarity="RARE",
      staff="WILD", sprite=dict(template="staff_root", metal="ashwood", handle="ashwood", accent="leaf")),
    W("staff_of_fracturing", "Staff of Fracturing", "STAFF", "ASHLANDS", {"blunt": 30}, {},
      recipe=["minecraft:crimson_stem", "minecraft:crimson_stem", FL, EI, "minecraft:blackstone", "minecraft:blackstone"], rarity="RARE",
      staff="FRACTURE", sprite=dict(template="staff_rock", metal="ashwood", handle="ashwood", accent="grausten")),
]

# ---------------------------------------------------------------------------
# Shields. Valheim block armor values are shown in the tooltip; block ratio is
# the fraction of a frontal hit the shield stops in Minecraft.
# ---------------------------------------------------------------------------
P = "#minecraft:planks"
SHIELDS = [
    #  id                       name                       kind       tier            valheim block armor  ratio  recipe
    ("wood_shield",            "Wood Shield",             "ROUND",   "MEADOWS",      6,   0.70, [P, P, P, P, S, L]),
    ("wood_tower_shield",      "Wood Tower Shield",       "TOWER",   "MEADOWS",      10,  0.80, [P, P, P, P, P, P, S, L]),
    ("bone_tower_shield",      "Bone Tower Shield",       "TOWER",   "BLACK_FOREST", 32,  0.85, ["minecraft:bone_block", "minecraft:bone_block", "minecraft:bone_block", P, P, L]),
    ("bronze_buckler",         "Bronze Buckler",          "BUCKLER", "BLACK_FOREST", 16,  0.60, [BR, BR, P, L]),
    ("banded_shield",          "Banded Shield",           "ROUND",   "SWAMP",        42,  0.85, [IR, IR, P, P, P, P, L]),
    ("iron_buckler",           "Iron Buckler",            "BUCKLER", "SWAMP",        28,  0.70, [IR, IR, IR, P, L]),
    ("iron_tower_shield",      "Iron Tower Shield",       "TOWER",   "SWAMP",        52,  0.95, [IR, IR, IR, IR, P, P, P, L]),
    ("silver_shield",          "Silver Shield",           "ROUND",   "MOUNTAINS",    60,  0.90, [SI, SI, SI, P, P, P, L]),
    ("serpent_scale_shield",   "Serpent Scale Shield",    "TOWER",   "MOUNTAINS",    60,  0.95, ["minecraft:turtle_scute", "minecraft:turtle_scute", "minecraft:turtle_scute", SI, SI, P, L]),
    ("blackmetal_shield",      "Blackmetal Shield",       "ROUND",   "PLAINS",       78,  0.92, [BM, BM, P, P, P, L, "minecraft:string"]),
    ("blackmetal_tower_shield","Blackmetal Tower Shield", "TOWER",   "PLAINS",       104, 1.00, [BM, BM, BM, BM, P, P, P, L]),
    ("carapace_buckler",       "Carapace Buckler",        "BUCKLER", "MISTLANDS",    78,  0.85, [CA, CA, CA, EI, L]),
    ("carapace_shield",        "Carapace Shield",         "ROUND",   "MISTLANDS",    96,  0.95, [CA, CA, CA, CA, EI, "minecraft:cherry_log", L]),
    ("flametal_shield",        "Flametal Shield",         "ROUND",   "ASHLANDS",     114, 0.97, [FL, FL, FL, "minecraft:crimson_stem", "minecraft:crimson_stem", L]),
    ("flametal_tower_shield",  "Flametal Tower Shield",   "TOWER",   "ASHLANDS",     140, 1.00, [FL, FL, FL, FL, FL, "minecraft:crimson_stem", L]),
]

SHIELD_SPRITES = {
    "wood_shield": dict(template="round_shield", metal="oak", handle="iron", accent="oak_dark"),
    "wood_tower_shield": dict(template="tower_shield", metal="oak", handle="iron"),
    "bone_tower_shield": dict(template="tower_shield", metal="bone", handle="leather"),
    "bronze_buckler": dict(template="buckler", metal="bronze", handle="copper"),
    "banded_shield": dict(template="banded_shield", metal="oak", handle="iron", accent="red_paint"),
    "iron_buckler": dict(template="buckler", metal="iron", handle="blackmetal"),
    "iron_tower_shield": dict(template="tower_shield", metal="iron", handle="iron"),
    "silver_shield": dict(template="round_shield", metal="silver", handle="silver", accent="wolf_blue"),
    "serpent_scale_shield": dict(template="scale_shield", metal="serpent", handle="iron"),
    "blackmetal_shield": dict(template="round_shield", metal="blackmetal", handle="blackmetal", accent="linen_paint"),
    "blackmetal_tower_shield": dict(template="tower_shield", metal="blackmetal", handle="blackmetal"),
    "carapace_buckler": dict(template="buckler", metal="carapace", handle="eitr"),
    "carapace_shield": dict(template="round_shield", metal="carapace", handle="eitr", accent="eitr"),
    "flametal_shield": dict(template="round_shield", metal="flametal", handle="flametal", accent="ember"),
    "flametal_tower_shield": dict(template="tower_shield", metal="flametal", handle="flametal"),
}

# ---------------------------------------------------------------------------
# Crafting materials (intermediate ingots). Valheim smelts ore; here they are
# alloyed from vanilla materials at a crafting table.
# ---------------------------------------------------------------------------
MATERIALS = [
    ("bronze_ingot", "Bronze", ["minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:iron_nugget"], 1,
     "Copper alloyed with a little tin (iron)."),
    ("silver_ingot", "Silver", ["minecraft:iron_ingot", "minecraft:quartz", "minecraft:quartz"], 1,
     "Mountain silver. Burns the dead."),
    ("black_metal_ingot", "Black Metal", ["minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:diamond", "minecraft:coal"], 2,
     "Fuling forge-craft from the Plains."),
    ("refined_eitr", "Refined Eitr", ["minecraft:sculk", "minecraft:sculk", "minecraft:lapis_lazuli", "minecraft:glowstone_dust"], 1,
     "Distilled magic from the Mistlands."),
    ("flametal_ingot", "Flametal", ["minecraft:netherite_scrap", "minecraft:gold_ingot", "minecraft:blaze_powder", "minecraft:magma_cream"], 1,
     "Still warm from the Ashlands."),
]
