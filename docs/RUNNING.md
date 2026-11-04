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
3. The person to capture right-clicks the chair with an **empty hand** to sit in it.
4. Right-click the chair with a **blank chip**. The seated player gets a consent screen. If they accept, they have to
   stay in the chair for 5 seconds, and then the chip with them on it goes into your inventory.
5. Right-click with the chip to install it in yourself. Or right-click another player, who will be asked to agree.

### Upgrading
- **Skill points:** the host presses `K` and clicks "Buy point" (paid with the host's XP; each point costs more).
- **Mk.2 / Mk.3:** craft a chip module and use it with the chip in your other hand, or while the chip is installed in you.
