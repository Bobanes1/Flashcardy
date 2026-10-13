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

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read decks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("decks").get());
});

test("Bob cannot read Alice's decks", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("decks").doc("deck_1").set({
      userId: ALICE_UID,
      title: "Alice Deck",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("decks").doc("deck_1").get());
});

test("Alice can create and read her own deck", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("decks").doc("deck_1").set({
      userId: ALICE_UID,
      title: "Spanish 101",
      description: "Basic vocabulary",
      category: "Languages",
      colorHex: "#4F46E5",
      cardCount: 0,
      isPublic: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("decks").doc("deck_1").get()
  );
});

test("Alice can create cards with SM-2 fields, Bob cannot read them", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("decks").doc("deck_1").collection("cards").doc("card_1").set({
      deckId: "deck_1",
      front: "Hola",
      back: "Hello",
      hint: "Greeting",
      notes: "Used anytime",
      isStarred: false,
      repetitions: 0,
      intervalDays: 1,
      easeFactor: 2.5,
      nextReviewDate: Date.now(),
      lastReviewedDate: 0,
      reviewCount: 0,
      masteryLevel: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("decks").doc("deck_1").collection("cards").doc("card_1").get()
  );
});

test("Authenticated user can browse public decks; Bob cannot overwrite Alice's public deck", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("public_decks").doc("shared_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      title: "Global Spanish",
      description: "Community deck",
      category: "Languages",
      cardCount: 10,
      cardsJson: "[]",
      downloads: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("public_decks").doc("shared_1").get());

  // Bob tries to change the title of Alice's deck
  await assertFails(
    bobDb.collection("public_decks").doc("shared_1").update({
      title: "Hacked by Bob",
      updatedAt: new Date(),
    })
  );

  // Bob can increment download count
  await assertSucceeds(
    bobDb.collection("public_decks").doc("shared_1").update({
      downloads: 1,
    })
  );
});
