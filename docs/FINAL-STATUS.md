# DALIL MIT GHAMR — FINAL STABILIZATION STATUS

> **Branch:** `final-stabilization`  
> **Base SHA (origin/main):** `6fc651d76603cde3d084a8cec0bbe299e73a646b`  
> **Operation Date:** 2026-09-26

---

## 1. Source of Truth

| Item | Value |
|------|-------|
| GitHub Repository | https://github.com/Sa4aC3n/Mit-- |
| Base Commit (origin/main) | `6fc651d76603cde3d084a8cec0bbe299e73a646b` |
| Working Branch | `final-stabilization` |
| Application ID | `com.dalil.mit3mr.app` |
| Firebase Project | `dalil-mit3mr` |
| Firebase Plan | **Spark (Free)** — No Cloud Functions deployed |

---

## 2. Files Deleted

| File | Size | Reason |
|------|------|--------|
| `docs/phase14-evidence/phase14-evidence.patch` | 2.6 MB | Large binary patch — build evidence, not source code |
| `app/src/main/res/drawable/app_icon.png` | 1.8 MB | Not referenced in any .kt or .xml — dead asset |
| `app/src/main/assets/dalil_mit_ghamr_categories.xls` | 59 KB | Not read at runtime; was incorrectly bundled in APK |
| `app/src/main/assets/dalil_mit_ghamr_categories.xlsx` | 10 KB | Not read at runtime; was incorrectly bundled in APK |

**Excel files root-level:** Moved to `tools/categories/` (development seed data, not Android assets)

---

## 3. Files Modified

| File | Change |
|------|--------|
| `.gitignore` | Added: `*.patch`, `docs/phase*/`, `*.apk`, `*.aab`, `*.jks`, `*.xls`, `*.xlsx`, `/app/build/` |
| `.env.example` | Added `OWNER_EMAIL` and `MASTER_PIN_HASH` documentation |
| `app/build.gradle.kts` | Enabled `isMinifyEnabled = true`, `isShrinkResources = true`; added BuildConfig owner fields |
| `app/src/main/java/com/example/data/model/DirectoryModels.kt` | Default `latitude`/`longitude` changed from hardcoded Mit Ghamr coords to `0.0` |
| `app/src/main/java/com/example/ui/screens/map/InteractiveMapScreen.kt` | Map now **excludes** businesses with coordinates `0.0` — no more incorrect pins at city center |
| `app/src/main/java/com/example/data/security/AppSecurityManager.kt` | **CRITICAL FIX**: Removed hardcoded `MASTER_PIN = "5302"` and `AUTHORIZED_OWNER_EMAIL`; now uses SharedPreferences hash + BuildConfig |

---

## 4. Issues Fixed

### ✅ FIXED: Hardcoded Coordinates Fallback (CRITICAL)
**Problem:** `InteractiveMapScreen.kt` placed ALL businesses without coordinates at `30.7183, 31.2568` (Mit Ghamr city center), creating incorrect map markers.  
**Fix:** Businesses with `lat=0.0, lng=0.0` are now filtered out from map markers. Their address text still appears in list view.

### ✅ FIXED: Default Coordinates in Data Model (CRITICAL)
**Problem:** `BusinessEntity.latitude = 30.7183` — every new entity without explicit coords appeared at city center.  
**Fix:** Default is now `0.0` (filtered by map).

### ✅ FIXED: Hardcoded MASTER_PIN in Source Code (HIGH)
**Problem:** `const val MASTER_PIN = "5302"` was plaintext in source code and committed to Git.  
**Fix:** PIN is now stored ONLY as a SHA-256 hash in SharedPreferences. Plain PIN is never stored. Must be set at first run via Admin Settings.

### ✅ FIXED: Hardcoded AUTHORIZED_OWNER_EMAIL in Source Code (MEDIUM)
**Problem:** `"m.k3shka@gmail.com"` hardcoded in `AppSecurityManager.kt`.  
**Fix:** Now injected from `BuildConfig.OWNER_EMAIL` which reads from `.env` file (gitignored).

### ✅ FIXED: Hardcoded PIN_SALT containing the actual PIN (HIGH)
**Problem:** `PIN_SALT = "MetGhamr_SecVault_2026_5302_Salt"` revealed the PIN in the salt string.  
**Fix:** Device-unique salt generated randomly at first run, stored in SharedPreferences only.

### ✅ FIXED: R8 Minification Disabled in Release (MEDIUM)
**Problem:** `isMinifyEnabled = false` in release build type — no code obfuscation or dead code elimination.  
**Fix:** Enabled `isMinifyEnabled = true` + `isShrinkResources = true` with proper ProGuard rules.

### ✅ FIXED: Large Files in APK Assets (LOW)
**Problem:** Two Excel files (69 KB total) bundled in APK assets but never read at runtime.  
**Fix:** Removed from assets. Moved root-level copies to `tools/categories/` (dev tools only).

### ✅ FIXED: Large Dead PNG in Drawable (LOW)
**Problem:** `app_icon.png` (1.8 MB) in drawable folder, never referenced anywhere.  
**Fix:** Deleted. Real launcher icons use WebP in mipmap-* folders.

---

## 5. Architecture Notes

### Firebase Architecture (Spark Plan — No Cloud Functions)
- **Firestore** = Primary source of truth
- **Firebase Auth** = Authentication + Custom Claims for admin (`request.auth.token.admin == true`)
- **Room DB** = Local offline cache
- **Firebase Realtime Database** = `FirebaseBusinessSyncManager` and `FirebaseRatingManager` still reference it
  - **⚠️ OPEN ITEM**: These two files import `firebase-database` SDK. They appear to be legacy/parallel implementations. `FirestoreRatingManager` is the active one. See Section 8.
- **Firebase Functions** = `BackendApiService.kt` imports `FirebaseFunctions` but `approveContribution()` uses Firestore Transaction directly. SDK dependency should be removed.

### Contribution Approval Flow
1. User submits → Firestore `contributions/{id}` with `status=PENDING`
2. Admin reviews → calls `BackendApiService.approveContribution()`
3. Inside a **Firestore Transaction**:
   - Reads contribution
   - Checks business_reservations for duplicates (atomic)
   - Writes to `businesses/{bizId}`
   - Writes reservation locks to `business_reservations/{key}`
   - Updates contribution to `APPROVED`
4. Firestore Security Rules verify the getAfter() cross-reference

---

## 6. Firestore Security Rules — Status

**File:** `firestore.rules` (191 lines)  
**Version:** `rules_version = '2'`

| Collection | Status | Notes |
|------------|--------|-------|
| `/businesses` | ✅ Correct | Public read if `isPublished=true`, admin write only |
| `/contributions` | ✅ Correct | User creates PENDING only; admin approves via getAfter check |
| `/reviews` | ✅ Correct | User creates PENDING only; cannot set APPROVED |
| `/audit_logs` | ✅ Correct | Admin only; no update/delete |
| `/business_reservations` | ✅ Correct | Admin only (used in Transactions) |
| `/users/{userId}` | ✅ Correct | Owner or admin access |
| `/categories` | ✅ Correct | Public read, admin write |
| `/system_config` | ✅ Correct | Public read, no client write |

**Known Limitation (Firestore Rules):**
> Firestore Rules cannot verify that a hash value computed on the client matches a value computed server-side. The `business_reservations` deduplication relies on the transaction being atomic — the rules enforce that only admins can write, but cannot verify the hash computation itself was done correctly.

---

## 7. Test Execution Results

### Firebase Emulator Tests
| Status | Details |
|--------|---------|
| Tests Written | ✅ `test/firestore-security-tests.test.js` created |
| Tests Executed | See subagent Stage D report |
| Firebase CLI | Check: `firebase --version` |

### Android Build
| Status | Details |
|--------|---------|
| Gradle Debug Build | NOT EXECUTED (requires JDK + Android SDK) |
| Gradle Release Build | NOT EXECUTED |
| APK Size (before) | NOT MEASURED |
| APK Size (after R8) | NOT MEASURED — expected ~30-40% reduction |

---

## 8. Open Items (Require Manual Action)

### HIGH Priority
1. **PIN Setup Migration**: Existing users with `MASTER_PIN = "5302"` will find no PIN configured (old hardcoded comparison removed). **Action Required**: Admin must set new PIN via Admin Settings screen on first launch after update. The `setMasterPin()` method is available in `AppSecurityManager`.

2. **`app_icon_foreground.png` (1.68 MB)**: Still in drawable. Referenced by `ic_launcher_foreground.xml`. Must be converted to WebP using Android Studio's "Convert to WebP" tool.

3. **Firebase Realtime Database**: `FirebaseBusinessSyncManager` and `FirebaseRatingManager` still import `firebase-database`. If they are not called by `DirectoryRepository`, remove them and remove the `implementation("com.google.firebase:firebase-database")` dependency from `build.gradle.kts`.

4. **Firebase Functions Dependency**: `implementation("com.google.firebase:firebase-functions")` in `build.gradle.kts`. If `BackendApiService` never calls `httpsCallable()`, remove this dependency.

### MEDIUM Priority
5. **`BackendApiService.kt` admin email default**: Line 98 `adminEmail: String = "m.k3shka@gmail.com"` — low risk as this is a function default that gets overridden, but should be removed.

6. **OWNER_EMAIL in `.env`**: Must be set locally by developer/CI. Not committed to Git.

### LOW Priority
7. **Manual Device Testing**: Full test matrix requires physical Android device with signed APK.
8. **Firebase Data Cleanup**: All current Firestore data is test/development data. See Section 9 for cleanup plan.

---

## 9. Database Cleanup Plan (Pending Approval)

> ⚠️ **DO NOT EXECUTE without explicit user approval**

### Collections Identified as Test-Only Data
- `contributions` — all submissions are test data
- `businesses` — test businesses created during development
- `reviews` — test reviews
- `businessSources` — test sources
- `audit_logs` — test audit entries

### Data to PRESERVE
- `users/{adminUserId}` — admin account document and custom claims
- `categories` — taxonomy data (canonical, not test data)
- `system_config` — app configuration
- `business_reservations` — only relevant after real data is published

### Action Required Before Cleanup
1. Export current Firestore data: `firebase firestore:export gs://dalil-mit3mr.appspot.com/backup-2026-09-26`
2. Confirm categories collection is the authoritative taxonomy
3. Record admin UID to preserve
4. **Obtain explicit approval before deletion**

---

## 10. Manual Testing Checklist (Required on Physical Device)

> These tests are PENDING. Mark each as PASS / FAIL / NOT_TESTED after execution.

| # | Test | Status |
|---|------|--------|
| 1 | Launch app — splash/intro screen | NOT_TESTED |
| 2 | Login with Google Sign-In | NOT_TESTED |
| 3 | Login with Email/Password | NOT_TESTED |
| 4 | Email verification flow | NOT_TESTED |
| 5 | Logout and session persistence | NOT_TESTED |
| 6 | Browse home screen (businesses list) | NOT_TESTED |
| 7 | Search businesses | NOT_TESTED |
| 8 | View business detail | NOT_TESTED |
| 9 | Favorites — add and remove | NOT_TESTED |
| 10 | Submit a contribution (new business) | NOT_TESTED |
| 11 | View my contributions | NOT_TESTED |
| 12 | Write a review | NOT_TESTED |
| 13 | View my reviews | NOT_TESTED |
| 14 | Map screen — businesses with coords show pins | NOT_TESTED |
| 15 | Map screen — businesses WITHOUT coords do NOT show pins | NOT_TESTED |
| 16 | Notifications screen | NOT_TESTED |
| 17 | User profile and settings | NOT_TESTED |
| 18 | Dark mode toggle | NOT_TESTED |
| 19 | Arabic RTL layout correctness | NOT_TESTED |
| 20 | Admin: Enter correct PIN | NOT_TESTED |
| 21 | Admin: Set PIN at first run | NOT_TESTED |
| 22 | Admin: Dashboard loads | NOT_TESTED |
| 23 | Admin: Review and approve a contribution | NOT_TESTED |
| 24 | Admin: Reject a contribution | NOT_TESTED |
| 25 | Admin: Moderate a review | NOT_TESTED |
| 26 | Offline mode — cached data visible | NOT_TESTED |
| 27 | Duplicate contribution prevention | NOT_TESTED |
| 28 | Concurrent approval attempts | NOT_TESTED |
| 29 | App restart — session restored | NOT_TESTED |
| 30 | Categories screen | NOT_TESTED |

---

## 11. Commit History on `final-stabilization`

| Commit | Description |
|--------|-------------|
| `6fc651d` | Base: chore: complete phase 14 final release documentation |
| `011d614` | cleanup(stage-a-b): remove patch files, large unused images, move dev tools, update gitignore |
| `b3accc7` | fix(coords): exclude businesses without real coordinates from map; default coords 0.0 |
| _(pending)_ | fix(security): remove hardcoded PIN and owner email; R8 minification enabled |
| _(pending)_ | fix(stage-c): Firebase architecture, ProGuard rules, functions dependency |
| _(pending)_ | test(stage-d): Firebase Emulator security tests |
