# Auth Endpoints

## Register
`POST /auth/register`

**Body:**
```json
{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

**→ 201:** returns user (`id`, `email`, `created_at`)

## Login
`POST /auth/login`

**Body:**
```json
{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

**→ 200:** creates session, returns `session_id` as cookie

## Logout
`POST /auth/logout`

**Header:** `Cookie: session_id=<SESSION_ID>`

**→ 200 OK**

## Get Current User
`GET /auth/me`

**Header:** `Cookie: session_id=<SESSION_ID>`

**→ 200:** returns user (`id`, `email`, `created_at`)



# Image Endpoints



- `POST   /api/images`             Upload image
- `GET    /api/images/my`           My uploaded images
- `GET    /api/images`              Home / search images      **_Public_**
- `GET    /api/images/{id}/content` Display image     **_Public_**
- `DELETE /api/images/{id}`         Delete my image