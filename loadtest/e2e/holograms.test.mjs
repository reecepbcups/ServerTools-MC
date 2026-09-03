import { after, before, describe, it } from "node:test";
import assert from "node:assert";
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { collectMessages, createBot, rcon, sleep } from "./helpers.mjs";

const REPO_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "../..");
const HOLO_CONFIG = resolve(REPO_ROOT, "server/plugins/ServerTools/Holograms.yml");

const ADMIN = "e2e_holograms";
const GUEST = "e2e_holo_guest";

// keys this suite owns - stripped from Holograms.yml before and after the run
const TEST_KEYS = ["e2e_holo_a", "e2e_holo_b", "e2e_holo_lines", "e2e_holo_badworld"];

// far from world spawn so no other suite's entities land inside the sweep radius
const AREA = { x: 2000, y: 100, z: 2000 };
// a hologram's lines all sit within ~1 block below the anchor, so this is generous
const SWEEP = 6;

// both implementations author config Y two blocks above where the text draws - a
// leftover of the ArmorStand nameplate that used to float above its stand
const Y_OFFSET = 2;

// --- server-side inspection -------------------------------------------------

// mineflayer never sees display entities (they are not tracked as mobs), so entity
// assertions go through the server's own selector engine
function displayCount(pos = AREA, radius = SWEEP) {
  const out = rcon(
    `execute positioned ${pos.x} ${pos.y} ${pos.z} if entity @e[type=text_display,distance=..${radius}]`
  );
  const counted = out.match(/Count:\s*(\d+)/i);
  if (counted) return Number(counted[1]);
  return /Test passed/i.test(out) ? 1 : 0; // vanilla omits the count when it is 1
}

// spawning is asynchronous on both APIs (region scheduler / delayed task), so poll
async function waitForDisplays(expected, { pos = AREA, radius = SWEEP, ms = 8000 } = {}) {
  const deadline = Date.now() + ms;
  let seen = -1;
  do {
    seen = displayCount(pos, radius);
    if (seen === expected) return seen;
    await sleep(250);
  } while (Date.now() < deadline);
  assert.fail(`expected ${expected} text display(s) near ${pos.x},${pos.y},${pos.z}, saw ${seen}`);
}

function killDisplays(pos = AREA, radius = 32) {
  rcon(`execute positioned ${pos.x} ${pos.y} ${pos.z} run kill @e[type=text_display,distance=..${radius}]`);
}

// --- Holograms.yml ----------------------------------------------------------

// Holograms.yml is a flat map of key -> { location, lines }; a real YAML parser
// would be a dependency for the two fields the tests actually care about
function readHoloConfig() {
  const holos = {};
  let key = null;
  let inLines = false;

  for (const line of readFileSync(HOLO_CONFIG, "utf-8").split("\n")) {
    if (/^\s*(#|$)/.test(line)) continue;

    const top = line.match(/^([\w.-]+):\s*$/);
    if (top) {
      key = top[1];
      holos[key] = { location: null, lines: [] };
      inLines = false;
      continue;
    }
    if (key === null) continue;

    const location = line.match(/^\s+location:\s*(.+?)\s*$/);
    if (location) {
      holos[key].location = unquote(location[1]);
      inLines = false;
      continue;
    }
    if (/^\s+lines:\s*$/.test(line)) {
      inLines = true;
      continue;
    }
    const item = line.match(/^\s*-\s?(.*)$/);
    if (inLines && item) holos[key].lines.push(unquote(item[1]));
  }
  return holos;
}

function unquote(value) {
  const quoted = value.match(/^'(.*)'$/) ?? value.match(/^"(.*)"$/);
  return quoted ? quoted[1].replace(/''/g, "'") : value;
}

// "world, 1000, 68.013, 1000" -> { world, x, y, z }. The two APIs format the
// numbers differently (#.### vs %.3f), so compare values and never the string.
function parseLocation(value) {
  const [world, x, y, z] = value.split(",").map((part) => part.trim());
  return { world, x: Number(x), y: Number(y), z: Number(z) };
}

function writeHoloConfig(holos) {
  const yaml = Object.entries(holos)
    .map(([key, { location, lines }]) => {
      const body = lines.map((line) => `  - '${line.replace(/'/g, "''")}'`).join("\n");
      return `${key}:\n  location: ${location}\n  lines:\n${body}`;
    })
    .join("\n");
  writeFileSync(HOLO_CONFIG, `${yaml}\n`);
}

function stripTestKeys() {
  const holos = readHoloConfig();
  for (const key of TEST_KEYS) delete holos[key];
  writeHoloConfig(holos);
}

// --- command output ---------------------------------------------------------

// Util.coloredMessage sends multi-line strings as one packet, so flatten before splitting
async function runCommand(bot, command, ms = 2500) {
  const collected = collectMessages(bot, ms);
  bot.chat(command);
  return (await collected).join("\n").split("\n");
}

// "- key: world, 1, 2, 3" and "key: world, 1, 2, 3" both parse; the header has no colon
function parseListing(lines) {
  const entries = {};
  for (const line of lines) {
    const entry = line.trim().match(/^-?\s*([\w.-]+):\s*([\w.-]+(?:\s*,\s*-?[\d.]+){3})\s*$/);
    if (entry) entries[entry[1]] = (entries[entry[1]] ?? []).concat(parseLocation(entry[2]));
  }
  return entries;
}

async function listHolograms(bot) {
  const lines = await runCommand(bot, "/hologram list");
  assert.match(lines.join("\n"), /SERVERTOOLS HOLOGRAMS/i, "expected the list header");
  return parseListing(lines);
}

describe("hologram features", () => {
  let bot;
  let guest;

  before(async () => {
    stripTestKeys();

    bot = await createBot(ADMIN);
    rcon(`op ${ADMIN}`);
    // /hologram create and removenear both key off the player's exact position, and
    // mineflayer runs its own physics - it would fall to the ground and drag the
    // hologram with it. Movement is client-authoritative, so a bot that stops
    // simulating simply stays wherever the server last put it.
    bot.physicsEnabled = false;
    rcon(`gamemode spectator ${ADMIN}`);
    rcon(`tp ${ADMIN} ${AREA.x} ${AREA.y} ${AREA.z}`);
    await sleep(2000);

    // every later assertion is relative to this anchor, so fail loudly if it drifted
    assert.ok(
      Math.abs(bot.entity.position.y - AREA.y) < 0.5,
      `bot did not hold its test position: ${bot.entity.position}`
    );

    // drop anything a previous run orphaned, then re-read the stripped config
    killDisplays();
    await runCommand(bot, "/hologram reload");
    await waitForDisplays(0);
  });

  after(async () => {
    try {
      stripTestKeys();
      killDisplays();
      if (bot) await runCommand(bot, "/hologram reload", 500);
    } catch {}
    try { bot?.quit(); } catch {}
    try { guest?.quit(); } catch {}
  });

  it("bare /hologram prints the help menu", async () => {
    const lines = (await runCommand(bot, "/hologram")).join("\n");
    assert.match(lines, /ServerTools Holograms/i);
    assert.match(lines, /\/hologram\s+create <name>/i);
    assert.match(lines, /\/hologram\s+removenear/i);
  });

  it("/hologram list includes the hologram shipped in Holograms.yml", async () => {
    const listed = await listHolograms(bot);
    assert.ok(listed.skyblock, `expected the default 'skyblock' key, got ${Object.keys(listed)}`);
    assert.deepStrictEqual(listed.skyblock[0], { world: "world", x: 32.5, y: 127.5, z: -568.5 });
  });

  it("/hologram create spawns one display per line at the player", async () => {
    const lines = (await runCommand(bot, "/hologram create e2e_holo_a")).join("\n");
    assert.match(lines, /created/i);
    // the default template is two lines, so two displays
    await waitForDisplays(2);
  });

  it("create records the player location plus the render offset", () => {
    const entry = readHoloConfig().e2e_holo_a;
    assert.ok(entry, "expected e2e_holo_a in Holograms.yml");

    const location = parseLocation(entry.location);
    assert.strictEqual(location.world, "world");
    assert.ok(Math.abs(location.x - AREA.x) < 0.5, `x was ${location.x}`);
    assert.ok(Math.abs(location.z - AREA.z) < 0.5, `z was ${location.z}`);
    assert.ok(
      Math.abs(location.y - (AREA.y + Y_OFFSET)) < 0.5,
      `expected y ~${AREA.y + Y_OFFSET} (player + offset), got ${location.y}`
    );
    assert.strictEqual(entry.lines.length, 2, "expected the two-line placeholder text");
  });

  it("the new hologram shows up in /hologram list exactly once", async () => {
    const listed = await listHolograms(bot);
    assert.ok(listed.e2e_holo_a, "expected e2e_holo_a to be listed");
    assert.strictEqual(listed.e2e_holo_a.length, 1, "expected exactly one entry per key");
    assert.ok(Math.abs(listed.e2e_holo_a[0].y - (AREA.y + Y_OFFSET)) < 0.5);
  });

  it("/hologram create rejects a name that already exists", async () => {
    const lines = (await runCommand(bot, "/hologram create e2e_holo_a")).join("\n");
    assert.match(lines, /already exist/i);
    await waitForDisplays(2); // and does not draw a second copy
  });

  it("/hologram hide despawns the displays", async () => {
    const lines = (await runCommand(bot, "/hologram hide e2e_holo_a")).join("\n");
    assert.match(lines, /\bhid\b/i);
    await waitForDisplays(0);
  });

  it("/hologram show draws them again", async () => {
    const lines = (await runCommand(bot, "/hologram show e2e_holo_a")).join("\n");
    assert.match(lines, /show/i);
    await waitForDisplays(2);
  });

  it("/hologram reload redraws without duplicating displays or list entries", async () => {
    await runCommand(bot, "/hologram reload");
    await waitForDisplays(2);

    const listed = await listHolograms(bot);
    assert.strictEqual(listed.e2e_holo_a?.length, 1, "reload duplicated the hologram in /hologram list");
    assert.strictEqual(listed.skyblock?.length, 1, "reload duplicated the shipped hologram");
  });

  it("edits to Holograms.yml are picked up on reload, and blank lines draw nothing", async () => {
    const holos = readHoloConfig();
    holos.e2e_holo_lines = {
      location: `world, ${AREA.x + 10}, ${AREA.y + Y_OFFSET}, ${AREA.z}`,
      lines: ["&aline one", "", "&bline two", "&cline three"],
    };
    writeHoloConfig(holos);

    await runCommand(bot, "/hologram reload");
    // the blank entry is a spacer: it moves the cursor down but gets no entity
    await waitForDisplays(3, { pos: { ...AREA, x: AREA.x + 10 } });
    await waitForDisplays(2); // and the neighbour is untouched
  });

  it("a hologram in an unloaded world is skipped without breaking the rest", async () => {
    const holos = readHoloConfig();
    holos.e2e_holo_badworld = {
      location: "no_such_world, 0, 100, 0",
      lines: ["&cthis world does not exist"],
    };
    writeHoloConfig(holos);

    await runCommand(bot, "/hologram reload");
    await waitForDisplays(2);

    // the broken entry must not take the command down with it - listing the bad key
    // itself is what proves the command ran to the end rather than throwing part way
    const listed = await listHolograms(bot);
    assert.ok(listed.e2e_holo_a, "a bad entry stopped /hologram list from reporting the good ones");
    assert.ok(listed.e2e_holo_badworld, "/hologram list gave up when it reached the bad entry");

    // and teleporting to it must leave the player where they are
    const before = bot.entity.position.clone();
    await runCommand(bot, "/hologram teleport e2e_holo_badworld", 1500);
    assert.ok(
      bot.entity.position.distanceTo(before) < 1,
      `teleport to an unloaded world moved the player to ${bot.entity.position}`
    );

    const holosAfter = readHoloConfig();
    delete holosAfter.e2e_holo_badworld;
    writeHoloConfig(holosAfter);
    await runCommand(bot, "/hologram reload");
  });

  it("/hologram teleport moves the player to the stored location", async () => {
    const target = parseLocation(readHoloConfig().e2e_holo_a.location);

    const lines = (await runCommand(bot, "/hologram teleport e2e_holo_a")).join("\n");
    assert.match(lines, /teleported to/i);

    await sleep(1500);
    const position = bot.entity.position;
    assert.ok(Math.abs(position.x - target.x) < 1.5, `x was ${position.x}, wanted ${target.x}`);
    assert.ok(Math.abs(position.y - target.y) < 1.5, `y was ${position.y}, wanted ${target.y}`);
    assert.ok(Math.abs(position.z - target.z) < 1.5, `z was ${position.z}, wanted ${target.z}`);

    rcon(`tp ${ADMIN} ${AREA.x} ${AREA.y} ${AREA.z}`);
    await sleep(1000);
  });

  it("/hologram removenear clears the displays around the player", async () => {
    const lines = (await runCommand(bot, "/hologram removenear")).join("\n");
    assert.match(lines, /removed\s+\d+/i);
    await waitForDisplays(0, { radius: 2 });
  });

  it("/hologram remove deletes the hologram, its displays and its config entry", async () => {
    await runCommand(bot, "/hologram show e2e_holo_a");
    await waitForDisplays(2);

    const lines = (await runCommand(bot, "/hologram remove e2e_holo_a")).join("\n");
    assert.match(lines, /removed/i);

    await waitForDisplays(0);
    assert.strictEqual(readHoloConfig().e2e_holo_a, undefined, "config entry survived the remove");

    const listed = await listHolograms(bot);
    assert.strictEqual(listed.e2e_holo_a, undefined, "removed hologram is still listed");
  });

  it("keyed subcommands reject an unknown name", async () => {
    for (const sub of ["remove", "hide", "show", "teleport"]) {
      const lines = (await runCommand(bot, `/hologram ${sub} e2e_holo_missing`, 1500)).join("\n");
      assert.match(
        lines,
        /does not ex[is]*t|no hologram named/i,
        `/hologram ${sub} on a missing key should report it, got: ${lines}`
      );
    }
  });

  it("tab completion offers the subcommands", async () => {
    const matches = (await bot.tabComplete("/hologram ", true)).map((m) => (typeof m === "string" ? m : m.match));
    for (const sub of ["create", "remove", "show", "hide", "list", "teleport", "removenear"]) {
      assert.ok(matches.includes(sub), `expected '${sub}' in tab completion, got ${matches}`);
    }
  });

  it("tab completion offers existing hologram names", async () => {
    await runCommand(bot, "/hologram create e2e_holo_b");
    await sleep(1000);

    const matches = (await bot.tabComplete("/hologram remove ", true)).map((m) => (typeof m === "string" ? m : m.match));
    assert.ok(matches.includes("e2e_holo_b"), `expected 'e2e_holo_b' in tab completion, got ${matches}`);
  });

  it("a player without the permission is refused", async () => {
    guest = await createBot(GUEST);
    rcon(`deop ${GUEST}`);
    rcon(`lp user ${GUEST} permission set hologram.admin false`);
    await sleep(1000);

    const lines = (await runCommand(guest, "/hologram list")).join("\n");
    assert.match(lines, /no permission|do not have access/i);
    assert.doesNotMatch(lines, /SERVERTOOLS HOLOGRAMS/i, "listing leaked to an unprivileged player");
  });

  it("the console can reload holograms", async () => {
    rcon("hologram reload");
    await waitForDisplays(3, { pos: { ...AREA, x: AREA.x + 10 } });
  });
});
