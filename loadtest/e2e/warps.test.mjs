import { after, before, describe, it } from "node:test";
import assert from "node:assert";
import { existsSync, readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { collectMessages, createBot, rcon, sleep, waitForPosition } from "./helpers.mjs";

const REPO_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "../..");
const WARPS_CONFIG = resolve(REPO_ROOT, "server/plugins/ServerTools/Warps.yml");

const ADMIN = "e2e_warps";
const GUEST = "e2e_warp_guest";

// warps this suite owns - deleted through the command before and after the run. The file is
// never edited directly: the pre-rewrite API loads Warps.yml once at startup and offers no
// reload, so an on-disk edit would not reach the running plugin.
const OPEN_WARP = "e2e_warp_open";
const LOCKED_WARP = "e2e_warp_locked";
const SPARE_WARP = "e2e_warp_spare";

// must fit in an int: the name check is Integer.parseInt, so a larger value is not "numeric"
// and would be created as an ordinary warp
const NUMERIC_WARP = "48151623";

const TEST_WARPS = [OPEN_WARP, LOCKED_WARP, SPARE_WARP, NUMERIC_WARP];

// gate for LOCKED_WARP - any node works, it only has to be one no player holds by default
const WARP_NODE = "e2e.warp.locked";

// far from the other suites' anchors. Positive coordinates on purpose: the stored location
// truncates each axis with an (int) cast, which rounds toward zero rather than down.
const HOME = { x: 3000, y: 100, z: 3000 };
const AWAY = { x: 3040, y: 100, z: 3040 };

// the reader adds a flat +0.1 to Y, and the writer drops the fractional part of every axis
const NEAR = 1.5;

let bot;
let guest;

// --- command output ---------------------------------------------------------

// an opped bot also sees the server's broadcast of every rcon command this suite runs, which
// arrives asynchronously and can land inside a collection window
const ADMIN_BROADCAST = /\[Rcon:|\[Server:/;

async function runCommand(sender, command, ms = 1500) {
  const collected = collectMessages(sender, ms);
  sender.chat(command);
  return (await collected).filter((line) => !ADMIN_BROADCAST.test(line)).join("\n");
}

// the server sends no packet at all when a completion has zero matches, so mineflayer waits out
// its timeout instead of resolving empty. that silence is the "no suggestions" result.
const NO_COMPLETIONS_MS = 2500;

async function completions(sender, line) {
  let matches;
  try {
    matches = await sender.tabComplete(line, true, true, NO_COMPLETIONS_MS);
  } catch (err) {
    if (/did not fire within timeout/i.test(err.message)) return [];
    throw err;
  }
  return matches.map((m) => (typeof m === "string" ? m : m.match));
}

// --- Warps.yml --------------------------------------------------------------

// a flat map of name -> { permission, location }. A real YAML parser would be a dependency for
// the two fields under test, and `permission` is optional: the pre-rewrite API omits the key
// entirely for a warp created without one.
function readWarps() {
  if (!existsSync(WARPS_CONFIG)) return {};

  const warps = {};
  let name = null;

  for (const line of readFileSync(WARPS_CONFIG, "utf-8").split("\n")) {
    if (/^\s*(#|$)/.test(line)) continue;

    const top = line.match(/^([\w.-]+):\s*$/);
    if (top) {
      name = top[1];
      warps[name] = { permission: null, location: null };
      continue;
    }
    if (name === null) continue;

    const field = line.match(/^\s+(permission|location):\s*(.*?)\s*$/);
    if (field) warps[name][field[1]] = unquote(field[2]);
  }
  return warps;
}

function unquote(value) {
  const quoted = value.match(/^'(.*)'$/) ?? value.match(/^"(.*)"$/);
  return quoted ? quoted[1].replace(/''/g, "'") : value;
}

// "world;3000;100;3000;0.0;0.0" -> { world, x, y, z }
function parseLocation(value) {
  const [world, x, y, z] = String(value).split(";");
  return { world, x: Number(x), y: Number(y), z: Number(z) };
}

function near(position, target, tolerance = NEAR) {
  return (
    Math.abs(position.x - target.x) <= tolerance &&
    Math.abs(position.y - target.y) <= tolerance &&
    Math.abs(position.z - target.z) <= tolerance
  );
}

function park(player, at) {
  rcon(`tp ${player} ${at.x} ${at.y} ${at.z}`);
}

async function deleteTestWarps(sender) {
  for (const warp of TEST_WARPS) {
    try { await runCommand(sender, `/delwarp ${warp}`, 400); } catch {}
  }
}

describe("warps", () => {
  before(async () => {
    bot = await createBot(ADMIN);
    guest = await createBot(GUEST);

    // both bots run their own physics and would fall out of the anchor the assertions use.
    // Movement is client authoritative, so a bot that stops simulating stays where the server
    // last put it.
    bot.physicsEnabled = false;
    guest.physicsEnabled = false;

    rcon(`op ${ADMIN}`);
    rcon(`deop ${GUEST}`);
    rcon(`gamemode spectator ${ADMIN}`);
    rcon(`gamemode spectator ${GUEST}`);
    park(ADMIN, HOME);
    park(GUEST, AWAY);
    await sleep(2000);

    assert.ok(
      near(bot.entity.position, HOME),
      `admin did not hold its anchor: ${bot.entity.position}`
    );

    await deleteTestWarps(bot);
  });

  after(async () => {
    try { await deleteTestWarps(bot); } catch {}
    try { rcon(`lp user ${GUEST} permission unset ${WARP_NODE}`); } catch {}
    try { bot?.quit(); } catch {}
    try { guest?.quit(); } catch {}
  });

  // --- creating ---

  it("/setwarp stores the sender's position and confirms", async () => {
    park(ADMIN, HOME);
    await sleep(500);

    const out = await runCommand(bot, `/setwarp ${OPEN_WARP}`);
    assert.match(out, /warp set as/i, `expected a confirmation, got: ${out}`);

    const stored = readWarps()[OPEN_WARP];
    assert.ok(stored, `${OPEN_WARP} should be written to Warps.yml`);

    const location = parseLocation(stored.location);
    assert.equal(location.world, "world");
    assert.ok(
      near(location, HOME),
      `stored location ${stored.location} should be the player's position`
    );
  });

  it("/addwarp is an alias for /setwarp", async () => {
    park(ADMIN, HOME);
    await sleep(500);

    const out = await runCommand(bot, `/addwarp ${SPARE_WARP}`);
    assert.match(out, /warp set as/i, `expected a confirmation, got: ${out}`);
    assert.ok(readWarps()[SPARE_WARP], `${SPARE_WARP} should be written to Warps.yml`);
  });

  it("/setwarp records the optional permission argument", async () => {
    park(ADMIN, HOME);
    await sleep(500);

    await runCommand(bot, `/setwarp ${LOCKED_WARP} ${WARP_NODE}`);

    const stored = readWarps()[LOCKED_WARP];
    assert.ok(stored, `${LOCKED_WARP} should be written to Warps.yml`);
    assert.equal(stored.permission, WARP_NODE);
  });

  it("refuses a name that is already taken", async () => {
    const out = await runCommand(bot, `/setwarp ${OPEN_WARP}`);
    assert.match(out, /already/i, `expected a duplicate-name refusal, got: ${out}`);
  });

  it("refuses a purely numeric name", async () => {
    const out = await runCommand(bot, `/setwarp ${NUMERIC_WARP}`);
    assert.match(out, /number/i, `expected a numeric-name refusal, got: ${out}`);
    assert.ok(!readWarps()[NUMERIC_WARP], "a numeric warp should not be written");
  });

  it("/setwarp prints usage when no name is given", async () => {
    const out = await runCommand(bot, "/setwarp");
    assert.match(out, /usage:\s*\/setwarp/i, `expected usage text, got: ${out}`);
  });

  // --- listing and detail ---

  it("/warps lists the created warps with a count", async () => {
    const out = await runCommand(bot, "/warps");
    assert.match(out, /warps/i, `expected the list header, got: ${out}`);
    assert.match(out, new RegExp(OPEN_WARP), `${OPEN_WARP} should be listed`);
    assert.match(out, new RegExp(LOCKED_WARP), `${LOCKED_WARP} should be listed`);
  });

  it("/warp with a number lists rather than looking up a warp", async () => {
    const out = await runCommand(bot, "/warp 1");
    assert.match(out, /warps/i, `expected the list header, got: ${out}`);
    assert.doesNotMatch(out, /not a warp location/i, "a page number is not a lookup");
  });

  it("/warpinfo reports the location and omits an unset permission", async () => {
    const out = await runCommand(bot, `/warpinfo ${OPEN_WARP}`);
    assert.match(out, new RegExp(`warp[^\\n]*${OPEN_WARP}`, "i"), `expected the name, got: ${out}`);
    assert.match(out, /location[^\n]*world/i, `expected the location, got: ${out}`);
    assert.doesNotMatch(out, /permission/i, "a warp with no permission has no permission line");
  });

  it("/warpinfo reports a permission when the warp has one", async () => {
    const out = await runCommand(bot, `/warpinfo ${LOCKED_WARP}`);
    assert.match(out, new RegExp(`permission[^\\n]*${WARP_NODE}`, "i"), `got: ${out}`);
  });

  it("/warpinfo on an unknown warp says so", async () => {
    const out = await runCommand(bot, "/warpinfo e2e_warp_nope");
    assert.match(out, /not found/i, `expected a not-found message, got: ${out}`);
  });

  // --- teleporting ---

  it("/warp moves the sender to the stored position", async () => {
    park(ADMIN, AWAY);
    await sleep(500);

    await runCommand(bot, `/warp ${OPEN_WARP}`, 500);
    const landed = await waitForPosition(bot, (p) => near(p, HOME), 8000);
    assert.ok(near(landed, HOME), `expected to land at the warp, got ${landed}`);
  });

  it("/warp on an unknown name says it is not a warp", async () => {
    const out = await runCommand(bot, "/warp e2e_warp_nope");
    assert.match(out, /not a warp/i, `expected a not-a-warp message, got: ${out}`);
  });

  // --- permissions ---

  it("refuses a player who lacks the warp's permission", async () => {
    rcon(`lp user ${GUEST} permission set ${WARP_NODE} false`);
    park(GUEST, AWAY);
    await sleep(1000);

    const out = await runCommand(guest, `/warp ${LOCKED_WARP}`);
    assert.match(out, /do not have access/i, `expected a refusal, got: ${out}`);
    assert.ok(
      near(guest.entity.position, AWAY),
      `a refused player should not move, got ${guest.entity.position}`
    );
  });

  it("lets a player through once the permission is granted", async () => {
    rcon(`lp user ${GUEST} permission set ${WARP_NODE} true`);
    park(GUEST, AWAY);
    await sleep(1000);

    await runCommand(guest, `/warp ${LOCKED_WARP}`, 500);
    const landed = await waitForPosition(guest, (p) => near(p, HOME), 8000);
    assert.ok(near(landed, HOME), `expected the guest to land at the warp, got ${landed}`);
  });

  it("an open warp needs no permission at all", async () => {
    park(GUEST, AWAY);
    await sleep(1000);

    await runCommand(guest, `/warp ${OPEN_WARP}`, 500);
    const landed = await waitForPosition(guest, (p) => near(p, HOME), 8000);
    assert.ok(near(landed, HOME), `expected the guest to land at the warp, got ${landed}`);
  });

  // --- sending another player ---

  it("/warp <name> <player> sends the target and bypasses their own permission", async () => {
    rcon(`lp user ${GUEST} permission set ${WARP_NODE} false`);
    park(GUEST, AWAY);
    park(ADMIN, AWAY);
    await sleep(1000);

    const out = await runCommand(bot, `/warp ${LOCKED_WARP} ${GUEST}`, 500);
    assert.match(out, /teleporting/i, `expected a sender confirmation, got: ${out}`);

    const landed = await waitForPosition(guest, (p) => near(p, HOME), 8000);
    assert.ok(landed, "a staff-sent player skips the warp's own permission check");
    assert.ok(
      near(bot.entity.position, AWAY),
      `the sender should stay put, got ${bot.entity.position}`
    );
  });

  it("resolves a partial player name", async () => {
    park(GUEST, AWAY);
    await sleep(1000);

    const partial = GUEST.slice(0, GUEST.length - 3);
    await runCommand(bot, `/warp ${OPEN_WARP} ${partial}`, 500);

    const landed = await waitForPosition(guest, (p) => near(p, HOME), 8000);
    assert.ok(landed, `'${partial}' should resolve to ${GUEST}`);
  });

  it("reports a target who is not online", async () => {
    const out = await runCommand(bot, `/warp ${OPEN_WARP} e2e_warp_absent`);
    assert.match(out, /not found online/i, `expected an offline-target message, got: ${out}`);
  });

  // --- tab completion ---

  it("completes warp names", async () => {
    const matches = await completions(bot, `/warp ${OPEN_WARP.slice(0, 8)}`);
    assert.ok(
      matches.includes(OPEN_WARP),
      `expected ${OPEN_WARP} among completions, got ${JSON.stringify(matches)}`
    );
  });

  it("hides a warp the player cannot use", async () => {
    rcon(`lp user ${GUEST} permission set ${WARP_NODE} false`);
    await sleep(500);

    const matches = await completions(guest, `/warp ${LOCKED_WARP.slice(0, 8)}`);
    assert.ok(
      !matches.includes(LOCKED_WARP),
      `${LOCKED_WARP} should not be offered, got ${JSON.stringify(matches)}`
    );
  });

  it("completes player names in the second argument", async () => {
    const matches = await completions(bot, `/warp ${OPEN_WARP} ${GUEST.slice(0, 8)}`);
    assert.ok(
      matches.includes(GUEST),
      `expected ${GUEST} among completions, got ${JSON.stringify(matches)}`
    );
  });

  it("offers a placeholder rather than existing names for /setwarp", async () => {
    const matches = await completions(bot, "/setwarp e2e_warp_");
    assert.ok(
      !matches.includes(OPEN_WARP),
      `/setwarp names a new warp, got ${JSON.stringify(matches)}`
    );
  });

  // --- deleting ---

  it("/delwarp removes the warp from the list and the file", async () => {
    const out = await runCommand(bot, `/delwarp ${SPARE_WARP}`);
    assert.match(out, /deleted/i, `expected a deletion confirmation, got: ${out}`);
    assert.ok(!readWarps()[SPARE_WARP], `${SPARE_WARP} should be gone from Warps.yml`);

    const after = await runCommand(bot, `/warp ${SPARE_WARP}`);
    assert.match(after, /not a warp/i, "a deleted warp is no longer a destination");
  });

  it("/remwarp is an alias for /delwarp", async () => {
    park(ADMIN, HOME);
    await sleep(500);
    await runCommand(bot, `/setwarp ${SPARE_WARP}`);

    const out = await runCommand(bot, `/remwarp ${SPARE_WARP}`);
    assert.match(out, /deleted/i, `expected a deletion confirmation, got: ${out}`);
    assert.ok(!readWarps()[SPARE_WARP], `${SPARE_WARP} should be gone from Warps.yml`);
  });

  it("/delwarp on an unknown warp says it does not exist", async () => {
    const out = await runCommand(bot, "/delwarp e2e_warp_nope");
    assert.match(out, /does not exist/i, `expected a not-found message, got: ${out}`);
  });

  it("/delwarp prints usage when no name is given", async () => {
    const out = await runCommand(bot, "/delwarp");
    assert.match(out, /usage:\s*\/delwarp/i, `expected usage text, got: ${out}`);
  });
});
