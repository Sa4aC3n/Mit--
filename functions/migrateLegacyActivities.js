const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}
const db = admin.firestore();

function normalizeArabic(text) {
  if (!text) return "";
  let str = text.trim();
  str = str.replace(/[أإآ]/g, "ا");
  str = str.replace(/ة/g, "ه");
  str = str.replace(/ى/g, "ي");
  str = str.replace(/[\u064B-\u065F\u0670]/g, "");
  str = str.replace(/[^\w\s\u0621-\u064A]/g, " ");
  return str.replace(/\s+/g, " ").trim().toLowerCase();
}

function normalizePhone(phone) {
  if (!phone) return "";
  let p = phone.replace(/[^\d+]/g, "").trim();
  const arabicDigits = ["٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"];
  arabicDigits.forEach((d, i) => {
    p = p.replace(new RegExp(d, "g"), i.toString());
  });
  if (p.startsWith("+20")) p = "0" + p.substring(3);
  else if (p.startsWith("0020")) p = "0" + p.substring(4);
  else if (p.startsWith("20") && p.length > 10) p = "0" + p.substring(2);
  else if (p.length === 10 && p.startsWith("1")) p = "0" + p;
  return p;
}

function normalizeWebsite(url) {
  if (!url) return "";
  let clean = url.trim().toLowerCase();
  clean = clean.replace(/^https?:\/\//, "");
  clean = clean.replace(/^www\./, "");
  clean = clean.split("?")[0];
  clean = clean.replace(/\/+$/, "");
  return clean;
}

function stripArabicPrefixes(word) {
  if (!word) return "";
  let w = word.trim();
  if (w.startsWith("وال") && w.length > 5) w = w.substring(3);
  else if (w.startsWith("ال") && w.length > 4) w = w.substring(2);
  else if (w.startsWith("لل") && w.length > 4) w = w.substring(2);
  else if (w.startsWith("و") && w.length > 3) w = w.substring(1);
  return w;
}

/**
 * Calculates Jaccard word-level similarity between two Arabic strings.
 * Returns float between 0.0 and 1.0.
 */
function calculateNameSimilarity(name1, name2) {
  if (!name1 || !name2) return 0;
  const n1 = normalizeArabic(name1);
  const n2 = normalizeArabic(name2);
  if (n1 === n2) return 1.0;
  if (n1.includes(n2) || n2.includes(n1)) return 0.85;

  const words1 = new Set(n1.split(" ").map(stripArabicPrefixes).filter((w) => w.length >= 2));
  const words2 = new Set(n2.split(" ").map(stripArabicPrefixes).filter((w) => w.length >= 2));
  if (words1.size === 0 || words2.size === 0) return 0;

  let intersection = 0;
  for (const w of words1) {
    if (words2.has(w)) intersection++;
  }
  const union = new Set([...words1, ...words2]).size;
  const jaccard = union > 0 ? intersection / union : 0;

  // If two names share 2 or more distinct significant words, it's a high confidence match
  if (intersection >= 2) return Math.max(jaccard, 0.65);
  return jaccard;
}

/**
 * Evaluates whether an entity possesses an explicit, authoritative proof of approval.
 * Invariant: Lack of explicit rejection is NOT approval.
 * Any negative status (isVerified=false, isActive=false, isDeleted=true, DRAFT, PENDING, REJECTED, ARCHIVED, DELETED)
 * categorically prevents publication.
 */
function hasExplicitApproval(data) {
  if (!data) return false;

  // 1. Explicit negative booleans
  if (data.isDeleted === true || data.isArchived === true) return false;
  if (data.isActive === false) return false;
  if (data.isVerified === false) return false;

  // 2. Explicit negative statuses (case-insensitive)
  const status = (data.status || "").toString().trim().toUpperCase();
  const vStatus = (data.verificationStatus || "").toString().trim().toUpperCase();
  const negativeStatuses = [
    "DRAFT",
    "PENDING",
    "PENDING_REVIEW",
    "REJECTED",
    "ARCHIVED",
    "DELETED",
    "SUSPENDED",
    "DISABLED",
    "INACTIVE",
    "UNVERIFIED",
    "INCOMPLETE_DATA_REVIEW",
    "AMBIGUOUS_PHONE_REVIEW"
  ];
  if (negativeStatuses.includes(status) || negativeStatuses.includes(vStatus)) {
    return false;
  }

  // 3. Positive approval signals:
  // - Document explicitly marked isPublished === true AND isVerified === true
  // - Or status / verificationStatus is explicitly "APPROVED", "PUBLISHED", or "VERIFIED"
  // - Or has explicit approval metadata (approvedContributionId, approvedBy, or approvedAt)
  if (data.isPublished === true && (data.isVerified === true || vStatus === "VERIFIED") && data.isActive !== false) {
    return true;
  }
  if (status === "APPROVED" || status === "PUBLISHED") {
    return true;
  }
  if (vStatus === "VERIFIED" || vStatus === "APPROVED") {
    return true;
  }
  if (data.approvedContributionId || data.approvedBy || data.approvedAt) {
    return true;
  }

  return false;
}

/**
 * Authoritative Safe Migration Script:
 * 
 * Rules & Safety Constraints:
 * 1. Does NOT blindly publish all legacy activities:
 *    - Valid, active items with explicit, reliable approval proof -> isPublished: true, verificationStatus: "VERIFIED"
 *    - Records lacking explicit approval, or marked DRAFT/PENDING/REJECTED/UNVERIFIED -> isPublished: false, verificationStatus: "PENDING_REVIEW" / "DRAFT_REVIEW"
 *    - Incomplete data -> isPublished: false, verificationStatus: "INCOMPLETE_DATA_REVIEW"
 *    - Deleted or archived -> isPublished: false, isDeleted: true, verificationStatus: "ARCHIVED" (never reactivated)
 * 2. Multi-Signal Matching (NEVER merges on phone number alone):
 *    - Matches ID -> Merge
 *    - Matches Phone AND Name Similarity >= 0.5 -> Merge
 *    - Matches Phone BUT Name Similarity < 0.5 -> Ambiguous! DO NOT merge. Flags as "AMBIGUOUS_PHONE_REVIEW" and keeps separate.
 * 3. Idempotent & Re-run safe:
 *    - Skips documents where migratedToBusinessId points to an existing business in /businesses.
 * 4. Data Provenance & Clean Public Data:
 *    - Updates /activities with migratedToBusinessId and migratedAt.
 *    - Creates /businessSources record for EACH activity without public rawPayloadJson.
 * 5. Type Uniformity:
 *    - All updatedAt / createdAt fields are standard numeric epoch milliseconds (Long).
 */
async function migrate(options = {}) {
  const isDryRun = options.dryRun !== undefined ? options.dryRun : process.argv.includes("--dry-run");
  console.log(`=======================================================`);
  console.log(`Mit Ghamr Directory: Legacy Activities Migration Tool`);
  console.log(`Mode: ${isDryRun ? "DRY-RUN (Simulated - No writes)" : "LIVE EXECUTION"}`);
  console.log(`=======================================================\n`);

  const nowTs = Date.now();

  // 1. Read existing businesses
  console.log("Reading /businesses collection...");
  const bizSnap = await db.collection("businesses").get();
  console.log(`Found ${bizSnap.size} existing documents in /businesses.`);

  const existingBizById = new Map();
  const existingBizByPhone = new Map();

  for (const doc of bizSnap.docs) {
    const d = doc.data();
    existingBizById.set(doc.id, d);
    const nPhone = d.normalizedPhone || normalizePhone(d.phone);
    // Only index published, active, non-deleted businesses for duplicate resolution
    if (nPhone && d.isPublished && !d.isDeleted && d.isActive !== false) {
      existingBizByPhone.set(nPhone, { id: doc.id, name: d.name });
    }
  }

  // 2. Read legacy activities
  console.log("Reading legacy /activities collection...");
  const actSnap = await db.collection("activities").get();
  console.log(`Found ${actSnap.size} documents in /activities.`);

  let businessesEnrichedCount = 0;
  let tombstonesEmittedCount = 0;
  let alreadyMigratedSkipped = 0;
  let migratedExplicitlyApprovedPublishedCount = 0;
  let migratedUnprovenPendingReviewCount = 0;
  let migratedIncompleteCount = 0;
  let migratedDeletedOrArchivedCount = 0;
  let ambiguousDuplicatePhoneCount = 0;
  let duplicatesMergedCount = 0;

  // Step A: Enrich existing businesses with search and sync fields (Long updatedAt)
  console.log("\nAuditing and enriching existing /businesses with search and sync fields...");
  for (const doc of bizSnap.docs) {
    const d = doc.data();
    const normPhone = d.normalizedPhone || normalizePhone(d.phone);
    const normName = d.normalizedName || normalizeArabic(d.name);
    const normWebsite = d.normalizedWebsite || normalizeWebsite(d.websiteUrl || d.facebookUrl);
    const normAddress = d.normalizedAddress || normalizeArabic(d.address);

    const isDeletedOrArchived =
      d.isDeleted === true ||
      d.isArchived === true ||
      (d.status || "").toString().toUpperCase() === "DELETED" ||
      (d.status || "").toString().toUpperCase() === "ARCHIVED" ||
      (d.verificationStatus || "").toString().toUpperCase() === "ARCHIVED" ||
      (d.verificationStatus || "").toString().toUpperCase() === "DELETED";

    let targetIsPublished = d.isPublished;
    let targetVerificationStatus = d.verificationStatus;
    let targetIsActive = d.isActive !== undefined ? d.isActive : true;

    if (isDeletedOrArchived) {
      targetIsPublished = false;
      targetIsActive = false;
      targetVerificationStatus = "ARCHIVED";
    } else if (d.isPublished === undefined) {
      // If isPublished is missing, default to false unless explicit approval proof exists!
      if (hasExplicitApproval(d)) {
        targetIsPublished = true;
        targetVerificationStatus = d.verificationStatus || "VERIFIED";
        targetIsActive = true;
      } else {
        targetIsPublished = false;
        targetVerificationStatus = d.verificationStatus || "LEGACY_STATUS_REVIEW";
        targetIsActive = false;
      }
    } else if (d.isPublished === true && !hasExplicitApproval(d)) {
      // If isPublished was true but flags explicitly show isVerified=false, isActive=false, or DRAFT/PENDING:
      if (d.isVerified === false || d.isActive === false || ["DRAFT", "PENDING", "REJECTED"].includes((d.status || "").toString().toUpperCase())) {
        targetIsPublished = false;
        targetVerificationStatus = d.verificationStatus || "LEGACY_STATUS_REVIEW";
        targetIsActive = false;
      }
    }

    const statusChangedToUnpublishedOrDeleted =
      (!targetIsPublished && d.isPublished === true) ||
      (!targetIsPublished && d.isPublished === undefined) ||
      (isDeletedOrArchived && !d.isDeleted);

    const freshUpdatedAt = statusChangedToUnpublishedOrDeleted
      ? nowTs
      : (typeof d.updatedAt === "number" ? d.updatedAt : nowTs);

    const needsUpdate =
      d.isPublished !== targetIsPublished ||
      d.verificationStatus !== targetVerificationStatus ||
      d.isActive !== targetIsActive ||
      d.isDeleted === undefined ||
      !d.normalizedPhone ||
      !d.normalizedName ||
      !d.updatedAt ||
      typeof d.updatedAt !== "number" ||
      statusChangedToUnpublishedOrDeleted;

    if (needsUpdate) {
      businessesEnrichedCount++;
      if (!isDryRun) {
        await doc.ref.set(
          {
            isPublished: targetIsPublished,
            isDeleted: isDeletedOrArchived || (d.isDeleted || false),
            isActive: targetIsActive,
            verificationStatus: targetVerificationStatus || "UNVERIFIED",
            normalizedPhone: normPhone,
            normalizedName: normName,
            normalizedWebsite: normWebsite,
            normalizedAddress: normAddress,
            updatedAt: freshUpdatedAt
          },
          { merge: true }
        );
      }
    }

    // Emit tombstone record with fresh updatedAt if unpublished, inactive, or deleted
    if (!targetIsPublished || isDeletedOrArchived || targetIsActive === false) {
      tombstonesEmittedCount++;
      if (!isDryRun) {
        await db.collection("business_tombstones").doc(doc.id).set({
          id: doc.id,
          businessId: doc.id,
          reason: isDeletedOrArchived ? "DELETED" : "UNPUBLISHED",
          updatedAt: freshUpdatedAt
        });
      }
    }

    // Keep in-memory lookup maps in sync for Step B (both in dry-run and live modes)
    const updatedRecord = {
      ...d,
      isPublished: targetIsPublished,
      isDeleted: isDeletedOrArchived || (d.isDeleted || false),
      isActive: targetIsActive,
      verificationStatus: targetVerificationStatus || "UNVERIFIED",
      normalizedPhone: normPhone,
      normalizedName: normName
    };
    existingBizById.set(doc.id, updatedRecord);

    if (normPhone && targetIsPublished && !isDeletedOrArchived && targetIsActive !== false) {
      existingBizByPhone.set(normPhone, { id: doc.id, name: d.name });
    } else if (normPhone && (!targetIsPublished || isDeletedOrArchived || targetIsActive === false)) {
      existingBizByPhone.delete(normPhone);
    }
  }

  // Step B: Migrate activities with strict publication criteria & multi-signal matching
  console.log("\nMigrating legacy /activities into /businesses...");
  for (const doc of actSnap.docs) {
    const act = doc.data();
    const actId = doc.id;

    // Idempotency check: Safe re-run support
    if (act.migratedToBusinessId && existingBizById.has(act.migratedToBusinessId)) {
      alreadyMigratedSkipped++;
      continue;
    }

    const normPhone = normalizePhone(act.phone);
    const normName = normalizeArabic(act.name);
    const normWebsite = normalizeWebsite(act.websiteUrl || act.facebookUrl);
    const normAddress = normalizeArabic(act.address);

    // 1. Check if legacy activity is deleted or archived
    const isDeletedOrArchived =
      act.isDeleted === true ||
      act.isArchived === true ||
      ["DELETED", "ARCHIVED"].includes((act.status || "").toString().toUpperCase()) ||
      ["DELETED", "ARCHIVED"].includes((act.verificationStatus || "").toString().toUpperCase());

    if (isDeletedOrArchived) {
      migratedDeletedOrArchivedCount++;
      const targetBusinessId = actId;
      if (!isDryRun) {
        await db.collection("businesses").doc(targetBusinessId).set({
          id: targetBusinessId,
          name: act.name || "نشاط مؤرشف",
          normalizedName: normName,
          categoryId: act.categoryId || "cat_general",
          categoryName: act.categoryName || "خدمات عامة",
          specialty: act.specialty || "",
          description: act.description || "",
          phone: act.phone || "",
          normalizedPhone: normPhone,
          address: act.address || "ميت غمر",
          normalizedAddress: normAddress,
          area: act.area || "وسط البلد",
          isPublished: false,
          isDeleted: true,
          isActive: false,
          isVerified: false,
          verificationStatus: "ARCHIVED",
          reviewNotes: "نشاط محذوف أو مؤرشف في الأرشيف القديم",
          createdAt: typeof act.createdAt === "number" ? act.createdAt : nowTs,
          updatedAt: nowTs
        }, { merge: true });

        await doc.ref.set({
          migratedToBusinessId: targetBusinessId,
          migratedAt: nowTs,
          migrationStatus: "MIGRATED_ARCHIVED"
        }, { merge: true });

        await db.collection("businessSources").doc("src_act_" + actId).set({
          id: "src_act_" + actId,
          businessId: targetBusinessId,
          sourceType: "LEGACY_ACTIVITIES_MIGRATION",
          sourceId: actId,
          sourceName: "أرشيف الأنشطة القديم (محذوف/مؤرشف)",
          discoveredAt: typeof act.createdAt === "number" ? act.createdAt : nowTs,
          lastCheckedAt: nowTs,
          lastVerifiedAt: typeof act.lastVerifiedAt === "number" ? act.lastVerifiedAt : nowTs,
          sourceConfidence: 20,
          isOfficial: true,
          isActive: false,
          updatedAt: nowTs
        });

        existingBizById.set(targetBusinessId, { isDeleted: true, isPublished: false });
      }
      continue;
    }

    // 2. Check if legacy activity is incomplete
    const isIncomplete =
      !act.name ||
      act.name.trim().length < 2 ||
      (!act.phone && !act.address);

    if (isIncomplete) {
      migratedIncompleteCount++;
      const targetBusinessId = actId;
      if (!isDryRun) {
        await db.collection("businesses").doc(targetBusinessId).set({
          id: targetBusinessId,
          name: act.name || "نشاط تجاري غير مكتمل البيانات",
          normalizedName: normName,
          categoryId: act.categoryId || "cat_general",
          categoryName: act.categoryName || "خدمات عامة",
          specialty: act.specialty || "",
          description: act.description || "",
          phone: act.phone || "",
          normalizedPhone: normPhone,
          address: act.address || "ميت غمر",
          normalizedAddress: normAddress,
          area: act.area || "وسط البلد",
          isPublished: false,
          isDeleted: false,
          isActive: false,
          isVerified: false,
          verificationStatus: "INCOMPLETE_DATA_REVIEW",
          reviewNotes: "بيانات غير مكتملة في الأرشيف القديم (ينقص الهاتف أو العنوان أو الاسم)",
          createdAt: typeof act.createdAt === "number" ? act.createdAt : nowTs,
          updatedAt: nowTs
        }, { merge: true });

        await doc.ref.set({
          migratedToBusinessId: targetBusinessId,
          migratedAt: nowTs,
          migrationStatus: "MIGRATED_INCOMPLETE_REVIEW"
        }, { merge: true });

        await db.collection("businessSources").doc("src_act_" + actId).set({
          id: "src_act_" + actId,
          businessId: targetBusinessId,
          sourceType: "LEGACY_ACTIVITIES_MIGRATION",
          sourceId: actId,
          sourceName: "أرشيف الأنشطة القديم (غير مكتمل)",
          discoveredAt: typeof act.createdAt === "number" ? act.createdAt : nowTs,
          lastCheckedAt: nowTs,
          lastVerifiedAt: typeof act.lastVerifiedAt === "number" ? act.lastVerifiedAt : nowTs,
          sourceConfidence: 30,
          isOfficial: true,
          isActive: false,
          updatedAt: nowTs
        });

        existingBizById.set(targetBusinessId, { isDeleted: false, isPublished: false });
      }
      continue;
    }

    // 3. Multi-signal deduplication: Check by ID and phone + name similarity
    let matchedId = null;
    let isAmbiguousPhone = false;

    if (existingBizById.has(actId)) {
      matchedId = actId;
    } else if (normPhone && existingBizByPhone.has(normPhone)) {
      const candidate = existingBizByPhone.get(normPhone);
      const similarity = calculateNameSimilarity(act.name, candidate.name);
      if (similarity >= 0.5) {
        // High confidence match: Same phone AND similar name
        matchedId = candidate.id;
      } else {
        // Multi-signal safeguard: Same phone BUT different names -> AMBIGUOUS!
        isAmbiguousPhone = true;
      }
    }

    if (matchedId) {
      // Confirmed Duplicate: Merge missing fields into existing business
      duplicatesMergedCount++;
      if (!isDryRun) {
        const existing = existingBizById.get(matchedId) || {};
        await db.collection("businesses").doc(matchedId).set(
          {
            phoneSecondary: existing.phoneSecondary || act.phoneSecondary || null,
            whatsapp: existing.whatsapp || act.whatsapp || null,
            facebookUrl: existing.facebookUrl || act.facebookUrl || null,
            websiteUrl: existing.websiteUrl || act.websiteUrl || null,
            workingHours: existing.workingHours || act.workingHours || "09:00 ص - 10:00 م",
            updatedAt: nowTs
          },
          { merge: true }
        );

        // Update legacy activity provenance
        await doc.ref.set(
          {
            migratedToBusinessId: matchedId,
            migratedAt: nowTs,
            migrationStatus: "MERGED_INTO_EXISTING"
          },
          { merge: true }
        );

        // Write provenance in businessSources (Clean public provenance, no raw payload)
        await db.collection("businessSources").doc("src_act_" + actId).set({
          id: "src_act_" + actId,
          businessId: matchedId,
          sourceType: "LEGACY_ACTIVITIES_MIGRATION",
          sourceId: actId,
          sourceName: "أرشيف الأنشطة القديم (مدمج)",
          discoveredAt: typeof act.createdAt === "number" ? act.createdAt : nowTs,
          lastCheckedAt: nowTs,
          lastVerifiedAt: typeof act.lastVerifiedAt === "number" ? act.lastVerifiedAt : nowTs,
          sourceConfidence: 90,
          isOfficial: true,
          isActive: true,
          updatedAt: nowTs
        });
      }
    } else if (isAmbiguousPhone) {
      ambiguousDuplicatePhoneCount++;
      const targetBusinessId = actId;
      if (!isDryRun) {
        const newBiz = {
          id: targetBusinessId,
          name: act.name,
          normalizedName: normName,
          categoryId: act.categoryId || "cat_general",
          categoryName: act.categoryName || "خدمات عامة",
          specialty: act.specialty || "",
          description: act.description || "",
          phone: act.phone || "",
          normalizedPhone: normPhone,
          phoneSecondary: act.phoneSecondary || null,
          whatsapp: act.whatsapp || null,
          city: act.city || "مدينة ميت غمر",
          address: act.address || "ميت غمر",
          normalizedAddress: normAddress,
          area: act.area || "وسط البلد",
          isPublished: false,
          isDeleted: false,
          isActive: false,
          isVerified: false,
          verificationStatus: "AMBIGUOUS_PHONE_REVIEW",
          reviewNotes: "يحمل نفس رقم الهاتف مع نشاط آخر باسم مختلف. يتطلب مراجعة بشرية قبل النشر.",
          dataQualityScore: 50,
          createdAt: typeof act.createdAt === "number" ? act.createdAt : nowTs,
          lastVerifiedAt: typeof act.lastVerifiedAt === "number" ? act.lastVerifiedAt : nowTs,
          updatedAt: nowTs
        };

        await db.collection("businesses").doc(targetBusinessId).set(newBiz);

        await doc.ref.set(
          {
            migratedToBusinessId: targetBusinessId,
            migratedAt: nowTs,
            migrationStatus: "MIGRATED_AMBIGUOUS_PHONE_REVIEW"
          },
          { merge: true }
        );

        await db.collection("businessSources").doc("src_act_" + actId).set({
          id: "src_act_" + actId,
          businessId: targetBusinessId,
          sourceType: "LEGACY_ACTIVITIES_MIGRATION",
          sourceId: actId,
          sourceName: "أرشيف الأنشطة القديم (رقم هاتف ملتبس)",
          discoveredAt: typeof act.createdAt === "number" ? act.createdAt : nowTs,
          lastCheckedAt: nowTs,
          lastVerifiedAt: typeof act.lastVerifiedAt === "number" ? act.lastVerifiedAt : nowTs,
          sourceConfidence: 50,
          isOfficial: true,
          isActive: false,
          updatedAt: nowTs
        });

        existingBizById.set(targetBusinessId, newBiz);
      }
    } else {
      // 4. Standard Activity: Check for explicit, reliable approval proof
      const hasApproval = hasExplicitApproval(act);
      let isPublished = false;
      let isActive = false;
      let isVerified = false;
      let verificationStatus = "PENDING_REVIEW";
      let reviewNotes = null;

      if (hasApproval) {
        migratedExplicitlyApprovedPublishedCount++;
        isPublished = true;
        isActive = true;
        isVerified = true;
        verificationStatus = "VERIFIED";
        reviewNotes = null;
      } else {
        migratedUnprovenPendingReviewCount++;
        isPublished = false;
        isActive = false;
        isVerified = false;
        const rawStatus = (act.status || "").toString().toUpperCase();
        const rawVStatus = (act.verificationStatus || "").toString().toUpperCase();
        if (rawStatus === "DRAFT" || rawVStatus === "DRAFT") {
          verificationStatus = "DRAFT_REVIEW";
          reviewNotes = "مسودة غير مكتملة أو غير معتمدة في الأرشيف القديم";
        } else if (rawStatus === "REJECTED") {
          verificationStatus = "REJECTED_LEGACY";
          reviewNotes = "نشاط مرفوض سابقًا في الأرشيف القديم";
        } else if (rawStatus === "PENDING" || rawVStatus === "PENDING") {
          verificationStatus = "PENDING_REVIEW";
          reviewNotes = "طلب مساهمة قديم قيد الانتظار لم يُعتمد";
        } else {
          verificationStatus = "LEGACY_STATUS_REVIEW";
          reviewNotes = "سجل قديم يفتقر لعلامة اعتماد صريحة موثوقة - قيد مراجعة المشرف";
        }
      }

      const targetBusinessId = actId;
      if (!isDryRun) {
        const newBiz = {
          id: targetBusinessId,
          name: act.name,
          normalizedName: normName,
          categoryId: act.categoryId || "cat_general",
          categoryName: act.categoryName || "خدمات عامة",
          specialty: act.specialty || "",
          description: act.description || "",
          phone: act.phone || "",
          normalizedPhone: normPhone,
          phoneSecondary: act.phoneSecondary || null,
          whatsapp: act.whatsapp || null,
          city: act.city || "مدينة ميت غمر",
          address: act.address || "ميت غمر",
          normalizedAddress: normAddress,
          area: act.area || "وسط البلد",
          facebookUrl: act.facebookUrl || null,
          websiteUrl: act.websiteUrl || null,
          normalizedWebsite: normWebsite,
          latitude: act.latitude || 30.7183,
          longitude: act.longitude || 31.2568,
          workingHours: act.workingHours || "09:00 ص - 10:00 م",
          isOpenNow: act.isOpenNow !== undefined ? act.isOpenNow : true,
          isVerified: isVerified,
          isActive: isActive,
          isPublished: isPublished,
          isDeleted: false,
          ratingAverage: act.ratingAverage || 5.0,
          ratingCount: act.ratingCount || 1,
          viewCount: act.viewCount || 1,
          imageUrl: act.imageUrl || null,
          verificationStatus: verificationStatus,
          reviewNotes: reviewNotes,
          dataQualityScore: act.dataQualityScore || (isPublished ? 85 : 45),
          createdAt: typeof act.createdAt === "number" ? act.createdAt : nowTs,
          lastVerifiedAt: typeof act.lastVerifiedAt === "number" ? act.lastVerifiedAt : nowTs,
          updatedAt: nowTs
        };

        await db.collection("businesses").doc(targetBusinessId).set(newBiz);

        // Update legacy activity provenance
        await doc.ref.set(
          {
            migratedToBusinessId: targetBusinessId,
            migratedAt: nowTs,
            migrationStatus: isPublished ? "MIGRATED_PUBLISHED" : "MIGRATED_PENDING_REVIEW"
          },
          { merge: true }
        );

        // Write provenance in businessSources (Clean public provenance, no raw payload)
        await db.collection("businessSources").doc("src_act_" + actId).set({
          id: "src_act_" + actId,
          businessId: targetBusinessId,
          sourceType: "LEGACY_ACTIVITIES_MIGRATION",
          sourceId: actId,
          sourceName: "أرشيف الأنشطة القديم (activities)",
          discoveredAt: typeof act.createdAt === "number" ? act.createdAt : nowTs,
          lastCheckedAt: nowTs,
          lastVerifiedAt: typeof act.lastVerifiedAt === "number" ? act.lastVerifiedAt : nowTs,
          sourceConfidence: isPublished ? 85 : 45,
          isOfficial: true,
          isActive: isPublished,
          updatedAt: nowTs
        });

        // Track in local map for rest of migration loop
        existingBizById.set(targetBusinessId, newBiz);
        if (normPhone && isPublished) {
          existingBizByPhone.set(normPhone, { id: targetBusinessId, name: newBiz.name });
        }
      }
    }
  }

  const result = {
    isDryRun,
    existingBusinesses: bizSnap.size,
    legacyActivities: actSnap.size,
    alreadyMigratedSkipped,
    businessesEnriched: businessesEnrichedCount,
    migratedExplicitlyApprovedPublished: migratedExplicitlyApprovedPublishedCount,
    migratedUnprovenPendingReview: migratedUnprovenPendingReviewCount,
    migratedIncomplete: migratedIncompleteCount,
    migratedDeletedOrArchived: migratedDeletedOrArchivedCount,
    ambiguousDuplicatePhoneReview: ambiguousDuplicatePhoneCount,
    duplicatesMerged: duplicatesMergedCount,
    tombstonesEmitted: tombstonesEmittedCount,
    finalTotalBusinesses:
      bizSnap.size +
      migratedExplicitlyApprovedPublishedCount +
      migratedUnprovenPendingReviewCount +
      migratedIncompleteCount +
      migratedDeletedOrArchivedCount +
      ambiguousDuplicatePhoneCount
  };

  console.log("\n=======================================================");
  console.log("Migration Audit Summary:");
  console.log(`- Existing in /businesses:                         ${result.existingBusinesses}`);
  console.log(`- Total in /activities:                            ${result.legacyActivities}`);
  console.log(`- Already Migrated (Skipped safely):               ${result.alreadyMigratedSkipped}`);
  console.log(`- Businesses Enriched with Fields:                 ${result.businessesEnriched}`);
  console.log(`- Published with Explicit Approval:                ${result.migratedExplicitlyApprovedPublished}`);
  console.log(`- Pending Review (Lack of Approval Proof/Draft):   ${result.migratedUnprovenPendingReview}`);
  console.log(`- Incomplete Data (Flagged for Review):            ${result.migratedIncomplete}`);
  console.log(`- Deleted or Archived (Preserved Inactive):        ${result.migratedDeletedOrArchived}`);
  console.log(`- Ambiguous Phone Collisions (Pending Review):     ${result.ambiguousDuplicatePhoneReview}`);
  console.log(`- Duplicates Merged (High Confidence):             ${result.duplicatesMerged}`);
  console.log(`- Final Expected /businesses Count:                ${result.finalTotalBusinesses}`);
  console.log(`=======================================================\n`);

  if (isDryRun) {
    console.log("✔ Dry-run completed safely. No changes were written to Firestore.");
  } else {
    console.log("✔ Live migration applied successfully!");
  }

  return result;
}

if (require.main === module) {
  migrate()
    .then(() => process.exit(0))
    .catch((err) => {
      console.error("Migration failed:", err);
      process.exit(1);
    });
}

module.exports = {
  migrate,
  calculateNameSimilarity,
  normalizeArabic,
  normalizePhone
};
