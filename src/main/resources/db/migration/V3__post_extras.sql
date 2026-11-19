-- ============================================================
-- Post extras: pinning and reading metadata.
--
-- pinned puts chosen posts at the top of the public list; archives stay purely chronological, so
-- the flag only affects the home listing. The old (status, published_at) index is replaced by one
-- that leads with pinned, matching the new ORDER BY.
--
-- word_count / reading_minutes are computed by the application on every write (see PostService for
-- the formula). The backfill below seeds existing rows with a rough char-length approximation so
-- the UI never shows 0; the next edit of any post replaces it with the exact value.
-- ============================================================

alter table posts add column pinned           boolean not null default false;
alter table posts add column word_count       int     not null default 0;
alter table posts add column reading_minutes  int     not null default 0;

update posts
set word_count      = char_length(content),
    reading_minutes = greatest(1, ceil(char_length(content)::numeric / 400)::int);

drop index idx_posts_published;
create index idx_posts_published_pinned on posts (status, pinned desc, published_at desc);

comment on column posts.pinned is 'true = pinned to the top of the public list (archives unaffected)';
comment on column posts.word_count is 'approximate content length, recomputed by the app on write';
comment on column posts.reading_minutes is 'estimated reading time in minutes, recomputed by the app on write';
