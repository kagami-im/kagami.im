# Kagami Public Site (`ui`)

Personal site built with Vue 3 + TypeScript + Vite (`<script setup>` SFCs, Pinia, `markdown-it`, zero component libraries). Articles currently load from local Markdown files in `src/content/posts`.

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
├── main.ts                  # applies saved theme before mount, registers Pinia, mounts App
├── App.vue                  # keeps HomeView and VinylPlayer always mounted side by side
├── assets/                  # theme-swapping avatars (avatar-light.png, avatar-dark.png)
├── content/posts/           # local Markdown articles with YAML-style frontmatter
├── data/
│   ├── types.ts             # Article, ArticleBody, Frontmatter, ParsedMarkdown
│   ├── frontmatter.ts       # minimal `---` frontmatter parser
│   ├── markdown.ts          # markdown-it renderer (html: false, linkify: true, breaks: true)
│   └── sources/
│       ├── index.ts         # swappable data-source facade (pinned first, then publishedAt desc)
│       └── local.ts         # eager import.meta.glob(?raw) loader for src/content/posts/*.md
├── stores/
│   ├── articles.ts          # article list + detail-view selection (selectedArticle / articleBody)
│   ├── player.ts            # player form ('expanded' | 'mini'), toggles html.player-expanded
│   ├── playlist.ts          # track queue from /playlist.json, current index & loop mode
│   └── theme.ts             # isDark flag, toggles html.dark
├── styles/tokens.css        # single-hue OKLCH palette (light & dark), radius, shadow, spacing, breakpoints
├── style.css                # reset, base typography, .selectable opt-in, root scroll lock
├── views/
│   ├── HomeView.vue         # 1200px shell (fluid main + 320px sidebar); toggles ArticleDetailView & restores scrollY
│   └── ArticleDetailView.vue # Markdown article reader
└── components/
    ├── VinylPlayer.vue      # persistent vinyl: full-screen stage <-> header orb + hover bar, drag seek & playlist
    ├── AppHeader.vue        # fixed transparent nav overlaid on banner
    ├── HeroBanner.vue       # full-bleed cover artwork with two-layer SVG wave into --color-bg
    ├── WelcomePanel.vue     # welcome card & quick links
    ├── ArticleList.vue      # renders articles from useArticlesStore(); opens ArticleDetailView
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
- **Intro stage color**: light mode uses pure white `--color-intro-bg: oklch(1 0 0)` for a product-shot look; dark mode aliases `--color-bg` for a seamless fade.
- **Theme persistence**: `useThemeStore` persists `kagami-ui:dark` and toggles `html.dark`; `main.ts` applies it before mount (falling back to `prefers-color-scheme`) to prevent FOUC.
- **Responsive grid**: centered `1200px` shell; above `1024px` fluid main + `320px` sidebar, stacked below. `HeroBanner` breaks out to full viewport width and blends into `--color-bg` via a two-layer SVG wave.

### State & Data Layer
- **Pinia setup stores (`src/stores/`)**:
  - `articles.ts` — synchronous list from `src/data/sources` + `selectedArticle` / `articleBody` / `openArticle()` / `closeArticle()`. Without Vue Router yet, `HomeView.vue` swaps to `ArticleDetailView.vue` and saves/restores `window.scrollY`.
  - `player.ts` — `form` (`'expanded' | 'mini'`, persisted as `kagami-ui:player-form`, default `'expanded'`), toggles `html.player-expanded`.
  - `playlist.ts` — fetches `/playlist.json`, tracks `status` (`'loading' | 'ready' | 'failed'`), `index`, and `loopMode` (`'all' | 'one'`), persisted as `kagami-ui:playlist-state`.
  - `theme.ts` — `isDark` (`kagami-ui:dark`).
- **Swappable article source**: components only talk to `useArticlesStore()`, which reads `src/data/sources/index.ts`. Wiring `/api/posts` later only replaces the source implementation.

### Persistent Vinyl Player
- **Never unmounts**: `App.vue` mounts `HomeView` and `VinylPlayer` side by side so playback survives form transitions.
- **Expanded vs. Mini**:
  - *Expanded*: fixed full-screen stage; `html.player-expanded { overflow: hidden; background: var(--color-intro-bg) }` locks scrolling seamlessly with `scrollbar-gutter: stable` on `<html>`. Double-click outside the record collapses it and lifts `.home-page` from its `.veiled` blur/translate state.
  - *Mini*: click-through root (`pointer-events: none`), only the 32px header orb and hover bar opt back in. Single-click toggles play/pause (250ms debounce); double-click on the orb or blank page space (`.self` / `HeroBanner`) re-expands. Interactive controls stop `dblclick` propagation.
- **Pure CSS morph**: `.album-anchor` interpolates `top` / `left` / `width` / `height` using `calc()` lengths (never `auto`, never `100vw` which includes scrollbar width); spin keyframes live on the inner `.album`. Hover detection is suppressed mid-flight and re-checked via `matches(':hover')` after landing.
- **Global selection rule**: because double-click is a global gesture, `body` sets `user-select: none`; opt reading content back in with `.selectable`.
- **Media & Controls**: `<audio preload="metadata">` streams on demand from Cloudflare R2 (`r2.kagami.im`); supports drag scrubbing, keyboard access, Media Session API, and surfaces playback errors in the bar title.

## Pending Work

Header navigation links, search, multi-page routing, and live backend API integration are not wired yet.
