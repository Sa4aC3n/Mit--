const functions = require("firebase-functions");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}
const db = admin.firestore();

// -------------------------------------------------------------
// Normalization Utilities (Arabic Text, Phones, URLs, Addresses)
// -------------------------------------------------------------

function normalizeArabic(text) {
  if (!text) return "";
  let str = text.trim();
  str = str.replace(/[أإآ]/g, "ا");
  str = str.replace(/ة/g, "ه");
  str = str.replace(/ى/g, "ي");
  str = str.replace(/[\u064B-\u065F\u0670]/g, ""); // Remove Arabic diacritics / tashkeel
  str = str.replace(/[^\w\s\u0621-\u064A]/g, " ");
  return str.replace(/\s+/g, " ").trim().toLowerCase();
}

function normalizePhone(phone) {
  if (!phone) return "";
  let p = phone.replace(/[^\d+]/g, "").trim();
  if (p.startsWith("+20")) {
    p = "0" + p.substring(3);
  } else if (p.startsWith("0020")) {
    p = "0" + p.substring(4);
  } else if (p.startsWith("20") && p.length > 10) {
    p = "0" + p.substring(2);
  } else if (p.length === 10 && p.startsWith("1")) {
    p = "0" + p;
  }
  return p;
}

function normalizeWebsite(url) {
  if (!url) return "";
  let clean = url.trim().toLowerCase();
  clean = clean.replace(/^https?:\/\//, "");
  clean = clean.replace(/^www\./, "");
  clean = clean.split("?")[0]; // remove query params
  clean = clean.replace(/\/+$/, ""); // remove trailing slashes
  return clean;
}

function computeTokenSimilarity(str1, str2) {
  if (!str1 || !str2) return 0;
  if (str1 === str2) return 1.0;
  const tokens1 = new Set(str1.split(" ").filter((t) => t.length > 1));
  const tokens2 = new Set(str2.split(" ").filter((t) => t.length > 1));
  if (tokens1.size === 0 || tokens2.size === 0) return 0;

  let intersection = 0;
  for (const t of tokens1) {
    if (tokens2.has(t)) intersection++;
  }
  const union = new Set([...tokens1, ...tokens2]).size;
  return union === 0 ? 0 : intersection / union;
}

// -------------------------------------------------------------
// Security: Verify Admin Custom Claim (context.auth.token.admin)
// -------------------------------------------------------------

function verifyAdmin(context) {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "يجب تسجيل الدخول أولاً بحساب مشرف"
    );
  }
  const token = context.auth.token || {};
  if (token.admin !== true) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "العملية تتطلب صلاحيات المشرف العام المعتمدة (Admin Custom Claim)"
    );
  }
}

/**
 * Callable Function: approveContribution
 * Authoritative Backend Mutation executed exclusively with Firebase Admin SDK privileges.
 * Performs:
 * 1. Admin claim verification
 * 2. Status verification (PENDING)
 * 3. Idempotency replay check
 * 4. Multi-signal deduplication using normalizedPhone, normalizedWebsite, normalizedName
 * 5. Atomic transaction publishing unified record to /businesses and updating /contributions
 * 6. Audit logging and multi-source provenance tracking
 */
exports.approveContribution = functions.https.onCall(async (data, context) => {
  verifyAdmin(context);

  const { contributionId, moderatorNote } = data || {};
  if (!contributionId) {
    throw new functions.https.HttpsError("invalid-argument", "معرف المساهمة مطلوب");
  }

  const contribRef = db.collection("contributions").doc(contributionId);
  const contribDoc = await contribRef.get();

  if (!contribDoc.exists) {
    throw new functions.https.HttpsError("not-found", "طلب المساهمة غير موجود");
  }

  const contribData = contribDoc.data();

  // 1. Idempotency Check: if already approved, return existing published business ID without recreating
  if (contribData.status === "APPROVED" && contribData.publishedBusinessId) {
    const existingBizDoc = await db.collection("businesses").doc(contribData.publishedBusinessId).get();
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
  const rawSecondaryPhone = payload.secondaryPhone || null;
  const rawWhatsapp = payload.whatsapp || null;
  const rawCategory = contribData.categoryId || payload.categoryId || "cat_general";
  const rawCategoryName = payload.categoryName || "خدمات معتمدة";
  const rawSpecialty = payload.specialization || "نشاط معتمد";
  const rawDescription = payload.description || "تم التحقق والاعتماد عبر نظام المشرفين الرسمي لدليل ميت غمر";
  const rawAddress = payload.address || "ميت غمر";
  const rawDistrict = payload.district || "وسط البلد";
  const rawWebsite = payload.websiteUrl || null;
  const rawFacebook = payload.facebookUrl || null;
  const rawWorkingHours = payload.workingHours || "9:00 ص - 10:00 م";
  const rawLat = payload.lat || 30.7183;
  const rawLng = payload.lng || 31.2568;
  const rawImage = (payload.imageUrls && payload.imageUrls[0]) || null;

  const normName = normalizeArabic(rawName);
  const normPhone = normalizePhone(rawPhone);
  const normWebsite = normalizeWebsite(rawWebsite || rawFacebook);
  const normAddress = normalizeArabic(rawAddress);

  // 2. Multi-Signal Deduplication Engine (Search by normalized fields, not raw strings)
  let matchedBusinessId = null;
  let matchedBusinessData = null;

  // Signal A: If contribution is SUGGEST_EDIT with target businessId
  if (contribData.businessId) {
    const directDoc = await db.collection("businesses").doc(contribData.businessId).get();
    if (directDoc.exists) {
      matchedBusinessId = directDoc.id;
      matchedBusinessData = directDoc.data();
    }
  }

  // Signal B: Match by normalized phone + name similarity or category
  if (!matchedBusinessId && normPhone) {
    const phoneQuery = await db.collection("businesses")
      .where("normalizedPhone", "==", normPhone)
      .limit(5)
      .get();

    for (const doc of phoneQuery.docs) {
      const data = doc.data();
      const existingNormName = data.normalizedName || normalizeArabic(data.name);
      const nameSim = computeTokenSimilarity(normName, existingNormName);
      const sameCategory = (data.categoryId === rawCategory);

      // Require corroborating signal: either similar name or same category/area
      if (nameSim >= 0.5 || sameCategory) {
        matchedBusinessId = doc.id;
        matchedBusinessData = data;
        break;
      }
    }
  }

  // Signal C: Match by normalized official website / facebook
  if (!matchedBusinessId && normWebsite) {
    const siteQuery = await db.collection("businesses")
      .where("normalizedWebsite", "==", normWebsite)
      .limit(3)
      .get();

    if (!siteQuery.empty) {
      const doc = siteQuery.docs[0];
      const data = doc.data();
      // Verify not completely contradictory
      const existingNormName = data.normalizedName || normalizeArabic(data.name);
      if (computeTokenSimilarity(normName, existingNormName) >= 0.35 || data.categoryId === rawCategory) {
        matchedBusinessId = doc.id;
        matchedBusinessData = data;
      }
    }
  }

  // Signal D: Match by exact normalizedName + categoryId + matching area
  if (!matchedBusinessId && normName) {
    const nameQuery = await db.collection("businesses")
      .where("normalizedName", "==", normName)
      .where("categoryId", "==", rawCategory)
      .limit(3)
      .get();

    for (const doc of nameQuery.docs) {
      const data = doc.data();
      const dataArea = normalizeArabic(data.area || "");
      const candArea = normalizeArabic(rawDistrict);
      if (!dataArea || !candArea || dataArea.includes(candArea) || candArea.includes(dataArea)) {
        matchedBusinessId = doc.id;
        matchedBusinessData = data;
        break;
      }
    }
  }

  const nowTs = Date.now();
  const serverTime = admin.firestore.FieldValue.serverTimestamp();
  const targetBusinessId = matchedBusinessId || ("biz_" + contributionId.replace("contrib_", ""));

  // 3. Atomic Transaction: Publish unified record, update contribution & audit log
  await db.runTransaction(async (t) => {
    const currentContrib = await t.get(contribRef);
    if (!currentContrib.exists) {
      throw new functions.https.HttpsError("not-found", "الطلب غير موجود أثناء المعاملة");
    }
    const currentData = currentContrib.data();
    if (currentData.status === "APPROVED" && currentData.publishedBusinessId) {
      return; // Already approved concurrently
    }

    const bizRef = db.collection("businesses").doc(targetBusinessId);
    const existingBiz = await t.get(bizRef);

    let unifiedBusiness = {};
    if (existingBiz.exists) {
      const prev = existingBiz.data();
      unifiedBusiness = {
        ...prev,
        name: prev.name || rawName,
        normalizedName: normName || prev.normalizedName || normalizeArabic(prev.name),
        phone: prev.phone || rawPhone,
        normalizedPhone: normPhone || prev.normalizedPhone || normalizePhone(prev.phone),
        phoneSecondary: rawSecondaryPhone || prev.phoneSecondary || null,
        whatsapp: rawWhatsapp || prev.whatsapp || null,
        address: rawAddress || prev.address || "ميت غمر",
        normalizedAddress: normAddress || prev.normalizedAddress || normalizeArabic(prev.address),
        area: rawDistrict || prev.area || "وسط البلد",
        specialty: rawSpecialty || prev.specialty || "",
        workingHours: rawWorkingHours || prev.workingHours || "9:00 ص - 10:00 م",
        facebookUrl: rawFacebook || prev.facebookUrl || null,
        websiteUrl: rawWebsite || prev.websiteUrl || null,
        normalizedWebsite: normWebsite || prev.normalizedWebsite || normalizeWebsite(prev.websiteUrl || prev.facebookUrl),
        imageUrl: rawImage || prev.imageUrl || null,
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
      unifiedBusiness = {
        id: targetBusinessId,
        name: rawName,
        normalizedName: normName,
        categoryId: rawCategory,
        categoryName: rawCategoryName,
        specialty: rawSpecialty,
        description: rawDescription,
        phone: rawPhone,
        normalizedPhone: normPhone,
        phoneSecondary: rawSecondaryPhone,
        whatsapp: rawWhatsapp,
        city: "مدينة ميت غمر",
        address: rawAddress,
        normalizedAddress: normAddress,
        area: rawDistrict,
        facebookUrl: rawFacebook,
        websiteUrl: rawWebsite,
        normalizedWebsite: normWebsite,
        latitude: rawLat,
        longitude: rawLng,
        workingHours: rawWorkingHours,
        isOpenNow: true,
        isVerified: true,
        isActive: true,
        isPublished: true,
        isDeleted: false,
        ratingAverage: 5.0,
        ratingCount: 1,
        viewCount: 1,
        imageUrl: rawImage,
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

    // Write immutable audit log
    const auditRef = db.collection("audit_logs").doc();
    t.set(auditRef, {
      id: auditRef.id,
      action: "APPROVE_CONTRIBUTION",
      performedBy: context.auth.uid,
      performerEmail: context.auth.token.email || null,
      targetContributionId: contributionId,
      publishedBusinessId: targetBusinessId,
      isMerged: !!matchedBusinessId,
      timestamp: nowTs,
      serverSyncedAt: serverTime
    });

    // Write public multi-source provenance record (no private rawPayloadJson)
    const sourceRef = db.collection("businessSources").doc("src_" + contributionId);
    t.set(sourceRef, {
      id: sourceRef.id,
      businessId: targetBusinessId,
      sourceType: "USER_CONTRIBUTION",
      sourceId: contributionId,
      sourceName: "مساهمة مستخدم معتمدة",
      discoveredAt: contribData.createdAt || nowTs,
      lastCheckedAt: nowTs,
      lastVerifiedAt: nowTs,
      sourceConfidence: 95,
      isOfficial: false,
      isActive: true,
      updatedAt: nowTs,
      serverSyncedAt: serverTime
    });

    // Quarantine private user request payload in protected subcollection (admin-only)
    if (contribData.payloadJson || contribData.userEmail || contribData.userReason) {
      const privatePayloadRef = sourceRef.collection("privatePayload").doc("request_details");
      t.set(privatePayloadRef, {
        rawPayloadJson: contribData.payloadJson || "",
        submittedByUserId: contribData.userId || null,
        userEmail: contribData.userEmail || null,
        userReason: contribData.userReason || null,
        createdAt: nowTs
      });
    }
  });

  return {
    success: true,
    publishedBusinessId: targetBusinessId,
    isMerged: !!matchedBusinessId,
    message: "تم اعتماد ونشر النشاط بنجاح على Cloud Firestore."
  };
});

/**
 * Callable Function: rejectContribution
 * Rejects contribution without publishing any business documents.
 */
exports.rejectContribution = functions.https.onCall(async (data, context) => {
  verifyAdmin(context);

  const { contributionId, reason } = data || {};
  if (!contributionId) {
    throw new functions.https.HttpsError("invalid-argument", "معرف المساهمة مطلوب");
  }

  const contribRef = db.collection("contributions").doc(contributionId);
  const nowTs = Date.now();
  const serverTime = admin.firestore.FieldValue.serverTimestamp();

  await db.runTransaction(async (t) => {
    const doc = await t.get(contribRef);
    if (!doc.exists) {
      throw new functions.https.HttpsError("not-found", "الطلب غير موجود");
    }

    t.update(contribRef, {
      status: "REJECTED",
      moderatorNote: reason || "تم رفض الطلب بعد مراجعة المشرف",
      reviewedAt: nowTs,
      reviewedBy: context.auth.uid,
      updatedAt: nowTs,
      serverSyncedAt: serverTime
    });

    const auditRef = db.collection("audit_logs").doc();
    t.set(auditRef, {
      id: auditRef.id,
      action: "REJECT_CONTRIBUTION",
      performedBy: context.auth.uid,
      performerEmail: context.auth.token.email || null,
      targetContributionId: contributionId,
      reason: reason || "غير مستوفٍ للشروط",
      timestamp: nowTs,
      serverSyncedAt: serverTime
    });
  });

  return {
    success: true,
    message: "تم رفض الطلب بنجاح ولم يتم إنشاء أي نشاط تجاري."
  };
});

/**
 * Callable Function: archiveOrDeleteBusiness
 * Tombstones a business with isDeleted = true, isPublished = true, and fresh updatedAt.
 * Allows client incremental sync to pick up the tombstone and remove from Room.
 */
exports.archiveOrDeleteBusiness = functions.https.onCall(async (data, context) => {
  verifyAdmin(context);

  const { businessId, isHardDelete } = data || {};
  if (!businessId) {
    throw new functions.https.HttpsError("invalid-argument", "معرف النشاط مطلوب");
  }

  const bizRef = db.collection("businesses").doc(businessId);
  const nowTs = Date.now();
  const serverTime = admin.firestore.FieldValue.serverTimestamp();

  await db.runTransaction(async (t) => {
    const doc = await t.get(bizRef);
    if (!doc.exists) {
      throw new functions.https.HttpsError("not-found", "النشاط غير موجود");
    }

    t.update(bizRef, {
      isDeleted: true,
      isActive: false,
      isPublished: true, // Keep isPublished=true so client incremental query can read tombstone
      verificationStatus: isHardDelete ? "DELETED" : "ARCHIVED",
      archivedAt: nowTs,
      updatedAt: nowTs,
      serverSyncedAt: serverTime
    });

    const auditRef = db.collection("audit_logs").doc();
    t.set(auditRef, {
      id: auditRef.id,
      action: isHardDelete ? "DELETE_BUSINESS" : "ARCHIVE_BUSINESS",
      performedBy: context.auth.uid,
      businessId: businessId,
      timestamp: nowTs,
      serverSyncedAt: serverTime
    });

    const tombstoneRef = db.collection("business_tombstones").doc(businessId);
    t.set(tombstoneRef, {
      id: businessId,
      businessId: businessId,
      reason: isHardDelete ? "DELETED" : "ARCHIVED",
      updatedAt: nowTs,
      serverSyncedAt: serverTime
    });
  });

  return {
    success: true,
    message: "تم إدراج شاهد الحذف (Tombstone) بنجاح وإشعار المزامنة السحابية."
  };
});

/**
 * Background Firestore Trigger: onBusinessWritten
 * Automatically publishes tombstones to /business_tombstones whenever an activity
 * is unpublished (isPublished: false), deleted (isDeleted: true), or removed from Firestore.
 * This guarantees that Room caches on user devices reliably receive the unpublish/delete event.
 */
exports.onBusinessWritten = functions.firestore
  .document("businesses/{businessId}")
  .onWrite(async (change, context) => {
    const businessId = context.params.businessId;

    if (!change.after.exists) {
      // Document was hard-deleted from Firestore
      const nowTs = Date.now();
      await db.collection("business_tombstones").doc(businessId).set({
        id: businessId,
        businessId: businessId,
        reason: "DELETED",
        updatedAt: nowTs,
        serverSyncedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      return;
    }

    const beforeData = change.before.exists ? change.before.data() : null;
    const afterData = change.after.data();

    const wasPublished = beforeData ? beforeData.isPublished === true : false;
    const isNowPublished = afterData.isPublished === true;
    const isDeletedOrArchived =
      afterData.isDeleted === true ||
      afterData.isActive === false ||
      afterData.verificationStatus === "ARCHIVED" ||
      afterData.verificationStatus === "DELETED";

    // If it was published and is now unpublished, OR if it has been deleted/archived:
    if ((wasPublished && !isNowPublished) || isDeletedOrArchived || !isNowPublished) {
      const nowTs =
        typeof afterData.updatedAt === "number" && afterData.updatedAt > (beforeData?.updatedAt || 0)
          ? afterData.updatedAt
          : Date.now();

      await db.collection("business_tombstones").doc(businessId).set({
        id: businessId,
        businessId: businessId,
        reason: isDeletedOrArchived ? (afterData.verificationStatus || "DELETED") : "UNPUBLISHED",
        updatedAt: nowTs,
        serverSyncedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    } else if (wasPublished === false && isNowPublished && !isDeletedOrArchived) {
      // Re-published: remove any existing tombstone
      await db.collection("business_tombstones").doc(businessId).delete().catch(() => {});
    }
  });

/**
 * Background Firestore Trigger: onReviewCreated
 * Automatically aggregates reviews into businesses/{businessId} ratingAverage and ratingCount
 * using authoritative Firebase Admin privileges.
 */
exports.onReviewCreated = functions.firestore
  .document("reviews/{reviewId}")
  .onCreate(async (snap) => {
    const review = snap.data();
    if (!review || !review.businessId || !review.rating) return null;

    const businessId = review.businessId;
    const bizRef = db.collection("businesses").doc(businessId);

    return db.runTransaction(async (t) => {
      const bizDoc = await t.get(bizRef);
      if (!bizDoc.exists) return;

      const prev = bizDoc.data();
      const currentAvg = prev.ratingAverage || 5.0;
      const currentCount = prev.ratingCount || 0;
      const newRating = Number(review.rating);

      const nextCount = currentCount + 1;
      const nextAvg = ((currentAvg * currentCount) + newRating) / nextCount;
      const nowTs = Date.now();

      t.update(bizRef, {
        ratingAverage: Math.round(nextAvg * 10) / 10,
        ratingCount: nextCount,
        updatedAt: nowTs
      });
    });
  });
