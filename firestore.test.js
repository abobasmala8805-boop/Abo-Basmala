const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validLevelPayload(uid, visibility = "PRIVATE") {
  const now = new Date();
  return {
    userId: uid,
    authorName: "Alice Designer",
    title: "Mystic Forest Lights",
    scenePrompt: "Bioluminescent forest with floating lanterns and glowing mushrooms",
    theme: "NATURE",
    variationCount: 5,
    timeLimitSeconds: 90,
    patternComplexity: "MODERATE",
    visibility: visibility,
    shareCode: "SPDF42",
    spotsData: "0.25,0.35,0.08,Lantern,COLOR_SHIFT,#38BDF8",
    playsCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validLeaderboardPayload(uid, boardId = "GLOBAL") {
  const now = new Date();
  return {
    userId: uid,
    playerName: "Alice Speedrunner",
    playerAvatarData: "preset:cyber_detective",
    boardId: boardId,
    theme: "SPACE",
    variationCount: 5,
    patternComplexity: "INTRICATE",
    completionTimeMillis: 24500,
    score: 1850,
    levelTitle: "Orbital Station Alpha",
    createdAt: now,
    updatedAt: now,
  };
}

function validUserProfilePayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    displayName: "Alice Speedrunner",
    avatarData: "preset:cyber_detective",
    avatarPrompt: "Neon cyber detective fox",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write custom_levels, leaderboard_entries, or user_profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("custom_levels").get());
  await assertFails(unauthDb.collection("leaderboard_entries").get());
  await assertFails(unauthDb.collection("user_profiles").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("custom_levels").doc("lvl_1").set(validLevelPayload(ALICE_UID)));
});

test("Authenticated user: can create their own valid custom level, leaderboard entry with avatar, and user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("custom_levels").doc("lvl_alice").set(validLevelPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("leaderboard_entries").doc("score_alice").set(validLeaderboardPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("user_profiles").doc(ALICE_UID).set(validUserProfilePayload(ALICE_UID)));
});

test("Identity spoofing: cannot create level, leaderboard entry, or profile with another user's userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("custom_levels").doc("lvl_spoof").set(validLevelPayload(BOB_UID)));
  await assertFails(aliceDb.collection("leaderboard_entries").doc("score_spoof").set(validLeaderboardPayload(BOB_UID)));
  await assertFails(aliceDb.collection("user_profiles").doc(BOB_UID).set(validUserProfilePayload(BOB_UID)));
});

test("Shadow update / ghost field: rejects extra unauthorized fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payloadWithGhost = { ...validLevelPayload(ALICE_UID), isAdminVerified: true };
  await assertFails(aliceDb.collection("custom_levels").doc("lvl_ghost").set(payloadWithGhost));
});

test("Cross-user isolation: cannot read or delete another user's private custom level", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("custom_levels").doc("bob_private").set(validLevelPayload(BOB_UID, "PRIVATE"));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("custom_levels").doc("bob_private").get());
  await assertFails(aliceDb.collection("custom_levels").doc("bob_private").delete());
});

test("Shared custom level: another user can read a SHARED custom level when filtering by visibility == SHARED, but cannot delete it", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("custom_levels").doc("bob_shared").set(validLevelPayload(BOB_UID, "SHARED"));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("custom_levels").where("visibility", "==", "SHARED").get());
  await assertFails(aliceDb.collection("custom_levels").doc("bob_shared").delete());
});

test("Query enforcer: unfiltered query on custom_levels or leaderboard_entries fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("custom_levels").get());
  await assertFails(aliceDb.collection("leaderboard_entries").get());
});

test("Global leaderboard: authenticated user can query GLOBAL leaderboard entries with boardId filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("leaderboard_entries").doc("bob_global").set(validLeaderboardPayload(BOB_UID, "GLOBAL"));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("leaderboard_entries").where("boardId", "==", "GLOBAL").get()
  );
});
