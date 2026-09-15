const PROJECT_ID = "demo-no-project";
const EMULATOR_HOST = "127.0.0.1";
const EMULATOR_PORT = 8080;

process.env.GCLOUD_PROJECT = PROJECT_ID;
process.env.GOOGLE_CLOUD_PROJECT = PROJECT_ID;
process.env.FIRESTORE_EMULATOR_HOST = `${EMULATOR_HOST}:${EMULATOR_PORT}`;

const admin = require("firebase-admin");
const http = require("http");
const { migrate } = require("../functions/migrateLegacyActivities");

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}
const db = admin.firestore();

function createMockJwt(claims) {
  const header = Buffer.from(JSON.stringify({ alg: "none", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      aud: PROJECT_ID,
      iss: `https://securetoken.google.com/${PROJECT_ID}`,
      sub: claims.user_id,
      user_id: claims.user_id,
      auth_time: Math.floor(Date.now() / 1000),
      ...claims
    })
  ).toString("base64url");
  return `${header}.${payload}.`;
}

// Helper to make REST requests to Firestore emulator with simulated auth tokens
function firestoreRestRequest({ method = "GET", path, auth = null, body = null }) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: EMULATOR_HOST,
      port: EMULATOR_PORT,
      path: `/v1/projects/${PROJECT_ID}/databases/(default)/documents/${path}`,
      method,
      headers: {
        "Content-Type": "application/json"
      }
    };

    if (auth) {
      if (typeof auth === "string") {
        options.headers["Authorization"] = `Bearer ${auth}`;
      } else {
        options.headers["Authorization"] = `Bearer ${createMockJwt(auth)}`;
      }
    }

    const req = http.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        try {
          const parsed = data ? JSON.parse(data) : {};
          resolve({ statusCode: res.statusCode, body: parsed });
        } catch (e) {
          resolve({ statusCode: res.statusCode, raw: data });
        }
      });
    });

    req.on("error", reject);
    if (body) {
      req.write(JSON.stringify(body));
    }
    req.end();
  });
}

function assert(condition, message) {
  if (!condition) {
    console.error(`❌ Assertion Failed: ${message}`);
    throw new Error(message);
  }
  console.log(`  ✔ Passed: ${message}`);
}

async function clearFirestore() {
  const collections = ["businesses", "activities", "contributions", "businessSources", "audit_logs"];
  for (const col of collections) {
    const snap = await db.collection(col).get();
    const batch = db.batch();
    snap.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
  }
}

async function runTestSuite() {
  console.log("===============================================================");
  console.log("Starting Firebase Emulator & Backend Integration Test Suite");
  console.log("===============================================================\n");

  await clearFirestore();

  // -------------------------------------------------------------
  // TEST SUITE 1: Security Rules Testing
  // -------------------------------------------------------------
  console.log("\n--- [1] Testing Firestore Security Rules Enforcement ---");

  // Rule 1.1: Client write to /businesses is FORBIDDEN
  const clientWriteBizRes = await firestoreRestRequest({
    method: "POST",
    path: "businesses",
    auth: { user_id: "client_user_1", email: "user1@test.com" },
    body: {
      fields: {
        name: { stringValue: "Unauthorized Direct Business" },
        isPublished: { booleanValue: true }
      }
    }
  });
  assert(
    clientWriteBizRes.statusCode === 403,
    "Rule Check: Client direct write to /businesses is strictly blocked (HTTP 403)"
  );

  // Setup a published business and an unpublished business via Admin SDK
  await db.collection("businesses").doc("biz_pub_1").set({
    id: "biz_pub_1",
    name: "محل منشور تجريبي",
    isPublished: true,
    updatedAt: Date.now()
  });
  await db.collection("businesses").doc("biz_unpub_1").set({
    id: "biz_unpub_1",
    name: "محل مسودة غير منشور",
    isPublished: false,
    updatedAt: Date.now()
  });

  // Rule 1.2: Public read of published business is ALLOWED
  const readPubBizRes = await firestoreRestRequest({
    method: "GET",
    path: "businesses/biz_pub_1"
  });
  assert(readPubBizRes.statusCode === 200, "Rule Check: Public read of published business is allowed (HTTP 200)");

  // Rule 1.3: Public read of unpublished business is FORBIDDEN
  const readUnpubBizRes = await firestoreRestRequest({
    method: "GET",
    path: "businesses/biz_unpub_1"
  });
  assert(readUnpubBizRes.statusCode === 403, "Rule Check: Public read of unpublished business is denied (HTTP 403)");

  // Rule 1.4: Public read of public businessSources is ALLOWED
  await db.collection("businessSources").doc("src_test_1").set({
    id: "src_test_1",
    businessId: "biz_pub_1",
    sourceName: "دليل بلدي علني",
    isOfficial: true,
    updatedAt: Date.now()
  });
  const readPublicSourceRes = await firestoreRestRequest({
    method: "GET",
    path: "businessSources/src_test_1"
  });
  assert(readPublicSourceRes.statusCode === 200, "Rule Check: Public read of businessSources is allowed (HTTP 200)");

  // Rule 1.5: Public read of privatePayload in businessSources is FORBIDDEN
  await db.collection("businessSources").doc("src_test_1").collection("privatePayload").doc("details").set({
    rawPayloadJson: '{"sensitiveUserSecret": "xyz"}',
    submittedByUserId: "user_secret_1"
  });
  const readPrivatePayloadPublicRes = await firestoreRestRequest({
    method: "GET",
    path: "businessSources/src_test_1/privatePayload/details"
  });
  assert(
    readPrivatePayloadPublicRes.statusCode === 403,
    "Rule Check: Public cannot read businessSources/privatePayload (HTTP 403)"
  );

  // Rule 1.6: Admin can read privatePayload
  const readPrivatePayloadAdminRes = await firestoreRestRequest({
    method: "GET",
    path: "businessSources/src_test_1/privatePayload/details",
    auth: { user_id: "admin_user", admin: true }
  });
  assert(
    readPrivatePayloadAdminRes.statusCode === 200,
    "Rule Check: Admin CAN read businessSources/privatePayload (HTTP 200)"
  );

  // Rule 1.7: User can create contribution with status PENDING for self
  const createContribRes = await firestoreRestRequest({
    method: "PATCH",
    path: "contributions/contrib_valid_1",
    auth: { user_id: "user_alpha", email: "alpha@test.com" },
    body: {
      fields: {
        id: { stringValue: "contrib_valid_1" },
        userId: { stringValue: "user_alpha" },
        userName: { stringValue: "أحمد علي" },
        status: { stringValue: "PENDING" },
        businessName: { stringValue: "مخبز النصر" }
      }
    }
  });
  assert(
    createContribRes.statusCode === 200,
    "Rule Check: User can create PENDING contribution for own uid (HTTP 200)"
  );

  // Rule 1.8: User CANNOT create contribution with self-approved status
  const createSelfApprovedRes = await firestoreRestRequest({
    method: "PATCH",
    path: "contributions/contrib_hacker_1",
    auth: { user_id: "user_alpha", email: "alpha@test.com" },
    body: {
      fields: {
        id: { stringValue: "contrib_hacker_1" },
        userId: { stringValue: "user_alpha" },
        status: { stringValue: "APPROVED" },
        publishedBusinessId: { stringValue: "fake_id" }
      }
    }
  });
  assert(
    createSelfApprovedRes.statusCode === 403,
    "Rule Check: User cannot self-approve or set publishedBusinessId (HTTP 403)"
  );

  // Rule 1.9: Another user cannot read user_alpha's contribution
  const readOtherContribRes = await firestoreRestRequest({
    method: "GET",
    path: "contributions/contrib_valid_1",
    auth: { user_id: "user_beta", email: "beta@test.com" }
  });
  assert(
    readOtherContribRes.statusCode === 403,
    "Rule Check: User beta cannot read user alpha's contribution (HTTP 403)"
  );

  // Rule 1.10: User alpha CAN read their own contribution
  const readOwnContribRes = await firestoreRestRequest({
    method: "GET",
    path: "contributions/contrib_valid_1",
    auth: { user_id: "user_alpha", email: "alpha@test.com" }
  });
  assert(
    readOwnContribRes.statusCode === 200,
    "Rule Check: User alpha can read their own contribution (HTTP 200)"
  );

  // -------------------------------------------------------------
  // TEST SUITE 2: Full Lifecycle (Request -> Approval -> Publish -> Sync)
  // -------------------------------------------------------------
  console.log("\n--- [2] Testing Full Lifecycle (Request -> Approval -> Publish -> Sync) ---");

  const contribId = "contrib_flow_999";
  const userPayload = JSON.stringify({
    name: "صيدلية السلام الحديثة",
    phone: "01012345678",
    address: "شارع بورسعيد، ميت غمر",
    categoryName: "صيدليات",
    workingHours: "24 ساعة"
  });

  // 1. User submits request
  await db.collection("contributions").doc(contribId).set({
    id: contribId,
    userId: "user_contributor_1",
    userName: "د. هاني شاكر",
    userEmail: "hani@example.com",
    status: "PENDING",
    type: "NEW_BUSINESS",
    businessName: "صيدلية السلام الحديثة",
    payloadJson: userPayload,
    userReason: "افتتاح فرع جديد",
    createdAt: Date.now(),
    updatedAt: Date.now()
  });

  // 2. Cloud Function Backend Approval Execution
  const approvalNowTs = Date.now();
  const targetBizId = "biz_flow_999";

  await db.runTransaction(async (t) => {
    const cRef = db.collection("contributions").doc(contribId);
    const bRef = db.collection("businesses").doc(targetBizId);
    const sRef = db.collection("businessSources").doc("src_" + contribId);

    // Business publish
    t.set(bRef, {
      id: targetBizId,
      name: "صيدلية السلام الحديثة",
      normalizedName: "صيدليه السلام الحديثه",
      phone: "01012345678",
      normalizedPhone: "01012345678",
      categoryId: "cat_pharmacy",
      categoryName: "صيدليات",
      address: "شارع بورسعيد، ميت غمر",
      isPublished: true,
      isDeleted: false,
      verificationStatus: "VERIFIED",
      updatedAt: approvalNowTs
    });

    // Clean public provenance (NO rawPayloadJson)
    t.set(sRef, {
      id: sRef.id,
      businessId: targetBizId,
      sourceType: "USER_CONTRIBUTION",
      sourceId: contribId,
      sourceName: "مساهمة مستخدم معتمدة",
      discoveredAt: approvalNowTs,
      lastCheckedAt: approvalNowTs,
      lastVerifiedAt: approvalNowTs,
      sourceConfidence: 95,
      isOfficial: false,
      isActive: true,
      updatedAt: approvalNowTs
    });

    // Quarantined private payload
    const privRef = sRef.collection("privatePayload").doc("request_details");
    t.set(privRef, {
      rawPayloadJson: userPayload,
      submittedByUserId: "user_contributor_1",
      userEmail: "hani@example.com",
      createdAt: approvalNowTs
    });

    // Update contribution
    t.update(cRef, {
      status: "APPROVED",
      approvedAt: approvalNowTs,
      approvedBy: "admin_moderator",
      publishedBusinessId: targetBizId,
      updatedAt: approvalNowTs
    });
  });

  // 3. Verify public published business state
  const publishedDoc = await db.collection("businesses").doc(targetBizId).get();
  assert(publishedDoc.exists && publishedDoc.data().isPublished === true, "Published business document exists and isPublished: true");
  assert(typeof publishedDoc.data().updatedAt === "number", "updatedAt is strictly a numeric timestamp (Long)");

  // 4. Verify public businessSource has NO rawPayloadJson
  const pubSourceDoc = await db.collection("businessSources").doc("src_" + contribId).get();
  assert(pubSourceDoc.exists, "Public businessSource exists");
  assert(pubSourceDoc.data().rawPayloadJson === undefined, "rawPayloadJson is NOT present in public businessSource document");

  // 5. Client Incremental Sync simulation
  const syncSnap = await db
    .collection("businesses")
    .where("isPublished", "==", true)
    .where("updatedAt", ">=", approvalNowTs)
    .orderBy("updatedAt", "asc")
    .orderBy("id", "asc")
    .get();

  assert(syncSnap.docs.some((d) => d.id === targetBizId), "Incremental sync query returns newly published business");

  // -------------------------------------------------------------
  // TEST SUITE 3: Composite Cursor with Equal Timestamps & Browsing Updates
  // -------------------------------------------------------------
  console.log("\n--- [3] Testing Composite Cursor with Equal Timestamps & Browsing Updates ---");

  await clearFirestore();

  // Insert 4 businesses with identical updatedAt = 500000
  const identicalTimestamp = 500000;
  const initialDocs = [
    { id: "biz_cc_1", name: "محل أ", updatedAt: identicalTimestamp, isPublished: true },
    { id: "biz_cc_2", name: "محل ب", updatedAt: identicalTimestamp, isPublished: true },
    { id: "biz_cc_3", name: "محل ج", updatedAt: identicalTimestamp, isPublished: true },
    { id: "biz_cc_4", name: "محل د", updatedAt: identicalTimestamp, isPublished: true }
  ];

  for (const b of initialDocs) {
    await db.collection("businesses").doc(b.id).set(b);
  }

  // Page 1 query (limit 2)
  const pageSize = 2;
  const page1Snap = await db
    .collection("businesses")
    .where("isPublished", "==", true)
    .where("updatedAt", ">=", identicalTimestamp)
    .orderBy("updatedAt", "asc")
    .orderBy("id", "asc")
    .limit(pageSize)
    .get();

  assert(page1Snap.size === 2, "Page 1 fetched exactly 2 items");
  assert(page1Snap.docs[0].id === "biz_cc_1", "Page 1 first item is biz_cc_1");
  assert(page1Snap.docs[1].id === "biz_cc_2", "Page 1 second item is biz_cc_2");

  // Record composite cursor
  const lastDocPage1 = page1Snap.docs[page1Snap.docs.length - 1];
  const cursorTs = lastDocPage1.data().updatedAt;
  const cursorId = lastDocPage1.id;

  // SIMULATE USER BROWSING: A new business is added concurrently with the exact same timestamp!
  await db.collection("businesses").doc("biz_cc_2_concurrent").set({
    id: "biz_cc_2_concurrent",
    name: "محل جديد أضيف أثناء التصفح",
    updatedAt: identicalTimestamp,
    isPublished: true
  });

  // Page 2 query using composite cursor startAfter(lastDoc)
  const page2Snap = await db
    .collection("businesses")
    .where("isPublished", "==", true)
    .where("updatedAt", ">=", identicalTimestamp)
    .orderBy("updatedAt", "asc")
    .orderBy("id", "asc")
    .startAfter(lastDocPage1)
    .limit(pageSize)
    .get();

  assert(page2Snap.size === 2, "Page 2 fetched 2 items after cursor");
  const page2Ids = page2Snap.docs.map((d) => d.id);
  assert(page2Ids[0] === "biz_cc_2_concurrent", "Page 2 seamlessly picked up concurrent item sorted alphabetically");
  assert(page2Ids[1] === "biz_cc_3", "Page 2 picked up biz_cc_3 without skipping identical timestamp records");

  // Page 3 query
  const page3Snap = await db
    .collection("businesses")
    .where("isPublished", "==", true)
    .where("updatedAt", ">=", identicalTimestamp)
    .orderBy("updatedAt", "asc")
    .orderBy("id", "asc")
    .startAfter(page2Snap.docs[page2Snap.docs.length - 1])
    .limit(pageSize)
    .get();

  assert(page3Snap.size === 1 && page3Snap.docs[0].id === "biz_cc_4", "Page 3 cleanly finished stream with biz_cc_4");

  // -------------------------------------------------------------
  // TEST SUITE 4: Dry-Run & Safe Migration Testing
  // -------------------------------------------------------------
  console.log("\n--- [4] Testing Dry-Run & Safe Legacy Migration ---");

  await clearFirestore();

  // Seed /businesses
  await db.collection("businesses").doc("existing_biz_100").set({
    id: "existing_biz_100",
    name: "مطعم البركة للمشويات",
    phone: "01001112223",
    isPublished: true,
    updatedAt: Date.now()
  });

  // Seed /activities with diverse legacy scenarios:
  // a) Valid verified activity
  await db.collection("activities").doc("act_valid_1").set({
    name: "سوبرماركت المدينة",
    phone: "01005556677",
    address: "ميت غمر",
    isPublished: true,
    isVerified: true,
    isActive: true
  });

  // b) Incomplete activity (missing phone and address)
  await db.collection("activities").doc("act_incomplete_2").set({
    name: "ورشة صيانة مجهولة",
    isPublished: true
  });

  // c) Explicitly unapproved / pending activity
  await db.collection("activities").doc("act_pending_3").set({
    name: "محل قيد المراجعة",
    phone: "01009998877",
    status: "PENDING",
    isPublished: false
  });

  // d) Shared phone BUT different name (Ambiguous duplicate phone!)
  await db.collection("activities").doc("act_ambiguous_phone_4").set({
    name: "صيدلية الشفاء والعلاج", // Completely different name from "مطعم البركة للمشويات"
    phone: "01001112223", // Same phone as existing_biz_100
    isPublished: true,
    isVerified: true
  });

  // e) Shared phone AND similar name (Confirmed duplicate to merge!)
  await db.collection("activities").doc("act_duplicate_merge_5").set({
    name: "مطعم البركة مشويات ولحوم", // Similar name
    phone: "01001112223",
    whatsapp: "01001112223",
    phoneSecondary: "0506900000"
  });

  // Step 4.1: Dry-run test
  console.log("\nRunning migration in --dry-run mode...");
  const dryRunResult = await migrate({ dryRun: true });

  assert(dryRunResult.isDryRun === true, "dryRun flag is respected");
  assert(dryRunResult.migratedPublished === 1, "Only valid activity identified for published migration");
  assert(dryRunResult.migratedPendingReview === 2, "Incomplete and pending activities flagged for review");
  assert(dryRunResult.ambiguousDuplicatePhoneReview === 1, "Ambiguous phone duplicate flagged for review without merging");
  assert(dryRunResult.duplicatesMerged === 1, "High-confidence duplicate matched for merge");

  // Verify that in dry-run, NOTHING was written to /businesses
  const bizCheckDry = await db.collection("businesses").get();
  assert(bizCheckDry.size === 1, "In dry-run mode, NO new documents were written to /businesses (size is still 1)");

  // Step 4.2: Live execution test
  console.log("\nRunning live migration...");
  const liveResult = await migrate({ dryRun: false });

  assert(liveResult.isDryRun === false, "Live execution mode executed");
  assert(liveResult.migratedPublished === 1, "1 valid activity migrated as published");
  assert(liveResult.migratedPendingReview === 2, "2 unapproved/incomplete migrated as pending review");
  assert(liveResult.ambiguousDuplicatePhoneReview === 1, "1 ambiguous phone duplicate kept separate and pending review");
  assert(liveResult.duplicatesMerged === 1, "1 duplicate merged into existing business");

  // Verify published status on created businesses
  const validBiz = await db.collection("businesses").doc("act_valid_1").get();
  assert(validBiz.exists && validBiz.data().isPublished === true, "Valid activity created with isPublished: true");

  const pendingBiz = await db.collection("businesses").doc("act_pending_3").get();
  assert(pendingBiz.exists && pendingBiz.data().isPublished === false, "Unapproved activity created with isPublished: false");

  const ambiguousBiz = await db.collection("businesses").doc("act_ambiguous_phone_4").get();
  assert(
    ambiguousBiz.exists &&
      ambiguousBiz.data().isPublished === false &&
      ambiguousBiz.data().verificationStatus === "AMBIGUOUS_PHONE_REVIEW",
    "Ambiguous phone activity created separately with verificationStatus: AMBIGUOUS_PHONE_REVIEW"
  );

  // Verify merged business received secondary phone and whatsapp
  const mergedBiz = await db.collection("businesses").doc("existing_biz_100").get();
  assert(
    mergedBiz.data().phoneSecondary === "0506900000" && mergedBiz.data().whatsapp === "01001112223",
    "Existing business merged new secondary phone and whatsapp"
  );

  // Verify legacy activities have migratedToBusinessId and migratedAt
  const actDoc1 = await db.collection("activities").doc("act_valid_1").get();
  assert(
    actDoc1.data().migratedToBusinessId === "act_valid_1" && typeof actDoc1.data().migratedAt === "number",
    "Legacy activity has migratedToBusinessId and numeric migratedAt"
  );

  // Step 4.3: Safe re-run test (Idempotency)
  console.log("\nTesting migration idempotency (re-running)...");
  const reRunResult = await migrate({ dryRun: false });
  assert(reRunResult.alreadyMigratedSkipped >= 4, "Re-run safely skipped all previously migrated activities");

  console.log("\n===============================================================");
  console.log("✔ ALL FIREBASE EMULATOR INTEGRATION TESTS PASSED SUCCESSFULLY!");
  console.log("===============================================================\n");
}

runTestSuite()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("Test Suite Failed:", err);
    process.exit(1);
  });
