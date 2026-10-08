#!/usr/bin/env python3
"""
Generates Java catalog + all JSON resources + textures from tools/catalog.py.

    python3 tools/generate.py

Requires Pillow (pip install pillow) for the textures.
"""
import json
import math
import os
import shutil
import sys
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)

from catalog import (MOD_ID, NS, TIERS, FAMILIES, TWO_HANDED_FAMILIES, WEAPONS, SHIELDS,  # noqa: E402
                     SHIELD_SPRITES, MATERIALS)

ASSETS = os.path.join(ROOT, "src/main/resources/assets", MOD_ID)
DATA = os.path.join(ROOT, "src/main/resources/data", MOD_ID)
JAVA_CATALOG = os.path.join(ROOT, "src/main/java/com/valheimarsenal/weapon/WeaponCatalog.java")

# Valheim -> Minecraft conversions ------------------------------------------------------

STAFF_POWER = {"EMBERS": 10.0, "FROST": 3.5, "PROTECTION": 0.0, "LIGHTNING": 10.0, "WILD": 7.0, "FRACTURE": 8.0}
CROSSBOW_MULT = {"arbalest": 1.8, "ripper": 1.9}


def half(x):
    return round(x * 2) / 2


def mc_damage(valheim_physical):
    """Valheim base damage -> Minecraft attack damage (iron sword 55 -> 6.5, Krom 150 -> 10)."""
    return half(0.5 + 0.8 * math.sqrt(valheim_physical))


def physical_type(phys):
    keys = {k for k, v in phys.items() if v > 0}
    if keys == {"blunt"}:
        return "BLUNT"
    if keys == {"slash"}:
        return "SLASH"
    if keys == {"pierce"}:
        return "PIERCE"
    if keys == {"blunt", "pierce"}:
        return "BLUNT_PIERCE"
    if keys == {"slash", "pierce"}:
        return "SLASH_PIERCE"
    return "NONE"


def stats_line(w):
    parts = []
    for key in ("blunt", "slash", "pierce"):
        if w["phys"].get(key):
            parts.append(f"{key.capitalize()} {w['phys'][key]}")
    for key in ("fire", "frost", "lightning", "poison", "spirit"):
        if w["elem"].get(key):
            parts.append(f"{key.capitalize()} {w['elem'][key]}")
    if w["elem"].get("pure"):
        parts.append(f"Pure {w['elem']['pure']} (animals only)")
    return "Valheim: " + " · ".join(parts) if parts else ""


def derive(w):
    """Compute every Minecraft-side number for a weapon dict from catalog.py."""
    fam = w["family"]
    speed, reach, knockback, two_handed, backstab, dur_mult = FAMILIES[fam]
    tier_label, material, tier_dur, ench, _ = TIERS[w["tier"]]
    phys_total = sum(w["phys"].values())
    d = dict(id=w["id"], family=fam, tier=w["tier"], rarity=w["rarity"])
    d["speed"] = w.get("speed", speed)
    d["reach"] = reach
    d["knockback"] = knockback
    d["two_handed"] = two_handed
    d["backstab"] = backstab
    d["durability"] = int(round(tier_dur * dur_mult / 10.0) * 10)
    d["physical"] = physical_type(w["phys"])
    d["stats"] = stats_line(w)

    if fam == "STAFF":
        d["damage"] = STAFF_POWER[w["staff"]]
        d["staff"] = w["staff"]
    elif fam == "BUTCHER":
        d["damage"] = 2.0
    elif fam in ("BOW", "CROSSBOW"):
        d["damage"] = 0.0
    else:
        dmg = mc_damage(phys_total) if phys_total else 1.0
        if two_handed:
            dmg = half(dmg * 1.2)
        d["damage"] = dmg

    if fam == "BOW":
        d["ranged"] = round(math.sqrt(phys_total / 32.0), 2)
    elif fam == "CROSSBOW":
        d["ranged"] = CROSSBOW_MULT[w["id"]]

    e = w["elem"]
    if e.get("spirit"):
        d["spirit"] = half(0.8 * math.sqrt(e["spirit"]))
    if e.get("fire") and fam != "STAFF":
        d["fire"] = int(min(8, max(2, round(2 + e["fire"] / 15))))
    if e.get("frost") and fam != "STAFF":
        d["frost"] = (40 + e["frost"], 1 if e["frost"] >= 40 else 0)
    if e.get("poison"):
        d["poison"] = (40 + 2 * e["poison"], 1 if e["poison"] >= 40 else 0)
    if e.get("lightning") and fam != "STAFF":
        d["lightning"] = half(0.5 * math.sqrt(e["lightning"]))
    d["fireproof"] = w["tier"] == "ASHLANDS"
    return d


def f(x):
    s = f"{x:.2f}".rstrip("0").rstrip(".")
    if "." not in s:
        s += ".0"
    return s + "F"


def java_builder(d):
    lines = [f'WeaponSpec.builder("{d["id"]}", Family.{d["family"]}, Tier.{d["tier"]})']
    if d["rarity"] != "COMMON":
        lines.append(f".rarity(Rarity.{d['rarity']})")
    lines.append(f".damage({f(d['damage'])}).speed({f(d['speed'])}).durability({d['durability']})")
    if d["physical"] != "NONE":
        lines.append(f".physical(WeaponSpec.PhysicalType.{d['physical']})")
    if d["stats"]:
        stats = d["stats"].replace("·", "\\u00b7")
        lines.append(f'.stats("{stats}")')
    if "spirit" in d:
        lines.append(f".spirit({f(d['spirit'])})")
    if "fire" in d:
        lines.append(f".fire({d['fire']})")
    if "frost" in d:
        lines.append(f".frost({d['frost'][0]}, {d['frost'][1]})")
    if "poison" in d:
        lines.append(f".poison({d['poison'][0]}, {d['poison'][1]})")
    if "lightning" in d:
        lines.append(f".lightning({f(d['lightning'])})")
    if d["backstab"] != 3.0:
        lines.append(f".backstab({f(d['backstab'])})")
    if d["two_handed"]:
        lines.append(".twoHanded()")
    if d["reach"]:
        lines.append(f".reach({f(d['reach'])})")
    if d["knockback"]:
        lines.append(f".knockback({f(d['knockback'])})")
    if "ranged" in d:
        lines.append(f".ranged({f(d['ranged'])})")
    if "staff" in d:
        lines.append(f".staff(WeaponSpec.StaffKind.{d['staff']})")
    if "shield" in d:
        lines.append(f".shield(WeaponSpec.ShieldKind.{d['shield']}, {f(d['block_ratio'])})")
    if d["fireproof"]:
        lines.append(".fireproof()")
    lines.append(".build()")
    return "\t\t\t" + lines[0] + "\n" + "\n".join("\t\t\t\t\t" + l for l in lines[1:])


def shield_specs():
    out = []
    for sid, name, kind, tier, armor, ratio, recipe in SHIELDS:
        tier_label, material, tier_dur, ench, _ = TIERS[tier]
        durability = int(round(tier_dur * (1.3 if kind == "TOWER" else 1.0) / 10.0) * 10)
        out.append(dict(id=sid, family="SHIELD", tier=tier, rarity="COMMON", damage=1.0, speed=4.0,
                        durability=durability, physical="NONE", stats=f"Valheim: Block armor {armor}",
                        backstab=1.0, two_handed=False, reach=0.0, knockback=0.0, shield=kind,
                        block_ratio=ratio, fireproof=tier == "ASHLANDS"))
    return out


def write_java(weapons, shields):
    entries = [java_builder(d) for d in weapons + shields]
    materials = ", ".join(f'"{m[0]}"' for m in MATERIALS)
    src = f"""package com.valheimarsenal.weapon;

import java.util.List;

import net.minecraft.world.item.Rarity;

/**
 * GENERATED by tools/generate.py from tools/catalog.py. Do not edit by hand.
 */
public final class WeaponCatalog {{
	public static final List<String> MATERIALS = List.of({materials});

	public static final List<WeaponSpec> ALL = List.of(
{",\n".join(entries)}
	);

	private WeaponCatalog() {{
	}}
}}
"""
    with open(JAVA_CATALOG, "w", encoding="utf-8") as fh:
        fh.write(src)


# Resources ------------------------------------------------------------------------------

def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh, indent=2, ensure_ascii=False)
        fh.write("\n")


def model(path_id, parent, texture):
    dump(os.path.join(ASSETS, "models/item", path_id + ".json"),
         {"parent": parent, "textures": {"layer0": f"{NS}item/{texture}"}})


def simple_item_def(item_id, model_id=None):
    dump(os.path.join(ASSETS, "items", item_id + ".json"),
         {"model": {"type": "minecraft:model", "model": f"{NS}item/{model_id or item_id}"}})


def ref(m):
    return {"type": "minecraft:model", "model": f"{NS}item/{m}"}


def write_models(weapons, shields):
    for m in MATERIALS:
        model(m[0], "minecraft:item/generated", m[0])
        simple_item_def(m[0])

    for d in weapons:
        wid = d["id"]
        if d["family"] == "BOW":
            model(wid, "minecraft:item/bow", wid)
            for i in range(3):
                model(f"{wid}_pulling_{i}", "minecraft:item/bow", f"{wid}_pulling_{i}")
            dump(os.path.join(ASSETS, "items", wid + ".json"), {"model": {
                "type": "minecraft:condition",
                "property": "minecraft:using_item",
                "on_false": ref(wid),
                "on_true": {
                    "type": "minecraft:range_dispatch",
                    "property": "minecraft:use_duration",
                    "scale": 0.05,
                    "entries": [
                        {"threshold": 0.65, "model": ref(f"{wid}_pulling_1")},
                        {"threshold": 0.9, "model": ref(f"{wid}_pulling_2")},
                    ],
                    "fallback": ref(f"{wid}_pulling_0"),
                },
            }})
        elif d["family"] == "CROSSBOW":
            model(wid, "minecraft:item/crossbow", wid)
            for i in range(3):
                model(f"{wid}_pulling_{i}", "minecraft:item/crossbow", f"{wid}_pulling_{i}")
            model(f"{wid}_arrow", "minecraft:item/crossbow", f"{wid}_arrow")
            dump(os.path.join(ASSETS, "items", wid + ".json"), {"model": {
                "type": "minecraft:select",
                "property": "minecraft:charge_type",
                "cases": [
                    {"when": "arrow", "model": ref(f"{wid}_arrow")},
                    {"when": "rocket", "model": ref(f"{wid}_arrow")},
                ],
                "fallback": {
                    "type": "minecraft:condition",
                    "property": "minecraft:using_item",
                    "on_false": ref(wid),
                    "on_true": {
                        "type": "minecraft:range_dispatch",
                        "property": "minecraft:crossbow/pull",
                        "entries": [
                            {"threshold": 0.58, "model": ref(f"{wid}_pulling_1")},
                            {"threshold": 1.0, "model": ref(f"{wid}_pulling_2")},
                        ],
                        "fallback": ref(f"{wid}_pulling_0"),
                    },
                },
            }})
        else:
            model(wid, "minecraft:item/handheld", wid)
            simple_item_def(wid)

    # Shields: flat, held face-out, raised while blocking.
    dump(os.path.join(ASSETS, "models/item/flat_shield.json"), {
        "parent": "minecraft:item/generated",
        "display": {
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 2.5], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 2.5], "scale": [0.85, 0.85, 0.85]},
            "firstperson_righthand": {"rotation": [0, 20, 0], "translation": [1.5, 1.0, -1.0], "scale": [0.7, 0.7, 0.7]},
            "firstperson_lefthand": {"rotation": [0, -20, 0], "translation": [1.5, 1.0, -1.0], "scale": [0.7, 0.7, 0.7]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
        },
    })
    dump(os.path.join(ASSETS, "models/item/flat_shield_blocking.json"), {
        "parent": f"{NS}item/flat_shield",
        "display": {
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [-1.5, 4.0, 3.0], "scale": [0.9, 0.9, 0.9]},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [-1.5, 4.0, 3.0], "scale": [0.9, 0.9, 0.9]},
            "firstperson_righthand": {"rotation": [0, 0, 0], "translation": [-3.0, 3.5, -2.0], "scale": [0.85, 0.85, 0.85]},
            "firstperson_lefthand": {"rotation": [0, 0, 0], "translation": [-3.0, 3.5, -2.0], "scale": [0.85, 0.85, 0.85]},
        },
    })
    for d in shields:
        sid = d["id"]
        model(sid, f"{NS}item/flat_shield", sid)
        model(sid + "_blocking", f"{NS}item/flat_shield_blocking", sid)
        dump(os.path.join(ASSETS, "items", sid + ".json"), {"model": {
            "type": "minecraft:condition",
            "property": "minecraft:using_item",
            "on_false": ref(sid),
            "on_true": ref(sid + "_blocking"),
        }})


def write_lang(weapons_raw, shields):
    lang = {"itemGroup.valheim_arsenal.arsenal": "Valheim Arsenal",
            "entity.valheim_arsenal.thrown_spear": "Thrown Spear",
            "entity.valheim_arsenal.magic_bolt": "Magic Bolt",
            "biome.valheim_arsenal.mistlands": "Mistlands"}
    for mid, name, *_ in MATERIALS:
        lang[f"item.{MOD_ID}.{mid}"] = name if name.endswith("Eitr") else (name + " Ingot" if not name.endswith("Ingot") else name)
    for w in weapons_raw:
        lang[f"item.{MOD_ID}.{w['id']}"] = w["name"]
    for sid, name, *_ in SHIELDS:
        lang[f"item.{MOD_ID}.{sid}"] = name
    for key, (biome, material, *_rest) in TIERS.items():
        lang[f"tooltip.{MOD_ID}.tier.{key.lower()}"] = f"{biome} · {material}"

    t = f"tooltip.{MOD_ID}."
    lang.update({
        t + "spirit": "Spirit: +%s damage to the undead",
        t + "fire": "Fire: sets enemies ablaze for %ss",
        t + "frost": "Frost: chills and slows enemies",
        t + "poison": "Poison: poisons enemies for %ss",
        t + "lightning": "Lightning: arcs to nearby enemies for %s",
        t + "blunt": "Blunt: shatters skeletons",
        t + "pierce": "Pierce: weak against skeletons",
        t + "primary.shockwave": "Attack: impact shockwave hits everything around the target",
        t + "primary.cleave": "Attack: wide swing carries into nearby enemies",
        t + "secondary.uppercut": "Secondary: Uppercut – 2× damage, heavy knockback, staggers",
        t + "secondary.ground_slam": "Secondary: Ground Slam – area slam that staggers everything nearby",
        t + "secondary.lunge": "Secondary: Lunge – dash forward and stab for 3× damage",
        t + "secondary.whirlwind": "Secondary: Whirlwind – 1.5× damage to everything around you",
        t + "secondary.leap": "Secondary: Leap – jump forward and stab for 2× damage",
        t + "secondary.heavy_chop": "Secondary: Heavy Chop – 2× damage, brief stagger",
        t + "secondary.haft_bash": "Secondary: Haft Bash – shove up to 3 enemies back and stagger them",
        t + "secondary.throw": "Secondary: Throw – hurl the spear (go pick it up!)",
        t + "secondary.spin": "Secondary: Spin – sweep everything around you",
        t + "secondary.kick": "Secondary: Heavy Blow – 2× damage, knockback, staggers",
        t + "butcher": "Kills animals in one blow. Harmless to anything else.",
        t + "ranged": "Arrow damage ×%s",
        t + "staff.embers": "Cast: fireball that bursts into flame (costs eitr)",
        t + "staff.frost": "Cast: rapid spray of ice shards (costs eitr)",
        t + "staff.protection": "Blood magic: costs 40% of your health, shields you and nearby allies",
        t + "staff.lightning": "Cast: call lightning down where you aim (costs eitr)",
        t + "staff.wild": "Cast: roots erupt and hold enemies in place (costs eitr)",
        t + "staff.fracture": "Cast: a line of erupting stone (costs eitr)",
        t + "block": "Blocks %s%% of a frontal hit",
        t + "shield.round": "Parry: raise just as a hit lands to block it all, stagger the attacker and deal 2× on your next hit",
        t + "shield.buckler": "Parry: wide timing window and a long stagger",
        t + "shield.tower": "Cannot parry. Heavy: slows you while held",
        t + "backstab": "Backstab: ×%s damage to unaware enemies",
        t + "two_handed": "Two-handed: slows you while held",
    })
    m = f"message.{MOD_ID}."
    lang.update({
        m + "backstab": "Sneak attack ×%s",
        m + "riposte": "Riposte ×2",
        m + "parry": "Parry!",
        m + "staggered": "Staggered!",
        m + "no_stamina": "Not enough stamina (eat something)",
        m + "no_eitr": "Not enough eitr (eat something)",
        m + "no_health": "Not enough health for blood magic",
    })
    dump(os.path.join(ASSETS, "lang/en_us.json"), lang)
    return lang


def ingredient_key(i):
    return i


def write_recipes(weapons_raw):
    recipes = {}
    for mid, name, ingredients, count, _ in MATERIALS:
        recipes[mid] = (ingredients, count, "misc")
    for w in weapons_raw:
        recipes[w["id"]] = (w["recipe"], 1, "equipment")
    for sid, name, kind, tier, armor, ratio, recipe in SHIELDS:
        recipes[sid] = (recipe, 1, "equipment")

    # Two shapeless recipes with the same ingredients would conflict.
    seen = {}
    for rid, (ingredients, count, cat) in recipes.items():
        assert 1 <= len(ingredients) <= 9, f"{rid}: {len(ingredients)} ingredients"
        key = tuple(sorted(Counter(ingredients).items()))
        assert key not in seen, f"recipe conflict: {rid} and {seen[key]}"
        seen[key] = rid
        dump(os.path.join(DATA, "recipe", rid + ".json"), {
            "type": "minecraft:crafting_shapeless",
            "category": cat,
            "ingredients": ingredients,
            "result": {"id": NS + rid, "count": count},
        })

    # Unlock every recipe in the recipe book as soon as a player joins.
    dump(os.path.join(DATA, "advancement/recipes/unlock_all.json"), {
        "criteria": {"joined": {"trigger": "minecraft:tick"}},
        "rewards": {"recipes": [NS + rid for rid in recipes]},
    })
    return recipes


def write_tags(weapons, shields):
    def tag(kind, ns, name, values):
        base = os.path.join(ROOT, "src/main/resources/data", ns, "tags", kind)
        dump(os.path.join(base, name + ".json"), {"replace": False, "values": values})

    for key, (*_x, repair) in TIERS.items():
        tag("item", MOD_ID, f"repairs_{key.lower()}", repair)

    incorrect = {"MEADOWS": "stone", "BLACK_FOREST": "stone", "SWAMP": "iron", "MOUNTAINS": "iron",
                 "PLAINS": "diamond", "MISTLANDS": "diamond", "ASHLANDS": "netherite"}
    for key, level in incorrect.items():
        vanilla = "wooden" if level == "wood" else level
        tag("block", MOD_ID, f"incorrect_for_{key.lower()}",
            [{"id": f"#minecraft:incorrect_for_{vanilla}_tool", "required": False}])

    ids = lambda fams: [NS + d["id"] for d in weapons if d["family"] in fams]
    swords = ids({"SWORD", "GREATSWORD"})
    axes = ids({"AXE", "BATTLEAXE"})
    other_melee = ids({"CLUB", "SLEDGE", "KNIFE", "SPEAR", "ATGEIR", "FIST", "BUTCHER"})
    bows = ids({"BOW"})
    crossbows = ids({"CROSSBOW"})
    staves = ids({"STAFF"})
    shield_ids = [NS + d["id"] for d in shields]
    everything = [NS + d["id"] for d in weapons + shields]

    # Valheim damage-type weaknesses (optional references so a renamed vanilla tag can't break loading)
    tag("entity_type", MOD_ID, "blunt_weak", [{"id": "#minecraft:skeletons", "required": False}])
    tag("entity_type", MOD_ID, "spirit_weak", [{"id": "#minecraft:undead", "required": False}])

    # Mistlands biome: keep structures and biome checks that relied on the forests it replaces
    mist = [NS + "mistlands"]
    for t in ("is_overworld", "is_forest", "stronghold_biased_to", "has_structure/mineshaft",
              "has_structure/ruined_portal_standard", "has_structure/trial_chambers",
              "has_structure/trail_ruins", "has_structure/woodland_mansion"):
        tag("worldgen/biome", "minecraft", t, mist)

    tag("item", "minecraft", "swords", swords)
    tag("item", "minecraft", "axes", axes)
    for t in ("enchantable/sharp_weapon", "enchantable/weapon", "enchantable/melee_weapon", "enchantable/fire_aspect"):
        tag("item", "minecraft", t, other_melee)
    tag("item", "minecraft", "enchantable/durability", everything)
    tag("item", "minecraft", "enchantable/vanishing", everything)
    tag("item", "minecraft", "enchantable/bow", bows)
    tag("item", "minecraft", "enchantable/crossbow", crossbows)
    # Fabric/NeoForge convention tags, for compatibility with other mods.
    tag("item", "c", "tools/melee_weapon", swords + axes + other_melee + staves)
    tag("item", "c", "tools/bow", bows)
    tag("item", "c", "tools/crossbow", crossbows)
    tag("item", "c", "tools/shield", shield_ids)


def main():
    weapons = [derive(w) for w in WEAPONS]
    shields = shield_specs()
    ids = [d["id"] for d in weapons + shields] + [m[0] for m in MATERIALS]
    dupes = [i for i, c in Counter(ids).items() if c > 1]
    assert not dupes, f"duplicate ids: {dupes}"

    for sub in ("models", "items", "lang"):
        shutil.rmtree(os.path.join(ASSETS, sub), ignore_errors=True)
    for sub in ("recipe", "advancement", "tags"):
        shutil.rmtree(os.path.join(DATA, sub), ignore_errors=True)
    for ns in ("minecraft", "c"):
        shutil.rmtree(os.path.join(ROOT, "src/main/resources/data", ns), ignore_errors=True)

    write_java(weapons, shields)
    write_models(weapons, shields)
    write_lang(WEAPONS, shields)
    write_recipes(WEAPONS)
    write_tags(weapons, shields)

    import sprites
    sprites.render_all(WEAPONS, SHIELDS, SHIELD_SPRITES, MATERIALS, os.path.join(ASSETS, "textures/item"),
                       os.path.join(ASSETS, "icon.png"))

    print(f"Generated {len(weapons)} weapons, {len(shields)} shields, {len(MATERIALS)} materials.")
    return weapons, shields


if __name__ == "__main__":
    main()
