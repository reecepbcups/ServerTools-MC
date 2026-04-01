# Load Testing

Uses [mineflayer](https://github.com/PrismarineJS/mineflayer) bots to simulate players on a local Paper server. Bots connect over the real protocol and trigger actual Bukkit events, so you can profile with [Spark](https://modrinth.com/mod/spark) to find bottlenecks.

## Setup

```bash
just server-up       # start paper server (first run downloads everything)
just build           # build + deploy plugin jar to server/plugins/
just loadtest-setup  # npm install mineflayer (one-time)
```

The server runs in Docker with `ONLINE_MODE=FALSE` so bots can connect without auth.

## Running Bots

```bash
# 10 bots, all scenarios, 3s between joins (defaults)
just loadtest

# 20 bots, only movement, 2s ramp
just loadtest 20 move 2

# 30 bots for 60 seconds then auto-stop
just loadtest-timed 30 all 60

node loadtest/loadtest.mjs --bots 300 --scenario all --ramp 0
```

## Scenarios

Each scenario targets specific plugin codepaths:

| Scenario | What it does | Plugin codepaths hit |
|-|-|-|
| `move` | Walk, jump, sprint randomly | PlayerMoveEvent (LaunchPads, Freeze) |
| `chat` | Send chat messages | AsyncPlayerChatEvent (ChatColor, NameColor) |
| `commands` | Cycle through /stafflist, /ping, /clearlag, etc. | PlayerCommandPreprocessEvent (CMDAlias), command handlers |
| `interact` | Right-click items, break blocks | PlayerInteractEvent (XPBottle, Withdraw), BlockBreakEvent |
| `all` | Everything above combined | All listeners under load |

## Profiling with Spark

Spark is auto-installed on the server via docker-compose.

```bash
# manual: start profiler, run bots, check results
just loadtest-profile 60
just loadtest 20 all 2
just loadtest-results

# or do it all at once
just loadtest-full 50 120
```

Check the Spark URL in the server console output for flame graphs. Look for your plugin's event handlers in the hot paths.

## Finding Your Breaking Point

The ramp test adds 5 bots at a time, waits 30s for the server to stabilize, checks TPS, and stops when it drops below 18:

```bash
just loadtest-ramp 100
```

Outputs CSV like:
```
timestamp,bots,tps
1711929600000,5,20.0
1711929630000,10,20.0
1711929660000,15,19.8
...
```

## TPS Monitoring

All test modes poll TPS every 10s via `spark tps` and log CSV to stdout. You can also check manually:

```bash
just loadtest-tps
```

## What to Look For

After a test run, check the Spark flame graph for:

- **LaunchPads.onMove** - runs every tick per player, scales linearly with bot count
- **CMDAlias.onCommand** - HIGHEST priority, intercepts every command from every player
- **StaffList** - iterates all online players with LuckPerms lookups per player
- **ClearLag.clearItemsInAllWorlds** - iterates all entities in all worlds
- **TimeChange** - modifies world time every second
- **AutoBroadcast** - broadcasts to all players on interval

If a handler shows up hot in the flame graph, that's your optimization target.
