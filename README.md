# Kagami

Kagami is a full-stack web application, currently in its early stage. The core is a complete user system: registration, login, stateless JWT authentication, and a current-user profile endpoint, plus a content domain on top of it: posts with Markdown bodies, tags, drafts and an explicit publish workflow, exposed both as a public read API and as an author management API.

## Project Status

Shipped, in build order:

1. **User system** — registration, JWT auth, profile (`/api/me`), rate limiting
2. **Content model** — posts/tags/post_tags, public read API + author management API, pagination envelope
3. **Console** (`kagami-ui`) — post management + markdown editor with live preview
4. **Post extras** — pinned posts, word count and reading time on the public detail DTO
5. **Comments** — two-level comment threads: comment API with moderation (hide/unhide/delete) and console moderation UI; first test suite (Testcontainers + MockMvc, 9 API tests)
6. **Public frontend** (`ui/`) — personal-site home shell with a centralized design-token layer, a 1200px centered layout and a responsive main/sidebar grid; content is currently simple placeholders, with the remaining pages, routing and public API integration still to come

Next up (not started): PostgreSQL full-text search, views/likes, and connecting the public frontend to the live API.

See [docs/learning-guide.md](docs/learning-guide.md) for the full build log: design decisions, knowledge points and pitfalls of every phase, written as a from-scratch roadmap.

## Tech Stack

**Backend** (`src/`)

- Java 25, Spring Boot 4.1.0 (Web MVC, Validation, Security, Flyway)
- Spring Security + Nimbus JOSE JWT (tokens signed and verified with an RSA key pair)
- MyBatis-Plus 3.5.17 + PostgreSQL 18 (schema managed by Flyway)
- Lombok

**Frontend** (`kagami-ui/`)

- Vue 3 + TypeScript + Vite 8
- Pinia, Vue Router, Axios; native controls styled by a small CSS token layer (no component library)
- markdown-it + shiki for the editor's live preview
- ESLint + Prettier

**Public site** (`front/`)

- Vue 3 + TypeScript + Vite 8 + Vue Router
- Centralized CSS design tokens (no component library), markdown-it for article bodies
- Typed local mock data behind a swappable data layer; all images use a placeholder component

**Infrastructure**

- Docker Compose provides PostgreSQL 18.4 (Redis / RabbitMQ / Elasticsearch went away with the features that needed them)

## Project Layout

```
kagami/
├── src/main/java/im/kagami/
│   ├── common/        # Shared layer: R envelope, PageResponse, global exceptions, audit field filling, Clock
│   ├── security/      # Auth: JWT filter, key config, login/register endpoints, @AnonymousAccess
│   ├── user/          # User module: users table, /api/me endpoint
│   ├── post/          # Content module: posts/tags tables, public read API, author management API
│   └── KagamiApplication.java
├── src/main/resources/
│   ├── application.yml        # Shared config (JWT parameters, Flyway)
│   ├── application-dev.yml    # Dev profile (local database, debug logging)
│   ├── application-prod.yml   # Prod profile (database credentials from environment variables)
│   ├── db/migration/          # Flyway scripts (V1 users → V2 posts/tags → V3 post extras → V4 comments; applied migrations are immutable)
│   └── keys/                  # RSA key pair (the private key is not tracked; see below)
├── kagami-ui/                 # Vue 3 console frontend (see kagami-ui/README.md)
├── front/                     # Vue 3 public site (see front/README.md)
├── compose.yaml               # Local PostgreSQL
├── docs/                      # learning-guide.md (build log & roadmap), mock_data.sql
└── pom.xml
```

## Getting Started

Prerequisites: JDK 25, Docker, Node.js ≥ 20.19 (required by Vite 8).

### 1. Start the database

```bash
docker compose up -d postgres
```

Default connection parameters are `localhost:5432`, database/user/password `kagami` / `kagami` / `123456`. Place a `.env` file in the repository root to override them (e.g. `POSTGRES_PASSWORD=xxx`).

### 2. Generate the JWT key pair

The private key is deliberately not tracked (see `.gitignore`), so a fresh clone needs its own RSA key pair in `src/main/resources/keys/`:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_key_pkcs1.pem
openssl pkcs8 -topk8 -inform PEM -outform PEM -in private_key_pkcs1.pem -out private_key.pem -nocrypt
openssl rsa -in private_key_pkcs1.pem -pubout -out public_key.pem
```

Place the resulting `private_key.pem` and `public_key.pem` under `src/main/resources/keys/`.

### 3. Start the backend

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

The `dev` profile is active by default; the server listens on `http://localhost:8080` and Flyway creates the schema (an empty database runs migration V1; a pre-populated one is baselined at version 1).

### 4. Start the console frontend

```bash
cd kagami-ui
npm install
npm run dev
```

The dev server runs at `http://localhost:5173`, with `/api` requests proxied to `http://localhost:8080` by Vite.

### 5. Start the public frontend

```bash
cd ui
npm install
npm run dev
```

It serves the public site from `http://localhost:5173` (use `-- --port 5174` if the console is already running on 5173). Content is local mock data, so the backend is not required. See `front/README.md` for architecture and image-replacement notes.

### 6. (Optional) Load test data

```bash
psql -h localhost -U kagami -d kagami -f docs/mock_data.sql
```

The script generates 1,000 test users (`user0001` – `user1000`), all with the password `12345678`, ready to log in, plus 40 posts (80% published) written by `user0001` – `user0050` and 10 tags.

## API Overview

| Method | Path                                  | Auth       | Description                                        |
| ------ | ------------------------------------- | ---------- | -------------------------------------------------- |
| POST   | `/api/register`                       | Anonymous  | Register (username, password, nickname)            |
| POST   | `/api/login`                          | Anonymous  | Log in, returns an access token                    |
| GET    | `/api/me`                             | JWT        | Current signed-in user                             |
| GET    | `/api/ciallo`                         | Anonymous  | Easter egg                                         |
| GET    | `/api/posts`                          | Anonymous  | Published posts, paginated (`page`, `size`, `tag`) |
| GET    | `/api/posts/archives`                 | Anonymous  | All published posts in minimal form                |
| GET    | `/api/posts/{slugOrId}`               | Anonymous  | One published post (numeric segment = id, else slug) |
| GET    | `/api/tags`                           | Anonymous  | All tags with published-post counts                |
| GET    | `/api/manage/posts`                   | JWT        | Own posts, paginated (`status`, `keyword` filters) |
| GET    | `/api/manage/posts/{id}`              | JWT        | Own post for editing, any status                   |
| POST   | `/api/manage/posts`                   | JWT        | Create a draft (title, content, tags, …)           |
| PUT    | `/api/manage/posts/{id}`              | JWT        | Update editable fields; publish state untouched    |
| POST   | `/api/manage/posts/{id}/publish`      | JWT        | Publish a draft (idempotent)                       |
| DELETE | `/api/manage/posts/{id}`              | JWT        | Delete own post                                    |
| GET    | `/api/posts/{slugOrId}/comments`      | Anonymous  | Visible two-level comment threads                  |
| POST   | `/api/posts/{slugOrId}/comments`      | JWT        | Create a comment or a reply (signed-in users)      |
| GET    | `/api/manage/comments?postId`         | JWT        | All comments on the author's post (moderation)     |
| POST   | `/api/manage/comments/{id}/hide`      | JWT        | Hide a comment (post author)                       |
| POST   | `/api/manage/comments/{id}/unhide`    | JWT        | Unhide a comment (post author)                     |
| DELETE | `/api/manage/comments/{id}`           | JWT        | Delete a comment (comment or post author)          |

Every response is wrapped into a uniform envelope by `ResultWrapperAdvice`:

```json
{ "code": 200, "msg": "success", "data": { } }
```

On login, `data` is `{ "accessToken": "...", "expiresIn": 900 }` (`expiresIn` in seconds); the frontend stores the token and sends it via the `Authorization: Bearer <token>` header.

## Authentication Design

- **Stateless JWT**: sessions and CSRF/CORS are disabled; `JwtAuthenticationFilter` validates the token on every request.
- **RS256 signing**: the private key signs, the public key verifies; key locations come from `jwt.rsa.*`.
- **15-minute expiry**: `jwt.expiration-in: 900` (seconds). Tokens are not renewed; a client signs in again after expiry.
- **Declarative anonymous endpoints**: annotate a controller class or method with `@AnonymousAccess` and `AnonymousUrlCollector` collects them at startup and permits them; everything else requires a valid JWT.
- **Password storage**: BCryptPasswordEncoder (strength 10).
- **Account status**: `users.status` `0` = active, `1` = banned; only `status = 0` may log in.

## Rate Limiting

Rate limiting is built on bucket4j: a token bucket per client IP and per route, declared with `@RateLimit` on controller classes or methods (method-level wins). Login allows 5 requests/minute/IP, register 3, and all other limited routes their own capacity; attempts are counted regardless of their outcome, and exceeding a limit returns `429` with an `R` body plus a `Retry-After` header. Behind a reverse proxy, forward the real client address in `X-Forwarded-For` so each client gets its own bucket instead of sharing the proxy's.

Buckets currently live in process memory (Caffeine, size-bounded and idle-evicted), which is right for single-node deployment. Storage sits behind the `BucketProvider` seam selected by `kagami.rate-limit.backend` — moving to a cluster-wide Redis backend later only adds a provider and flips that one property; annotations, interceptor and controllers stay untouched.

## Configuration

| Environment variable           | Purpose                            | Default              |
| ------------------------------ | ---------------------------------- | -------------------- |
| `SPRING_PROFILES_ACTIVE`       | Spring profile (dev/prod)          | `dev`                |
| `DB_HOST` / `DB_PORT`          | Database address (prod)            | `localhost` / `5432` |
| `DB_NAME`                      | Database name (prod)               | `kagami`             |
| `DB_USERNAME` / `DB_PASSWORD`  | Database credentials (prod)        | `kagami` / `123456`  |
| `POSTGRES_*`                   | Container bootstrap (compose)      | see `compose.yaml`   |

Production notes:

- Activate the production profile with `SPRING_PROFILES_ACTIVE=prod`; all database credentials come from environment variables.
- Inject the JWT keys via environment variables or a mounted volume — never ship them inside the artifact.
- Flyway does not baseline in prod (`baseline-on-migrate: false`); baseline by hand before the first deployment.

## Common Commands

```bash
# Backend
./mvnw test                     # Run tests
./mvnw clean package            # Build the executable jar

# Frontend (inside kagami-ui/)
npm run build                   # vue-tsc type check + production build
npm run lint / lint:fix         # ESLint check / autofix
npm run format / format:check   # Prettier format / verify

# Public site (inside front/)
npm run build                   # vue-tsc type check + production build
npm run typecheck               # type check only

# Database
docker compose ps               # Container status
docker compose down             # Stop, keeping the data volume
docker compose down -v          # Stop and delete the volume (Flyway re-runs V1)
```

## Further Reading

- Frontend guide: [kagami-ui/README.md](kagami-ui/README.md)
- Public site guide: [front/README.md](front/README.md)
- Frontend style guide: [kagami-ui/docs/development/frontend-style-guide.md](kagami-ui/docs/development/frontend-style-guide.md)
- Build log & learning roadmap: [docs/learning-guide.md](docs/learning-guide.md)
