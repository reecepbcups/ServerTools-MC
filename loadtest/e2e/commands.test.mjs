import { describe, it, before, after } from "node:test";
import assert from "node:assert";
import { createBot, rcon, waitForMessage, sleep } from "./helpers.mjs";

describe("commands (single bot)", () => {
  let bot;

  before(async () => {
    bot = await createBot("e2e_cmd");
    rcon("op e2e_cmd");
    rcon("tp e2e_cmd 0 100 0");
    await sleep(1000);
  });

  after(() => { try { bot?.quit(); } catch {} });

  it("/spawn teleports player", async () => {
    const p = waitForMessage(bot, /spawn/i, 5000);
    bot.chat("/spawn");
    await p;
  });

  it("/stafflist shows output", async () => {
    // stafflist always outputs group headers even if empty (shows N/A)
    const p = waitForMessage(bot, /N\/A|online/i, 5000);
    bot.chat("/stafflist");
    await p;
  });

  it("/itemdb shows held item", async () => {
    const p = waitForMessage(bot, /holding/i, 5000);
    bot.chat("/itemdb");
    await p;
  });

  it("/hat with empty hand shows error", async () => {
    const p = waitForMessage(bot, "can not set your hat", 5000);
    bot.chat("/hat");
    await p;
  });

  it("/trash opens inventory", async () => {
    bot.chat("/trash");
    await sleep(1000);
    assert.ok(bot.currentWindow != null, "expected /trash to open a window");
    bot.closeWindow(bot.currentWindow);
  });

  it("/toggledeathmessages toggles and responds", async () => {
    const p = waitForMessage(bot, /show death messages/i, 5000);
    bot.chat("/toggledeathmessages");
    await p;
  });

  it("/namecolor opens GUI", async () => {
    bot.chat("/namecolor");
    await sleep(1000);
    assert.ok(bot.currentWindow != null, "expected /namecolor to open a GUI");
    bot.closeWindow(bot.currentWindow);
  });
});

describe("messaging (two bots)", () => {
  let bot1, bot2;

  before(async () => {
    bot1 = await createBot("e2e_msg1");
    bot2 = await createBot("e2e_msg2");
    rcon("op e2e_msg1");
    rcon("op e2e_msg2");
    await sleep(1000);
  });

  after(() => {
    try { bot1?.quit(); } catch {}
    try { bot2?.quit(); } catch {}
  });

  it("/msg delivers message to target player", async () => {
    const p = waitForMessage(bot2, "hello there", 5000);
    bot1.chat("/msg e2e_msg2 hello there");
    await p;
  });

  it("sender sees confirmation of sent message", async () => {
    const p = waitForMessage(bot1, "outgoing test", 5000);
    bot1.chat("/msg e2e_msg2 outgoing test");
    await p;
  });
});
