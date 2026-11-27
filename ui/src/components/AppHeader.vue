<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '../stores/theme'

const pendingNavItems = ['文章', '归档', '标签']

const route = useRoute()
const themeStore = useThemeStore()
const { isDark } = storeToRefs(themeStore)

const isScrolled = ref(false)
const isElevated = computed(() => route.name !== 'home' || isScrolled.value)

const handleScroll = () => {
  isScrolled.value = window.scrollY > 16
}

onMounted(() => {
  handleScroll()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <header class="app-header" :class="{ 'is-elevated': isElevated }" @dblclick.stop>
    <div class="header-inner">
      <RouterLink to="/" class="brand">
        <img src="/favicon.svg" class="brand-mark" alt="" aria-hidden="true" />
        <span class="brand-name">kagami</span>
      </RouterLink>

      <nav class="site-nav" aria-label="主导航">
        <RouterLink to="/" class="nav-link" exact-active-class="is-active">首页</RouterLink>
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
        <button
          type="button"
          class="icon-button is-disabled"
          aria-label="搜索（即将上线）"
          title="搜索即将上线"
          disabled
        >
          <svg viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.6">
            <circle cx="9" cy="9" r="5.5" />
            <path d="M13.2 13.2L16.5 16.5" stroke-linecap="round" />
          </svg>
        </button>
        <button
          type="button"
          class="icon-button"
          :aria-label="isDark ? '切换为日间' : '切换为暮色'"
          :title="isDark ? '切换为日间' : '切换为暮色'"
          @click="themeStore.toggleTheme()"
        >
          <svg
            v-if="isDark"
            viewBox="0 0 20 20"
            width="16"
            height="16"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
          >
            <path
              d="M15.4 11.9A6 6 0 0 1 8.1 4.6a6.2 6.2 0 1 0 7.3 7.3Z"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <svg
            v-else
            viewBox="0 0 20 20"
            width="16"
            height="16"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
          >
            <circle cx="10" cy="10" r="3.4" />
            <path
              d="M10 2.8v1.6M10 15.6v1.6M2.8 10h1.6M15.6 10h1.6M4.9 4.9l1.1 1.1M14 14l1.1 1.1M15.1 4.9 14 6M6 14l-1.1 1.1"
              stroke-linecap="round"
            />
          </svg>
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 20;
  background: transparent;
  border-bottom: 1px solid transparent;
  transition:
    background-color var(--duration-base) ease,
    border-color var(--duration-base) ease,
    backdrop-filter var(--duration-base) ease;
}

.app-header.is-elevated {
  background: var(--color-header-bg);
  border-bottom-color: var(--color-border);
  backdrop-filter: blur(14px);
}

.header-inner {
  width: 100%;
  max-width: var(--layout-max-width);
  margin-inline: auto;
  display: flex;
  align-items: center;
  gap: var(--space-4);
  min-height: 58px;
  padding: var(--space-2) var(--space-6);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
  color: var(--color-text);
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.brand-mark {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  opacity: 0.9;
}

.site-nav {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: var(--space-6);
}

.nav-link {
  position: relative;
  padding-block: var(--space-1);
  color: var(--color-text-muted);
  font-size: 13.5px;
  letter-spacing: 0.03em;
  transition: color var(--duration-fast) ease;
}

.nav-link:hover,
.nav-link:focus-visible,
.nav-link.is-active {
  color: var(--color-text);
}

.nav-link.is-active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -2px;
  height: 1.5px;
  background: var(--color-primary);
  border-radius: var(--radius-pill);
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

.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  transition:
    background-color var(--duration-fast) ease,
    border-color var(--duration-fast) ease,
    color var(--duration-fast) ease;
}

.icon-button:hover:not(:disabled) {
  background: var(--color-surface-muted);
  border-color: var(--color-border);
  color: var(--color-text);
}

.icon-button.is-disabled {
  color: var(--color-text-faint);
  cursor: default;
  opacity: 0.65;
}

.icon-button:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
}

@media (max-width: 767px) {
  .header-inner {
    flex-wrap: wrap;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-4);
  }

  .site-nav {
    order: 3;
    flex-basis: 100%;
    justify-content: flex-start;
    gap: var(--space-4);
    padding-bottom: var(--space-1);
  }
}
</style>
