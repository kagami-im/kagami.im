<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useThemeStore } from '../stores/theme'

const navItems = ['首页', '文章', '归档', '标签']

const themeStore = useThemeStore()
const { isDark } = storeToRefs(themeStore)
</script>

<template>
  <header class="app-header">
    <div class="brand">
      <img src="/favicon.svg" class="brand-mark" alt="" aria-hidden="true" />
      <span class="brand-name">Kagami</span>
    </div>

    <nav class="site-nav" aria-label="主导航">
      <a v-for="item in navItems" :key="item" href="#" class="nav-link">{{ item }}</a>
    </nav>

    <div class="header-actions">
      <button type="button" class="icon-button" aria-label="搜索">⌕</button>
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
.nav-link:focus-visible {
  color: var(--color-text);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-left: auto;
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

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--color-avatar);
  color: var(--color-text);
  font-size: 13px;
  font-weight: 600;
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
