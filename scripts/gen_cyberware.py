"""Generates cyberware item models, recipes, lang and damage type. Run: python3 -I scripts/gen_cyberware.py
Art and names: A24-N22 (Project: Edgerunner), used with permission."""
import json, os
R = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
A = os.path.join(R, "assets", "constructchip"); D = os.path.join(R, "data")
EDGE_LANG = "/tmp/claude-0/-home-user-constructchip/0373e500-2042-50f1-80a0-d96b417c3161/scratchpad/edge/assets/project_edgerunner/lang/en_us.json"

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, "w"), indent=2); open(path, "a").write("\n")

# id -> (texture, slot, description)
IMPLANTS = {
 "warp_dancer": ("sandevistan_warp_dancer", "Sandevistan: the world slows 20% for 9 s. Fire, frost, shock and poison can't touch you while it runs. 70 s cooldown."),
 "zetatech": ("sandevistan_zetatech", "Sandevistan: 30% slowdown for 6 s, +12% damage (+60% against airborne targets). 45 s cooldown."),
 "dynalar": ("sandevistan_dynalar", "Sandevistan: 50% slowdown for 8 s, +10% damage. Fully upgraded it runs longer. 55 s cooldown."),
 "falcon": ("sandevistan_falcon", "Militech Sandevistan: 70% slowdown for 9 s. Every kill while it runs heals 10% of your health. 40 s cooldown."),
 "apogee": ("sandevistan_apogee", "Militech's apex Sandevistan: 85% slowdown for 6 s, +18% damage, kills extend it. 30 s cooldown."),
 "axolotl": ("axolotl", "Frontal cortex processor: every kill cuts all your cyberware cooldowns by 5% (7.5% upgraded)."),
 "basic_kiroshi": ("basic_kiroshi_optics", "Optics: see the health of whatever you aim at, and zoom (hold C)."),
 "clairvoyant": ("clairvoyant", "Optics + scan (V): hostiles within 19.5 m glow, night vision. 60 s, 30 s cooldown."),
 "doomsayer": ("doomsayer", "Optics + scan (V): marks TNT, pistons, sculk sensors, tripwires and other hazards within 29 m."),
 "stalker": ("stalker", "Optics + scan (V): hostiles within 53 m glow and leave red trails, night vision."),
 "the_oracle": ("the_oracle", "Military optics: all three scan modes, night vision, widest zoom."),
 "gorilla_arms": ("gorilla_arms", "Deploy (B): +6 melee damage, hits bleed and build Fury, bare hands mine like iron. Right-click empty-handed at 50+ Fury to Slam. Use with a blaze rod, lightning rod, fermented spider eye or blue ice in your other hand to add an element."),
 "blood_pump": ("blood_pump", "Press J: instant healing plus regeneration. Upgrades heal more. 30 s cooldown."),
 "fortified_ankles": ("fortified_ankles", "Crouch to charge, then jump: the longer the charge, the higher you go. Softer landings."),
 "reinforced_tendons": ("reinforced_tendons", "Press jump again in mid-air to double jump."),
 "titanium_bones": ("titanium_bones", "Skeleton: physical and elemental resistance, knockback relief, but falls hurt more."),
 "bionic_joints": ("bionic_joints", "Skeleton: faster movement, softer falls, less knockback."),
 "hollow_bones": ("hollow_bones", "Skeleton: ultralight. Faster, higher jumps, safe landings, 2 fewer hearts. Can't share the skeleton with Titanium Bones, Dense Marrow or Rara Avis."),
 "scarab": ("scarab", "Skeleton: crouch to brace. 35% less physical and projectile damage while braced."),
 "scar_coalescer": ("scar_coalescer", "Skeleton: below 40% health you take less physical damage and hit harder. Stops bleeding."),
 "dense_marrow": ("dense_marrow", "Skeleton: +2 melee damage, faster Gorilla Fury and stronger Slams."),
 "kinetic_frame": ("kinetic_frame", "Skeleton: while sprinting, a chance to shrug off a hit completely (not while Scarab-braced)."),
 "spring_joints": ("spring_joints", "Skeleton: much softer falls; hard landings give a rebound jump boost."),
 "para_bellum": ("para_bellum", "Skeleton: 12% less damage of every kind."),
 "universal_booster": ("universal_booster", "Skeleton: whenever you're healed, a short resistance boost that also bleeds off pressure."),
 "epimorphic_skeleton": ("epimorphic_skeleton", "Skeleton: +4 hearts, 20% stronger healing, +10% cyberware capacity, steady pressure relief."),
 "rara_avis": ("rara_avis", "Skeleton: heavy iconic armor, 25% less damage but 10% slower; below 30% health you hit 20% harder. Fighting adds pressure."),
}
PARTS = ["internal_parts_t1","internal_parts_t2","internal_parts_t3","internal_parts_t4","internal_parts_t5",
 "kinetic_actuator_t1","os_frame_t1","temporal_microdriver_t1","skeletal_anchor_t1","joint_servo_t1",
 "hemo_chamber_t2","bio_monitor_t2","myofiber_bundle_t2","ocular_socket_t2","os_core_t2","temporal_regulator_t2","zoom_lens_t2",
 "hollow_lattice_t2","coagulation_mesh_t2","scarab_shell_plate_t2",
 "hazard_scan_kernel_t3","hostile_scan_kernel_t3","os_core_t3","phase_stabilizer_t3","trail_tracker_array_t3","arms_chassis_t3",
 "marrow_pressure_core_t3","kinetic_damper_frame_t3","spring_recoil_array_t3","ballistic_spine_plate_t3",
 "os_core_t4","falcon_overclocker_t4","latency_shunt_t4","neural_matrix_t4","cooldown_optimizer_t4","optic_fusion_controller_t4",
 "arms_actuator_matrix_t4","arms_power_core_t4","trauma_booster_module_t4",
 "os_core_t5","apogee_overdrive_kernel_t5","quantum_sync_lattice_t5","iconic_skeletal_chassis_t5","regen_nanite_scaffold_t5"]
def part_tex(p): return "cyberware_" + p if p.startswith("internal_parts") else p

edge = json.load(open(EDGE_LANG))
lang_p = os.path.join(A, "lang", "en_us.json"); lang = json.load(open(lang_p))

for iid, (tex, desc) in IMPLANTS.items():
    w(f"{A}/models/item/{iid}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"constructchip:item/cyberware/{tex}"}})
    lang[f"item.constructchip.{iid}"] = edge.get(f"item.project_edgerunner.{tex}", iid.replace("_", " ").title())
    lang[f"implant.constructchip.{iid}.desc"] = desc
for p in PARTS:
    w(f"{A}/models/item/{p}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"constructchip:item/cyberware/{part_tex(p)}"}})
    lang[f"item.constructchip.{p}"] = edge.get(f"item.project_edgerunner.{part_tex(p)}", p.replace("_", " ").title())
for iid, tex in (("soldering_iron", "soldering_iron"), ("immunosuppressants", "inmunosupresores")):
    w(f"{A}/models/item/{iid}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"constructchip:item/cyberware/{tex}"}})
lang["item.constructchip.soldering_iron"] = "Soldering Iron"
lang["item.constructchip.immunosuppressants"] = "Immunosuppressants"

# ---- recipes: every implant/part is assembled around the (returned) Soldering Iron
def shapeless(name, items, result, count=1):
    w(f"{D}/constructchip/recipe/cyberware/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
      "ingredients": [{"item": i if ":" in i else "constructchip:" + i} for i in items],
      "result": {"id": "constructchip:" + result, "count": count}})
IRON = "soldering_iron"
PART_RECIPES = {
 "kinetic_actuator_t1": ["internal_parts_t1", "minecraft:piston", "minecraft:copper_ingot"],
 "os_frame_t1": ["internal_parts_t1", "minecraft:redstone", "minecraft:iron_ingot", "minecraft:quartz"],
 "temporal_microdriver_t1": ["internal_parts_t1", "minecraft:clock", "minecraft:redstone"],
 "skeletal_anchor_t1": ["internal_parts_t1", "minecraft:iron_ingot", "minecraft:bone_block"],
 "joint_servo_t1": ["internal_parts_t1", "minecraft:copper_ingot", "minecraft:redstone"],
 "hemo_chamber_t2": ["internal_parts_t2", "minecraft:glass_bottle", "minecraft:redstone", "minecraft:iron_ingot"],
 "bio_monitor_t2": ["internal_parts_t2", "minecraft:comparator", "minecraft:glow_ink_sac"],
 "myofiber_bundle_t2": ["internal_parts_t2", "minecraft:string", "minecraft:string", "minecraft:slime_ball"],
 "ocular_socket_t2": ["internal_parts_t2", "minecraft:ender_eye", "minecraft:iron_ingot"],
 "os_core_t2": ["internal_parts_t2", "os_frame_t1", "minecraft:gold_ingot"],
 "temporal_regulator_t2": ["internal_parts_t2", "temporal_microdriver_t1", "minecraft:amethyst_shard"],
 "zoom_lens_t2": ["internal_parts_t2", "minecraft:spyglass"],
 "hollow_lattice_t2": ["internal_parts_t2", "minecraft:phantom_membrane", "minecraft:bone"],
 "coagulation_mesh_t2": ["internal_parts_t2", "minecraft:honey_bottle", "minecraft:string"],
 "scarab_shell_plate_t2": ["internal_parts_t2", "minecraft:turtle_scute", "minecraft:iron_ingot"],
 "hazard_scan_kernel_t3": ["internal_parts_t3", "minecraft:observer", "minecraft:tnt"],
 "hostile_scan_kernel_t3": ["internal_parts_t3", "minecraft:observer", "minecraft:rotten_flesh"],
 "os_core_t3": ["internal_parts_t3", "os_core_t2", "minecraft:diamond"],
 "phase_stabilizer_t3": ["internal_parts_t3", "temporal_regulator_t2", "minecraft:ender_pearl"],
 "trail_tracker_array_t3": ["internal_parts_t3", "minecraft:observer", "minecraft:compass"],
 "arms_chassis_t3": ["internal_parts_t3", "minecraft:iron_block", "kinetic_actuator_t1"],
 "marrow_pressure_core_t3": ["internal_parts_t3", "minecraft:bone_block", "minecraft:redstone_block"],
 "kinetic_damper_frame_t3": ["internal_parts_t3", "minecraft:slime_block", "joint_servo_t1"],
 "spring_recoil_array_t3": ["internal_parts_t3", "minecraft:sticky_piston", "minecraft:slime_ball"],
 "ballistic_spine_plate_t3": ["internal_parts_t3", "minecraft:iron_block", "minecraft:obsidian"],
 "os_core_t4": ["internal_parts_t4", "os_core_t3", "minecraft:diamond"],
 "falcon_overclocker_t4": ["internal_parts_t4", "phase_stabilizer_t3", "minecraft:blaze_rod"],
 "latency_shunt_t4": ["internal_parts_t4", "minecraft:repeater", "minecraft:ender_pearl"],
 "neural_matrix_t4": ["internal_parts_t4", "minecraft:sculk_catalyst"],
 "cooldown_optimizer_t4": ["internal_parts_t4", "minecraft:clock", "minecraft:packed_ice"],
 "optic_fusion_controller_t4": ["internal_parts_t4", "hostile_scan_kernel_t3", "zoom_lens_t2"],
 "arms_actuator_matrix_t4": ["internal_parts_t4", "minecraft:piston", "minecraft:piston", "myofiber_bundle_t2"],
 "arms_power_core_t4": ["internal_parts_t4", "minecraft:redstone_block", "minecraft:blaze_powder"],
 "trauma_booster_module_t4": ["internal_parts_t4", "minecraft:golden_apple", "coagulation_mesh_t2"],
 "os_core_t5": ["internal_parts_t5", "os_core_t4", "minecraft:netherite_scrap"],
 "apogee_overdrive_kernel_t5": ["internal_parts_t5", "falcon_overclocker_t4", "minecraft:nether_star"],
 "quantum_sync_lattice_t5": ["internal_parts_t5", "neural_matrix_t4", "minecraft:echo_shard"],
 "iconic_skeletal_chassis_t5": ["internal_parts_t5", "minecraft:netherite_ingot", "ballistic_spine_plate_t3"],
 "regen_nanite_scaffold_t5": ["internal_parts_t5", "minecraft:totem_of_undying", "trauma_booster_module_t4"],
}
for p, ing in PART_RECIPES.items(): shapeless(p, [IRON] + ing, p)
IMPLANT_RECIPES = {
 "warp_dancer": ["os_frame_t1", "temporal_microdriver_t1"],
 "zetatech": ["warp_dancer", "os_core_t2", "temporal_regulator_t2"],
 "dynalar": ["zetatech", "os_core_t3", "phase_stabilizer_t3"],
 "falcon": ["dynalar", "os_core_t4", "falcon_overclocker_t4", "latency_shunt_t4", "neural_matrix_t4"],
 "apogee": ["falcon", "os_core_t5", "apogee_overdrive_kernel_t5", "quantum_sync_lattice_t5"],
 "axolotl": ["neural_matrix_t4", "cooldown_optimizer_t4", "minecraft:axolotl_bucket"],
 "basic_kiroshi": ["ocular_socket_t2", "zoom_lens_t2"],
 "clairvoyant": ["basic_kiroshi", "hostile_scan_kernel_t3"],
 "doomsayer": ["basic_kiroshi", "hazard_scan_kernel_t3"],
 "stalker": ["basic_kiroshi", "trail_tracker_array_t3"],
 "the_oracle": ["clairvoyant", "hazard_scan_kernel_t3", "trail_tracker_array_t3", "optic_fusion_controller_t4"],
 "gorilla_arms": ["kinetic_actuator_t1", "myofiber_bundle_t2", "arms_chassis_t3", "arms_actuator_matrix_t4", "arms_power_core_t4"],
 "blood_pump": ["hemo_chamber_t2", "bio_monitor_t2"],
 "fortified_ankles": ["kinetic_actuator_t1", "joint_servo_t1", "minecraft:iron_boots"],
 "reinforced_tendons": ["myofiber_bundle_t2", "joint_servo_t1", "skeletal_anchor_t1"],
 "titanium_bones": ["skeletal_anchor_t1", "minecraft:iron_block"],
 "bionic_joints": ["joint_servo_t1", "joint_servo_t1"],
 "hollow_bones": ["hollow_lattice_t2", "skeletal_anchor_t1"],
 "scarab": ["scarab_shell_plate_t2", "skeletal_anchor_t1"],
 "scar_coalescer": ["coagulation_mesh_t2", "skeletal_anchor_t1"],
 "dense_marrow": ["marrow_pressure_core_t3", "skeletal_anchor_t1"],
 "kinetic_frame": ["kinetic_damper_frame_t3", "skeletal_anchor_t1"],
 "spring_joints": ["spring_recoil_array_t3", "joint_servo_t1"],
 "para_bellum": ["ballistic_spine_plate_t3", "titanium_bones"],
 "universal_booster": ["trauma_booster_module_t4", "skeletal_anchor_t1"],
 "epimorphic_skeleton": ["regen_nanite_scaffold_t5", "skeletal_anchor_t1"],
 "rara_avis": ["iconic_skeletal_chassis_t5", "para_bellum"],
}
for i, ing in IMPLANT_RECIPES.items(): shapeless(i, [IRON] + ing, i)

# ---- damage type + tags
w(f"{D}/constructchip/damage_type/cyberpsychosis.json", {"message_id": "constructchip.cyberpsychosis", "exhaustion": 0.0, "scaling": "never"})
for tag in ("bypasses_armor", "bypasses_invulnerability", "bypasses_effects", "bypasses_resistance", "bypasses_enchantments"):
    p = f"{D}/minecraft/tags/damage_type/{tag}.json"
    obj = json.load(open(p)) if os.path.exists(p) else {"values": []}
    if "constructchip:cyberpsychosis" not in obj["values"]: obj["values"].append("constructchip:cyberpsychosis")
    w(p, obj)

lang.update({
 "death.attack.constructchip.cyberpsychosis": "%1$s flatlined under the cyberware overload",
 "itemGroup.constructchip.cyberware": "Cyberware",
 "cyberslot.constructchip.os": "OS", "cyberslot.constructchip.cortex": "Cortex", "cyberslot.constructchip.face": "Face",
 "cyberslot.constructchip.arms": "Arms", "cyberslot.constructchip.circulatory": "Circulatory", "cyberslot.constructchip.legs": "Legs",
 "cyberslot.constructchip.skeleton": "Skeleton",
 "tooltip.constructchip.cyber.slot": "%s slot · Load %s · Tier %s",
 "tooltip.constructchip.cyber.upgrade": "Upgrade %s/%s",
 "tooltip.constructchip.cyber.cooldown": "Cooldown %ss",
 "tooltip.constructchip.part.tier": "Tier %s part",
 "tooltip.constructchip.immuno": "Right-click: 30 min of immunosuppression. +30% cyberware capacity (+10% per extra dose), drains pressure, can roll back cyberpsychosis.",
 "element.constructchip.thermal": "Thermal", "element.constructchip.electric": "Electric", "element.constructchip.toxic": "Toxic", "element.constructchip.frozen": "Frozen",
 "message.constructchip.arms.element": "Gorilla Arms element: %s",
 "message.constructchip.arms.not_deployed": "Deploy your Gorilla Arms first",
 "message.constructchip.arms.fury": "Not enough Fury for a Slam (%s/50)",
 "message.constructchip.cyber.locked": "Your cyberware is locked",
 "message.constructchip.cyber.disabled": "Your cyberware is short-circuited",
 "message.constructchip.cyber.none": "No implant in your %s slot",
 "message.constructchip.cyber.no_scan": "These optics have no scan mode",
 "message.constructchip.cyber.nothing_active": "No active implants",
 "message.constructchip.override.none": "Your host has no Sandevistan or Blood Pump ready",
 "message.constructchip.psychosis.phase": "Cyberpsychosis: Phase %s",
 "message.constructchip.psychosis.error.0": "ERR 0x4F: neural desync",
 "message.constructchip.psychosis.error.1": "WARN: implant handshake failed",
 "message.constructchip.psychosis.error.2": "ERR: IFF table corrupted",
 "message.constructchip.psychosis.error.3": "SYS: who is watching?",
 "message.constructchip.scan.implant": "Chrome: %s",
 "message.constructchip.scan.psychosis": "Cyberpsychosis phase %s",
 "message.constructchip.surgery.cost": "Needs %s × %s",
 "container.constructchip.surgery": "Ripperdoc",
 "gui.constructchip.surgery.load": "LOAD %s / %s",
 "gui.constructchip.surgery.overload": "OVERLOAD: cyberpsychosis accelerates",
 "gui.constructchip.surgery.psyche": "Cyberpsychosis %s · pressure %s",
 "gui.constructchip.surgery.mult": "Capacity Lv %s",
 "gui.constructchip.surgery.capacity": "Capacity +",
 "gui.constructchip.surgery.capacity.tip": "Raise capacity to level %s: 3 × Internal Parts T%s",
 "gui.constructchip.surgery.upgrade": "Upgrade to %s: %s × Internal Parts T%s",
 "hud.constructchip.chrome.load": "LOAD %s/%s",
 "hud.constructchip.chrome.phase": "CYBERPSYCHOSIS %s",
 "hud.constructchip.chrome.stable": "STABLE",
 "hud.constructchip.chrome.immuno": "IMMUNO %sm",
 "hud.constructchip.chrome.terminal": "TERMINAL %ss",
 "hud.constructchip.chrome.host": "Host chrome %s/%s · psychosis %s",
 "key.constructchip.cyber_os": "Sandevistan",
 "key.constructchip.cyber_arms": "Gorilla Arms deploy",
 "key.constructchip.cyber_pump": "Blood Pump",
 "key.constructchip.zoom": "Optics zoom (hold)",
 "key.constructchip.scan": "Scan / optics scan",
 "effect.constructchip.bleed": "Bleed", "effect.constructchip.cryolock": "Cryolock", "effect.constructchip.electroshock": "Electroshock",
 "effect.constructchip.cyberpsychosis": "Cyberpsychosis", "effect.constructchip.immunosuppression": "Immunosuppression",
 "entity.minecraft.villager.constructchip.ripperdoc": "Ripperdoc",
 "quickhack.constructchip.chrome_override": "Chrome Override",
})
for k in list(json.load(open(EDGE_LANG)).keys()):
    pass
for ev in ["sandevistan_activation","sandevistan_exit","postripperdoc","gorilla_arms_throw","ground_slam_weak","ground_slam_strong",
           "immunosupresors_inject","heal_bloodpump","activate_optics","deactivate_optics","extend_shrink_gorilla_arms",
           "cyberpsychosis_stinger_phase","cyberpsychosis_stinger_critical","cyberpsychosis_stinger_watcher","cyberpsychosis_stinger_hallucination",
           "cyberpsychosis_stinger_episode","cyberpsychosis_channel_visor","cyberpsychosis_channel_system","cyberpsychosis_channel_motor",
           "cyberpsychosis_channel_sync","cyberpsychosis_heartbeat","cyberpsychosis_whisper"]:
    lang["subtitles.constructchip.er." + ev] = ev.replace("_", " ").capitalize()
json.dump(dict(sorted(lang.items())), open(lang_p, "w"), indent=2, ensure_ascii=False); open(lang_p, "a").write("\n")
print("implants", len(IMPLANTS), "parts", len(PARTS))
