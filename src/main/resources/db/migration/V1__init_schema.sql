-- Initial schema: only the users table, which is all authentication needs.
-- The content domain (categories, tags, posts, comments, likes, favourites) was removed entirely.
--
-- Timestamps: updated_at is owned by application code (the MyBatis-Plus MetaObjectHandler plus the
-- Clock time source). Database triggers are deliberately absent so there is only one writer, and the
-- DEFAULT now() on the column is just a fallback.

create table users
(
    id            bigserial primary key,
    username      varchar(50)  not null unique,
    password_hash varchar(255) not null,
    email         varchar(100) unique,
    avatar_url    varchar(255),
    bio           varchar(500),
    status        smallint     default 1 not null,
    created_at    timestamp with time zone default now() not null,
    updated_at    timestamp with time zone default now() not null,
    nickname      varchar(50)
);

-- Matches the login check in AuthService: only status = 0 can log in, and registration stores 0.
comment on column users.status is '0 = active, 1 = banned';
