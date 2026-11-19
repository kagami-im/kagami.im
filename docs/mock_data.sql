-- ============================================================
-- Sample data generator (schema: Flyway migrations V1 + V2)
-- Target: about 1000 users and 40 posts (with tags)
-- Requires: the schema already created by Flyway; this script only inserts rows
-- Database: PostgreSQL (uses generate_series / random / pgcrypto)
-- Every account has the plaintext password: 12345678
--   The hash is generated row by row by pgcrypto's crypt() with gen_salt('bf', 10), which matches the
--   backend's Spring Security BCryptPasswordEncoder default strength of 10, so these accounts can log
--   in straight away. Nothing is hardcoded, so nobody has to reverse-engineer a constant digest.
--
-- Usernames: user0001 .. user1000; posts are written by user0001 .. user0050
--
-- To change the volume, adjust the upper bounds of the generate_series(1, N) below
-- ============================================================

-- The hash needs pgcrypto, which is off by default in dev/test databases
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Clear before loading so the script can be re-run (development and test databases only). CASCADE
-- from users also wipes posts; the explicit tables keep the intent readable.
TRUNCATE users, posts, tags, post_tags RESTART IDENTITY CASCADE;


-- ---------- users (1000 rows) ----------
WITH d AS (
  SELECT ARRAY[
    'Loves tinkering with anything technical.','Just an ordinary programmer.','life is short, code more','Learning backend development, one step at a time.',
    'Photography enthusiast / coder','Reading, writing, writing code.','Backend developer, occasionally frontend','Posts rarely, whenever the mood strikes.'
  ] AS bio,
  ARRAY[
    'Alex','Jamie','Robin','Casey','Coco','Rune','Nyx','Sam','Max','Eleven',
    'Clementine','Nova','Quinn','Skyler','Ash','Iris','Orion','Pike','Cola','Aza'
  ] AS nick
)
INSERT INTO users (username, password_hash, email, avatar_url, bio, status, created_at, updated_at, nickname)
SELECT
  'user' || lpad(i::text, 4, '0')                                              AS username,
  crypt('12345678', gen_salt('bf', 10))                                        AS password_hash,
  'user' || lpad(i::text, 4, '0') || '@example.com'                           AS email,
  null                                                                        AS avatar_url,
  CASE WHEN random() < 0.2 THEN NULL
       ELSE d.bio[1 + floor(random() * 8)::int] END                           AS bio,
  -- status: 0 = active, 1 = banned, matching the login check in AuthService. Roughly 2% come out
  -- banned so the rejection path has something to exercise.
  CASE WHEN random() < 0.02 THEN 1 ELSE 0 END                                 AS status,
  x.c                                                                          AS created_at,
  x.c + ((random() * 30) || ' days')::interval                                AS updated_at,
  d.nick[1 + floor(random() * 20)::int] || (1 + floor(random() * 99)::int)::text AS nickname
FROM generate_series(1, 1000) AS i,
     LATERAL (SELECT random() AS r, (now() - (random() * 730 || ' days')::interval) AS c) x,
     d;


-- ---------- posts (40 rows, 80% published) ----------
WITH d AS (
  SELECT ARRAY[
    'Why I still write SQL by hand','A gentle tour of Spring Security filters','What SSR actually changes for SEO',
    'Setting up Flyway without the footguns','Rate limiting a Spring Boot API with bucket4j','Markdown pipelines for a blog frontend',
    'Naive UI theming in practice','JWT stateless auth from first principles','Designing a pagination envelope',
    'MyBatis-Plus wrappers vs hand-written XML','Choosing between Astro and Nuxt','Postgres indexes I wish I knew earlier'
  ] AS title,
  ARRAY[
    'A short walk through the idea, with working code at the end.','Notes from rebuilding this twice; the second time was faster.',
    'Everything below fits in one afternoon of tinkering.','The result surprised me more than the process did.',
    'Written after debugging this at 2am, so the pitfalls are real.','A checklist I now copy into every new project.'
  ] AS summary
), raw AS (
  SELECT
    i,
    1 + floor(random() * 50)::int                                               AS author_id,
    d.title[1 + floor(random() * 12)::int] || ' #' || i                         AS title,
    CASE WHEN random() < 0.25 THEN NULL
         ELSE d.summary[1 + floor(random() * 6)::int] END                       AS summary,
    CASE WHEN random() < 0.6 THEN NULL
         ELSE 'https://picsum.photos/seed/post' || i || '/1280/720' END         AS cover_url,
    CASE WHEN random() < 0.8 THEN 1 ELSE 0 END                                  AS status,
    x.c                                                                         AS created_at
  FROM generate_series(1, 40) AS i,
       LATERAL (SELECT (now() - (random() * 365 || ' days')::interval) AS c) x,
       d
), stamped AS (
  -- published_at derives from the same status draw, never an independent random(), so a published
  -- post can never end up with a null published_at (which would make updated_at null)
  SELECT r.*,
         CASE WHEN r.status = 1
              THEN r.created_at + ((random() * 30) || ' days')::interval
         END                                                                   AS published_at
  FROM raw r
)
INSERT INTO posts (author_id, title, slug, summary, cover_url, content, status, published_at, created_at, updated_at)
SELECT
  r.author_id,
  r.title,
  'post-' || r.i,
  r.summary,
  r.cover_url,
  E'# ' || r.title || E'\n\nSome **bold**, some *italic*, and a `code span`.\n\n## Why this matters\n\nA short section body with a [link](https://example.com).\n\n```sql {1,3}\nselect id, title\nfrom posts\nwhere status = 1\norder by published_at desc\nlimit ' || r.i || E';\n```\n\n## Takeaways\n\nA closing paragraph with enough text to make list-page excerpts look real.\n',
  r.status,
  r.published_at,
  r.created_at,
  CASE WHEN r.status = 1
       THEN r.published_at + ((random() * 10) || ' days')::interval
       ELSE r.created_at + ((random() * 90) || ' days')::interval END          AS updated_at
FROM stamped r;


-- ---------- tags (10 rows) ----------
INSERT INTO tags (name, created_at, updated_at)
SELECT
  t.name,
  now() - (random() * 365 || ' days')::interval,
  now()
FROM (VALUES ('Java'), ('Spring Boot'), ('Vue'), ('TypeScript'), ('PostgreSQL'),
             ('Docker'), ('Nuxt'), ('CSS'), ('Architecture'), ('Learning Notes')) AS t(name);


-- ---------- post_tags (1-3 tags per post) ----------
-- The lateral must reference the outer i (via i % 3): an uncorrelated subquery is evaluated once
-- and every post would receive the same tag set. ON CONFLICT swallows the odd duplicate tag id.
INSERT INTO post_tags (post_id, tag_id)
SELECT i, 1 + floor(random() * 10)::int AS tag_id
FROM generate_series(1, 40) AS i,
     LATERAL generate_series(1, 1 + (i % 3)) AS g
ON CONFLICT DO NOTHING;
