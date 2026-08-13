# Data Model

## PostgreSQL / Supabase

### categories
Primary key: id UUID

Indexes:
- slug unique
- is_active
- sort_order

### businesses
Primary key: id UUID

Indexes:
- category_id
- locality
- status
- is_verified
- updated_at
- rating_average

### business_hours
Composite uniqueness:
- business_id + day_of_week

### business_images
Indexes:
- business_id

### profiles
Primary key: auth user id

Fields:
- display_name
- email
- avatar_url
- provider
- status
- created_at
- updated_at

### reviews
Indexes:
- business_id
- user_id
- status
- created_at

Constraint:
- unique(user_id, business_id)

### favorites
Constraint:
- unique(user_id, business_id)

### reports
Indexes:
- status
- business_id
- review_id

### notifications
Indexes:
- created_at
- status

### audit_logs
Indexes:
- actor_id
- entity_type
- entity_id
- created_at

## Realm / Room Local Models

Keep a local subset optimized for read/offline use:
- LocalCategory
- LocalBusiness
- LocalBusinessHour
- LocalBusinessImage
- LocalReviewSummary
- LocalFavorite
- LocalNotification

Do not store administrative secrets locally.
