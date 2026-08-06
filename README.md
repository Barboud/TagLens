# TagLens

A backend image hosting service with session-based authentication, cloud object storage, AI-powered image tagging, and full-text tag search - built with Spring Boot.

**Live deployment:** [https://taglens.onrender.com](https://taglens.onrender.com)

---

## Overview

TagLens lets users register, log in, and upload images that are automatically tagged by an AI vision model. Every uploaded image is analyzed in the background and enriched with structured tags (objects, descriptive tags, and dominant colors), which are then searchable by anyone - no login required for browsing or search.

---

## Features

- 🔐 **Session-based authentication** - opaque UUID sessions stored in PostgreSQL (not JWT), with bcrypt-hashed passwords
- 📤 **Image upload** - stored in Backblaze B2 (S3-compatible object storage), 10MB size limit
- 🤖 **AI-powered tagging** - Google Gemini (`gemini-3.6-flash`) analyzes each image and generates structured tags, run asynchronously so uploads never block on AI processing
- 🔎 **Public search & browsing** - anyone can browse the 50 most recent uploads or free-text search across all AI-generated tags, no account needed
- 🗂️ **JSONB storage** - tags stored as PostgreSQL `JSONB` with a GIN index for efficient querying
- 🐳 **Containerized & deployed** - Dockerized and running live on Render, built via CI/CD

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 25 |
| Framework | Spring Boot 4.1 (Spring Web, Spring Security, Spring JDBC) |
| Database | PostgreSQL, versioned with Flyway migrations |
| Object storage | Backblaze B2 (via AWS S3 SDK) |
| AI / tagging | Google Gemini API (`gemini-3.6-flash`) |
| Auth | Custom session-based auth (UUID sessions + bcrypt), not JWT |
| Logging | Loki (via Logback appender) |
| Containerization | Docker |
| CI/CD | GitHub Actions → GHCR → Render deploy hook |
| Hosting | Render |

---

## Architecture Notes

- **Sessions over JWT**: session IDs are opaque UUIDs stored server-side in a `sessions` table, giving immediate session invalidation, per-device auditing, and easy bulk logout - trade-offs a stateless JWT can't offer.
- **Async AI tagging**: tagging is triggered via Spring's `@Async` after an image and its metadata are persisted, so the upload response returns immediately. Tags may take a few seconds to appear.
- **JSONB tag storage**: tags are stored as a single `JSONB` column (`{"objects": [...], "tags": [...], "colors": [...]}`) with a GIN index, avoiding a rigid multi-column schema for inherently variable AI output.

---

## API Reference

### Auth

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register` | ❌ | Register a new user |
| `POST` | `/auth/login` | ❌ | Log in, sets `session_id` cookie |
| `POST` | `/auth/logout` | ✅ | Invalidate the current session |
| `GET` | `/auth/me` | ✅ | Get the current authenticated user |

**Register / Login body:**
```json
{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

### Images

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/images/upload` | ✅ | Upload an image (max 10MB). AI tags generate in the background. |
| `GET` | `/images/{id}` | ✅ (owner only) | Get a temporary link to view a specific image |
| `DELETE` | `/images/{id}` | ✅ (owner only) | Delete an image |
| `GET` | `/images/my` | ✅ | List the current user's images |
| `GET` | `/images` | ❌ public | Browse the 50 most recently uploaded images, across all users |
| `GET` | `/images?search=<text>` | ❌ public | Free-text search across all images' AI-generated tags |

**Example tag shape returned with an image:**
```json
{
  "objects": ["car", "tree", "road"],
  "tags": ["outdoor", "sunset", "urban"],
  "colors": ["orange", "gray", "blue"]
}
```

> Ownership is enforced on `GET /images/{id}` and `DELETE /images/{id}` — a user can only act on their own images.

---

## Running Locally

```bash
git clone https://github.com/Barboud/TagLens.git
cd TagLens
```

Set the following environment variables:

```
DB_URL=jdbc:postgresql://localhost:5432/taglens
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
B2_ACCESS_KEY=your_b2_key
B2_SECRET_KEY=your_b2_secret
B2_BUCKET_NAME=your_bucket
GEMINI_API_KEY=your_gemini_key
```

Then run:

```bash
./mvnw spring-boot:run
```

Flyway will run all migrations automatically on startup.

---

## CI/CD

Every push to `main` triggers a GitHub Actions pipeline that:
1. Spins up a PostgreSQL service container and runs the full test suite
2. Builds a Docker image and pushes it to GitHub Container Registry (GHCR)
3. Triggers a deploy hook on Render, which pulls and runs the new image

---

## Project Status

This project was built as a multi-week course assignment covering authentication, cloud storage, AI integration, and deployment. Core functionality - auth, upload, AI tagging, public search, and cloud deployment - is complete and live.