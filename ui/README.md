# Kagami Public Site (`ui`)

Personal site built with Vue 3 + TypeScript + Vite (`<script setup>` SFCs, Vue Router, Pinia, `markdown-it`, zero component libraries). Articles currently load from local Markdown files in `src/content/posts`.

## Commands

```bash
npm install
npm run dev        # dev server at http://localhost:5173
npm run build      # vue-tsc -b type check + Vite production build
npm run preview    # preview production build
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
├── composables/
│   └── useAudioPlayer.ts    # <audio> engine, drag scrubbing, keyboard seek, and Media Session API
├── data/
│   ├── types.ts             # Article, ArticleBody, Frontmatter, ParsedMarkdown
│   ├── frontmatter.ts       # minimal `---` frontmatter parser
│   ├── markdown.ts          # markdown-it renderer (html: false, linkify: true, breaks: true)
│   └── sources/
│       ├── index.ts         # swappable async data-source facade (pinned first, then publishedAt desc)
│       └── local.ts         # eager import.meta.glob(?raw) loader for src/content/posts/*.md
├── stores/
│   ├── articles.ts          # async article list + detail-view loader with loading/error/not-found states
│   ├── player.ts            # player form ('expanded' | 'mini'), dockRect from AppHeader, toggles html.player-expanded
│   ├── playlist.ts          # track queue from /playlist.json (with retry), current index & loop mode
│   └── theme.ts             # isDark flag, toggles html.dark
├── styles/tokens.css        # single-hue OKLCH palette (light & dark), header frosted bg, radius, shadow, spacing, breakpoints
├── style.css                # reset, base typography, .selectable opt-in, root scroll lock, reduced-motion base
├── views/
│   ├── HomeView.vue         # full-bleed HeroBanner + 1200px shell (fluid main + 320px sidebar)
│   └── ArticleDetailView.vue # route-driven Markdown article reader (.selectable, full typography, veiled reveal)
└── components/
    ├── VinylPlayer.vue      # persistent vinyl morph shell: full-screen stage <-> measured header orb slot
    ├── MiniPlayerBar.vue    # hover/tap floating control bar with responsive layout and drag scrubber
    ├── PlaylistPopover.vue  # track list popover with loop-mode toggle and failed-fetch retry
    ├── AppHeader.vue        # fixed nav (transparent over banner, frosted on scroll/detail) + measured dock slot
    ├── HeroBanner.vue       # full-bleed cover artwork with two-layer SVG wave into --color-bg
    ├── WelcomePanel.vue     # welcome card
    ├── ArticleList.vue      # renders articles from useArticlesStore() as RouterLinks with cover thumbnails
    ├── ProfileCard.vue      # author card with theme-swapping avatar
    └── DailyNote.vue        # daily note card

public/
├── playlist.json            # 8-track queue pointing to Cloudflare R2 (r2.kagami.im)
├── heroBanner.webp          # 1672x941 WebP banner artwork
├── favicon.svg
└── audio/                   # (gitignored) optional local media for dev
```

## Architecture & Conventions

### Design Tokens & Layout
- **Single-hue OKLCH (`src/styles/tokens.css`)**: `--hue: 286` drives the whole palette (`--hue-pink: +60`, `--hue-blue: -55`, `--hue-bg: +48` sampled from the banner). `:root` and `:root.dark` share identical semantic token names, so components never branch on theme mode.
- **Strict token rule**: no hex colors or `color-mix()` in components — tune colors only in `tokens.css`.
- **Frosted scrolled header**: `AppHeader` stays transparent at the top of `HomeView` and elevates to `--color-header-bg` with `backdrop-filter: blur(12px)` once scrolled or on inner routes.
- **Intro stage color**: light mode uses pure white `--color-intro-bg: oklch(1 0 0)` for a product-shot look; dark mode aliases `--color-bg` for a seamless fade.
- **Theme persistence**: `useThemeStore` persists `kagami-ui:dark` and toggles `html.dark`; `main.ts` applies it before mount (falling back to `prefers-color-scheme`) to prevent FOUC.
- **Responsive grid**: `HeroBanner` spans `width: 100%` directly inside `HomeView` (no `100vw` breakout), followed by a centered `1200px` shell (`<main>` + `320px` `<aside>` above `1024px`, stacked below).
- **Reduced motion**: views and `VinylPlayer` respect `@media (prefers-reduced-motion: reduce)` by disabling blur/translate reveals and vinyl spin.

### State, Routing & Data Layer
- **Vue Router (`src/router/index.ts`)**: `/` renders `HomeView`, `/posts/:slug` renders `ArticleDetailView`, and `scrollBehavior` restores `savedPosition` on back/forward navigation.
- **Pinia setup stores (`src/stores/`)**:
  - `articles.ts` — async `loadArticles()` / `loadArticleBySlug(slug)` with `listStatus` (`'idle' | 'loading' | 'ready' | 'error'`) and `detailStatus` (`'idle' | 'loading' | 'ready' | 'not-found' | 'error'`).
  - `player.ts` — `form` (`'expanded' | 'mini'`, persisted as `kagami-ui:player-form`, default `'expanded'`), `dockRect` measured from `AppHeader`, toggles `html.player-expanded`.
  - `playlist.ts` — fetches `/playlist.json` via `loadPlaylist()` (retryable), tracks `status` (`'loading' | 'ready' | 'failed'`), `index`, and `loopMode` (`'all' | 'one'`), persisted as `kagami-ui:playlist-state`.
  - `theme.ts` — `isDark` (`kagami-ui:dark`).
- **Swappable async article source**: components only talk to `useArticlesStore()`, which reads async Promise functions from `src/data/sources/index.ts`. Wiring `/api/posts` later only replaces the source implementation.

### Persistent Vinyl Player
- **Never unmounts**: `App.vue` mounts `AppHeader`, `<RouterView />`, and `VinylPlayer` side by side so playback survives form and route transitions.
- **Dynamic header docking**: `AppHeader` renders a 32×32 `.player-dock-slot` placeholder and reports its `getBoundingClientRect()` via `ResizeObserver` to `usePlayerStore().dockRect`; `VinylPlayer` binds `--orb-top`, `--orb-right`, and `--orb-size` dynamically (with CSS fallbacks).
- **Expanded vs. Mini**:
  - *Expanded*: fixed full-screen stage; `html.player-expanded { overflow: hidden; background: var(--color-intro-bg) }` locks scrolling seamlessly with `scrollbar-gutter: stable` on `<html>`. Double-click outside the record collapses it and lifts the active page from its `.veiled` blur/translate state.
  - *Mini*: click-through root (`pointer-events: none`), only the docked orb, `MiniPlayerBar`, and `PlaylistPopover` opt back in. Desktop single-click toggles play/pause (250ms debounce) and double-click re-expands; touch devices tap the orb to toggle `MiniPlayerBar` and tap outside to dismiss. Double-clicking blank page space is handled centrally in `App.vue` (ignoring interactive elements, content cards, and `.selectable` regions).
- **Pure CSS morph**: `.album-anchor` interpolates `top` / `left` / `width` / `height` using `calc()` lengths (never `auto`, never `100vw`); spin keyframes live on the inner `.album`. Hover detection is suppressed mid-flight and re-checked via `matches(':hover')` after landing.
- **Global selection rule**: because double-click is a global gesture, `body` sets `user-select: none`; opt reading content (such as `ArticleDetailView`'s `.detail-card`) back in with `.selectable`.
- **Media & Controls**: `useAudioPlayer.ts` drives `<audio preload="metadata">` streaming on demand from Cloudflare R2 (`r2.kagami.im`); supports commit-on-release drag scrubbing, keyboard access, Media Session API, and surfaces playback errors in `MiniPlayerBar`.

## Pending Work

Additional navigation pages (archives, tags), search, and live backend API integration are not wired yet.
