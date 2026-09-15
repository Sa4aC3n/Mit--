const PROJECT_ID = "demo-no-project";
const EMULATOR_HOST = "127.0.0.1";
const EMULATOR_PORT = 8080;

process.env.GCLOUD_PROJECT = PROJECT_ID;
process.env.GOOGLE_CLOUD_PROJECT = PROJECT_ID;
process.env.FIRESTORE_EMULATOR_HOST = `${EMULATOR_HOST}:${EMULATOR_PORT}`;

const admin = require("firebase-admin");
const http = require("http");
const { migrate } = require("../functions/migrateLegacyActivities");
const { approveContribution, rejectContribution } = require("../functions/index");

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

  // Rule 1.5: Public / regular client write to businessSources is FORBIDDEN
  const writePublicSourceRes = await firestoreRestRequest({
    method: "PATCH",
    path: "businessSources/src_test_1",
    body: {
      fields: {
        sourceName: { stringValue: "Hacked Source" }
      }
    }
  });
  assert(writePublicSourceRes.statusCode === 403, "Rule Check: Public cannot write to businessSources (HTTP 403)");

  // Rule 1.6: Public read of privatePayload in businessSources is FORBIDDEN
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

  // Rule 1.7: Regular user read of privatePayload is FORBIDDEN
  const readPrivatePayloadUserRes = await firestoreRestRequest({
    method: "GET",
    path: "businessSources/src_test_1/privatePayload/details",
    auth: { user_id: "regular_user_1", email: "user@test.com" }
  });
  assert(
    readPrivatePayloadUserRes.statusCode === 403,
    "Rule Check: Regular user cannot read businessSources/privatePayload (HTTP 403)"
  );

  // Rule 1.8: Admin user CAN read privatePayload
  const readPrivatePayloadAdminRes = await firestoreRestRequest({
    method: "GET",
    path: "businessSources/src_test_1/privatePayload/details",
    auth: { user_id: "admin_user", admin: true }
  });
  assert(
    readPrivatePayloadAdminRes.statusCode === 200,
    "Rule Check: Admin CAN read businessSources/privatePayload (HTTP 200)"
  );

  // Rule 1.9: Regular user client CANNOT write to privatePayload
  const writePrivatePayloadUserRes = await firestoreRestRequest({
    method: "PATCH",
    path: "businessSources/src_test_1/privatePayload/details",
    auth: { user_id: "regular_user_1", email: "user@test.com" },
    body: {
      fields: {
        rawPayloadJson: { stringValue: "malicious_write" }
      }
    }
  });
  assert(
    writePrivatePayloadUserRes.statusCode === 403,
    "Rule Check: Regular user client cannot write to privatePayload (HTTP 403)"
  );

  // Rule 1.10: Admin client SDK is ALSO blocked from writing to privatePayload (Admin SDK in Cloud Function only!)
  const writePrivatePayloadAdminRes = await firestoreRestRequest({
    method: "PATCH",
    path: "businessSources/src_test_1/privatePayload/details",
    auth: { user_id: "admin_user", admin: true },
    body: {
      fields: {
        rawPayloadJson: { stringValue: "admin_client_direct_write" }
      }
    }
  });
  assert(
    writePrivatePayloadAdminRes.statusCode === 403,
    "Rule Check: Admin client SDK is strictly forbidden from writing privatePayload directly (HTTP 403)"
  );

  // Rule 1.11: User can create contribution with status PENDING for self
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

  // Rule 1.12: User CANNOT create contribution with self-approved status
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

  // Rule 1.13: Another user cannot read user_alpha's contribution
  const readOtherContribRes = await firestoreRestRequest({
    method: "GET",
    path: "contributions/contrib_valid_1",
    auth: { user_id: "user_beta", email: "beta@test.com" }
  });
  assert(
    readOtherContribRes.statusCode === 403,
    "Rule Check: User beta cannot read user alpha's contribution (HTTP 403)"
  );

  // Rule 1.14: User alpha CAN read their own contribution
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
  // TEST SUITE 2: Real Cloud Function Invocation (approveContribution & rejectContribution)
  // -------------------------------------------------------------
  console.log("\n--- [2] Testing Real Cloud Functions (approveContribution & rejectContribution) ---");

  const contribId = "contrib_real_cf_101";
  const userPayload = JSON.stringify({
    name: "صيدلية النور والشفاء",
    phone: "01099887766",
    address: "شارع بورسعيد، ميت غمر",
    categoryName: "صيدليات",
    workingHours: "24 ساعة"
  });

  // Step 2.1: Regular user submits PENDING contribution via Client SDK / REST
  const submitContribRes = await firestoreRestRequest({
    method: "PATCH",
    path: `contributions/${contribId}`,
    auth: { user_id: "user_contributor_77", email: "contributor77@test.com" },
    body: {
      fields: {
        id: { stringValue: contribId },
        userId: { stringValue: "user_contributor_77" },
        userName: { stringValue: "د. هاني" },
        userEmail: { stringValue: "contributor77@test.com" },
        status: { stringValue: "PENDING" },
        type: { stringValue: "NEW_BUSINESS" },
        businessName: { stringValue: "صيدلية النور والشفاء" },
        payloadJson: { stringValue: userPayload },
        userReason: { stringValue: "افتتاح صيدلية جديدة" }
      }
    }
  });
  assert(submitContribRes.statusCode === 200, "User created PENDING contribution successfully");

  // Step 2.2: Regular user attempts to invoke approveContribution Cloud Function
  const regularUserContext = {
    auth: {
      uid: "user_contributor_77",
      token: {
        user_id: "user_contributor_77",
        email: "contributor77@test.com",
        admin: false
      }
    }
  };

  let regularCallBlocked = false;
  try {
    await approveContribution.run({ contributionId: contribId }, regularUserContext);
  } catch (err) {
    if (err.code === "permission-denied" || err.message.includes("Admin Custom Claim")) {
      regularCallBlocked = true;
    }
  }
  assert(regularCallBlocked, "Real Cloud Function: Regular user call to approveContribution was strictly blocked (permission-denied)");

  // Step 2.3: Admin user invokes real approveContribution Cloud Function
  const adminUserContext = {
    auth: {
      uid: "admin_super_user",
      token: {
        user_id: "admin_super_user",
        email: "admin@mitghamrdirectory.com",
        admin: true
      }
    }
  };

  const approveResult = await approveContribution.run(
    { contributionId: contribId, moderatorNote: "معتمد بعد التحقق الميداني" },
    adminUserContext
  );

  assert(approveResult.success === true, "approveContribution returned success: true");
  const publishedBizId = approveResult.publishedBusinessId;
  assert(Boolean(publishedBizId), `Published business ID assigned: ${publishedBizId}`);

  // Verify business document in /businesses
  const bizDoc = await db.collection("businesses").doc(publishedBizId).get();
  assert(bizDoc.exists, "Published business document exists in /businesses");
  assert(bizDoc.data().isPublished === true, "Published business has isPublished: true");
  assert(bizDoc.data().verificationStatus === "VERIFIED", "Published business has verificationStatus: VERIFIED");
  assert(typeof bizDoc.data().updatedAt === "number", "updatedAt is strictly a numeric timestamp (Long)");

  // Verify contribution status updated to APPROVED with publishedBusinessId
  const contribDoc = await db.collection("contributions").doc(contribId).get();
  assert(contribDoc.data().status === "APPROVED", "Contribution doc updated to status: APPROVED");
  assert(contribDoc.data().publishedBusinessId === publishedBizId, "Contribution doc holds publishedBusinessId");

  // Verify audit log record
  const auditSnap = await db.collection("audit_logs").where("targetContributionId", "==", contribId).get();
  assert(auditSnap.size === 1, "Audit log record created for APPROVE_CONTRIBUTION");
  assert(auditSnap.docs[0].data().action === "APPROVE_CONTRIBUTION", "Audit log action is APPROVE_CONTRIBUTION");

  // Verify businessSources public record has NO rawPayloadJson, userEmail, or userReason
  const sourceDoc = await db.collection("businessSources").doc("src_" + contribId).get();
  assert(sourceDoc.exists, "Public businessSource record created");
  assert(sourceDoc.data().rawPayloadJson === undefined, "Public businessSource has NO rawPayloadJson");
  assert(sourceDoc.data().userEmail === undefined, "Public businessSource has NO userEmail");
  assert(sourceDoc.data().userReason === undefined, "Public businessSource has NO userReason");

  // Verify privatePayload subcollection is created with user request details (Admin only)
  const privatePayloadDoc = await sourceDoc.ref.collection("privatePayload").doc("request_details").get();
  assert(privatePayloadDoc.exists, "Private payload quarantined in subcollection exists");
  assert(privatePayloadDoc.data().rawPayloadJson === userPayload, "Private payload contains rawPayloadJson");

  // Step 2.4: Call approveContribution AGAIN (Idempotency check)
  const approveResultReplay = await approveContribution.run(
    { contributionId: contribId },
    adminUserContext
  );
  assert(approveResultReplay.success === true, "Replay approval returned success");
  assert(approveResultReplay.publishedBusinessId === publishedBizId, "Replay approval returned identical business ID");

  // Ensure NO duplicate business documents were created
  const duplicateCheck = await db.collection("businesses").where("approvedContributionId", "==", contribId).get();
  assert(duplicateCheck.size === 1, "Exactly one business document exists; no duplicates generated");

  // Step 2.5: Test rejectContribution Cloud Function
  const rejectContribId = "contrib_to_reject_202";
  await db.collection("contributions").doc(rejectContribId).set({
    id: rejectContribId,
    userId: "user_contributor_77",
    status: "PENDING",
    businessName: "كيان وهمي للرفض",
    createdAt: Date.now()
  });

  let rejectByRegularBlocked = false;
  try {
    await rejectContribution.run({ contributionId: rejectContribId }, regularUserContext);
  } catch (err) {
    if (err.code === "permission-denied") rejectByRegularBlocked = true;
  }
  assert(rejectByRegularBlocked, "Regular user call to rejectContribution blocked (permission-denied)");

  const rejectResult = await rejectContribution.run(
    { contributionId: rejectContribId, reason: "بيانات وهمية غير مطابقة" },
    adminUserContext
  );
  assert(rejectResult.success === true, "rejectContribution executed successfully");

  const rejectedContribDoc = await db.collection("contributions").doc(rejectContribId).get();
  assert(rejectedContribDoc.data().status === "REJECTED", "Contribution status updated to REJECTED");
  assert(rejectedContribDoc.data().publishedBusinessId === undefined, "Rejected contribution has NO publishedBusinessId");

  // Ensure NO business was created for rejected contribution
  const rejectedBizCheck = await db.collection("businesses").where("name", "==", "كيان وهمي للرفض").get();
  assert(rejectedBizCheck.empty, "No business document was published for rejected contribution");

  // -------------------------------------------------------------
  // TEST SUITE 3: Composite Cursor with Equal Timestamps & Browsing Updates
  // -------------------------------------------------------------
  console.log("\n--- [3] Testing Composite Cursor with Equal Timestamps & Browsing Updates ---");

  await clearFirestore();

  // Insert 4 businesses with identical updatedAt = 600000
  const identicalTimestamp = 600000;
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

  const lastDocPage1 = page1Snap.docs[page1Snap.docs.length - 1];

  // SIMULATE BROWSING: A new business is added concurrently with the exact same timestamp!
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

  // SIMULATE ANOTHER UPDATE WHILE BROWSING: An existing later item is modified with a newer timestamp
  await db.collection("businesses").doc("biz_cc_4").update({
    updatedAt: 750000
  });

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

  assert(page3Snap.size === 1 && page3Snap.docs[0].id === "biz_cc_4", "Page 3 cleanly finished stream with biz_cc_4 even after timestamp update");

  // -------------------------------------------------------------
  // TEST SUITE 4: Safe Legacy Migration & Explicit Approval Verification
  // -------------------------------------------------------------
  console.log("\n--- [4] Testing Safe Legacy Migration & Explicit Approval Verification ---");

  await clearFirestore();

  // Case 4.1: Existing business in /businesses WITHOUT isPublished and isVerified: false
  await db.collection("businesses").doc("biz_legacy_unverified").set({
    id: "biz_legacy_unverified",
    name: "سجل قديم غير موثق وبدون isPublished",
    phone: "01011112222",
    isVerified: false,
    // isPublished is deliberately undefined!
    updatedAt: Date.now()
  });

  // Case 4.2: Existing business in /businesses WITH explicit approval
  await db.collection("businesses").doc("biz_legacy_approved").set({
    id: "biz_legacy_approved",
    name: "سجل قديم معتمد صراحة",
    phone: "01033334444",
    isVerified: true,
    isActive: true,
    verificationStatus: "VERIFIED",
    // isPublished undefined, but has explicit proof!
    updatedAt: Date.now()
  });

  // Case 4.3: Legacy activity in /activities with status DRAFT
  await db.collection("activities").doc("act_draft_1").set({
    name: "مسودة نشاط لم تكتمل",
    phone: "01055556666",
    address: "ميت غمر",
    status: "DRAFT"
  });

  // Case 4.4: Legacy activity with isDeleted: true
  await db.collection("activities").doc("act_deleted_2").set({
    name: "نشاط تم حذفه سابقًا",
    phone: "01077778888",
    address: "ميت غمر",
    isDeleted: true
  });

  // Case 4.5: Legacy activity with explicit approval (isPublished: true, isVerified: true, isActive: true)
  await db.collection("activities").doc("act_explicit_approved_3").set({
    name: "محل معتمد وموثق تمامًا",
    phone: "01099990000",
    address: "ميت غمر - شارع بورسعيد",
    isPublished: true,
    isVerified: true,
    isActive: true
  });

  // Case 4.6: Incomplete legacy activity (missing phone and address)
  await db.collection("activities").doc("act_incomplete_4").set({
    name: "ورشة بدون تليفون أو عنوان"
  });

  // Case 4.7: Ambiguous duplicate phone (same phone as biz_legacy_approved, completely different name)
  await db.collection("activities").doc("act_ambiguous_5").set({
    name: "صيدلية مختلفة تمامًا برقم مكرر",
    phone: "01033334444", // Same phone as biz_legacy_approved
    isPublished: true,
    isVerified: true,
    isActive: true
  });

  // Case 4.8: Duplicate with similar name to merge
  await db.collection("activities").doc("act_duplicate_merge_6").set({
    name: "سجل قديم معتمد صراحة فرع 2", // Similar name to biz_legacy_approved
    phone: "01033334444",
    whatsapp: "01033334444",
    phoneSecondary: "0506999999"
  });

  // Test 4.A: Dry-Run Mode
  console.log("\nExecuting migration in --dry-run mode...");
  const dryResult = await migrate({ dryRun: true });

  assert(dryResult.isDryRun === true, "dryRun is recognized");
  assert(dryResult.migratedExplicitlyApprovedPublished === 1, "Dry-run: Exactly 1 legacy activity has explicit approval for publishing");
  assert(dryResult.migratedUnprovenPendingReview === 1, "Dry-run: Draft activity flagged as unproven pending review");
  assert(dryResult.migratedDeletedOrArchived === 1, "Dry-run: Deleted activity flagged as deleted/archived");
  assert(dryResult.migratedIncomplete === 1, "Dry-run: Incomplete activity flagged as incomplete");
  assert(dryResult.ambiguousDuplicatePhoneReview === 1, "Dry-run: Ambiguous phone flagged as review");
  assert(dryResult.duplicatesMerged === 1, "Dry-run: Duplicate matched for merge");

  // Verify NO writes occurred in dry-run mode
  const bizCheckDry = await db.collection("businesses").get();
  assert(bizCheckDry.size === 2, "In dry-run, no new documents were written to /businesses");

  // Test 4.B: Live Execution
  console.log("\nExecuting migration in LIVE mode...");
  const liveResult = await migrate({ dryRun: false });

  assert(liveResult.isDryRun === false, "Live execution mode executed");
  assert(liveResult.migratedExplicitlyApprovedPublished === 1, "Live: 1 explicitly approved published");
  assert(liveResult.migratedUnprovenPendingReview === 1, "Live: 1 unproven pending review");
  assert(liveResult.migratedDeletedOrArchived === 1, "Live: 1 deleted/archived");
  assert(liveResult.migratedIncomplete === 1, "Live: 1 incomplete");
  assert(liveResult.ambiguousDuplicatePhoneReview === 1, "Live: 1 ambiguous phone");
  assert(liveResult.duplicatesMerged === 1, "Live: 1 duplicate merged");

  // Verify Case 4.1: Existing business without isPublished & isVerified=false became isPublished: false
  const updatedUnverifiedBiz = await db.collection("businesses").doc("biz_legacy_unverified").get();
  assert(
    updatedUnverifiedBiz.data().isPublished === false &&
      updatedUnverifiedBiz.data().verificationStatus === "LEGACY_STATUS_REVIEW",
    "Existing business with no isPublished & isVerified=false default safely to isPublished: false"
  );

  // Verify Case 4.2: Existing business with explicit approval became isPublished: true
  const updatedApprovedBiz = await db.collection("businesses").doc("biz_legacy_approved").get();
  assert(
    updatedApprovedBiz.data().isPublished === true &&
      updatedApprovedBiz.data().verificationStatus === "VERIFIED",
    "Existing business with explicit approval resolved to isPublished: true"
  );

  // Verify Case 4.3: Legacy DRAFT activity migrated as isPublished: false
  const draftBiz = await db.collection("businesses").doc("act_draft_1").get();
  assert(
    draftBiz.exists && draftBiz.data().isPublished === false && draftBiz.data().verificationStatus === "DRAFT_REVIEW",
    "Legacy DRAFT activity migrated with isPublished: false and verificationStatus: DRAFT_REVIEW"
  );

  // Verify Case 4.4: Deleted activity migrated as isPublished: false, isDeleted: true
  const deletedBiz = await db.collection("businesses").doc("act_deleted_2").get();
  assert(
    deletedBiz.exists &&
      deletedBiz.data().isPublished === false &&
      deletedBiz.data().isDeleted === true &&
      deletedBiz.data().isActive === false,
    "Deleted activity preserved with isPublished: false, isDeleted: true (NEVER reactivated)"
  );

  // Verify Case 4.5: Explicitly approved activity migrated as isPublished: true
  const explicitBiz = await db.collection("businesses").doc("act_explicit_approved_3").get();
  assert(
    explicitBiz.exists && explicitBiz.data().isPublished === true && explicitBiz.data().isVerified === true,
    "Explicitly approved legacy activity published safely with isPublished: true"
  );

  // Verify Case 4.6: Incomplete activity migrated as isPublished: false
  const incompleteBiz = await db.collection("businesses").doc("act_incomplete_4").get();
  assert(
    incompleteBiz.exists && incompleteBiz.data().isPublished === false && incompleteBiz.data().verificationStatus === "INCOMPLETE_DATA_REVIEW",
    "Incomplete activity migrated with isPublished: false"
  );

  // Verify Case 4.7: Ambiguous phone activity kept separate with isPublished: false
  const ambiguousBiz = await db.collection("businesses").doc("act_ambiguous_5").get();
  assert(
    ambiguousBiz.exists && ambiguousBiz.data().isPublished === false && ambiguousBiz.data().verificationStatus === "AMBIGUOUS_PHONE_REVIEW",
    "Ambiguous phone collision kept separate with verificationStatus: AMBIGUOUS_PHONE_REVIEW"
  );

  // Verify Case 4.8: Merged business received secondary phone and whatsapp
  const mergedBiz = await db.collection("businesses").doc("biz_legacy_approved").get();
  assert(
    mergedBiz.data().phoneSecondary === "0506999999" && mergedBiz.data().whatsapp === "01033334444",
    "Merged business updated with secondary phone and whatsapp"
  );

  // Test 4.C: Idempotency (Safe Re-run)
  console.log("\nTesting migration idempotency (re-running)...");
  const reRunResult = await migrate({ dryRun: false });
  assert(reRunResult.alreadyMigratedSkipped >= 5, "Re-run safely skipped all previously migrated activities without duplicate writes");

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
