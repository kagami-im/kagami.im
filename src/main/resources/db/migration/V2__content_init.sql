-- ============================================================
-- Content domain: posts, tags and the post_tags link table.
--
-- posts.content stores the raw Markdown source; rendering to HTML is the frontend's job, so the
-- backend never has to ship a Markdown parser.
--
-- slug is the SEO-friendly URL segment for the future SSR frontend. It is nullable: a post without
-- one is addressed by id instead. PostgreSQL allows any number of NULLs in a unique index, so
-- slug-less posts never collide; duplicate non-null slugs are rejected by the application with 409.
--
-- published_at is written by the application (via the shared Clock) the first time a post is
-- published. Like in V1 there are no triggers, so the application stays the single writer.
--
-- Timestamps: created_at/updated_at are owned by the MyBatis-Plus MetaObjectHandler (see the V1
-- header for the reasoning).
-- ============================================================

create table posts
(
    id           bigserial primary key,
    author_id    bigint       not null references users (id),
    title        varchar(150) not null,
    slug         varchar(150) unique,
    summary      varchar(500),
    cover_url    varchar(500),
    content      text         not null,
    status       smallint     default 0 not null,
    published_at timestamp with time zone,
    created_at   timestamp with time zone default now() not null,
    updated_at   timestamp with time zone default now() not null
);

-- Public listings only ever read status = 1 ordered by published_at; the manage console reads
-- (author_id, status). Two narrow indexes beat one wide one.
create index idx_posts_published on posts (status, published_at desc);
create index idx_posts_author_status on posts (author_id, status);

comment on column posts.status is '0 = draft, 1 = published';
comment on column posts.slug is 'SEO-friendly URL segment; null means the post is addressed by id';

create table tags
(
    id         bigserial primary key,
    name       varchar(30) not null unique,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null
);

create table post_tags
(
    post_id bigint not null references posts (id) on delete cascade,
    tag_id  bigint not null references tags (id) on delete cascade,
    primary key (post_id, tag_id)
);

-- The PK covers lookups by post (deleteByPostId / the IN (...) tag fetch); this one covers the
-- per-tag post counting on the tag page.
create index idx_post_tags_tag on post_tags (tag_id);
