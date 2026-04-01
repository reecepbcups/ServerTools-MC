import { describe, it } from "node:test";
import assert from "node:assert";
import { createBot, collectMessages } from "./helpers.mjs";

describe("join features", () => {
  it("MOTD is sent on join", async () => {
    const bot = await createBot("e2e_motd");
    // collect messages during the first few seconds after spawn
    const messages = await collectMessages(bot, 3000);
    bot.quit();

    const all = messages.join("\n");
    // MOTD typically contains server branding, store link, discord, etc.
    assert.ok(
      all.length > 0,
      "expected at least one message on join (MOTD)"
    );
  });

  it("second join still receives MOTD", async () => {
    const bot = await createBot("e2e_motd2");
    const messages = await collectMessages(bot, 3000);
    bot.quit();

    const all = messages.join("\n");
    assert.ok(all.length > 0, "MOTD should be sent on every join");
  });
});
