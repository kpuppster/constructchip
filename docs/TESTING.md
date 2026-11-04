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
16. Give Player2 a chip and install it on Player1 (`/construct givechip Player1 Player2`, then right-click).
    Stand near a stone block and a cow. Run `/execute as Player2 run construct hack constructchip:scanner block <x y z of the stone>`
    (press Tab to fill in the coordinates of the block you're looking at). Player2's chat should show the stone's info: hardness 1.5, pickaxe, wood tier.
17. Same with `entity @e[type=cow,limit=1,sort=nearest]`: health, armor, and how hard Player1 hits it. Then hold a sword and scan again; the damage should go up.
18. Failures. Each should give a clear message:
    - run it twice quickly (cooldown 0.5 s)
    - target a block more than 16 blocks away (out of range)
    - target a cow behind a wall (line of sight)
    - run it with no target
    - run it with Player2's chip ejected (not installed)
    - `/execute as Player1 run construct hack constructchip:scanner` (not a construct)
19. Deep Scan: hold the chip (eject it first), `/construct skill points 5`, `/construct skill unlock constructchip:deep_scan`, reinstall. Blocks 20 blocks away should now scan (range 24).

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
