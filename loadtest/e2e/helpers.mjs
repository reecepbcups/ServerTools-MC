import mineflayer from "mineflayer";
import { execSync } from "child_process";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(__dirname, "../..");

// connect a bot, resolve when it spawns
export function createBot(username) {
  return new Promise((resolve, reject) => {
    const bot = mineflayer.createBot({
      host: "localhost",
      port: 25565,
      username,
      version: "1.21.5",
    });

    const timeout = setTimeout(() => {
      try { bot.quit(); } catch {}
      reject(new Error(`${username} spawn timed out`));
    }, 15_000);

    bot.once("spawn", () => {
      clearTimeout(timeout);
      resolve(bot);
    });

    bot.once("error", (err) => {
      clearTimeout(timeout);
      reject(err);
    });
  });
}

// connect a bot and collect all messages from the moment it connects
// resolves with { bot, messages } after spawn + extra wait
export function createBotCollecting(username, extraMs = 3000) {
  return new Promise((resolve, reject) => {
    const bot = mineflayer.createBot({
      host: "localhost",
      port: 25565,
      username,
      version: "1.21.5",
    });

    const messages = [];
    bot.on("messagestr", (msg) => {
      messages.push(typeof msg === "string" ? msg : String(msg));
    });

    const timeout = setTimeout(() => {
      try { bot.quit(); } catch {}
      reject(new Error(`${username} spawn timed out`));
    }, 15_000);

    bot.once("spawn", () => {
      clearTimeout(timeout);
      setTimeout(() => resolve({ bot, messages }), extraMs);
    });

    bot.once("error", (err) => {
      clearTimeout(timeout);
      reject(err);
    });
  });
}

// run a command on the server via rcon
export function rcon(cmd) {
  return execSync(`docker compose exec mc rcon-cli "${cmd}"`, {
    cwd: REPO_ROOT,
    encoding: "utf-8",
    timeout: 10_000,
  }).trim();
}

// wait for a chat message matching a pattern (string substring or regex)
export function waitForMessage(bot, pattern, ms = 5000) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      bot.removeListener("messagestr", handler);
      reject(new Error(`no message matching ${pattern} within ${ms}ms`));
    }, ms);

    function handler(msg) {
      const str = typeof msg === "string" ? msg : String(msg);
      const match = pattern instanceof RegExp ? pattern.test(str) : str.includes(pattern);
      if (match) {
        clearTimeout(timeout);
        bot.removeListener("messagestr", handler);
        resolve(str);
      }
    }

    bot.on("messagestr", handler);
  });
}

// collect all messages for a duration
export function collectMessages(bot, ms = 2000) {
  return new Promise((resolve) => {
    const messages = [];
    function handler(msg) {
      messages.push(typeof msg === "string" ? msg : String(msg));
    }
    bot.on("messagestr", handler);
    setTimeout(() => {
      bot.removeListener("messagestr", handler);
      resolve(messages);
    }, ms);
  });
}

// wait until bot position satisfies a predicate
export function waitForPosition(bot, predicate, ms = 5000) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      clearInterval(interval);
      reject(new Error(`position predicate not met within ${ms}ms`));
    }, ms);

    const interval = setInterval(() => {
      if (bot.entity && predicate(bot.entity.position)) {
        clearInterval(interval);
        clearTimeout(timeout);
        resolve(bot.entity.position);
      }
    }, 100);
  });
}

export function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}
