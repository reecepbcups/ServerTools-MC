import { describe, it, before, after } from "node:test";
import assert from "node:assert";
import { createBot, rcon, waitForPosition, sleep } from "./helpers.mjs";

describe("launchpads", () => {
  let bot;

  before(async () => {
    bot = await createBot("e2e_move");
    // build a launchpad at known coords
    rcon("setblock 50 99 50 minecraft:emerald_block");
    rcon("setblock 50 100 50 minecraft:stone_pressure_plate");
    await sleep(500);
  });

  after(() => {
    // clean up the launchpad
    try {
      rcon("setblock 50 100 50 minecraft:air");
      rcon("setblock 50 99 50 minecraft:air");
    } catch {}
    try { bot?.quit(); } catch {}
  });

  it("pressure plate over emerald block launches player upward", async () => {
    // tp bot away first to ensure block-change triggers PlayerMoveEvent
    rcon("tp e2e_move 48 100 50");
    await sleep(1000);

    const startY = bot.entity.position.y;

    // tp directly onto the pressure plate to trigger the move event
    rcon("tp e2e_move 50 100 50");
    await sleep(500);

    // should get launched upward
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
