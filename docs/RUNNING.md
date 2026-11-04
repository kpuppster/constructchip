# Running the mod on your own computer (first-time guide)

You don't need to install Minecraft, NeoForge or the mod by hand. The project ships with a launcher script
(`gradlew`) that downloads everything and opens a test copy of Minecraft with the mod loaded. That test copy is separate
from your normal Minecraft and doesn't touch your saves.

## 1. One-time setup

### Install Java 21
1. Go to https://adoptium.net/temurin/releases/?version=21
2. Choose your system (Windows x64, macOS, Linux) and download the **JDK** installer (`.msi` on Windows, `.pkg` on macOS).
3. Run it. **On Windows**, in the "Custom Setup" step, click the red ✗ next to **Set JAVA_HOME variable** and pick
   "Will be installed on local hard drive". Also keep "Add to PATH" on.
4. Check it worked: open a **new** terminal (Windows: press Start, type `PowerShell`, open it. macOS: open
   the Terminal app) and run:
   ```
   java -version
   ```
   It should say `openjdk version "21...`. If it says "not recognized" or shows another version, restart your computer and try again.

### Install Git (recommended)
- Windows: https://git-scm.com/download/win. Run the installer and click Next through all the defaults.
- macOS: run `git --version` in Terminal. If Git isn't installed, macOS offers to install it.

(No Git? On https://github.com/kpuppster/constructchip click the green **Code** button → **Download ZIP**, and unzip it
somewhere easy like your Documents folder. You'll have to download it again each time the code changes.)

### Download the project
In PowerShell / Terminal:
```
cd ~/Documents
git clone https://github.com/kpuppster/constructchip
cd constructchip
```
The repository is public, so no sign-in is needed.

## 2. Start the game

From inside the `constructchip` folder:

| | Windows (PowerShell) | macOS / Linux |
|---|---|---|
| Player 1 | `.\gradlew.bat runClient` | `./gradlew runClient` |
| Player 2 (second window) | `.\gradlew.bat runClient2` | `./gradlew runClient2` |

- **The first run is slow** (5–20 minutes). It downloads Minecraft, NeoForge and its tools (about 1 GB). Later runs take
  about a minute. Lots of text scrolling past is normal. Wait until the Minecraft window appears.
- Leave the terminal open while you play. Closing the game window ends the command.
- To check the mod loaded: on the title screen click **Mods**. "Personality Construct" should be in the list.

## 3. Set up a two-player test

Most tests need a host (**Player1**) and someone to capture (**Player2**). Both run on your one computer.

1. In the **Player1** window: Singleplayer → Create New World. Set **Game Mode: Creative**, turn **Allow Commands: ON**,
   then Create.
2. Press `Esc` → **Open to LAN** → keep **Allow Commands ON** → **Start LAN World**. The chat shows a port number,
   e.g. `Local game hosted on port 52345`.
3. Open a **second** PowerShell/Terminal window, `cd` into the same `constructchip` folder, and run the Player 2 command
   from the table above.
4. In the **Player2** window: Multiplayer → (Proceed) → **Direct Connection** → type `localhost:52345` (your port) → Join Server.
5. Switch between the two windows with `Alt+Tab` (Windows) or `Cmd+Tab` (macOS). Player1 has commands; Player2 doesn't
   need them.

Type commands in **Player1's** chat (press `T`, then type, e.g. `/construct givechip Player1 Player2`, then Enter).

## 4. What to try

Follow `docs/TESTING.md` from top to bottom. In it, **A = Player1** and **B = Player2**. Useful extras:
- `/construct chips` shows where every chip is, at any time.
- `/construct chipinfo` (while holding a chip) shows its RAM, skills and serial.
- Press `F3 + H` to turn on advanced tooltips, which show the chip's id when you hover over it.

If something goes wrong, note what you did, what you expected, and what happened. Then copy the end of
`run/logs/latest.log` (Player2: `run-client2/logs/latest.log`) and paste it to Claude.

## 5. Getting new versions

When new code has been pushed, close both game windows and run in the project folder:
```
git pull
```
Then start the game again as in step 2.

## Common problems

- **"JAVA_HOME is not set" / "java is not recognized"**: Java isn't on your PATH yet. Reinstall with the JAVA_HOME
  option (step 1), then open a *new* terminal or restart your computer.
- **"Permission denied" on macOS/Linux**: run `chmod +x gradlew` once.
- **"Failed to connect" from Player2**: use the exact port from Player1's chat, and type `localhost:` in front of it. Open to LAN has to be done again each time Player1 reloads the world.
- **"Name already taken" / Player2 kicked straight away**: make sure the second window was started with `runClient2`, not `runClient`.
- **The build fails right away with download errors**: check your internet connection and run the command again. Gradle picks up where it stopped.
- **The game is very slow with two windows open**: that's normal on smaller computers. Lower the render distance in Options → Video Settings for both.

## Playing with the full mod

Vista is downloaded and loaded automatically in the test game. To test without Vista, add
`-PnoVista` to the command, e.g. `.\gradlew.bat runClient -PnoVista`.

### Controls (change them in Options → Controls → Personality Construct)
| Key | What it does | Who |
|---|---|---|
| `R` | Use the selected quickhack on what you're looking at | construct |
| `Z` | Next quickhack (hold sneak for previous) | construct |
| `V` | Scan what you're looking at | construct |
| `H` | Look at your host's inventory (read-only) | construct |
| `K` | Skill tree | construct and host |

### Capturing someone in survival (the Soul Killer chair)
1. Craft a **Soul Killer** and a **Personality Construct Chip** (vanilla items; see the recipe book), or take them
   from the creative tab "Personality Construct".
2. Place the Soul Killer and power it with redstone: a lever, a redstone block or a redstone torch next to it.
   It's a big ripperdoc station (about 5 x 4 blocks of lights, arm, table and stool around the chair), so give it room.
   Only the chair itself is solid.
   If the sitter doesn't line up with the cushion, edit `riderX`, `riderY`, `riderZ` under `[soul_killer]` in
   `run/saves/<world>/serverconfig/constructchip-server.toml`, then sit down again.
3. The person to capture right-clicks the chair with an **empty hand** to sit in it.
4. Right-click the chair with a **blank chip**. The seated player gets a consent screen. If they accept, they have to
   stay in the chair for 5 seconds, and then the chip with them on it goes into your inventory.
5. Right-click with the chip to install it in yourself. Or right-click another player, who will be asked to agree.

### Upgrading
- **Skill points:** the host presses `K` and clicks "Buy point" (paid with the host's XP: 30 XP for the first, about 5% more each after that).
- **Mk.2 / Mk.3:** craft a blank Mk.2 or Mk.3 construct chip (cyan or gold) and use it with the construct chip in your other hand, or while the chip is installed in you. The construct moves onto the new chip.

## Cyberware (ported from Project: Edgerunner)

**Getting started.** Put a Soul Killer station near an unemployed villager: they become a **Ripperdoc**. Trade for the
**Soldering Iron** (18 emeralds + 12 iron), **Immunosuppressants** and **Internal Parts T1-T5**. Every part and implant is
crafted in a normal crafting table with the Soldering Iron in the grid (you get it back). Check the recipe book.

**Surgery.** Sneak + right-click the Soul Killer station with an empty hand. Drag implants into body slots: OS, Cortex,
Face, Arms, Circulatory, Legs, 2× Skeleton. The bar shows **Load / Capacity** (capacity is rolled 160-320 per player).
Going over capacity speeds up cyberpsychosis. The **+** buttons upgrade an implant, and **Capacity +** raises capacity
(both cost Internal Parts). Changes apply when you close the screen.

**Keys** (if you aren't a construct): `X` Sandevistan · `B` Gorilla Arms · `J` Blood Pump · `V` optic scan · hold `C` zoom ·
hold `G` chrome wheel · right-click with an empty hand while the arms are out = Slam · double jump (Tendons) / crouch-charge
then jump (Ankles).

**Cyberpsychosis.** Running implants builds pressure, and high pressure grows the disease (bottom-left HUD). There are 5 phases,
from glitches to hallucinations to motor trouble, up to a 120 s **terminal** countdown in Phase V. Immunosuppressants buy capacity
and roll it back. With any implant installed, **food stops healing you**.

**Constructs and chrome.** Short Circuit / System Lock knock out a player's implants. Deep Scan lists them. New Support skills:
**Chrome Link** (see your host's load and cyberpsychosis), **Neural Buffer** (slows your host's cyberpsychosis) and
**Chrome Override** (quickhack: fire your host's Sandevistan or Blood Pump). A construct chip in your head counts 20 Load.

**More chrome:**
- **Implant models** show on your body (Sandevistan spine, Gorilla Arms, Blood Pump, Ankles, Tendons). Turn any of them off in
  `config/constructchip-client.toml` (`[implant_models]`).
- **Grab & throw** (Gorilla Arms out, empty hand): right-click a creature to lift it, right-click again to throw it. It's hurt
  when it hits something; below 30% health the throw finishes it.
- **Cyberware Painting Station**: right-click, put an implant in the slot, paint its texture (brush, bucket, eraser, picker,
  undo, `<>` switches texture), **Save**. The paint shows on your body for everyone.
- **Edgerunner's Guide** (book + redstone + iron nugget): the full manual in game.
- **Phase V berserk**: during the terminal window the chrome sometimes takes over. Third person, it hunts the nearest
  creature or player and you can't steer, but nothing can hurt you.

### The construct talking to its host
While the construct's chip is installed in someone, anything the construct types in chat goes **only to that host**. It
doesn't appear in chat. The host sees it in a speech box next to the chat area, under the construct's name.

### Lightning
Every quickhack now fires red lightning from the host's hand to the target (the Scanner shows a thin pale strand, and
hacks without a target make sparks crawl over the host). **Fry** (the mob or player nearest your crosshair) and **High Voltage** (that target and every mob or player within 6 blocks of it, with bolts
out of the sky) have bigger versions, and the Soul Killer pours lightning into the sitter while it
captures them.
