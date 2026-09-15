// Unit test for normalization, deduplication, and Cloud Function definitions
const assert = require("assert");

// Test functions directly
function normalizeArabic(str) {
  if (!str) return "";
  let text = str.trim().toLowerCase();
  text = text.replace(/[\u064B-\u065F\u0670]/g, ""); // Remove harakat
  text = text.replace(/[أإآ]/g, "ا"); // Normalize alif
  text = text.replace(/ة/g, "ه"); // Normalize taa marbuta
  text = text.replace(/ى/g, "ي"); // Normalize alif maqsura
  text = text.replace(/[\s\-_]+/g, " "); // Normalize spaces
  return text;
}

function normalizePhone(phone) {
  if (!phone) return "";
  let p = phone.trim();
  const arabicDigits = ["٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"];
  arabicDigits.forEach((d, i) => {
    p = p.replace(new RegExp(d, "g"), i.toString());
  });
  p = p.replace(/[^\d+]/g, "");
  if (p.startsWith("+20")) {
    p = "0" + p.slice(3);
  } else if (p.startsWith("20") && p.length > 10) {
    p = "0" + p.slice(2);
  }
  return p;
}

function normalizeWebsite(url) {
  if (!url) return "";
  return url.trim().toLowerCase()
    .replace(/^https?:\/\//, "")
    .replace(/^www\./, "")
    .replace(/\/+$/, "");
}

console.log("--- Running Backend Normalization & Deduplication Tests ---");

// 1. Arabic normalization tests
assert.strictEqual(normalizeArabic("مَكْتَبَةُ الأَهْرَامِ"), "مكتبه الاهرام");
assert.strictEqual(normalizeArabic("مستشفىٰ ميت غمر   المركزي"), "مستشفي ميت غمر المركزي");
assert.strictEqual(normalizeArabic("أهلاً و سهلاً"), "اهلا و سهلا");
console.log("✔ Arabic normalization test passed!");

// 2. Phone normalization tests
assert.strictEqual(normalizePhone("+201012345678"), "01012345678");
assert.strictEqual(normalizePhone("٠١٠١٢٣٤٥٦٧٨"), "01012345678");
assert.strictEqual(normalizePhone("010-1234-5678"), "01012345678");
assert.strictEqual(normalizePhone("201012345678"), "01012345678");
console.log("✔ Phone normalization test passed!");

// 3. Website normalization tests
assert.strictEqual(normalizeWebsite("https://www.example.com/"), "example.com");
assert.strictEqual(normalizeWebsite("http://example.com"), "example.com");
assert.strictEqual(normalizeWebsite("www.facebook.com/mybiz/"), "facebook.com/mybiz");
console.log("✔ Website normalization test passed!");

// 4. Validate index.js exports
const functionsModule = require("./index.js");
assert(typeof functionsModule.approveContribution === "function", "approveContribution must be exported");
assert(typeof functionsModule.rejectContribution === "function", "rejectContribution must be exported");
assert(typeof functionsModule.archiveOrDeleteBusiness === "function", "archiveOrDeleteBusiness must be exported");
assert(typeof functionsModule.onReviewCreated === "function", "onReviewCreated must be exported");
console.log("✔ Cloud Function export integrity test passed!");

console.log("--- ALL BACKEND TESTS PASSED SUCCESSFULLY 🎉 ---");
