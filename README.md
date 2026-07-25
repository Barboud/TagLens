# Authentication Endpoints

## 1. Register User

Creates a new user account.

- **URL:** `/auth/register`
- **Method:** `POST`

**Request Body:**

```json
{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

**Success Response:** `201 Created`

```json
{
  "id": "f5a2dad6-aff3-4a3e-9365-6e70936af223",
  "email": "user@example.com",
  "created_at": "2026-07-24T18:00:00Z"
}
```

---

## 2. Login User

Authenticates user credentials and creates an active session.

- **URL:** `/auth/login`
- **Method:** `POST`

**Request Body:**

```json
{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

**Success Response:** `200 OK`

The server creates a new session and returns the session ID as an HTTP-only cookie.

---

## 3. Logout User

Terminates the current user session.

- **URL:** `/auth/logout`
- **Method:** `POST`
- **Headers:** `Cookie: session_id=<SESSION_ID>`

**Success Response:** `200 OK`

---

## 4. Get Current User Profile

Retrieves details of the currently authenticated user.

- **URL:** `/auth/me`
- **Method:** `GET`
- **Headers:** `Cookie: session_id=<SESSION_ID>`

**Success Response:** `200 OK`

```json
{
  "id": "f5a2dad6-aff3-4a3e-9365-6e70936af223",
  "email": "user@example.com",
  "created_at": "2026-07-24T18:00:00Z"
}
```