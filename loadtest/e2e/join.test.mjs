import { describe, it } from "node:test";
import assert from "node:assert";
import { createBotCollecting } from "./helpers.mjs";

describe("join features", () => {
  it("MOTD is sent on join", async () => {
    const { bot, messages } = await createBotCollecting("e2e_motd");
    bot.quit();

    const all = messages.join("\n");
    assert.ok(
      all.length > 0,
      "expected at least one message on join (MOTD)"
    );
  });

  it("second join still receives MOTD", async () => {
    const { bot, messages } = await createBotCollecting("e2e_motd2");
    bot.quit();

    const all = messages.join("\n");
    assert.ok(all.length > 0, "MOTD should be sent on every join");
  });
});
