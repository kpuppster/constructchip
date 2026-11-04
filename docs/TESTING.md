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
