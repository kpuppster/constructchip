# Personality Construct Chip: design notes

These came from a planning conversation. Treat them as decisions unless the user changes them.
Anything marked **VERIFY** was inferred, not tested.

A player is captured onto a "personality construct" chip (in the style of Capri from Cyberpunk: Edgerunners 2).
The captured player lives in a host's mind, is visible only to the host, and acts only through RAM-costing
"quickhacks", like a cyberdeck.

## Core decisions
- The construct is a real `ServerPlayer`. Hide them on every client except the host's with a client-side render cancel
  (`RenderPlayerEvent.Pre`, `RenderNameTagEvent`) plus a sync payload saying "player X is a construct of host Y".
  Do not use the vanilla invisibility flag. The hide is client-side only (accepted: a hacked client could still see them).
- The construct is adventure-mode-like and invulnerable. Vanilla interaction is cancelled with events (PlayerInteractEvent.*,
  AttackEntityEvent, BlockEvent.BreakEvent, ItemTossEvent, pickups, LivingChangeTargetEvent). Quickhacks are the only way
  to affect the world. Leash the construct to the host by distance. Turn collisions off (team with CollisionRule.NEVER, or a mixin).
- Consent: a game rule `allowConstructCapture` (global kill switch) plus a per-player opt-out attachment (`/construct optout`,
  copyOnDeath). The server re-validates everything on the consent reply (still seated, in range, not timed out). Admin override,
  `/construct release <player>`.
- Capture machine ("Soul Killer") is a Create addon (Create is a hard dependency). **Build it LAST.** Reuse Create's SeatBlock/SeatEntity
  if possible. States: IDLE, SEATED, AWAITING_CONSENT, CAPTURING, DONE.
- Before capture, save inventory, XP, gamemode and position into a persistent attachment so release can always restore them.

## The chip
- While in use the chip is NOT an inventory item. "Install" moves the stack into a host attachment (`installedChip`, `ItemStack.OPTIONAL_CODEC`).
  Eject returns it. Re-call `setData` after mutating the stack. No copyOnDeath.
- Components: `ChipIdentity(chipId, capturedUuid, capturedName)`, `ChipRam(max, regenPerSecond, stored, stampTick)`,
  `ChipSkills(tier, points, unlocked)`, `ChipTimers(overclockUntil, lockoutUntil, cooldowns)`.
- RAM lives on the chip. Lazy regen: `current = min(max, stored + regen * (now - stamp) / 20)` with `level.getGameTime()`.
  No per-tick writes; sync to the client only on spend.
- Dupe guard: a unique `chipId` generated at capture, stored on the chip and in the captured player's attachment. Only the registered stack is valid.

## Chip tracking and host death
- Tracked by `chipId` in an overworld `SavedData` `ChipRegistry`: `ChipLocation(state, entityOrPlayer, GlobalPos, lastSeenTick)`,
  states INSTALLED, DROPPED, CARRIED, STORED, RELEASED.
- When the host dies, the chip drops with their drops (add it in `LivingDropsEvent`, clear the attachment). It always drops, whatever keepInventory says.
  The dropped entity is invulnerable and never despawns (custom `Item#createEntity`). Handle the void.
- In DROPPED/CARRIED/STORED: the construct is anchored (spectator camera on the item or carrier, re-applied from the server tick), with all abilities off and no pickups.
  Heartbeat via `Item#inventoryTick` + the custom ItemEntity tick.
- Anyone can pick it up. Installing on another player triggers their consent (game rule + blacklist). Self-install needs no extra consent.
- Containers: block the common routes (`overrideOtherStackedOnMe`, `overrideStackedOnOther`, `Slot#mayPlace` mixin). Fallback: the STORED state
  (anchor at last known position, admin release, optional timeout).
- Fail-safe: if the chip is confirmed gone, release the construct and restore their saved state.
- **DECIDED:** the construct gets NO veto when a new holder installs the chip. Only the new host's consent (game rule + opt-out) applies.

## Quickhacks and skills
- `Quickhack` interface (id, ramCost, cooldownTicks, range, targetType, execute) in a custom registry. A client keybind sends `UseQuickhackPayload`.
  The server checks that the sender is a construct, the hack exists and is unlocked, there is no lockout, and RAM, cooldown, range, LOS and host validity. Deduct RAM only on success.
- A transient "hack in progress" flag lets the cancel handlers pass hack interactions through. Remote interactions use the host's identity (permissions)
  and fire the normal events so other mods can veto them.
- Skills are a datapack registry (`DataPackRegistryEvent.NewRegistry`) with `neoforge:conditions` (`mod_loaded`). **VERIFY** conditions work on datapack registries in 21.1.
- Base effects (always on): host status HUD (throttled `HostStatusPayload`) and a read-only host inventory view (custom menu, all slots locked).
- Tier 1 Scanner: client raycast + highlight; the server returns authoritative info (block name, tool tag, tier tag; entity name, health, armor, host's main-hand damage).
- Tier 2 Overclock: 30 s of heal, damage boost, slight damage reduction, half RAM costs, then a 60 s lockout of all abilities.
  `overclockUntil = now+600`, `lockoutUntil = now+600+1200`, stored on the chip.
- Cyberware: a `Hackable` interface (`canShortCircuit`, `shortCircuit(ticks)`) with per-wearer disable timers in an attachment.

## Vista compat (optional)
Inspected `vista-1.21.1-5.6.2-neoforge.jar`. It requires Moonlight Lib (>= 1.21.1-3.4.0). All Vista references go in one compat package behind
`ModList.get().isLoaded("vista")`, with `type="optional"` in mods.toml. License: Supplementaries Team License 1.5. Depending on it is OK; never bundle the jar or reuse its assets.
From class signatures only (**VERIFY** with a decompiler):
- Cameras: `net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlock` / `ViewFinderBlockEntity` (`controllingEntity`, `setCurrentUser`, `AdventureModeOperation`).
- TVs: `common.tv.TVBlock.POWER_STATE` (`PowerState` OFF/DIRECT/INDIRECT). `neighborChanged` may revert a forced state, so add a "forced until tick" timer or a hook.
- Camera view: client `ViewFinderController.startControlling` via `ClientBoundControlViewFinderPacket(TileOrEntityTarget)` (Moonlight `NetworkHelper.sendToClientPlayer`).
  `ViewFinderBlock.startViewing` is package-private. Check `isAdventureNoInteraction`.
- Chunks: Vista has `ServerCameraChunkManager`. **VERIFY** it covers direct control from far away.
- Blind: candidate is `BroadcastManager.getInstance(level).unlinkFeed(...)`, then `linkFeed(...)`. **VERIFY** that TVs show static.
- Vista mixes into ChunkMap, TrackedEntity, ChunkTrackingView, Camera, LocalPlayer, Player. Avoid our own entity-tracking mixins.
- Planned hacks: `tv_power`, `camera_view`, `camera_blind`.

## Reference mod (do NOT copy)
`sorcery_age_incarnation` (Forge 1.20.1, ARR, requires the JJK mod). Use it only as a design cross-check. Ask MrShrimpyUW before reusing anything.

## Build order
1. Chip components, chip item, `/construct givechip`. **(done, not yet compiled)**
2. Install/eject with the chipId check, `ChipRegistry`, anchor logic (test via drop/pickup/stash commands)
3. Skill registry + central hack validator
4. Save/restore of captured-player state (test heavily by command)
5. Ghost restrictions, host link, client render hiding, sync payload
6. Host HUD + read-only inventory menu
7. Scanner, then Overclock and lockout
8. `Hackable` + cyberware skills
9. Skill tree screen
10. Vista compat
11. Create Soul Killer, consent screen, polish

Edge cases to design in from the start: host/construct logout, death, dimension change, a server crash mid-capture (self-heal on login),
the chip destroyed/dropped/stored, and release always restoring inventory and position.

## Setup
- Official NeoForge MDK (`NeoForgeMDKs/MDK-1.21.1-ModDevGradle`), MC 1.21.1, NeoForge 21.1.x.
- Reference jars (Vista, Moonlight, Create) go in `libs/` or `reference/` (gitignored, never shipped).
- Create's Maven coordinates change between releases, so check Create's addon docs.
