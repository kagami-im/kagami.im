# Kagami

A full-stack personal site and content backend: stateless RS256 JWT authentication, Markdown posts & tags (public read + author management APIs), two-level comments with moderation, and a Vue 3 public frontend (`ui/`).

## Project Status

Shipped:

1. **User system** — registration, login, RS256 JWT auth, `/api/me`, per-IP rate limiting
2. **Content model** — `posts` / `tags` / `post_tags`, public read API, author management API (drafts, explicit publish, edit, delete), pagination envelope
3. **Post extras** — pinned posts, word count, and reading time computed on write
4. **Comments** — two-level threads (public read, signed-in write, author hide/unhide/delete) + Testcontainers/MockMvc integration suite (9 tests)
5. **Public frontend (`ui/`)** — OKLCH light/dark theme, 1200px responsive home shell, local Markdown article list & detail reader (`src/content/posts` behind `src/data/sources`), and an always-mounted morphing vinyl player streaming an 8-track playlist from Cloudflare R2

Next up: PostgreSQL full-text search, views/likes, frontend routing/nav/search, and wiring `ui/` to the live backend API.

## Tech Stack

- **Backend (`src/`)**: Java 25, Spring Boot 4.1.0 (Web MVC, Validation, Security, Flyway), Nimbus JOSE JWT 10.9.1 (RS256), Bucket4j 8.21.0 + Caffeine, MyBatis-Plus 3.5.17, PostgreSQL 18, Lombok, JUnit 5 + Testcontainers 1.21.4 + MockMvc
- **Public frontend (`ui/`)**: Vue 3, TypeScript 6, Vite 8, Pinia 4, `markdown-it` (no component library, no router yet), single-hue OKLCH design tokens
- **Infrastructure**: Docker Compose (`postgres:18.4`), Cloudflare R2 (`r2.kagami.im`) for player media

## Project Layout

```text
kagami/
├── src/main/java/im/kagami/
│   ├── common/        # R envelope, PageResponse, GlobalExceptionHandler/ErrorCode, CurrentUser, Clock
│   ├── data/          # MyBatis-Plus pagination plugin, AuditMetaObjectHandler
│   ├── security/      # JWT filter/keys, AuthController, @AnonymousAccess, @RateLimit (Bucket4j)
│   ├── user/          # users table, /api/me
│   ├── post/          # posts/tags tables, public read API, author management API
│   ├── comment/       # two-level comment threads, public & moderation APIs
│   └── KagamiApplication.java
├── src/main/resources/
│   ├── application{,-dev,-prod}.yml
│   ├── db/migration/  # Immutable Flyway scripts: V1 users → V2 posts/tags → V3 post extras → V4 comments
│   ├── mapper/        # MyBatis XML mappers (User, Post, Tag, PostTag)
│   └── keys/          # RSA key pair (private_key.pem is gitignored)
├── src/test/java/im/kagami/comment/
│   └── CommentApiControllerTest.java
├── ui/                # Vue 3 public site (see ui/README.md)
├── compose.yaml       # Local PostgreSQL 18.4
├── docs/              # learning-guide.md (design notes & pitfalls), mock_data.sql
└── pom.xml
```

## Getting Started

Prerequisites: JDK 25, Docker, Node.js >= 20.19 (Vite 8).

1. **Start PostgreSQL**:
   ```bash
   docker compose up -d postgres
   ```
   Defaults: `localhost:5432`, DB/user/password `kagami` / `kagami` / `123456` (override via `.env`).

2. **Generate RSA key pair** (placed under `src/main/resources/keys/`; `private_key.pem` is not tracked):
   ```bash
   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_key_pkcs1.pem
   openssl pkcs8 -topk8 -inform PEM -outform PEM -in private_key_pkcs1.pem -out private_key.pem -nocrypt
   openssl rsa -in private_key_pkcs1.pem -pubout -out public_key.pem
   ```

3. **Run backend** (`dev` profile by default, listens on `http://localhost:8080`, Flyway auto-migrates):
   ```bash
   ./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
   ```

4. **Run public frontend** (`http://localhost:5173`; reads local Markdown, backend not required):
   ```bash
   cd ui && npm install && npm run dev
   ```

5. **Load mock data (optional)**:
   ```bash
   psql -h localhost -U kagami -d kagami -f docs/mock_data.sql
   ```
   Seeds 1,000 users (`user0001`–`user1000`, password `12345678`), 40 posts, and 10 tags.

## API Overview

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/api/register` | Anonymous | Register (`username`, `password`, `nickname`) |
| POST | `/api/login` | Anonymous | Log in, returns `{ accessToken, expiresIn }` |
| GET | `/api/me` | JWT | Current signed-in user |
| GET | `/api/ciallo` | Anonymous | Easter egg |
| GET | `/api/posts` | Anonymous | Published posts, paginated (`page`, `size`, `tag`) |
| GET | `/api/posts/archives` | Anonymous | All published posts in minimal form |
| GET | `/api/posts/{slugOrId}` | Anonymous | Published post detail (numeric = id, else slug) |
| GET | `/api/tags` | Anonymous | All tags with published-post counts |
| GET | `/api/manage/posts` | JWT | Own posts, paginated (`status`, `keyword`) |
| GET | `/api/manage/posts/{id}` | JWT | Own post for editing, any status |
| POST | `/api/manage/posts` | JWT | Create draft |
| PUT | `/api/manage/posts/{id}` | JWT | Update editable fields (publish state untouched) |
| POST | `/api/manage/posts/{id}/publish` | JWT | Publish draft (idempotent) |
| DELETE | `/api/manage/posts/{id}` | JWT | Delete own post |
| GET | `/api/posts/{slugOrId}/comments` | Anonymous | Visible two-level comment threads |
| POST | `/api/posts/{slugOrId}/comments` | JWT | Create top-level comment or reply |
| GET | `/api/manage/comments?postId` | JWT | All comments on author's post (moderation) |
| POST | `/api/manage/comments/{id}/hide` | JWT | Hide comment (post author) |
| POST | `/api/manage/comments/{id}/unhide` | JWT | Unhide comment (post author) |
| DELETE | `/api/manage/comments/{id}` | JWT | Delete comment (comment or post author) |

All responses are wrapped by `ResultWrapperAdvice` as `{ "code": 200, "msg": "success", "data": ... }` (business error codes: `1xxxx` user/auth, `2xxxx` content/comments, `9xxxx` common/system). Pass JWT via `Authorization: Bearer <token>`.

## Auth & Rate Limiting

- **Auth**: Stateless RS256 JWT (`jwt.expiration-in: 900`s, no refresh); sessions and CSRF/CORS are disabled. Endpoints annotated with `@AnonymousAccess` are collected at startup by `AnonymousUrlCollector`; all other routes require JWT. Passwords use BCrypt (strength 10); only `users.status = 0` (active) can log in.
- **Rate limiting**: `@RateLimit` + Bucket4j token buckets per client IP (`X-Forwarded-For` aware) and route (e.g. login 5/min, register 3/min). Returns `429` with `R` body and `Retry-After`. Buckets live in Caffeine (`kagami.rate-limit.backend: local`) behind `BucketProvider` for future Redis swap.

## Configuration

| Environment variable | Purpose | Default |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Active profile (`dev` / `prod`) | `dev` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | Database address (`prod`) | `localhost` / `5432` / `kagami` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials (`prod`) | `kagami` / `123456` |
| `POSTGRES_*` | Container bootstrap (`compose.yaml`) | `kagami` / `123456` |

Production notes: set `SPRING_PROFILES_ACTIVE=prod`, mount/inject RSA keys outside the JAR, and baseline Flyway manually (`baseline-on-migrate: false` in prod).

## Common Commands

```bash
# Backend (use mvnw.cmd on Windows)
./mvnw compile                  # Compile check
./mvnw test                     # Run tests (requires Docker for Testcontainers)
./mvnw clean package            # Build executable JAR

# Public frontend (inside ui/)
npm run dev                     # Dev server at http://localhost:5173
npm run build                   # vue-tsc type check + production build
npm run preview                 # Preview production build

# Database
docker compose ps               # Container status
docker compose down             # Stop, keeping volume
docker compose down -v          # Stop and wipe volume (Flyway re-runs V1)
```

## Further Reading

- Public frontend architecture: [ui/README.md](ui/README.md)
- Design notes & pitfalls: [docs/learning-guide.md](docs/learning-guide.md)
