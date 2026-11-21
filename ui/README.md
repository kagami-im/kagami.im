# Kagami Public Site (`ui`)

The public-facing personal site: home shell, articles, and the always-mounted vinyl player. Built with Vue 3 + TypeScript + Vite using `<script setup>` SFCs (see the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup)); content currently comes from local markdown files in `src/content/posts`.

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
├── App.vue              # mounts HomeView and VinylPlayer side by side; both stay alive
├── assets/              # bundled album cover and background music for the player
├── style.css            # global reset and base typography
├── stores/
│   ├── player.ts        # Pinia store: player form (expanded/mini), persisted to localStorage
│   └── theme.ts         # Pinia store: isDark flag, applies html.dark, persisted to localStorage
├── styles/tokens.css    # OKLCH color system (single hue + light/dark), radius, shadow, spacing, breakpoints
├── views/
│   └── HomeView.vue     # 1200px centered shell; Grid main column + 320px sidebar
└── components/
    ├── VinylPlayer.vue  # always-mounted vinyl: full-screen record or header orb with hover bar
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

Pinia is registered in `main.ts` and shared state lives in `src/stores/` as setup stores. `player.ts` owns the player `form` (`'expanded' | 'mini'`, persisted as `kagami-ui:player-form`, fresh visits default to expanded) with `expandPlayer()` / `collapsePlayer()`; `theme.ts` owns `isDark` (persisted as `kagami-ui:dark`) with `toggleTheme()`, applying the `dark` class to `<html>`. Components read/write stores through `usePlayerStore()` / `useThemeStore()` rather than passing props or touching storage directly. Add future shared state (profile, posts) as additional stores in the same folder.

## Vinyl player

The player is a persistent control that only changes form — it never unmounts. `App.vue` renders `HomeView` and `VinylPlayer` side by side for the whole app lifetime, so audio keeps playing while the player morphs.

On a fresh visit the player boots in its **expanded** form: a fixed full-screen layer whose opaque backdrop hides the home page, leaving only the record visible (the record spins only while audio is playing; playback starts on the first click — autoplay is intentionally left off). Page scrolling is locked while covered — the player store toggles `html.player-expanded` and `style.css` hides the scrollbar at root level — so no dead scrollbar floats over the stage; `scrollbar-gutter: stable` on `<html>` keeps the layout width constant so docking back causes no reflow, and the html background matches the backdrop while covered so the reserved gutter strip is seamless. Double-clicking anywhere outside the record **collapses** it: the backdrop fades and the home page underneath **surfaces** — `.home-page` carries a veil (driven by the player store) that sinks the content `opacity: 0 + translateY(16px) + blur(6px)` while covered and lifts it back in one motion, with the welcome panel and content columns rising on a slight stagger after the banner, which stays put as the water surface.

The morph is a pure CSS transition on `.album-anchor`: both endpoints (viewport center ↔ the header's action row) are written with the same interpolable longhands — `top` / `left` / `width` / `height` as `calc()` values, never `auto` — so the record continuously shrinks and flies to its dock instead of jumping. The spin keyframes live on the inner `.album` so the morph's position math and the rotation never fight over the same `transform`. Dock geometry mirrors `AppHeader` via `--orb-*` custom properties: a 32px orb, 12px from the top, 96px from the right (92px and 8px top on mobile, where the header uses 12px inline padding and wraps). Docking on `left: calc(100% - …)` — the containing block is the fixed root, so `100%` tracks the real layout viewport excluding the scrollbar, which `100vw` would include.

In the **mini** form the root layer is `pointer-events: none`; only the orb (and its hover bar) opt back in, so the page stays fully clickable underneath. A single click on the orb toggles playback — delayed 250ms so a pending action can be cancelled by a double-click, which re-expands. Hovering opens a control bar (play/pause, seekable progress, track title, time) anchored beside the orb; hiding is delayed so the bar survives moving the pointer onto it. Hover is ignored while the record is mid-morph — Chrome fires enter/leave as the flying element's bounds sweep across a stationary pointer — and after landing a `:hover` re-check opens the bar if the pointer genuinely sits on the dock. Clicks and double-clicks bubble from `pointer-events: auto` descendants through the click-through root, so the bar and the orb stop `dblclick` propagation to avoid accidentally re-triggering the collapse gesture. Double-clicking blank layout space on the home page (containers via `.self`, not content cards) or the `HeroBanner` still re-expands the player — the pre-dock gesture, now mapped to `expandPlayer()`. Because double-click remains a global gesture, `body` keeps `user-select: none` as the site-wide default; opt real reading content back in with the `.selectable` utility only where copying matters (e.g. future article bodies).

The form is persisted under `kagami-ui:player-form` on every change: a refresh while docked boots straight to the mini orb with no intro replay. Playback position is not persisted — audio restarts on reload, subject to browser autoplay rules.

Content is placeholder-only for now. Routing and the public API will be wired in when the remaining pages are implemented.
