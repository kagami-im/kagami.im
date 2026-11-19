-- ============================================================
-- Comments: two-level threads on posts (a top-level comment plus flat
-- replies to it, HN/YouTube style).
--
-- parent_id is NULL for top-level comments and points at a TOP-LEVEL
-- comment for replies (enforced by CommentService, not the schema).
-- The self-referential FK cascades: deleting a top-level comment removes
-- its replies with it.
--
-- status has no "pending" state: only registered users can comment, so
-- moderation is post-author-only hide/unhide (1) rather than pre-moderation.
-- ============================================================

create table comments
(
    id         bigserial primary key,
    post_id    bigint        not null references posts (id) on delete cascade,
    author_id  bigint        not null references users (id),
    parent_id  bigint        references comments (id) on delete cascade,
    content    varchar(1000) not null,
    status     smallint      default 0 not null,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null
);

create index idx_comments_post on comments (post_id, status);
create index idx_comments_parent on comments (parent_id);

comment on column comments.parent_id is 'NULL = top-level comment; replies point at a top-level comment of the same post';
comment on column comments.status is '0 = visible, 1 = hidden by the post author';
