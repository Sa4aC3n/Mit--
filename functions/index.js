const functions = require("firebase-functions");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}
const db = admin.firestore();

// Arabic text normalizer utility
function normalizeArabic(text) {
  if (!text) return "";
  let str = text.trim();
  str = str.replace(/[أإآ]/g, "ا");
  str = str.replace(/ة/g, "ه");
  str = str.replace(/ى/g, "ي");
  str = str.replace(/[\u064B-\u065F\u0670]/g, ""); // Remove Arabic diacritics
  str = str.replace(/[^\w\s\u0621-\u064A]/g, " ");
  return str.replace(/\s+/g, " ").trim().toLowerCase();
}

function normalizePhone(phone) {
  if (!phone) return "";
  let p = phone.replace(/[^\d+]/g, "");
  if (p.startsWith("+20")) p = "0" + p.substring(3);
  else if (p.startsWith("20") && p.length > 10) p = "0" + p.substring(2);
  return p;
}

function verifyAdmin(context) {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "يجب تسجيل الدخول أولاً");
  }
  const token = context.auth.token || {};
  const isAdmin = token.admin === true || token.email === "m.k3shka@gmail.com";
  if (!isAdmin) {
    throw new functions.https.HttpsError("permission-denied", "العملية تتطلب صلاحيات المشرف العام");
  }
}

/**
 * Callable Function: approveContribution
 * Authoritative Backend Mutation executed with Firebase Admin SDK privileges.
 * Performs idempotency check, deduplication, and atomic publishing in Firestore.
 */
exports.approveContribution = functions.https.onCall(async (data, context) => {
  verifyAdmin(context);

  const { contributionId, moderatorNote } = data;
  if (!contributionId) {
    throw new functions.https.HttpsError("invalid-argument", "معرف المساهمة مطلوب");
  }

  const contribRef = db.collection("contributions").document(contributionId);
  const contribDoc = await contribRef.get();

  if (!contribDoc.exists) {
    throw new functions.https.HttpsError("not-found", "طلب المساهمة غير موجود");
  }

  const contribData = contribDoc.data();

  // 1. Idempotency Check: if already approved, return existing published business ID without recreating
  if (contribData.status === "APPROVED" && contribData.publishedBusinessId) {
    const existingBizDoc = await db.collection("businesses").document(contribData.publishedBusinessId).get();
    return {
      success: true,
      alreadyApproved: true,
      publishedBusinessId: contribData.publishedBusinessId,
      business: existingBizDoc.exists ? existingBizDoc.data() : null,
      message: "تم اعتماد هذه المساهمة مسبقاً (Idempotent replay)."
    };
  }

  let payload = {};
  if (contribData.payloadJson) {
    try {
      payload = JSON.parse(contribData.payloadJson);
    } catch (e) {
      payload = {};
    }
  }

  const rawName = payload.name || contribData.businessName || "نشاط معتمد";
  const rawPhone = payload.phone || "";
  const normName = normalizeArabic(rawName);
  const normPhone = normalizePhone(rawPhone);

  // 2. Deduplication Search: Look for existing matching business in /businesses
  let matchedBusinessId = null;
  let matchedBusinessData = null;

  if (normPhone) {
    const phoneQuery = await db.collection("businesses")
      .where("phone", "==", rawPhone)
      .limit(1)
      .get();

    if (!phoneQuery.empty) {
      matchedBusinessId = phoneQuery.docs[0].id;
      matchedBusinessData = phoneQuery.docs[0].data();
    }
  }

  if (!matchedBusinessId && normName) {
    const nameQuery = await db.collection("businesses")
      .where("name", "==", rawName)
      .limit(1)
      .get();

    if (!nameQuery.empty) {
      matchedBusinessId = nameQuery.docs[0].id;
      matchedBusinessData = nameQuery.docs[0].data();
    }
  }

  const nowTs = Date.now();
  const serverTime = admin.firestore.FieldValue.serverTimestamp();
  const targetBusinessId = matchedBusinessId || ("biz_" + contributionId.replace("contrib_", ""));

  // 3. Atomic Transaction: Publish to /businesses and update /contributions and /audit_logs
  await db.runTransaction(async (t) => {
    const currentContrib = await t.get(contribRef);
    if (currentContrib.data().status === "APPROVED" && currentContrib.data().publishedBusinessId) {
      return; // Already approved concurrently
    }

    const bizRef = db.collection("businesses").document(targetBusinessId);
    const existingBiz = await t.get(bizRef);

    let unifiedBusiness = {};
    if (existingBiz.exists) {
      // Update existing unified entity
      const prev = existingBiz.data();
      unifiedBusiness = {
        ...prev,
        name: prev.name || rawName,
        phoneSecondary: payload.secondaryPhone || prev.phoneSecondary || null,
        whatsapp: payload.whatsapp || prev.whatsapp || null,
        address: payload.address || prev.address || "ميت غمر",
        area: payload.district || prev.area || "وسط البلد",
        specialty: payload.specialization || prev.specialty || "",
        workingHours: payload.workingHours || prev.workingHours || "9:00 ص - 10:00 م",
        facebookUrl: payload.facebookUrl || prev.facebookUrl || null,
        websiteUrl: payload.websiteUrl || prev.websiteUrl || null,
        isVerified: true,
        isActive: true,
        isPublished: true,
        isDeleted: false,
        verificationStatus: "VERIFIED",
        updatedAt: nowTs,
        serverSyncedAt: serverTime
      };
      t.set(bizRef, unifiedBusiness, { merge: true });
    } else {
      // Create new unified business entity
      unifiedBusiness = {
        id: targetBusinessId,
        name: rawName,
        categoryId: contribData.categoryId || payload.categoryId || "cat_general",
        categoryName: payload.categoryName || "خدمات معتمدة",
        specialty: payload.specialization || "نشاط معتمد",
        description: payload.description || "تم التحقق والاعتماد عبر نظام المشرفين الرسمي لدليل ميت غمر",
        phone: rawPhone,
        phoneSecondary: payload.secondaryPhone || null,
        whatsapp: payload.whatsapp || null,
        city: payload.city || "مدينة ميت غمر",
        address: payload.address || "ميت غمر",
        area: payload.district || "وسط البلد",
        facebookUrl: payload.facebookUrl || null,
        websiteUrl: payload.websiteUrl || null,
        latitude: payload.lat || 30.7183,
        longitude: payload.lng || 31.2568,
        workingHours: payload.workingHours || "9:00 ص - 10:00 م",
        isOpenNow: true,
        isVerified: true,
        isActive: true,
        isPublished: true,
        isDeleted: false,
        ratingAverage: 5.0,
        ratingCount: 1,
        viewCount: 1,
        imageUrl: (payload.imageUrls && payload.imageUrls[0]) || null,
        verificationStatus: "VERIFIED",
        dataQualityScore: 90,
        approvedContributionId: contributionId,
        createdAt: nowTs,
        lastVerifiedAt: nowTs,
        updatedAt: nowTs,
        serverSyncedAt: serverTime
      };
      t.set(bizRef, unifiedBusiness);
    }

    // Update contribution in the same transaction
    t.update(contribRef, {
      status: "APPROVED",
      approvedAt: nowTs,
      approvedBy: context.auth.uid,
      publishedBusinessId: targetBusinessId,
      moderatorNote: moderatorNote || "تمت المراجعة والاعتماد بنجاح",
      updatedAt: nowTs,
      serverSyncedAt: serverTime
    });

    // Write audit log entry
    const auditRef = db.collection("audit_logs").doc();
    t.set(auditRef, {
      id: auditRef.id,
      action: "APPROVE_CONTRIBUTION",
      performedBy: context.auth.uid,
      performerEmail: context.auth.token.email || null,
      targetContributionId: contributionId,
      publishedBusinessId: targetBusinessId,
      isNewEntity: !matchedBusinessId,
      timestamp: nowTs,
      serverSyncedAt: serverTime
    });
  });

  return {
    success: true,
    publishedBusinessId: targetBusinessId,
    isMerged: !!matchedBusinessId,
    message: "تم النشر بنجاح على Cloud Firestore."
  };
});

/**
 * Callable Function: rejectContribution
 * Rejects contribution without publishing any business documents.
 */
exports.rejectContribution = functions.https.onCall(async (data, context) => {
  verifyAdmin(context);

  const { contributionId, reason } = data;
  if (!contributionId) {
    throw new functions.https.HttpsError("invalid-argument", "معرف المساهمة مطلوب");
  }

  const contribRef = db.collection("contributions").document(contributionId);
  const nowTs = Date.now();
  const serverTime = admin.firestore.FieldValue.serverTimestamp();

  await db.runTransaction(async (t) => {
    t.update(contribRef, {
      status: "REJECTED",
      moderatorNote: reason || "تم رفض الطلب بعد المراجعة",
      reviewedAt: nowTs,
      updatedAt: nowTs,
      serverSyncedAt: serverTime
    });

    const auditRef = db.collection("audit_logs").doc();
    t.set(auditRef, {
      id: auditRef.id,
      action: "REJECT_CONTRIBUTION",
      performedBy: context.auth.uid,
      targetContributionId: contributionId,
      reason: reason || "غير مستوفٍ للشروط",
      timestamp: nowTs,
      serverSyncedAt: serverTime
    });
  });

  return {
    success: true,
    message: "تم رفض الطلب بنجاح ولم يتم نشر أي نشاط تجاري."
  };
});
