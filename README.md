# ServerTools

**One jar. 120+ commands and features. Replaces a whole stack of plugins.**

**[100% Open Source](https://github.com/reecepbcups/ServerTools-MC)** - full source on GitHub. Audit it, fork it, or contribute.

ServerTools puts every core feature a server needs into a single modular plugin. Every module can be toggled off from the config, and disabled modules don't even register their listeners, so you only use compute for what you run.

Perfect for Skyblock, Factions, Survival, or Hub servers. Tested by over 6,000 unique players.

**Optional integrations:** [LuckPerms](https://luckperms.net/), [Vault](https://www.spigotmc.org/resources/vault.34315/), [PlaceholderAPI](https://hangar.papermc.io/HelpChat/PlaceholderAPI), [TAB](https://www.spigotmc.org/resources/tab-1-5-x-1-21-4.57806/), and [WorldGuard](https://enginehub.org/worldguard). All soft depends. ServerTools runs fine without them and lights up extra features when they're installed.

<p align="center">
  <img width="984" height="522" alt="ServerTools feature banner" src="https://github.com/user-attachments/assets/693f9c31-6d66-4995-af98-3a7516b5581c" />
</p>

[![Spigot](https://img.shields.io/badge/Spigot-Resource-orange)](https://www.spigotmc.org/resources/servertools-%E2%9E%9C-modular-server-management-1-8-1-21-open-source.95853/)
[![Hangar](https://img.shields.io/badge/Hangar-PaperMC-004ee9)](https://hangar.papermc.io/reecepbcups/ServerTools-MC)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1bd96a)](https://modrinth.com/plugin/servertools-mc)
[![Wiki](https://img.shields.io/badge/Docs-servertools.reece.sh-blue)](https://servertools.reece.sh/)
[![License](https://img.shields.io/badge/Source-100%25%20Open-brightgreen)](./LICENSE)
[![Discord](https://img.shields.io/badge/Discord-reecepbcups-5865F2)](https://discord.com/)

---

## Why ServerTools

- **One plugin instead of twenty.** Holograms, withdraw, tags, vouchers, chat formatting, moderation, and the core commands all live in one jar. Fewer plugins means fewer version conflicts and faster startup (50ms).
- **Modular by design.** Prefer another plugin for a given feature? Set it to `false` in the config and reload. ServerTools steps out of the way and stops registering that module entirely.
- **100% open source.** Full source on [GitHub](https://github.com/reecepbcups/ServerTools-MC). Audit it, fork it, or contribute.
- **Battle tested.** Over 6,000 unique players across Skyblock, Factions, Survival, and Hub setups.
- **Built for performance.** Tickless holograms and heavy features like TPAll and ClearLag are written to stay lagless.

---

## Version and Compatibility

| Version | Minecraft | Notes |
|-|-|-|
| **8.0.0+** | 1.20 / 1.21+ | Current, Java 21 |
| **7.0.0+** | 1.18+ | |
| **6.4.9-ALL** | 1.8 - 1.17 | [Legacy download](https://www.spigotmc.org/resources/servertools-%E2%9E%9C-modular-server-management-1-8-1-18-2-open-source.95853/download?version=455997) |

Runs on **Paper** and **Spigot**.

**Downloads:** [Hangar](https://hangar.papermc.io/reecepbcups/ServerTools-MC) - [Modrinth](https://modrinth.com/plugin/servertools-mc) - [Spigot](https://www.spigotmc.org/resources/servertools-%E2%9E%9C-modular-server-management-1-8-1-21-open-source.95853/)

**Soft depends (all optional):** LuckPerms, Vault, PlaceholderAPI, TAB, WorldGuard. ServerTools works without them and lights up extra features when they're present.

---

## Features

### Core commands (Essentials replacement)

Fly, god, heal, repair, enchant, hat, invsee, enderchest, trash, workbench, speed, gamemode, tp, top, spawn, warps, compass, ping, messaging (msg/reply), nicknames, and realname. Full command and permission list is in the [Wiki](https://servertools.reece.sh/).

### Chat and formatting

Chat formatting, emotes, chat cooldowns, join MOTD, MuteChat, ClearChat, plus **ChatColor** and **NameColor** GUIs with HEX support. ChatPolls let players vote inline.

<p align="center">
  <img width="555" height="205" alt="Chat Welcome Message and Announcements" src="https://github.com/user-attachments/assets/b9ba606f-dcdd-496e-b5ea-d05b76deffc0" />
  <br/><em>Chat Welcome Message (with PAPI support) and recurring timed announcements</em>
</p>

### Moderation

Freeze, reports, command spy, **Command Protection**, staff AFK, whitelist bypass, and admin chat.

<p align="center">
  <img width="624" height="236" alt="Chat Moderation" src="https://github.com/user-attachments/assets/dc2e8cb3-3915-49d6-b51f-4d08ab39bcc7" />
  <br/><em>Chat moderation</em>
</p>

### Holograms

HEX-colored & gradient holographic displays with placeholder support. Great for Hub info panels and Tebex (Buycraft) store links.

<p align="center">
  <img width="1031" height="516" alt="image" src="https://github.com/user-attachments/assets/cb473ea0-7a84-4f2b-8319-66f03a81ea07" />
  <br/><em>Example spawn hologram with HEX color codes and gradients. Holograms support non-player-specific PAPI placeholders (e.g. <code>%server_time%</code>).</em>
</p>

### Economy and items

Money and EXP **Withdraw**, **XP Bottles**, item **Rename**, and stack-unstackables.

### Rewards and cosmetics

**Vouchers** and bundles, **Tags**, daily rewards, and **Launchpads**.

<p align="center">
  <img width="441" height="222" alt="Tags GUI" src="https://github.com/user-attachments/assets/8f95b6d7-e8c2-46a9-8b39-fe687036a7f4" />
  <br/><em>Tags that support full colors, hex, and gradients</em>
</p>

### Server automation

**AutoBroadcast**, **Custom Announcements**, **Scheduled Tasks**, and **Server-Age** (PAPI `%stools_age_<EPOCHTIME>%` ) tracking.

<p align="center">
  <img width="1335" height="268" alt="Custom Announcement" src="https://github.com/user-attachments/assets/693be474-07fb-4081-9b1e-325981830d1e" />
  <br/><em>Custom Announcement - format and replace entirely in the config (PAPI placeholders work as well)</em>
</p>

### Events

**Command Aliases** (replace or remap text), custom death messages, death cooldowns, **Anti-Crafting** (block crafting of specific items), on-join commands, no bed explosions, and world effects.

<p align="center">
  <img width="492" height="107" alt="Chat alias replacing text" src="https://github.com/user-attachments/assets/f6b619b0-42d2-4d3d-9118-5ba6d0555b99" />
  <br/><em>Chat alias replacing text</em>
</p>

### Utility

**TPAll** (lagless), **ClearLag**, player **Visibility** toggles, join and leave messages, and PlaceholderAPI placeholders.

### Staff list

Pulls staff from a LuckPerms group and displays it in-game.

<p align="center">
  <img width="266" height="187" alt="Staff list" src="https://github.com/user-attachments/assets/8314161d-a7ee-4a05-98c5-74a7cbe265e1" />
  <br/><em>Staff list (requires LuckPerms)</em>
</p>

---

## What it replaces

Drop ServerTools in and you can pull out most of this stack. Every row is a feature ServerTools already ships.

| Feature | Plugins it replaces |
|-|-|
| Core commands (fly, god, heal, repair, enchant, hat, invsee, enderchest, trash, workbench, speed, gamemode, tp, top, msg) | EssentialsX, CMI |
| Spawn | EssentialsSpawn, CMI |
| Warps | EssentialsX, HuskHomes |
| Chat formatting | EssentialsChat, VentureChat, DeluxeChat, ChatManager |
| Chat/name color | ChatColor+, SimpleChatColor |
| Mute chat / clear chat | MuteChat, ClearChat |
| Chat polls | Polls, VoteParty-style poll plugins |
| Holograms | HolographicDisplays, DecentHolograms, GHolo |
| Withdraw money / EXP | BeastWithdraw, Physical-Money, EconomyShopGUI withdraw |
| XP bottles | XPBottles, BottledExp |
| Item rename | SimpleRename, ItemEdit |
| Anti-crafting | CraftControl, RecipeManager, CraftBlocker |
| Command aliases | CommandAlias, MyCommand, ExecutableItems aliases |
| Command protection | CommandBlockerUltimate, CommandWhitelist |
| Auto broadcast / announcements | Announcer, AutoBroadcaster, BroadcastPlus |
| Scheduled tasks | CommandSchedule, CommandTimer |
| Vouchers / bundles | DeluxeVouchers, BossShopPro vouchers |
| Crates | CrazyCrates, ExcellentCrates, Galaxy Crates |
| Tags | DeluxeTags, BossShop tags |
| Daily rewards | DailyRewards, AutoRewards |
| Launchpads | LaunchpadX, JumpPads |
| Freeze / reports / command spy | EssentialsX commandspy, Reports, FreezePlus |
| Custom death messages | DeathMessagesPrime |
| Staff AFK | EssentialsX AFK |
| Visibility toggle | HideStream, per-player visibility plugins |
| TPAll (lagless) | EssentialsX |
| ClearLag | ClearLagg, LagAssist |
| Join & leave messages | JoinMessage, CustomJoinMessages |
| Server-age tracking | uptime/age plugins |
| PlaceholderAPI placeholders | custom expansion plugins |

...plus a solid chunk of **Essentials** itself. Prefer a dedicated plugin for any of these? Toggle the ServerTools module off in the config and reload.

---

## Configuration

Everything is config-driven. Toggle any module on or off and reload.

**Main**

- `config.yml`

**Optional**

- `FeaturesGUI.yml` (Features GUI)
- `Tags.yml`
- `ScheduledTask.yml`
- `Vouchers.yml` (Vouchers / Bundles)
- `CommandProtect.yml` (Command Protection)
- `Announcements.yml`
- `Holograms.yml`, `Warps.yml`, `AntiCraft.yml`, `spawn.yml`
- **[NEW]** Messages / translations

---

## Documentation and Support

- **Wiki / Docs:** https://servertools.reece.sh/
- **Discord:** `reecepbcups`
- **Version history:** [Spigot page](https://www.spigotmc.org/resources/servertools-%E2%9E%9C-modular-server-management-1-8-1-21-open-source.95853/history)

---

## Building from source

Needs Java 21, Maven, Docker, and [just](https://github.com/casey/just).

```bash
git clone https://github.com/reecepbcups/ServerTools-MC.git
cd ServerTools-MC
```

```bash
just build      # mvn package + copy jar to server/plugins/
just server-up  # paper server in docker (first run downloads everything)
just test-e2e   # e2e tests against the running server
just loadtest   # mineflayer bots for load testing
```

See [TESTING.md](TESTING.md) for load testing with bots and Spark profiling, and [INFRA.md](INFRA.md) for the local server setup.
