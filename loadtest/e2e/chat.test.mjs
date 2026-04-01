import { describe, it, before, after } from "node:test";
import assert from "node:assert";
import { createBot, waitForMessage, sleep } from "./helpers.mjs";

describe("chat features", () => {
  let bot;

  before(async () => {
    bot = await createBot("e2e_chat");
    await sleep(6000);
  });

  after(() => { try { bot?.quit(); } catch {} });

  it("chat message includes player name", async () => {
    const p = new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        bot.removeListener("message", handler);
        reject(new Error("no message with player name within 5000ms"));
      }, 5000);
      function handler(jsonMsg, position, sender) {
        const text = jsonMsg.toString();
        if (text.includes("e2e_chat") || (sender && sender.toString() === bot.player?.uuid)) {
          clearTimeout(timeout);
          bot.removeListener("message", handler);
          resolve({ text, sender });
        }
      }
      bot.on("message", handler);
    });
    bot.chat("format test");
    const { text, sender } = await p;
    assert.ok(
      text.includes("e2e_chat") || sender != null,
      "chat should identify sender"
    );
  });

  it("chat cooldown blocks rapid messages", async () => {
    // first message (may still be on cooldown from previous test)
    await sleep(6000);
    bot.chat("first msg");
    await sleep(500);
    // second message should trigger cooldown
    const p = waitForMessage(bot, /must wait/i, 5000);
    bot.chat("second msg");
    await p;
  });

  it("chat works again after cooldown expires", async () => {
    // config has 5s cooldown
    await sleep(6000);
    const p = waitForMessage(bot, "after cooldown", 5000);
    bot.chat("after cooldown");
    await p;
  });
});
