# Security & Privacy

## Authentication
- OAuth only for the initial release.
- Normalize provider identity into one profile.
- Never trust client-supplied role.

## Authorization
- RLS for data access.
- Server-side role checks.
- Admin operations require explicit permissions.

## Reviews
- One review per user/business.
- Rate limit creation and edits.
- Sanitize text.
- Do not allow arbitrary HTML.
- Log moderation decisions.

## Privacy
Display only the user's public profile information needed for review attribution:
- display name
- profile image if available

Do not expose:
- OAuth tokens
- internal IDs unnecessarily
- private account metadata
- provider access tokens

## Data Retention
Define retention rules for:
- deleted users
- deleted reviews
- reports
- audit logs

## Backup
Enable scheduled backups for production database and verify restore procedure.
