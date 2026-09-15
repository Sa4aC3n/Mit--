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

/**
 * Safe Migration Script:
 * 1. Ensures all existing /businesses have:
 *    - isPublished: true
 *    - isDeleted: false
 *    - normalizedPhone, normalizedName, normalizedWebsite, normalizedAddress
 *    - updatedAt
 * 2. Migrates all /activities into /businesses:
 *    - Resolves duplicates between activities and businesses by ID and normalized phone
 *    - Preserves existing IDs, ratings, reviews, working hours, and coordinates
 *    - Creates records in /businessSources with sourceType = "LEGACY_ACTIVITIES_MIGRATION"
 * 3. Supports dry-run flag: node migrateLegacyActivities.js --dry-run
 */
async function migrate() {
  const isDryRun = process.argv.includes("--dry-run");
  console.log(`=======================================================`);
  console.log(`Mit Ghamr Directory: Legacy Activities Migration Tool`);
  console.log(`Mode: ${isDryRun ? "DRY-RUN (Simulated - No writes)" : "LIVE EXECUTION"}`);
  console.log(`=======================================================\n`);

  const nowTs = Date.now();

  // 1. Fetch current businesses
  console.log("Reading /businesses collection...");
  const bizSnap = await db.collection("businesses").get();
  console.log(`Found ${bizSnap.size} existing documents in /businesses.`);

  const existingBizById = new Map();
  const existingBizByPhone = new Map();

  for (const doc of bizSnap.docs) {
    const d = doc.data();
    existingBizById.set(doc.id, d);
    const nPhone = d.normalizedPhone || normalizePhone(d.phone);
    if (nPhone) existingBizByPhone.set(nPhone, doc.id);
  }

  // 2. Fetch legacy activities
  console.log("Reading legacy /activities collection...");
  const actSnap = await db.collection("activities").get();
  console.log(`Found ${actSnap.size} documents in /activities.`);

  let businessesEnrichedCount = 0;
  let activitiesMigratedCount = 0;
  let duplicatesMergedCount = 0;

  // Step A: Enrich existing businesses with required fields
  console.log("\nEnriching existing /businesses with search and sync fields...");
  for (const doc of bizSnap.docs) {
    const d = doc.data();
    const normPhone = d.normalizedPhone || normalizePhone(d.phone);
    const normName = d.normalizedName || normalizeArabic(d.name);
    const normWebsite = d.normalizedWebsite || normalizeWebsite(d.websiteUrl || d.facebookUrl);
    const normAddress = d.normalizedAddress || normalizeArabic(d.address);

    const needsUpdate =
      d.isPublished !== true ||
      d.isDeleted !== false ||
      !d.normalizedPhone ||
      !d.normalizedName ||
      !d.updatedAt;

    if (needsUpdate) {
      businessesEnrichedCount++;
      if (!isDryRun) {
        await doc.ref.set(
          {
            isPublished: true,
            isDeleted: d.isDeleted || false,
            normalizedPhone: normPhone,
            normalizedName: normName,
            normalizedWebsite: normWebsite,
            normalizedAddress: normAddress,
            updatedAt: d.updatedAt || nowTs
          },
          { merge: true }
        );
      }
    }
  }

  // Step B: Migrate activities to businesses
  console.log("\nMigrating legacy /activities into /businesses...");
  for (const doc of actSnap.docs) {
    const act = doc.data();
    const actId = doc.id;
    const normPhone = normalizePhone(act.phone);
    const normName = normalizeArabic(act.name);
    const normWebsite = normalizeWebsite(act.websiteUrl || act.facebookUrl);
    const normAddress = normalizeArabic(act.address);

    // Check duplicate by ID or Phone
    const matchedId = existingBizById.has(actId)
      ? actId
      : (normPhone && existingBizByPhone.has(normPhone))
      ? existingBizByPhone.get(normPhone)
      : null;

    if (matchedId) {
      duplicatesMergedCount++;
      // Merge any missing fields into existing business
      if (!isDryRun) {
        const existing = existingBizById.get(matchedId) || {};
        await db.collection("businesses").document(matchedId).set(
          {
            phoneSecondary: existing.phoneSecondary || act.phoneSecondary || null,
            whatsapp: existing.whatsapp || act.whatsapp || null,
            facebookUrl: existing.facebookUrl || act.facebookUrl || null,
            websiteUrl: existing.websiteUrl || act.websiteUrl || null,
            workingHours: existing.workingHours || act.workingHours || "9:00 ص - 10:00 م",
            updatedAt: nowTs
          },
          { merge: true }
        );
      }
    } else {
      activitiesMigratedCount++;
      if (!isDryRun) {
        const newBiz = {
          id: actId,
          name: act.name || "نشاط تجاري",
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
          workingHours: act.workingHours || "9:00 ص - 10:00 م",
          isOpenNow: act.isOpenNow !== undefined ? act.isOpenNow : true,
          isVerified: act.isVerified !== undefined ? act.isVerified : true,
          isActive: act.isActive !== undefined ? act.isActive : true,
          isPublished: true,
          isDeleted: false,
          ratingAverage: act.ratingAverage || 5.0,
          ratingCount: act.ratingCount || 1,
          viewCount: act.viewCount || 1,
          imageUrl: act.imageUrl || null,
          verificationStatus: "VERIFIED",
          dataQualityScore: act.dataQualityScore || 85,
          createdAt: act.createdAt || nowTs,
          lastVerifiedAt: act.lastVerifiedAt || nowTs,
          updatedAt: nowTs
        };

        await db.collection("businesses").document(actId).set(newBiz);

        // Record provenance in businessSources
        await db.collection("businessSources").doc("src_mig_" + actId).set({
          id: "src_mig_" + actId,
          businessId: actId,
          sourceType: "LEGACY_ACTIVITIES_MIGRATION",
          sourceId: actId,
          sourceName: "أرشيف الأنشطة القديم (activities)",
          discoveredAt: act.createdAt || nowTs,
          lastCheckedAt: nowTs,
          sourceConfidence: 90,
          isOfficial: true,
          isActive: true,
          updatedAt: nowTs
        });
      }
    }
  }

  console.log("\n=======================================================");
  console.log("Migration Audit Summary:");
  console.log(`- Existing Businesses in /businesses: ${bizSnap.size}`);
  console.log(`- Legacy Activities in /activities:   ${actSnap.size}`);
  console.log(`- Businesses Enriched with Fields:   ${businessesEnrichedCount}`);
  console.log(`- New Activities Migrated:           ${activitiesMigratedCount}`);
  console.log(`- Duplicates Merged into Businesses: ${duplicatesMergedCount}`);
  console.log(`- Final Expected Businesses Count:   ${bizSnap.size + activitiesMigratedCount}`);
  console.log(`=======================================================\n`);

  if (isDryRun) {
    console.log("Dry-run complete. No changes were applied to Firestore.");
  } else {
    console.log("Migration applied successfully!");
  }
}

migrate()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("Migration failed:", err);
    process.exit(1);
  });
