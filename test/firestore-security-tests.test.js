/**
 * Dalil Mit Ghamr — Firebase Security Rules Tests
 * Uses @firebase/rules-unit-testing v3 (no Cloud Functions required)
 * Tests Firestore Security Rules directly against the emulator.
 *
 * Run with:
 *   cd test
 *   npm install
 *   firebase emulators:exec --only firestore "npm test" --project dalil-mit3mr
 *
 * Or start emulator separately then:
 *   firebase emulators:start --only firestore --project dalil-mit3mr
 *   npm test
 */

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const {
  doc,
  setDoc,
  getDoc,
  updateDoc,
  deleteDoc,
  addDoc,
  collection,
} = require("firebase/firestore");
const fs = require("fs");
const path = require("path");

// ============================================================
// Setup
// ============================================================

jest.setTimeout(60000);

let testEnv;
let activeContexts = [];

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "dalil-mit3mr",
    firestore: {
      rules: fs.readFileSync(
        path.join(__dirname, "..", "firestore.rules"),
        "utf8"
      ),
      host: "127.0.0.1",
      port: 8080,
    },
  });

  // Patch RulesTestContextImpl.prototype.firestore to prevent "Firestore has already been started"
  const dummyCtx = testEnv.unauthenticatedContext();
  const proto = Object.getPrototypeOf(dummyCtx);
  if (!proto._patchedForEmulator) {
    const origFirestore = proto.firestore;
    proto.firestore = function(settings) {
      if (!this._cachedFirestore) {
        this._cachedFirestore = origFirestore.call(this, settings);
      }
      return this._cachedFirestore;
    };
    proto._patchedForEmulator = true;
  }
}, 120000);

afterAll(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
}, 30000);

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

afterEach(async () => {
  activeContexts = [];
});

// ============================================================
// Helpers
// ============================================================

function unauth() {
  const ctx = testEnv.unauthenticatedContext();
  activeContexts.push(ctx);
  return ctx;
}

function user(uid = "user-123", extra = {}) {
  const ctx = testEnv.authenticatedContext(uid, extra);
  activeContexts.push(ctx);
  return ctx;
}

function admin(uid = "admin-uid") {
  const ctx = testEnv.authenticatedContext(uid, { admin: true });
  activeContexts.push(ctx);
  return ctx;
}

function parseSeedArgs(arg1, arg2, arg3) {
  let docId, overrides;
  if (typeof arg1 === "string") {
    docId = arg1;
    overrides = arg2 || {};
  } else {
    docId = arg2;
    overrides = arg3 || {};
  }
  return { docId, overrides };
}

async function seedBusiness(arg1, arg2, arg3) {
  const { docId, overrides } = parseSeedArgs(arg1, arg2, arg3);
  const data = {
    name: "مطعم السلام",
    categoryId: "cat_restaurants",
    isPublished: true,
    isDeleted: false,
    ...overrides,
  };
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "businesses", docId), data);
  });
}

async function seedContribution(arg1, arg2, arg3) {
  const { docId, overrides } = parseSeedArgs(arg1, arg2, arg3);
  const data = {
    userId: "user-123",
    businessName: "نشاط تجريبي",
    status: "PENDING",
    approvedAt: null,
    approvedBy: null,
    publishedBusinessId: null,
    ...overrides,
  };
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "contributions", docId), data);
  });
}

async function seedReview(arg1, arg2, arg3) {
  const { docId, overrides } = parseSeedArgs(arg1, arg2, arg3);
  const data = {
    userId: "user-123",
    businessId: "biz-1",
    rating: 4,
    comment: "جيد",
    status: "PENDING",
    approvedAt: null,
    approvedBy: null,
    ownerReply: null,
    ...overrides,
  };
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "reviews", docId), data);
  });
}

async function seedAuditLog(arg1, arg2, arg3) {
  const { docId, overrides } = parseSeedArgs(arg1, arg2, arg3);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "audit_logs", docId), overrides);
  });
}

async function seedUser(arg1, arg2, arg3) {
  const { docId, overrides } = parseSeedArgs(arg1, arg2, arg3);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users", docId), overrides);
  });
}

async function seedCategory(arg1, arg2, arg3) {
  const { docId, overrides } = parseSeedArgs(arg1, arg2, arg3);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "categories", docId), overrides);
  });
}

// ============================================================
// GROUP 1: /businesses
// ============================================================

describe("businesses collection", () => {
  test("PASS — unauthenticated user CAN read a published business", async () => {
    const adminCtx = admin();
    await seedBusiness(adminCtx, "biz-pub", { isPublished: true });
    const ctx = unauth();
    await assertSucceeds(getDoc(doc(ctx.firestore(), "businesses", "biz-pub")));
  });

  test("FAIL — unauthenticated user CANNOT read an unpublished business", async () => {
    const adminCtx = admin();
    await seedBusiness(adminCtx, "biz-unpub", { isPublished: false });
    const ctx = unauth();
    await assertFails(
      getDoc(doc(ctx.firestore(), "businesses", "biz-unpub"))
    );
  });

  test("FAIL — regular user CANNOT create a business directly", async () => {
    const ctx = user();
    await assertFails(
      setDoc(doc(ctx.firestore(), "businesses", "biz-new"), {
        name: "نشاط",
        isPublished: true,
      })
    );
  });

  test("PASS — admin CAN create a business", async () => {
    const ctx = admin();
    await assertSucceeds(
      setDoc(doc(ctx.firestore(), "businesses", "biz-admin"), {
        name: "نشاط إداري",
        isPublished: true,
      })
    );
  });

  test("FAIL — regular user CANNOT update a business", async () => {
    const adminCtx = admin();
    await seedBusiness(adminCtx, "biz-edit", { isPublished: true });
    const ctx = user();
    await assertFails(
      updateDoc(doc(ctx.firestore(), "businesses", "biz-edit"), {
        name: "محدّث",
      })
    );
  });
});

// ============================================================
// GROUP 2: /contributions
// ============================================================

describe("contributions collection", () => {
  test("PASS — regular user CAN create a contribution with status PENDING", async () => {
    const ctx = user("user-123");
    await assertSucceeds(
      setDoc(doc(ctx.firestore(), "contributions", "c-1"), {
        userId: "user-123",
        businessName: "مشروع",
        status: "PENDING",
        approvedAt: null,
        approvedBy: null,
        publishedBusinessId: null,
      })
    );
  });

  test("FAIL — regular user CANNOT create a contribution with status APPROVED", async () => {
    const ctx = user("user-123");
    await assertFails(
      setDoc(doc(ctx.firestore(), "contributions", "c-2"), {
        userId: "user-123",
        businessName: "مشروع",
        status: "APPROVED",
      })
    );
  });

  test("FAIL — regular user CANNOT set approvedAt on contribution creation", async () => {
    const ctx = user("user-123");
    await assertFails(
      setDoc(doc(ctx.firestore(), "contributions", "c-3"), {
        userId: "user-123",
        businessName: "مشروع",
        status: "PENDING",
        approvedAt: Date.now(),
      })
    );
  });

  test("FAIL — regular user CANNOT create contribution for another user", async () => {
    const ctx = user("user-123");
    await assertFails(
      setDoc(doc(ctx.firestore(), "contributions", "c-4"), {
        userId: "another-user",
        businessName: "مشروع",
        status: "PENDING",
        approvedAt: null,
        approvedBy: null,
        publishedBusinessId: null,
      })
    );
  });

  test("PASS — regular user CAN read their own contribution", async () => {
    const adminCtx = admin();
    await seedContribution(adminCtx, "c-own", { userId: "user-123" });
    const ctx = user("user-123");
    await assertSucceeds(
      getDoc(doc(ctx.firestore(), "contributions", "c-own"))
    );
  });

  test("FAIL — regular user CANNOT read another user's contribution", async () => {
    const adminCtx = admin();
    await seedContribution(adminCtx, "c-other", { userId: "other-user" });
    const ctx = user("user-123");
    await assertFails(
      getDoc(doc(ctx.firestore(), "contributions", "c-other"))
    );
  });

  test("PASS — admin CAN read any contribution", async () => {
    const adminCtx = admin();
    await seedContribution(adminCtx, "c-admin-read", { userId: "user-123" });
    await assertSucceeds(
      getDoc(doc(adminCtx.firestore(), "contributions", "c-admin-read"))
    );
  });

  test("PASS — regular user CAN delete their own PENDING contribution", async () => {
    const adminCtx = admin();
    await seedContribution(adminCtx, "c-del", {
      userId: "user-123",
      status: "PENDING",
    });
    const ctx = user("user-123");
    await assertSucceeds(
      deleteDoc(doc(ctx.firestore(), "contributions", "c-del"))
    );
  });

  test("FAIL — regular user CANNOT delete another user's contribution", async () => {
    const adminCtx = admin();
    await seedContribution(adminCtx, "c-del-other", { userId: "other-user" });
    const ctx = user("user-123");
    await assertFails(
      deleteDoc(doc(ctx.firestore(), "contributions", "c-del-other"))
    );
  });

  test("FAIL — regular user CANNOT update (change status) of a contribution", async () => {
    const adminCtx = admin();
    await seedContribution(adminCtx, "c-upd", { userId: "user-123" });
    const ctx = user("user-123");
    await assertFails(
      updateDoc(doc(ctx.firestore(), "contributions", "c-upd"), {
        status: "APPROVED",
      })
    );
  });
});

// ============================================================
// GROUP 3: /reviews
// ============================================================

describe("reviews collection", () => {
  test("PASS — unauthenticated user CAN read an APPROVED review", async () => {
    const adminCtx = admin();
    await seedReview(adminCtx, "r-approved", { status: "APPROVED" });
    const ctx = unauth();
    await assertSucceeds(
      getDoc(doc(ctx.firestore(), "reviews", "r-approved"))
    );
  });

  test("FAIL — unauthenticated user CANNOT read a PENDING review", async () => {
    const adminCtx = admin();
    await seedReview(adminCtx, "r-pending-unauth", {
      status: "PENDING",
      userId: "other-user",
    });
    const ctx = unauth();
    await assertFails(
      getDoc(doc(ctx.firestore(), "reviews", "r-pending-unauth"))
    );
  });

  test("PASS — authenticated user CAN read their own PENDING review", async () => {
    const adminCtx = admin();
    await seedReview(adminCtx, "r-own-pending", {
      status: "PENDING",
      userId: "user-123",
    });
    const ctx = user("user-123");
    await assertSucceeds(
      getDoc(doc(ctx.firestore(), "reviews", "r-own-pending"))
    );
  });

  test("PASS — regular user CAN create a review with status PENDING", async () => {
    const ctx = user("user-123");
    await assertSucceeds(
      setDoc(doc(ctx.firestore(), "reviews", "r-create-ok"), {
        userId: "user-123",
        businessId: "biz-1",
        rating: 4,
        comment: "جيد جداً",
        status: "PENDING",
        approvedAt: null,
        approvedBy: null,
        ownerReply: null,
      })
    );
  });

  test("FAIL — regular user CANNOT create a review with status APPROVED", async () => {
    const ctx = user("user-123");
    await assertFails(
      setDoc(doc(ctx.firestore(), "reviews", "r-create-bad"), {
        userId: "user-123",
        businessId: "biz-1",
        rating: 4,
        comment: "جيد",
        status: "APPROVED",
      })
    );
  });

  test("FAIL — regular user CANNOT create a review with approvedAt set", async () => {
    const ctx = user("user-123");
    await assertFails(
      setDoc(doc(ctx.firestore(), "reviews", "r-create-approvedat"), {
        userId: "user-123",
        businessId: "biz-1",
        rating: 4,
        comment: "جيد",
        status: "PENDING",
        approvedAt: Date.now(),
        approvedBy: null,
        ownerReply: null,
      })
    );
  });

  test("PASS — user CAN update own PENDING review (keep status=PENDING)", async () => {
    const adminCtx = admin();
    await seedReview(adminCtx, "r-upd-ok", {
      userId: "user-123",
      status: "PENDING",
    });
    const ctx = user("user-123");
    await assertSucceeds(
      updateDoc(doc(ctx.firestore(), "reviews", "r-upd-ok"), {
        rating: 5,
        comment: "ممتاز",
        userId: "user-123",
        status: "PENDING",
      })
    );
  });

  test("FAIL — user CANNOT change their review status from PENDING to APPROVED", async () => {
    const adminCtx = admin();
    await seedReview(adminCtx, "r-upd-bad", {
      userId: "user-123",
      status: "PENDING",
    });
    const ctx = user("user-123");
    await assertFails(
      updateDoc(doc(ctx.firestore(), "reviews", "r-upd-bad"), {
        rating: 5,
        comment: "ممتاز",
        userId: "user-123",
        status: "APPROVED",
      })
    );
  });

  test("FAIL — user editing an APPROVED review cannot keep status as APPROVED", async () => {
    const adminCtx = admin();
    await seedReview(adminCtx, "r-upd-keep-approved", {
      userId: "user-123",
      businessId: "biz-1",
      status: "APPROVED",
      approvedAt: Date.now(),
      approvedBy: "admin-uid",
      rating: 4,
      comment: "تعليق سابق معتمد",
    });
    const ctx = user("user-123");
    // Editing must revert to PENDING; keeping APPROVED is rejected by rules
    await assertFails(
      updateDoc(doc(ctx.firestore(), "reviews", "r-upd-keep-approved"), {
        comment: "تعديل خبيث بعد الاعتماد",
        status: "APPROVED",
      })
    );
  });

  test("PASS — user editing an APPROVED review reverts status to PENDING for re-review", async () => {
    const adminCtx = admin();
    await seedReview(adminCtx, "r-upd-revert-pending", {
      userId: "user-123",
      businessId: "biz-1",
      status: "APPROVED",
      approvedAt: Date.now(),
      approvedBy: "admin-uid",
      rating: 4,
      comment: "تعليق سابق معتمد",
    });
    const ctx = user("user-123");
    // Author resets status to PENDING and clears approval metadata
    await assertSucceeds(
      updateDoc(doc(ctx.firestore(), "reviews", "r-upd-revert-pending"), {
        comment: "تعديل مشروع ينتظر إعادة المراجعة",
        rating: 5,
        userId: "user-123",
        businessId: "biz-1",
        status: "PENDING",
        approvedAt: null,
        approvedBy: null,
      })
    );
  });

  test("FAIL — regular user cannot forge or alter approvedBy / approvedAt", async () => {
    const adminCtx = admin();
    await seedReview(adminCtx, "r-upd-forge-admin", {
      userId: "user-123",
      businessId: "biz-1",
      status: "PENDING",
      rating: 3,
      comment: "تعليق عادي",
    });
    const ctx = user("user-123");
    await assertFails(
      updateDoc(doc(ctx.firestore(), "reviews", "r-upd-forge-admin"), {
        approvedBy: "fake-admin",
        approvedAt: Date.now(),
      })
    );
  });

  test("PASS — admin CAN update review status to APPROVED", async () => {
    const adminCtx = admin();
    await seedReview(adminCtx, "r-admin-approve", {
      userId: "user-123",
      status: "PENDING",
    });
    await assertSucceeds(
      updateDoc(doc(adminCtx.firestore(), "reviews", "r-admin-approve"), {
        status: "APPROVED",
        approvedAt: Date.now(),
        approvedBy: "admin-uid",
      })
    );
  });

  test("PASS — admin CAN reject review (status=REJECTED)", async () => {
    const adminCtx = admin();
    await seedReview(adminCtx, "r-admin-reject", {
      userId: "user-123",
      status: "PENDING",
    });
    await assertSucceeds(
      updateDoc(doc(adminCtx.firestore(), "reviews", "r-admin-reject"), {
        status: "REJECTED",
        approvedBy: "admin-uid",
      })
    );
  });
});

// ============================================================
// GROUP 4: /audit_logs
// ============================================================

describe("audit_logs collection", () => {
  test("FAIL — regular user CANNOT read audit logs", async () => {
    await seedAuditLog("log-1", {
      actorId: "admin-uid",
      action: "test",
    });
    const ctx = user("user-123");
    await assertFails(
      getDoc(doc(ctx.firestore(), "audit_logs", "log-1"))
    );
  });

  test("FAIL — unauthenticated user CANNOT read audit logs", async () => {
    await seedAuditLog("log-2", {
      actorId: "admin-uid",
      action: "test",
    });
    const ctx = unauth();
    await assertFails(
      getDoc(doc(ctx.firestore(), "audit_logs", "log-2"))
    );
  });

  test("FAIL — regular user CANNOT create audit logs", async () => {
    const ctx = user("user-123");
    await assertFails(
      setDoc(doc(ctx.firestore(), "audit_logs", "log-user"), {
        actorId: "user-123",
        action: "fake_log",
      })
    );
  });

  test("PASS — admin CAN read audit logs", async () => {
    await seedAuditLog("log-admin-read", {
      actorId: "admin-uid",
      action: "read_test",
    });
    const adminCtx = admin();
    await assertSucceeds(
      getDoc(doc(adminCtx.firestore(), "audit_logs", "log-admin-read"))
    );
  });

  test("PASS — admin CAN create audit logs with actorId == uid", async () => {
    const adminCtx = admin("admin-uid");
    await assertSucceeds(
      setDoc(doc(adminCtx.firestore(), "audit_logs", "log-admin-create"), {
        actorId: "admin-uid",
        action: "contribution_approved",
        timestamp: Date.now(),
      })
    );
  });

  test("FAIL — admin CANNOT create audit log with wrong actorId", async () => {
    const adminCtx = admin("admin-uid");
    await assertFails(
      setDoc(doc(adminCtx.firestore(), "audit_logs", "log-bad-actor"), {
        actorId: "someone-else",
        action: "fake",
      })
    );
  });

  test("FAIL — nobody can update audit logs", async () => {
    await seedAuditLog("log-update", {
      actorId: "admin-uid",
      action: "original",
    });
    const adminCtx = admin("admin-uid");
    await assertFails(
      updateDoc(doc(adminCtx.firestore(), "audit_logs", "log-update"), {
        action: "tampered",
      })
    );
  });

  test("FAIL — nobody can delete audit logs", async () => {
    await seedAuditLog("log-delete", {
      actorId: "admin-uid",
      action: "to_delete",
    });
    const adminCtx = admin("admin-uid");
    await assertFails(
      deleteDoc(doc(adminCtx.firestore(), "audit_logs", "log-delete"))
    );
  });
});

// ============================================================
// GROUP 5: /users
// ============================================================

describe("users collection", () => {
  test("PASS — user CAN read their own profile", async () => {
    await seedUser("user-123", {
      email: "test@test.com",
    });
    const ctx = user("user-123");
    await assertSucceeds(
      getDoc(doc(ctx.firestore(), "users", "user-123"))
    );
  });

  test("FAIL — user CANNOT read another user's profile", async () => {
    await seedUser("other-user", {
      email: "other@test.com",
    });
    const ctx = user("user-123");
    await assertFails(
      getDoc(doc(ctx.firestore(), "users", "other-user"))
    );
  });

  test("PASS — admin CAN read any user profile", async () => {
    await seedUser("any-user", {
      email: "any@test.com",
    });
    const adminCtx = admin();
    await assertSucceeds(
      getDoc(doc(adminCtx.firestore(), "users", "any-user"))
    );
  });
});

// ============================================================
// GROUP 6: /categories
// ============================================================

describe("categories collection", () => {
  test("PASS — unauthenticated user CAN read categories", async () => {
    await seedCategory("cat-1", {
      nameAr: "مطاعم",
    });
    const ctx = unauth();
    await assertSucceeds(
      getDoc(doc(ctx.firestore(), "categories", "cat-1"))
    );
  });

  test("FAIL — regular user CANNOT write categories", async () => {
    const ctx = user("user-123");
    await assertFails(
      setDoc(doc(ctx.firestore(), "categories", "cat-new"), {
        nameAr: "تصنيف جديد",
      })
    );
  });

  test("PASS — admin CAN write categories", async () => {
    const ctx = admin();
    await assertSucceeds(
      setDoc(doc(ctx.firestore(), "categories", "cat-admin"), {
        nameAr: "تصنيف إداري",
      })
    );
  });
});

// ============================================================
// GROUP 7: Security Attack Scenarios
// ============================================================

describe("security attack scenarios", () => {
  test("FAIL — user cannot inject admin claim by forging userId", async () => {
    // Regular user tries to write directly to businesses (admin-only)
    const ctx = user("attacker", { admin: false });
    await assertFails(
      setDoc(doc(ctx.firestore(), "businesses", "injected-biz"), {
        name: "نشاط مزوّر",
        isPublished: true,
      })
    );
  });

  test("FAIL — user cannot create contribution with publishedBusinessId pre-set", async () => {
    const ctx = user("user-123");
    await assertFails(
      setDoc(doc(ctx.firestore(), "contributions", "c-bypass"), {
        userId: "user-123",
        businessName: "محاولة تجاوز",
        status: "PENDING",
        approvedAt: null,
        approvedBy: null,
        publishedBusinessId: "existing-biz-id",
      })
    );
  });

  test("FAIL — user cannot create contribution with approvedBy pre-set", async () => {
    const ctx = user("user-123");
    await assertFails(
      setDoc(doc(ctx.firestore(), "contributions", "c-bypass-2"), {
        userId: "user-123",
        businessName: "محاولة تجاوز 2",
        status: "PENDING",
        approvedAt: null,
        approvedBy: "fake-admin-uid",
        publishedBusinessId: null,
      })
    );
  });

  test("FAIL — user cannot escalate review to APPROVED on create", async () => {
    const ctx = user("user-123");
    await assertFails(
      setDoc(doc(ctx.firestore(), "reviews", "r-escalate"), {
        userId: "user-123",
        businessId: "biz-1",
        rating: 5,
        comment: "ممتاز",
        status: "APPROVED",
        approvedAt: null,
        approvedBy: null,
        ownerReply: null,
      })
    );
  });

  test("FAIL — unauthenticated user cannot write to any protected collection", async () => {
    const ctx = unauth();
    await assertFails(
      setDoc(doc(ctx.firestore(), "contributions", "c-unauth"), {
        userId: "hacker",
        status: "PENDING",
      })
    );
    await assertFails(
      setDoc(doc(ctx.firestore(), "businesses", "biz-unauth"), {
        name: "نشاط غير مصرّح",
        isPublished: true,
      })
    );
  });
});

// ============================================================
// GROUP 8: /business_reservations & Concurrency / Deduplication
// ============================================================

describe("business_reservations collection & deduplication", () => {
  test("FAIL — unauthenticated user cannot read or write to business_reservations", async () => {
    const ctx = unauth();
    await assertFails(
      getDoc(doc(ctx.firestore(), "business_reservations", "phone_01012345678"))
    );
    await assertFails(
      setDoc(doc(ctx.firestore(), "business_reservations", "phone_01012345678"), {
        publishedBusinessId: "biz-1",
      })
    );
  });

  test("FAIL — regular user cannot read or write to business_reservations", async () => {
    const ctx = user("user-123");
    await assertFails(
      getDoc(doc(ctx.firestore(), "business_reservations", "phone_01012345678"))
    );
    await assertFails(
      setDoc(doc(ctx.firestore(), "business_reservations", "phone_01012345678"), {
        publishedBusinessId: "biz-1",
      })
    );
  });

  test("PASS — admin CAN read and write reservation lock keys", async () => {
    const adminCtx = admin();
    const lockRef = doc(adminCtx.firestore(), "business_reservations", "phone_01012345678");
    await assertSucceeds(
      setDoc(lockRef, {
        publishedBusinessId: "biz-1",
        approvedBy: "admin-uid",
        approvedAt: Date.now(),
      })
    );
    await assertSucceeds(getDoc(lockRef));
  });

  test("CONCURRENCY / DEDUPLICATION — atomic reservation prevents duplicate approvals", async () => {
    const adminCtx = admin();
    const db = adminCtx.firestore();

    const reservationRef = doc(db, "business_reservations", "phone_01011112222");

    // Helper simulating atomic approval transaction with reservation lock
    async function tryApproveWithLock(bizId, contribId) {
      const { runTransaction } = require("firebase/firestore");
      return runTransaction(db, async (transaction) => {
        const lockSnap = await transaction.get(reservationRef);
        if (lockSnap.exists()) {
          throw new Error(`DUPLICATE_DETECTED: already published as ${lockSnap.data().publishedBusinessId}`);
        }
        transaction.set(reservationRef, {
          contributionId: contribId,
          publishedBusinessId: bizId,
          approvedAt: Date.now(),
        });
        return bizId;
      });
    }

    // First approval succeeds
    const firstResult = await tryApproveWithLock("biz-first", "c-1");
    expect(firstResult).toBe("biz-first");

    // Second concurrent approval for same phone fails with duplicate error
    await expect(tryApproveWithLock("biz-second", "c-2")).rejects.toThrow("DUPLICATE_DETECTED");
  });
});
