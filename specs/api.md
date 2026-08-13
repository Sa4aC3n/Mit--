# API Contract

## Categories
GET /categories
GET /categories/{id}

## Businesses
GET /businesses
GET /businesses/{id}
GET /businesses/{id}/reviews

Query parameters:
- category
- specialty
- locality
- q
- min_rating
- open_now
- verified
- lat
- lng
- radius
- page
- limit

## Reviews
POST /businesses/{id}/reviews
PATCH /reviews/{id}
DELETE /reviews/{id}
POST /reviews/{id}/report

## Favorites
GET /me/favorites
POST /businesses/{id}/favorite
DELETE /businesses/{id}/favorite

## Reports
POST /reports

## Notifications
GET /me/notifications
POST /notifications/{id}/read

## Admin
GET /admin/dashboard
GET /admin/businesses
POST /admin/businesses
PATCH /admin/businesses/{id}
DELETE /admin/businesses/{id}

GET /admin/reviews
PATCH /admin/reviews/{id}/status

GET /admin/reports
PATCH /admin/reports/{id}

POST /admin/notifications/send

All admin endpoints require server-side role authorization.
