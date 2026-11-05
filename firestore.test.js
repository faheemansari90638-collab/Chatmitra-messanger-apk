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

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      phoneNumber: "+919876543210",
      chatMitraId: "alice_mitra",
      about: "Available on ChatMitra",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create profile for another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      phoneNumber: "+919876543211",
      chatMitraId: "bob_mitra",
      about: "Hey",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create a conversation they participate in", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_123").set({
      id: "conv_123",
      type: "direct",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot read conversation they do not belong to", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("conversations").doc("secret_conv").set({
      id: "secret_conv",
      type: "direct",
      participantUids: [BOB_UID, "charlie_789"],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("conversations").doc("secret_conv").get());
});

test("Participant: can send message to conversation", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("conversations").doc("conv_123").set({
      id: "conv_123",
      type: "direct",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_123").collection("messages").doc("msg_1").set({
      id: "msg_1",
      conversationId: "conv_123",
      senderId: ALICE_UID,
      senderName: "Alice",
      content: "Hello Bob!",
      type: "text",
      status: "sent",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Non-participant: cannot send message to conversation", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("conversations").doc("conv_bob_charlie").set({
      id: "conv_bob_charlie",
      type: "direct",
      participantUids: [BOB_UID, "charlie_789"],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("conversations").doc("conv_bob_charlie").collection("messages").doc("msg_bad").set({
      id: "msg_bad",
      conversationId: "conv_bob_charlie",
      senderId: ALICE_UID,
      senderName: "Alice",
      content: "Sneaky message",
      type: "text",
      status: "sent",
      participantUids: [BOB_UID, "charlie_789"],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
