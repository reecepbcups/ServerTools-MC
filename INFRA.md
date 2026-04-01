# Local E2E Setup

Paper 1.20.4 in Docker via `itzg/minecraft-server`. Plugin compiles against Spigot API 1.18.2 but we test on 1.20.4.

`ONLINE_MODE=FALSE` so you can connect with any username, no Mojang auth needed. RCON is local-only (not exposed outside the container).

## Soft-depends included

Auto-downloaded on first `server-up`:

- **Vault** - SpigotMC #34315
- **PlaceholderAPI** - SpigotMC #6245
- **LuckPerms** - Modrinth
- **PlugManX** - SpigotMC #88135 (hot-reload plugins without restart)

## Quick start

```bash
just build && just server-up # blocks with logs, ctrl-c to stop (~2 min first time)
```

## Commands

```
just server-up       start the Paper server (blocks, shows logs)
just server-down     stop it
just deploy          build jar + copy to server/plugins/
just e2e             deploy + hot-reload via PlugManX
just mc-cmd "<cmd>"  run any console command (e.g. "lp user Reece permission set some.perm")
```

## Connecting

Direct connect in your MC client to `localhost`. Use any username since online mode is off.

## Resetting

```bash
just server-down
docker volume rm servertools-mc_mc-data   # nuke world + server config
rm -rf server/plugins/                     # remove all plugin jars
just server-up                             # fresh start
```

## Memory

Default 2G. Override with `MC_MEMORY=4G just server-up`.
