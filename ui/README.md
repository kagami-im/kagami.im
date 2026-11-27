# Kagami Public Site (`ui`)

Personal site built with Vue 3 + TypeScript + Vite (`<script setup>` SFCs, Vue Router, Pinia, `markdown-it`, zero component libraries). Articles currently load from local Markdown files in `src/content/posts`.

## Commands

```bash
npm install
npm run dev        # dev server at http://localhost:5173
npm run build      # vue-tsc -b type check + Vite production build
npm run preview    # preview production build
```

For offline development with local media in `public/audio/`, create a gitignored `ui/.env.development.local` containing:

```bash
VITE_MEDIA_BASE_URL=/audio
```

## Layout

```text
src/
├── main.ts                  # applies saved theme before mount, registers Pinia & Vue Router, mounts App
├── App.vue                  # keeps AppHeader, RouterView (with unified dblclick-to-expand), and VinylPlayer mounted
├── router/
│   └── index.ts             # routes: / (HomeView), /posts/:slug (ArticleDetailView), fallback -> /
├── assets/                  # theme-swapping avatars (avatar-light.png, avatar-dark.png)
├── content/posts/           # local Markdown articles with YAML-style frontmatter
├── data/
│   ├── types.ts             # Article, ArticleBody, Frontmatter, ParsedMarkdown
│   ├── frontmatter.ts       # minimal `---` frontmatter parser
│   ├── markdown.ts          # markdown-it renderer (html: false, linkify: true, breaks: true)
│   └── sources/
│       ├── index.ts         # swappable async data-source facade (pinned first, then publishedAt desc)
│       └── local.ts         # eager import.meta.glob(?raw) loader for src/content/posts/*.md
├── stores/
│   ├── articles.ts          # async article list + detail-view loader with loading/error/not-found states
│   ├── audio.ts             # shared <audio> engine, drag scrubbing, keyboard seek, and Media Session API
│   ├── player.ts            # player form ('expanded' | 'mini') and home listening-desk platter slot rect
│   ├── playlist.ts          # track queue from /playlist.json (with retry & optional VITE_MEDIA_BASE_URL), index & loop mode
│   └── theme.ts             # isDark flag, toggles html.dark
├── styles/tokens.css        # afternoon light & dusk dark OKLCH palette, glass surfaces, offline CJK/Latin font stacks, motion tokens
├── style.css                # reset, base typography, ambient backdrop, .selectable opt-in, root scroll lock, reduced-motion base
├── views/
│   ├── HomeView.vue         # afternoon listening desk (stage vinyl slot + track console + inline queue) + 1080px shelf
│   └── ArticleDetailView.vue # route-driven Markdown reader (.selectable, reading progress hairline, veiled reveal)
└── components/
    ├── VinylPlayer.vue      # persistent vinyl morph shell: home desk platter slot <-> full-screen immersive stage
    ├── AppHeader.vue        # fixed book-header nav (frosted on scroll/detail)
    ├── ArticleList.vue      # numbered hairline article index reading from useArticlesStore()
    └── ProfileCard.vue      # quiet desk corner with cross-fading day/dusk avatar and contact link

public/
├── playlist.json            # 8-track queue pointing to Cloudflare R2 (r2.kagami.im)
├── favicon.svg
└── audio/                   # (gitignored) optional local media for offline dev
```

## Architecture & Conventions

### Design Tokens & Layout
- **Afternoon & Dusk OKLCH (`src/styles/tokens.css`)**: Light mode uses warm ivory paper surfaces with sky-blue (`--color-primary`) and warm amber (`--color-accent-warm`) accents; dark mode shifts to soft slate-blue dusk (never harsh pure black). `:root` and `:root.dark` share identical semantic token names, so components never branch on theme mode.
- **Strict token rule**: no hex colors or `color-mix()` in components — tune colors only in `tokens.css`.
- **Offline typography**: `--font-serif`, `--font-sans`, and `--font-mono` use local CJK + Latin system stacks with `tabular-nums` for track numbers, timestamps, and dates (zero external font CDN requests).
- **Theme persistence**: `useThemeStore` persists `kagami-ui:dark` and toggles `html.dark`; `main.ts` applies it before mount (falling back to `prefers-color-scheme`) to prevent FOUC.
- **Reduced motion**: views, transitions, and `VinylPlayer` respect `@media (prefers-reduced-motion: reduce)` by disabling blur/translate reveals and vinyl spin.

### State, Routing & Data Layer
- **Vue Router (`src/router/index.ts`)**: `/` renders `HomeView`, `/posts/:slug` renders `ArticleDetailView`, and `scrollBehavior` restores `savedPosition` on back/forward navigation.
- **Pinia setup stores (`src/stores/`)**:
  - `articles.ts` — async `loadArticles()` / `loadArticleBySlug(slug)` with `listStatus` (`'idle' | 'loading' | 'ready' | 'error'`) and `detailStatus` (`'idle' | 'loading' | 'ready' | 'not-found' | 'error'`).
  - `audio.ts` — single `<audio>` playback controller bound by `VinylPlayer` and shared with `HomeView`.
  - `player.ts` — `form` (`'mini'` default, `'expanded'` on demand) and `stageDockRect` measured from `HomeView`.
  - `playlist.ts` — fetches `/playlist.json` via `loadPlaylist()` (retryable), supports optional `VITE_MEDIA_BASE_URL` override for offline development, tracks `status`, `index`, and `loopMode` (`'all' | 'one'`), persisted as `kagami-ui:playlist-state`.
  - `theme.ts` — `isDark` (`kagami-ui:dark`).
- **Swappable async article source**: components only talk to `useArticlesStore()`, which reads async Promise functions from `src/data/sources/index.ts`. Wiring `/api/posts` later only replaces the source implementation.

### Persistent Vinyl Player
- **Never unmounts**: `App.vue` mounts `AppHeader`, `<RouterView />`, and `VinylPlayer` side by side so playback survives form morphs and route transitions.
- **Two visual states**:
  - *Home desk (`form === 'mini'`)*: `HomeView` measures `.vinyl-stage-slot` via `ResizeObserver` (observing both the desk container and platter slot) + rAF-throttled scroll and reports coordinates to `usePlayerStore().setStageDockRect()`. The vinyl sits directly on the transparent slot with zero scroll lag and scrolls cleanly beneath the fixed `AppHeader`; on inner routes the visual vinyl hides immediately while audio continues playing.
  - *Immersive stage (`form === 'expanded'`)*: on the home page only, double-clicking blank page space (or clicking "沉浸" on the desk) opens the full-screen stage with `html.player-expanded { overflow: hidden }`, transport controls, and veils the underlying view; double-clicking anywhere on the stage returns to the desk.
- **Global selection rule**: because double-click is a global gesture, `body` sets `user-select: none`; opt reading content (such as `ArticleDetailView`'s `.detail-article` and track titles) back in with `.selectable`, and add `@dblclick.stop` on interactive panels.

## Pending Work

Additional navigation pages (archives, tags), search, comments UI, and live backend API integration are not wired yet.
