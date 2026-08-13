# Specify — Met Ghamr Directory

## Problem
Residents and visitors of Met Ghamr need a single, searchable, current directory of local businesses, services, doctors, technicians, factories and organizations.

## Users
### Visitor
Can browse, search, filter, view details, call, navigate, share and see reviews.

### Registered User
Can do everything a visitor can do plus:
- rate
- review
- favorite
- report inaccurate content
- receive personalized notifications

### Moderator
Reviews user-generated content and handles reports.

### Editor
Manages directory records.

### Admin
Manages users, categories, businesses, reviews, notifications and analytics.

### Super Admin
Full access including roles and system configuration.

## Core User Journeys

### Journey A — Find a Restaurant
1. User opens Home.
2. User selects Restaurants.
3. App shows cached/local results immediately.
4. Remote refresh runs if online.
5. User filters by open now/rating/location.
6. User opens a restaurant.
7. User calls or opens map.

### Journey B — Submit a Review
1. User opens business.
2. User taps Add Review.
3. If not authenticated, show OAuth login.
4. User selects 1–5 stars.
5. User writes optional text.
6. Client validates.
7. Server validates ownership/rate limit.
8. Review enters moderation or is published according to policy.
9. User receives status feedback.

### Journey C — Offline Search
1. Device has no internet.
2. User opens app.
3. App reads Realm.
4. User searches locally.
5. App shows last known update timestamp.
6. Any online-only operation displays an appropriate message.

### Journey D — Admin Adds Business
1. Admin logs in.
2. Admin opens Businesses.
3. Admin creates record.
4. Admin validates phone/address/category.
5. Admin optionally places map marker.
6. Record is saved.
7. Audit event is generated.
8. Mobile clients receive it during next sync.

## Acceptance
All journeys must work end-to-end and have automated tests for critical paths.
