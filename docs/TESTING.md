# Manual test plan (steps 1-2)

Two clients (or a client + a second account): **A** = host/admin, **B** = construct. Run `/construct chips` after each step to see the registry.

1. `/construct givechip A B`. A gets a glowing chip and B goes spectator with their camera on A. `chips` shows `carried by A`.
2. A presses Q. B's camera moves to the item on the ground and the registry shows `dropped`. B (still spectator) can't pick it up.
3. A picks it up again. The registry shows `carried by A`, and the chip is NOT wiped (pickup same-tick check).
4. A right-clicks with the chip, or runs `/construct install`. The chip leaves the inventory, the registry shows `installed`, B is un-anchored and back in their old game mode.
5. A runs `/construct eject`. The chip is back in the inventory, its serial is +1, and B is anchored again.
6. Dupe: in creative, A middle-click copies the chip. One copy becomes blank within a tick. Copy, then install the other: the leftover copy goes blank.
7. Containers: try to put the chip in a chest, an ender chest, a shulker, a bundle, the crafting grid and a decorated pot. All should be refused.
8. Install, then `/kill A`. The chip drops with A's loot even with `/gamerule keepInventory true`, and B's camera moves to it.
9. Throw the chip into lava, a cactus, and the void (in the End). It survives and comes back to the last ground position.
10. A logs out while carrying it. After about 5 s the registry shows `stored`, and B is held at A's logout spot. A logs back in: `carried`.
11. `/kill @e[type=constructchip:chip_item]`. B is released (the chip is confirmed gone).
12. `/construct release B` while installed. A gets a blank chip back and B is restored. Restart the server while B is offline, then B logs in and gets cleaned up.
13. `/construct optout` on A, `/gamerule allowConstructCapture false`. Right-clicking another player with a chip reports the reason.
14. Skills: hold the chip, `/construct skill list`, `/construct skill points 10`, `/construct skill unlock constructchip:overclock` (should fail: Mk.1), `/construct skill tier 2`, then unlock again. Vista skills should only be listed when Vista is installed (this checks that conditions work on datapack registries).
15. XP cost: `/xp set @s 0 levels`, hold the chip, `/construct skill buy` (should fail), `/xp add @s 10 levels` (160 XP),
    `/construct skill buy` (works, and you drop to level 0), then `/construct skill price` (should say 192 XP for the next one).
    Install the chip and buy again without holding it. Check the price keeps going up after eject and a hand-over.

## Step 3: quickhack validator (Player1 types everything; `/execute as Player2 run ...` runs it as the construct)
Results show up in **Player2's** chat (failures on Player2's action bar). Player1 only gets a one-line confirmation.
16. Give Player2 a chip and install it on Player1 (`/construct givechip Player1 Player2`, then right-click).
    As Player1, look at a stone block and run `/execute as Player2 run construct hack constructchip:scanner look`.
    Player2's chat should show the stone's info: hardness 1.5, pickaxe, wood tier.
17. Look at a cow and run the same command: health, armor, and `Player1 damage: 1.0`. Hold a sword and scan again; the damage should go up.
18. Failures. Each should give a clear message on Player2's action bar:
    - run it twice quickly (cooldown 0.5 s)
    - look at the sky, or at something more than 16 blocks away: "Nothing within 16 blocks"
    - `... scanner block <x y z of an air block>`: "Nothing there to target" (air is never scanned)
    - `... scanner block <x y z of a block 30 blocks away>`: out of range
    - `... scanner entity @e[type=cow,limit=1,sort=nearest]` with a wall in between: line of sight
    - run it with Player2's chip ejected (not installed)
    - `/execute as Player1 run construct hack constructchip:scanner look` (not a construct)
19. Deep Scan: hold the chip (eject it first), `/construct skill points 5`, `/construct skill unlock constructchip:deep_scan`, reinstall.
    Blocks 20 blocks away should now scan with `look` (range 24).

## Step 4: capture snapshot and restore
20. Give Player2 a mixed inventory first: armor on, something in the offhand, a full hotbar, enchanted items, a filled shulker box,
    some XP levels (`/xp add Player2 25 levels`), an effect (`/effect give Player2 speed 600`), and put them in survival
    in the Nether. Lower their health a bit. Then `/construct givechip Player1 Player2`.
    Player2 should end up with an empty inventory, 0 XP, no effects, in adventure (then spectator while the chip is loose).
21. `/construct snapshot Player2` should list what was saved (stack count, level 25, survival, the Nether, health, effects).
22. `/construct release Player2`. Every item should be back in the same slot (armor and offhand too), plus level 25, the speed effect with its
    remaining time, the same health and hunger, survival, and the exact spot and facing in the Nether.
23. Offline release: capture again, have Player2 disconnect, `/construct release Player2`, Player2 rejoins. They should be fully restored on join.
24. Crash: capture Player2, then close the Player1 window without saving (or kill the terminal). Start again. Player2 should
    still be a construct with the snapshot intact (`/construct snapshot Player2`), and releasing should still restore everything.
25. Player2 carrying someone else's captured chip when captured: that chip should drop on the ground (still tracked) and not vanish.
26. `/construct givechip Player2 Player2` should be refused (a construct can't carry their own chip).

## Steps 5-11 (full mod)
Automated checks in CI already cover: capture/restore, install, the Scanner checks, the dupe guard, skill rules and prices,
Overclock and lockout, and that the server and client start with Vista. Check these by hand:

**Ghost (5):** capture Player2 and install the chip on Player1.
27. A third client (or Player2 un-captured, watching) must NOT see Player2. Player1 must see them. No name tag for others.
28. Player2 can fly, makes no footstep sounds, can't open doors or chests, break or place blocks, attack, drop or pick up items,
    or take damage (lava, fall, mobs). Mobs ignore them. Player2 doesn't push Player1 and arrows pass through them.
29. Fly Player2 more than 16 blocks away: they snap back to Player1. Player1 goes through a portal: Player2 follows.

**HUD + inventory (6):**
30. Player2 sees top-left: host name, Player1's health, food and armor, the RAM bar, the selected hack. Player1 sees a one-line chip summary.
31. Player2 presses `H`: Player1's inventory shows up, updates live, and nothing can be taken, moved or shift-clicked.

**Hacks (7-8):** use `/construct skill points 40` and `/construct skill tier 3` on the chip (eject, hold, set, reinstall),
then unlock everything in the `K` screen.
32. `V` (scan) on blocks and mobs; the scanned mob glows briefly for Player2 only. With Deep Scan: effects, gear, blast resistance.
33. Overclock: Player1 gets Regeneration II, Strength and Resistance for 30 s, and hacks cost half (watch the HUD). Then
    LOCKED OUT for 60 s and every hack refuses.
34. Ping (hostiles glow for both), Weak Point (more damage from Player1), Short Circuit / System Lock (slowness, a jolt, buffs removed),
    Adrenal Surge, Remote Switch (lever, button, wooden and iron door, iron trapdoor, fence gate), Signal Inject (aim at a
    redstone lamp or piston face: it fires for 1 s).
35. Threat Alert: a zombie targeting Player1 shows a direction warning plus a bell. Pain Editor: Player1 takes 10% less damage.
36. Vitals Link: Player1's effects and nearly broken gear appear on Player2's HUD.

**Skill tree (9):** 37. Click-unlock works for both players; locked nodes explain why; only Player1 sees "Buy point".

**Vista (10):** 38. With a Vista TV and View Finder: TV Override toggles the TV and holds it for 60 s against redstone;
Camera Jack puts Player2 behind the camera; Camera Blind cuts the feed for 20 s and then it comes back.

**Soul Killer + consent (11):** follow "Capturing someone in survival" in RUNNING.md, then:
39. Decline, wait out the 30 s timeout, stand up mid-capture, turn the redstone off, break the chair, and walk the operator away:
    each one aborts and returns the blank chip.
40. Quit the game mid-capture and reload: the machine resets and returns the chip; the player isn't captured.
41. Right-click another player with a captured chip: they get a consent screen; accept installs, decline doesn't.
42. Mk.2 / Mk.3 chips (cyan / gold) move the construct up a tier and recolor it (wrong tier is refused); recipes show up in the recipe book.

## Cyberware
Automated in CI: load/capacity/conflicts, Blood Pump heal + cooldown, cyberpsychosis climb + immuno rollback, Short Circuit.
By hand (creative tab "Cyberware" has everything):
43. Sneak + right-click the Soul Killer: surgery screen; install Warp Dancer, Gorilla Arms, Kiroshi, Blood Pump, Tendons and two skeleton
    pieces; Hollow Bones refuses Titanium Bones next to it. The load bar turns red over capacity. Close: glitch + Ripperdoc sound.
44. X: Sandevistan slows nearby mobs, you speed up; HUD shows the timer and then the cooldown. B: arms out, punch a zombie (bleeds,
    Fury rises); at 50 Fury right-click air = Slam. Bare-handed iron ore mining works with arms out.
45. V with Clairvoyant/Stalker/Doomsayer/Oracle: hostiles glow / trails / hazard markers. Hold C to zoom. Aiming shows enemy HP.
46. J heals. Double jump with Tendons; crouch-charge + jump with Ankles; no fall damage from either.
47. With chrome installed, food doesn't heal. Overload yourself: pressure and cyberpsychosis climb, phases show symptoms;
    immunosuppressants pull you back. In Phase V the terminal countdown kills you, and you respawn in Phase IV.
48. Villager next to the station becomes a Ripperdoc with the trades listed in RUNNING.md.
49. As a construct with Chrome Link: host chrome line on the HUD. Chrome Override fires the host's Sandevistan. Short Circuit
    a chromed player: their Sandevistan/arms stop and go on cooldown.
50. Implant models visible on yourself (F5) and on the other player; toggles in constructchip-client.toml hide each one. Normal arms
    vanish under the Gorilla Arms in third person, first-person hands stay normal. Electric arms use the electric texture.
51. Grab & throw a zombie with the arms out; it takes damage on impact. Bosses can't be grabbed.
52. Painting station: paint the Gorilla Arms, Save, install: the paint shows on your arms (also for the other player).
53. Guide opens and pages flip. Phase V: berserk episode takes control near a mob, you can't be hurt, arrows bounce off.
    Phase II+: red eyes in the dark. Phase III+: other players' name tags read HOSTILE.
