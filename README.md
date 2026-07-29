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



- `POST   /images/upload`             Upload image
- `GET    /images/{id}`               Get image link
- `GET    /images/my`           My uploaded images
- `GET    /images`              Home / search images      **_Public_**
- `DELETE /images/{id}`         Delete my image