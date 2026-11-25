<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '../stores/theme'
import { usePlayerStore } from '../stores/player'

const pendingNavItems = ['文章', '归档', '标签']

const route = useRoute()
const themeStore = useThemeStore()
const { isDark } = storeToRefs(themeStore)
const playerStore = usePlayerStore()

const headerElRef = ref<HTMLElement | null>(null)
const dockSlotRef = ref<HTMLElement | null>(null)
const isScrolled = ref(false)

// Keep the header transparent while sitting at the top of the home banner;
// apply a frosted surface once scrolled or on inner pages so text stays readable.
const isElevated = computed(() => route.name !== 'home' || isScrolled.value)

const handleScroll = () => {
  isScrolled.value = window.scrollY > 24
}

const syncDockRect = () => {
  const slot = dockSlotRef.value
  if (!slot) {
    return
  }
  const rect = slot.getBoundingClientRect()
  if (rect.width <= 0 || rect.height <= 0) {
    return
  }
  playerStore.setDockRect({
    top: rect.top,
    left: rect.left,
    right: window.innerWidth - rect.right,
    width: rect.width,
    height: rect.height,
  })
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  handleScroll()
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('resize', syncDockRect, { passive: true })

  void nextTick(syncDockRect)
  if ('ResizeObserver' in window) {
    resizeObserver = new ResizeObserver(syncDockRect)
    if (headerElRef.value) resizeObserver.observe(headerElRef.value)
    if (dockSlotRef.value) resizeObserver.observe(dockSlotRef.value)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', syncDockRect)
  resizeObserver?.disconnect()
  resizeObserver = null
  playerStore.setDockRect(null)
})
</script>

<template>
  <header ref="headerElRef" class="app-header" :class="{ 'is-elevated': isElevated }" @dblclick.stop>
    <RouterLink to="/" class="brand">
      <img src="/favicon.svg" class="brand-mark" alt="" aria-hidden="true" />
      <span class="brand-name">Kagami</span>
    </RouterLink>

    <nav class="site-nav" aria-label="主导航">
      <RouterLink to="/" class="nav-link" active-class="is-active">首页</RouterLink>
      <span
        v-for="item in pendingNavItems"
        :key="item"
        class="nav-link is-pending"
        aria-disabled="true"
        title="即将上线"
      >
        {{ item }}
      </span>
    </nav>

    <div class="header-actions">
      <!-- Placeholder slot measured at runtime so VinylPlayer docks cleanly without hardcoded offsets. -->
      <span ref="dockSlotRef" class="player-dock-slot" aria-hidden="true"></span>
      <button type="button" class="icon-button" aria-label="搜索（即将上线）" title="搜索即将上线">
        ⌕
      </button>
      <button
        type="button"
        class="icon-button"
        :aria-label="isDark ? '切换为浅色' : '切换为深色'"
        @click="themeStore.toggleTheme()"
      >
        {{ isDark ? '🌙' : '🔆' }}
      </button>
    </div>
  </header>
</template>

<style scoped>
/* Stays pinned to the viewport while floating over the banner artwork. */
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: var(--space-4);
  min-height: 56px;
  padding: var(--space-2) var(--space-4);
  background: transparent;
  border-bottom: 1px solid transparent;
  transition:
    background 0.25s ease,
    border-color 0.25s ease,
    backdrop-filter 0.25s ease;
}

.app-header.is-elevated {
  background: var(--color-header-bg);
  border-bottom-color: var(--color-border);
  backdrop-filter: blur(12px);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
}

.brand-mark {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.site-nav {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: var(--space-4);
}

.nav-link {
  padding-block: var(--space-1);
  color: var(--color-text-muted);
  font-size: 14px;
}

.nav-link:hover,
.nav-link:focus-visible,
.nav-link.is-active {
  color: var(--color-text);
}

.nav-link.is-pending {
  color: var(--color-text-faint);
  cursor: default;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-left: auto;
}

.player-dock-slot {
  display: inline-block;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  pointer-events: none;
}

.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 18px;
  cursor: pointer;
}

.icon-button:hover {
  background: var(--color-surface);
  color: var(--color-text);
}

.icon-button:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
}

@media (max-width: 767px) {
  .app-header {
    flex-wrap: wrap;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-3);
  }

  .site-nav {
    order: 3;
    flex-basis: 100%;
    justify-content: flex-start;
    gap: var(--space-4);
  }
}
</style>
