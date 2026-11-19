# Vue 3 + TypeScript + Vite

This template should help get you started developing with Vue 3 and TypeScript in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

## Development

```bash
npm install
npm run dev        # dev server at http://localhost:5173
npm run build      # vue-tsc type check + production build
npm run preview    # preview the production build
```

## Layout

```
src/
├── main.ts              # app entry; registers Pinia, imports tokens, base styles and App
├── App.vue              # swaps PlayerIntro for HomeView based on the view store
├── assets/              # bundled album cover and background music for the intro
├── style.css            # global reset and base typography
├── stores/
│   ├── view.ts          # Pinia store: showIntro flag, persisted to localStorage
│   └── theme.ts         # Pinia store: isDark flag, applies html.dark, persisted to localStorage
├── styles/tokens.css    # OKLCH color system (single hue + light/dark), radius, shadow, spacing, breakpoints
├── views/
│   └── HomeView.vue     # 1200px centered shell; Grid main column + 320px sidebar
└── components/
    ├── PlayerIntro.vue  # spinning record splash; dblclick outside the album fades into HomeView
    ├── AppHeader.vue    # transparent nav overlaid on the banner artwork
    ├── HeroBanner.vue   # full-width cover artwork with a two-layer SVG wave into the page
    ├── WelcomePanel.vue # welcome copy and quick entries
    ├── ArticleList.vue  # latest articles (skeleton placeholders)
    ├── ProfileCard.vue  # author card
    └── DailyNote.vue    # daily note card
```

## Design tokens

Colors use the OKLCH color space, driven by a single `--hue: 286` in `src/styles/tokens.css`. The pink and blue accents are hue offsets of the primary (`+60` / `-55`), so changing one hue rethemes the whole site with related accents. Because OKLCH lightness (`L`) is perceptually uniform, each semantic token sets an explicit `L`/`C` step rather than mixing colors ad hoc — this is what keeps module boundaries legible instead of washing out into grey. Two token blocks exist: `:root` (light) and `:root.dark`; both define the same semantic names (`--color-bg`, `--color-surface`, `--color-text`, `--color-border`, shadows, etc.), so components never branch on the mode. No hex values or `color-mix()` remain in components — add or tune colors only in `tokens.css`.

Dark mode is toggled by the theme button in `AppHeader`: `useThemeStore` flips `isDark`, applies the `dark` class to `<html>`, and persists the choice under `kagami-ui:dark`. `main.ts` re-applies the saved class before mount (falling back to `prefers-color-scheme`) to avoid a flash of the wrong theme.

The home page uses a centered `max-width: 1200px` shell. Above 1024px the content area is a Grid with a fluid main column and a 320px sidebar; below that the sidebar stacks underneath. The banner breaks out of the shell to sit flush with the page top, spanning the full viewport width and two-thirds of its height. The image uses `cover` with a centered focus: it scales proportionally, crops the excess instead of leaving gaps or stretching, and keeps the artwork's baked-in title in frame. A two-layer SVG wave (`BannerWaves`-style, paths generated from Bézier curves) fades the bottom into the page background; both layers use `--color-bg` so they stay derived from the five tokens, never hardcoded. The transparent `AppHeader` is fixed over the artwork so the nav stays pinned while not taking up a separate row. Content layout itself uses flow and Grid with no fixed coordinates.

## State management

Pinia is registered in `main.ts` and shared state lives in `src/stores/` as setup stores. `view.ts` owns the `showIntro` boolean (persisted as `kagami-ui:show-intro`) with `toggleView()`; `theme.ts` owns `isDark` (persisted as `kagami-ui:dark`) with `toggleTheme()`, applying the `dark` class to `<html>`. Components read/write stores through `useViewStore()` / `useThemeStore()` rather than passing props or touching storage directly. Add future shared state (profile, posts) as additional stores in the same folder.

## Intro splash

On a fresh visit the app shows `PlayerIntro`: a vinyl that spins only while audio is playing. Autoplay is attempted with sound; if the browser blocks it the disc stays still until the album is clicked. Clicking the album toggles play/pause, which starts/stops the spin. Double-clicking anywhere outside the album (but not on the album itself) emits `enter`, and `<Transition mode="out-in">` in `App.vue` fades the splash out before fading `HomeView` in. The transition is reversible: double-clicking blank space in `HomeView` (its layout containers via `.self`, not the content cards) emits `back` and fades the player splash back in. Double-clicking the `HeroBanner` also emits `back`. Because double-click-to-switch is a global gesture, `body` sets `user-select: none` as the site-wide default: browsers resolve a double-click on a textless area to the nearest selectable text node, which previously highlighted unrelated text at random. Adding a new surface is therefore safe by default; opt real reading content back in with the `.selectable` utility (`user-select: text`) only where copying matters (e.g. future article bodies). `App.vue` reads `showIntro` from the Pinia `useViewStore` and calls `toggleView()` on either transition; the store persists the boolean under `kagami-ui:show-intro` in localStorage — first visit defaults to the intro, and a refresh restores whichever view was last shown (intro or home).

Content is placeholder-only for now. Routing and the public API will be wired in when the remaining pages are implemented.
