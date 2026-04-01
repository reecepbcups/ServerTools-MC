#!/usr/bin/env node

import mineflayer from "mineflayer";
import { execSync, exec } from "child_process";

// -- CLI args --

const args = parseArgs(process.argv.slice(2));
const BOT_COUNT = parseInt(args.bots ?? "10");
const SCENARIO = args.scenario ?? "all";
const RAMP_DELAY = parseInt(args.ramp ?? "3") * 1000; // seconds between bot joins
const DURATION = parseInt(args.duration ?? "0") * 1000; // 0 = run forever
const HOST = args.host ?? "localhost";
const PORT = parseInt(args.port ?? "25565");
const RAMP_MODE = args["ramp-test"] === "true"; // ramp until TPS drops

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
const intervals = [];
let stopping = false;

function spawnBot(index) {
  return new Promise((resolve, reject) => {
    const name = `bot_${index}`;
    const bot = mineflayer.createBot({
      host: HOST,
      port: PORT,
      username: name,
      version: "1.20.4",
      hideErrors: true,
    });

    bot._index = index;

    bot.once("spawn", () => {
      console.log(`[+] ${name} joined (${bots.length + 1}/${BOT_COUNT})`);
      bots.push(bot);
      startScenario(bot, SCENARIO);
      resolve(bot);
    });

    bot.on("error", (err) => {
      console.error(`[!] ${name} error: ${err.message}`);
      reject(err);
    });

    bot.on("kicked", (reason) => {
      console.log(`[-] ${name} kicked: ${reason}`);
      const idx = bots.indexOf(bot);
      if (idx !== -1) bots.splice(idx, 1);
    });

    bot.on("end", () => {
      const idx = bots.indexOf(bot);
      if (idx !== -1) bots.splice(idx, 1);
    });
  });
}

async function rampUp(count, delayMs) {
  for (let i = 0; i < count; i++) {
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
// Each scenario attaches intervals to a bot. They target specific plugin codepaths.

function startScenario(bot, scenario) {
  const scenarios = { move, chat, commands, interact, all };
  const fn = scenarios[scenario];
  if (!fn) {
    console.error(`unknown scenario: ${scenario}. options: ${Object.keys(scenarios).join(", ")}`);
    process.exit(1);
  }
  fn(bot);
}

// helper to track intervals so cleanup can clear them all
function track(id) { intervals.push(id); return id; }

// PlayerMoveEvent -> LaunchPads.onMove, Freeze.onMove
function move(bot) {
  bot.setControlState("forward", true);
  track(setInterval(() => {
    bot.look(Math.random() * Math.PI * 2, -0.2 + Math.random() * 0.4, false);
    if (Math.random() > 0.6) {
      bot.setControlState("jump", true);
      setTimeout(() => bot.setControlState("jump", false), 200);
    }
    bot.setControlState("sprint", Math.random() > 0.5);
  }, 500 + Math.random() * 500));
}

// AsyncPlayerChatEvent -> ChatColor, NameColor, ChatCooldown
function chat(bot) {
  const messages = [
    "hello everyone", "testing chat", "how is everyone", "gg", "nice",
    "lol", "brb", "anyone here?", "server is great", "whats up",
  ];
  track(setInterval(() => {
    const msg = messages[Math.floor(Math.random() * messages.length)];
    bot.chat(msg);
  }, 2000 + Math.random() * 3000));
}

// PlayerCommandPreprocessEvent -> CMDAlias (HIGHEST), AlternateCommandHandler
// + the actual command handlers (stafflist is expensive)
function commands(bot) {
  const cmds = [
    "/ping", "/stafflist", "/compass", "/itemdb", "/top", "/fly", "/heal",
    "/speed 2", "/speed 1", `/msg bot_${(bot._index + 1) % BOT_COUNT} hey`,
    "/warp", "/clearlag", "/god", "/visibility",
  ];
  let i = 0;
  track(setInterval(() => {
    bot.chat(cmds[i % cmds.length]);
    i++;
  }, 1500 + Math.random() * 2000));
}

// PlayerInteractEvent -> XPBottle, Withdraw, NoBedExplosion
// InventoryClickEvent -> Tags, AntiCraft, ChatColor, NameColor, FeaturesGUI
function interact(bot) {
  track(setInterval(() => {
    bot.activateItem();
    setTimeout(() => bot.deactivateItem(), 100);
  }, 1000 + Math.random() * 1000));

  track(setInterval(async () => {
    const block = bot.blockAtCursor(4);
    if (block && block.name !== "air" && block.name !== "bedrock") {
      try { await bot.dig(block); } catch {}
    }
  }, 3000 + Math.random() * 2000));
}

// combined - maximum stress
function all(bot) {
  move(bot);
  chat(bot);
  commands(bot);
  interact(bot);
}

// -- TPS monitoring --

function pollTps() {
  return new Promise((resolve) => {
    const cwd = process.cwd().replace(/\/loadtest$/, "");
    const child = exec('docker compose exec mc rcon-cli "spark tps"', {
      cwd,
      timeout: 5000,
      encoding: "utf-8",
    }, (err, stdout) => {
      if (err) { resolve(null); return; }
      // spark outputs like: TPS from last 5s, 10s, 1m, 5m, 15m:
      //  *20.0, *20.0, *20.0, *20.0, *20.0
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
    const line = `${Date.now()},${bots.length},${tps ?? "N/A"}`;
    console.log(line);
  }, 10_000);
}

// -- Ramp test mode --
// Adds bots in batches, waits for stabilization, checks TPS. Stops when TPS < 18.

async function rampTest() {
  const step = 5;
  const maxBots = BOT_COUNT;
  console.log(`ramp-test: adding ${step} bots at a time, up to ${maxBots}`);
  console.log("timestamp,bots,tps");

  for (let count = 0; count < maxBots; count += step) {
    const toSpawn = Math.min(step, maxBots - count);
    for (let i = 0; i < toSpawn; i++) {
      try {
        await spawnBot(count + i);
      } catch {
        // skip failed bot
      }
      await sleep(RAMP_DELAY);
    }

    // stabilization period
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
  for (const id of intervals) clearInterval(id);
  intervals.length = 0;
  for (const bot of bots) {
    try { bot.quit(); } catch {}
  }
  bots.length = 0;
  setTimeout(() => process.exit(0), 500);
}

process.on("SIGINT", cleanup);
process.on("SIGTERM", cleanup);

async function main() {
  if (RAMP_MODE) {
    await rampTest();
    cleanup();
    return;
  }

  startTpsMonitor();
  await rampUp(BOT_COUNT, RAMP_DELAY);
  console.log(`[*] all ${bots.length} bots connected. scenario: ${SCENARIO}`);

  if (DURATION > 0) {
    console.log(`[*] running for ${DURATION / 1000}s...`);
    await sleep(DURATION);
    cleanup();
  }
  // else: run until ctrl+c
}

main().catch((err) => {
  console.error("fatal:", err);
  cleanup();
});
