#!/usr/bin/env node

import mineflayer from "mineflayer";
import { exec } from "child_process";

// suppress unhandled errors from disconnected bots (EPIPE, etc)
process.on("uncaughtException", (err) => {
  if (err.code === "EPIPE" || err.code === "ECONNRESET") return;
  console.error("[!] uncaught:", err.message);
});
process.on("unhandledRejection", (err) => {
  if (err?.code === "EPIPE" || err?.code === "ECONNRESET") return;
  console.error("[!] unhandled:", err?.message ?? err);
});

// -- CLI args --

const args = parseArgs(process.argv.slice(2));
const BOT_COUNT = parseInt(args.bots ?? "10");
const SCENARIO = args.scenario ?? "all";
const RAMP_DELAY = parseInt(args.ramp ?? "3") * 1000;
const DURATION = parseInt(args.duration ?? "0") * 1000; // 0 = run forever
const HOST = args.host ?? "localhost";
const PORT = parseInt(args.port ?? "25565");
const RAMP_MODE = args["ramp-test"] === "true";

function parseArgs(argv) {
  const result = {};
  for (let i = 0; i < argv.length; i++) {
    if (argv[i].startsWith("--")) {
      const key = argv[i].slice(2);
      result[key] = argv[i + 1] ?? "true";
      i++;
    }
  }
  return result;
}

console.log(
  `loadtest: ${BOT_COUNT} bots, scenario=${SCENARIO}, ramp=${RAMP_DELAY}ms, host=${HOST}:${PORT}`
);

// -- Bot management --

const bots = [];
let stopping = false;

const SPAWN_TIMEOUT = 15_000;
const RESPAWN_DELAY = 3_000;

const botIntervals = new WeakMap();

function trackBot(bot, id) {
  const ids = botIntervals.get(bot) ?? [];
  ids.push(id);
  botIntervals.set(bot, ids);
  return id;
}

function clearBotIntervals(bot) {
  const ids = botIntervals.get(bot) ?? [];
  for (const id of ids) clearInterval(id);
  botIntervals.delete(bot);
}

function removeBot(bot) {
  clearBotIntervals(bot);
  const idx = bots.indexOf(bot);
  if (idx !== -1) bots.splice(idx, 1);
}

function isBotAlive(bot) {
  return bot.entity != null && !bot._dead;
}

// random int in [min, max]
function rand(min, max) {
  return min + Math.floor(Math.random() * (max - min + 1));
}

function spawnBot(index) {
  return new Promise((resolve, reject) => {
    const name = `bot_${index}`;
    const bot = mineflayer.createBot({
      host: HOST,
      port: PORT,
      username: name,
      version: "1.20.4",
    });

    bot._index = index;
    bot._dead = false;
    let settled = false;

    const timeout = setTimeout(() => {
      if (settled) return;
      settled = true;
      console.log(`[!] ${name} spawn timed out after ${SPAWN_TIMEOUT / 1000}s`);
      try { bot.quit(); } catch {}
      reject(new Error(`${name} spawn timeout`));
    }, SPAWN_TIMEOUT);

    bot.once("spawn", () => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      bot._dead = false;
      console.log(`[+] ${name} joined (${bots.length + 1}/${BOT_COUNT})`);
      bots.push(bot);
      startScenario(bot, SCENARIO);
      resolve(bot);
    });

    bot.on("error", (err) => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      console.error(`[!] ${name} error: ${err.message}`);
      reject(err);
    });

    bot.on("kicked", (reason) => {
      try {
        const parsed = JSON.parse(reason);
        console.log(`[-] ${name} kicked: ${parsed.text ?? parsed.translate ?? reason}`);
      } catch {
        console.log(`[-] ${name} kicked: ${reason}`);
      }
      removeBot(bot);
    });

    bot.on("death", () => {
      bot._dead = true;
      clearBotIntervals(bot);
      setTimeout(() => {
        if (stopping) return;
        try { bot.respawn(); } catch {}
      }, RESPAWN_DELAY);
    });

    bot.on("spawn", () => {
      if (!settled) return;
      bot._dead = false;
      startScenario(bot, SCENARIO);
    });

    bot.on("end", () => {
      removeBot(bot);
    });
  });
}

async function rampUp(count, delayMs) {
  for (let i = 0; i < count; i++) {
    if (stopping) break;
    try {
      await spawnBot(i);
    } catch {
      console.log(`[!] bot_${i} failed to connect, retrying in 5s...`);
      await sleep(5000);
      try {
        await spawnBot(i);
      } catch {
        console.log(`[!] bot_${i} failed again, skipping`);
      }
    }
    if (i < count - 1) await sleep(delayMs);
  }
}

// -- Scenarios --

function startScenario(bot, scenario) {
  const scenarios = { move, chat, commands, mine, interact, all };
  const fn = scenarios[scenario];
  if (!fn) {
    console.error(`unknown scenario: ${scenario}. options: ${Object.keys(scenarios).join(", ")}`);
    process.exit(1);
  }
  fn(bot);
}

// walk around like a player exploring
function move(bot) {
  let yaw = Math.random() * Math.PI * 2;
  let lastPos = null;
  let stuckTicks = 0;

  bot.look(yaw, 0, false);
  bot.setControlState("forward", true);

  trackBot(bot, setInterval(() => {
    if (!isBotAlive(bot)) return;
    try {
      const pos = bot.entity.position;

      // stuck detection - turn away like a real player bumping into something
      if (lastPos && pos.distanceTo(lastPos) < 0.5) {
        stuckTicks++;
        if (stuckTicks >= 3) {
          bot.clearControlStates();
          yaw += Math.PI * (0.5 + Math.random());
          bot.look(yaw, 0, false);
          setTimeout(() => {
            try {
              bot.setControlState("forward", true);
              bot.setControlState("sprint", Math.random() > 0.5);
            } catch {}
          }, 500);
          stuckTicks = 0;
        }
      } else {
        stuckTicks = 0;
      }
      lastPos = pos.clone();

      // gradual direction changes
      if (Math.random() > 0.75) {
        yaw += (Math.random() - 0.5) * 1.5;
        bot.look(yaw, 0, false);
      }

      bot.setControlState("sprint", Math.random() > 0.4);

      if (Math.random() > 0.85) {
        bot.setControlState("jump", true);
        setTimeout(() => {
          try { bot.setControlState("jump", false); } catch {}
        }, 200);
      }

      // occasionally stop and idle for a bit
      if (Math.random() > 0.95) {
        bot.clearControlStates();
        setTimeout(() => {
          try {
            bot.setControlState("forward", true);
          } catch {}
        }, rand(2000, 5000));
      }
    } catch {}
  }, 1000));
}

// chat like a normal player
function chat(bot) {
  const messages = [
    "hey", "anyone on?", "gg", "nice", "lol", "brb", "whats up",
    "where is everyone", "this server is cool", "first time here",
    "how do i get to spawn", "lag?", "ok", "ty", "np", "yo",
    "whos the owner", "any events?", "nice build", "hello",
  ];

  trackBot(bot, setInterval(() => {
    if (!isBotAlive(bot)) return;
    try {
      bot.chat(messages[rand(0, messages.length - 1)]);
    } catch {}
  }, rand(15000, 45000)));
}

// run commands a regular player has access to (no perms needed, enabled in config)
function commands(bot) {
  const cmds = [
    "/stafflist",
    "/itemdb",
    "/hat",
    "/trash",
    "/spawn",
    "/namecolor",
    "/toggledeathmessages",
    "/buy",
    `/msg bot_${(bot._index + 1) % BOT_COUNT} hey`,
    `/msg bot_${rand(0, BOT_COUNT - 1)} whats up`,
  ];

  trackBot(bot, setInterval(() => {
    if (!isBotAlive(bot)) return;
    try {
      bot.chat(cmds[rand(0, cmds.length - 1)]);
    } catch {}
  }, rand(10000, 30000)));
}

// try to mine nearby blocks
function mine(bot) {
  trackBot(bot, setInterval(() => {
    if (!isBotAlive(bot)) return;
    try {
      // look down slightly and try to dig whatever's in front
      const block = bot.blockAtCursor(4);
      if (block && block.name !== "air" && block.name !== "bedrock" && block.name !== "barrier") {
        bot.dig(block).catch(() => {});
      }
    } catch {}
  }, rand(5000, 15000)));
}

// right click, swap hotbar slots, sneak - general player interaction
function interact(bot) {
  // use items occasionally
  trackBot(bot, setInterval(() => {
    if (!isBotAlive(bot)) return;
    try {
      bot.activateItem();
      setTimeout(() => {
        try { bot.deactivateItem(); } catch {}
      }, 200);
    } catch {}
  }, rand(8000, 20000)));

  // swap hotbar slots like a player cycling through items
  trackBot(bot, setInterval(() => {
    if (!isBotAlive(bot)) return;
    try {
      bot.setQuickBarSlot(rand(0, 8));
    } catch {}
  }, rand(5000, 15000)));

  // sneak toggle (looking over edges, shifting)
  trackBot(bot, setInterval(() => {
    if (!isBotAlive(bot)) return;
    try {
      bot.setControlState("sneak", true);
      setTimeout(() => {
        try { bot.setControlState("sneak", false); } catch {}
      }, rand(500, 2000));
    } catch {}
  }, rand(15000, 40000)));
}

// everything mixed - realistic player session
function all(bot) {
  move(bot);
  chat(bot);
  commands(bot);
  mine(bot);
  interact(bot);
}

// -- TPS monitoring --

function pollTps() {
  return new Promise((resolve) => {
    const cwd = process.cwd().replace(/\/loadtest$/, "");
    exec('docker compose exec mc rcon-cli "spark tps"', {
      cwd,
      timeout: 5000,
      encoding: "utf-8",
    }, (err, stdout) => {
      if (err) { resolve(null); return; }
      const match = stdout.match(/(\d+\.?\d*),\s*[*]?(\d+\.?\d*)/);
      if (match) { resolve(parseFloat(match[1])); return; }
      const numMatch = stdout.match(/\*?(\d+\.?\d*)/);
      resolve(numMatch ? parseFloat(numMatch[1]) : null);
    });
  });
}

let tpsInterval;
function startTpsMonitor() {
  console.log("timestamp,bots,tps");
  tpsInterval = setInterval(async () => {
    const tps = await pollTps();
    console.log(`${Date.now()},${bots.length},${tps ?? "N/A"}`);
  }, 10_000);
}

// -- Ramp test mode --

async function rampTest() {
  const step = 5;
  const maxBots = BOT_COUNT;
  console.log(`ramp-test: adding ${step} bots at a time, up to ${maxBots}`);
  console.log("timestamp,bots,tps");

  for (let count = 0; count < maxBots; count += step) {
    if (stopping) break;
    const toSpawn = Math.min(step, maxBots - count);
    for (let i = 0; i < toSpawn; i++) {
      if (stopping) break;
      try { await spawnBot(count + i); } catch {}
      await sleep(RAMP_DELAY);
    }

    console.log(`[*] ${bots.length} bots active, waiting 30s to stabilize...`);
    await sleep(30_000);

    const tps = await pollTps();
    console.log(`${Date.now()},${bots.length},${tps ?? "N/A"}`);

    if (tps !== null && tps < 18.0) {
      console.log(`[!] TPS dropped to ${tps} at ${bots.length} bots. Stopping ramp.`);
      break;
    }
  }

  console.log(`ramp-test complete. Peak: ${bots.length} bots`);
}

// -- Main --

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function cleanup() {
  if (stopping) return;
  stopping = true;
  console.log("\n[*] disconnecting all bots...");
  if (tpsInterval) clearInterval(tpsInterval);
  for (const bot of bots) {
    clearBotIntervals(bot);
    try { bot.quit(); } catch {}
  }
  bots.length = 0;
  setTimeout(() => process.exit(0), 1000);
}

process.on("SIGINT", () => {
  if (stopping) { console.log("\n[*] forced exit"); process.exit(1); }
  cleanup();
});
process.on("SIGTERM", cleanup);

async function main() {
  if (RAMP_MODE) {
    await rampTest();
    cleanup();
    return;
  }

  startTpsMonitor();

  if (DURATION > 0) {
    const deadline = sleep(DURATION).then(() => {
      console.log(`[*] duration reached (${DURATION / 1000}s), shutting down...`);
      cleanup();
    });

    await rampUp(BOT_COUNT, RAMP_DELAY);
    if (!stopping) {
      console.log(`[*] all ${bots.length} bots connected. scenario: ${SCENARIO}`);
    }

    await deadline;
  } else {
    await rampUp(BOT_COUNT, RAMP_DELAY);
    console.log(`[*] all ${bots.length} bots connected. scenario: ${SCENARIO}`);
  }
}

main().catch((err) => {
  console.error("fatal:", err);
  cleanup();
});
