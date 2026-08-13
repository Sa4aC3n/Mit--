# Plan — Met Ghamr Directory

## Recommended Stack

### Mobile
Flutter + Dart (or Native Kotlin + Jetpack Compose on Android platform).

### State/Architecture
Clean-ish layered architecture:
- presentation
- domain
- data

Use a lightweight state-management solution chosen during implementation based on current ecosystem compatibility.

### Local
Room / Realm local database for fast offline search and caching.

Important: use local database strictly as the on-device cache and offline storage. Do not couple product architecture to legacy cloud sync functionality.

### Backend
Supabase / Firebase / REST Services:
- PostgreSQL / Firestore
- Auth
- Storage
- Edge Functions where needed

### Push
Firebase Cloud Messaging (FCM).

### Admin
React + TypeScript + Vite / Native Jetpack Compose Admin Panel.

### Maps
Google Maps or another cost-controlled provider selected during implementation.

## Repository Layout

```text
met-ghamr-directory/
  apps/
    mobile/
    admin/
  packages/
    shared-models/
    shared-utils/
  supabase/
    migrations/
    functions/
  docs/
  specs/
  tests/
```

## Mobile Layers

```text
presentation/
domain/
data/
core/
```

### Data Flow
UI -> Use Case -> Repository -> Local/Remote Data Source

Remote response -> Mapper -> Domain Model -> Local persistence -> UI

## Sync Strategy

1. Pull paginated records.
2. Upsert by stable ID.
3. Compare updated_at/version.
4. Mark deleted records.
5. Never replace the entire database for normal refresh.
6. Retry transient failures with exponential backoff.
7. Show stale-data timestamp.

## Search Strategy

Phase 1:
- Local database search for cached content.
- Remote server search for large/global result sets.

Phase 2:
- Add optimized full-text search if needed.

## Authentication

OAuth providers:
- Google
- Facebook
- Microsoft

Auth service returns normalized User model.

Never put provider secrets in mobile app.

## Reviews

Client:
- input validation
- optimistic UI only when safe

Server:
- authenticated user check
- duplicate review rule
- rating range
- spam/rate limit
- moderation status
- aggregate recalculation

## Analytics

Track:
- app_open
- search
- category_view
- business_view
- call_click
- map_click
- favorite_add/remove
- review_submit
- notification_open
- report_submit

Avoid collecting unnecessary personal data.

## Environments

- local
- staging
- production

Each environment has separate keys and database project.
