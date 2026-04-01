import { describe, it, before, after } from "node:test";
import assert from "node:assert";
import { createBot, rcon, waitForPosition, sleep } from "./helpers.mjs";

describe("launchpads", () => {
  let bot;

  before(async () => {
    bot = await createBot("e2e_move");
    // build a runway leading to the launchpad along +Z axis
    rcon("setblock 50 99 48 minecraft:stone");
    rcon("setblock 50 99 49 minecraft:stone");
    rcon("setblock 50 99 50 minecraft:emerald_block");
    rcon("setblock 50 100 50 minecraft:stone_pressure_plate");
    await sleep(500);
  });

  after(() => {
    try {
      rcon("setblock 50 100 50 minecraft:air");
      rcon("setblock 50 99 50 minecraft:air");
      rcon("setblock 50 99 49 minecraft:air");
      rcon("setblock 50 99 48 minecraft:air");
    } catch {}
    try { bot?.quit(); } catch {}
  });

  // TODO: mineflayer bots trigger "moved too quickly" server-side, skipping for now
  it.skip("pressure plate over emerald block launches player upward", async () => {
    rcon("tp e2e_move 50.5 100 48.5 0 0");
    await sleep(1000);

    const startY = bot.entity.position.y;

    bot.setControlState("forward", true);
    await sleep(2000);
    bot.setControlState("forward", false);

    const pos = await waitForPosition(bot, (p) => p.y > startY + 1, 5000);
    assert.ok(pos.y > startY + 1, `expected launch upward, got y=${pos.y}`);
  });
});

describe("spawn void protection", () => {
  let bot;

  before(async () => {
    bot = await createBot("e2e_void");
    await sleep(1000);
  });

  after(() => { try { bot?.quit(); } catch {} });

  it("falling into void teleports player to spawn", async () => {
    const startY = bot.entity.position.y;
    // tp below the world
    rcon("tp e2e_void 0 -100 0");
    await sleep(3000);
    // should be back above ground (spawn or safe location)
    assert.ok(
      bot.entity.position.y > 0,
      `expected void protection to tp player up, got y=${bot.entity.position.y}`
    );
  });
});
