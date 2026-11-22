# GamblerHud

Minecraft 1.21.11 Fabric client mod. Made by Onxon.

## Features

- **UI**: Menu key (default G, change it in the menu), menu and accent colours, text boxes with font, colour, size, shadow and snap position, and a Layout mode to drag and resize every box.
- **Profit**: Session or all-time profit, a +/- box (green up, red down), a balance graph with a zero baseline, and boxes for who paid you most and who you paid most.
- **Auctions**: Pick any item by searching and clicking its picture, set a duration and a worth. The top 3 single payments are shown live, the timer changes colour, and profit = money in - money out - worth.
- **Sessions**: New session resets the session stats and archives the old one. Browse the history, or switch to all-time totals.

## Building on GitHub

1. Create a new GitHub repository and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab. The "Build GamblerHud" workflow runs on every push.
3. When it finishes, download the **GamblerHud** artifact. It contains the jar for `mods/`.

To build locally: `gradle build` (Gradle 8.14+, Java 21).

## Payment detection

Payments are read from chat messages with two regular expressions in `config/gamblerhud/config.json`:

- `incomingRegex`: default matches `Name paid you 500` / `Name sent you 1.5m`
- `outgoingRegex`: default matches `You paid Name 500`

If Donut SMP words its payment messages differently, edit these two patterns. Each one needs three groups: player name, amount, and an optional k/m/b suffix.

## Notes

- Versions: Minecraft 1.21.11, Fabric Loader 0.18.1, Fabric API 0.139.5+1.21.11, Loom 1.14. These are set in `gradle.properties`.
- Data is saved to `config/gamblerhud/`: `config.json` for settings and layout, `sessions.json` for session history.
