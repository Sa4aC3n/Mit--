# Testing Strategy

## Unit
- validators
- rating calculations
- opening-hours logic
- search filters
- sync merge rules

## Integration
- auth
- business fetch
- review submit
- moderation
- favorite sync
- notification deep links

## Offline
- app startup offline
- local search
- local detail page
- stale data display
- reconnect and refresh
- retry failed operations

## Security
- unauthorized review creation
- duplicate reviews
- role escalation
- RLS bypass attempts
- invalid input
- oversized text
- spam/rate limiting

## Acceptance
Critical user journeys from specify.md must pass on a release candidate.

## AI-generated code gate
No generated code is accepted solely because it compiles. It must pass lint, tests, static analysis and human review.
