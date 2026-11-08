# Credits

- **Ikeda** ([Capri](https://www.curseforge.com/minecraft/mc-mods/capri-chip)): chip art (item and illustration),
  chip panel GUI texture, glitch post shader (`capri_glitch.fsh`, `capri_screen.vsh`) and sound effects. Used with the author's
  permission. Their sounds are made from CC0 recordings listed in `src/main/resources/licenses/SOUNDS_CC0.txt`.
  The mod logo is also their chip art.
  Fry, High Voltage, Patch Job and Ping's side count are our own implementations of ideas from that mod.
- **A24-N22** ([Project: Edgerunner](https://www.curseforge.com/minecraft/mc-mods/project-edgerunner)): the Soul Killer
  station model, texture, idle animation and sitting animation. Used with the author's permission. Nothing else from that
  mod is included.
- **GeckoLib** renders the station model; **PlayerAnimator** (KosmX) plays the sitting animation (also by A24-N22).
- **Create** (MIT): the Soul Killer's seat positioning follows the approach of Create's seats.
- **Rajdhani** font (HUD text): Copyright (c) 2014, Indian Type Foundry. SIL Open Font License 1.1, see
  `src/main/resources/licenses/OFL-Rajdhani.txt`.

- Electric crackle and zap sounds (`crackle_1-4.ogg`, `zap.ogg`) are our own, synthesized by `scripts/gen_crackle.py`. CC0.
- The quickhack lightning renderer follows Ikeda's Capri bolt look (path shape, widths, branching, flicker), re-implemented in our code with permission.
- **breenihilation** ([Create: Chromeware](https://www.curseforge.com/minecraft/mc-mods/cyberpunk2077-mod)): optional
  compatibility only, with the author's permission. When it is installed, Short Circuit, System Lock and Chrome Override
  call Chromeware's own quickhacks and implant abilities. Nothing from it is included in this mod; it is fetched at
  build time to compile against (CurseMaven) and never shipped.
