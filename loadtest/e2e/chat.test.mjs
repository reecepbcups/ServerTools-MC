import { describe, it, before, after } from "node:test";
import assert from "node:assert";
import { createBot, waitForMessage, sleep } from "./helpers.mjs";

describe("chat features", () => {
  let bot;

  before(async () => {
    bot = await createBot("e2e_chat");
    await sleep(1000);
  });

  after(() => { try { bot?.quit(); } catch {} });

  it("chat message includes player name", async () => {
    const p = waitForMessage(bot, "e2e_chat", 5000);
    bot.chat("format test");
    const msg = await p;
    assert.ok(msg.includes("e2e_chat"), "chat should contain sender name");
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
