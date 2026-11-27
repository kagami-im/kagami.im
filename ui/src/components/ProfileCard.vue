<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useThemeStore } from '../stores/theme'
import avatarDark from '../assets/avatar-dark.png'
import avatarLight from '../assets/avatar-light.png'

const themeStore = useThemeStore()
const { isDark } = storeToRefs(themeStore)
</script>

<template>
  <section class="desk-corner" aria-label="资料面板" @dblclick.stop>
    <div class="corner-identity">
      <div class="avatar-frame" aria-hidden="true">
        <img
          :src="avatarLight"
          alt=""
          class="avatar-img"
          :class="{ 'is-visible': !isDark }"
          draggable="false"
        />
        <img
          :src="avatarDark"
          alt=""
          class="avatar-img"
          :class="{ 'is-visible': isDark }"
          draggable="false"
        />
      </div>

      <div class="identity-copy">
        <h2 class="identity-name">Kagami</h2>
      </div>
    </div>

    <div class="corner-links">
      <a href="mailto:admin@kagami.im" class="contact-link">
        admin@kagami.im
      </a>
    </div>
  </section>
</template>

<style scoped>
.desk-corner {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-glass);
  backdrop-filter: blur(12px);
  transition:
    background-color var(--duration-base) ease,
    border-color var(--duration-base) ease;
}

.corner-identity {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.avatar-frame {
  position: relative;
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  border-radius: 50%;
  border: 1px solid var(--color-border-strong);
  background: var(--color-avatar);
  overflow: hidden;
  transition:
    background-color var(--duration-base) ease,
    border-color var(--duration-base) ease;
}

.avatar-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity var(--duration-slow) var(--ease-out);
}

.avatar-img.is-visible {
  opacity: 1;
}

.identity-copy {
  min-width: 0;
}

.identity-name {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.03em;
  color: var(--color-text);
  transition: color var(--duration-base) ease;
}

.identity-mode {
  margin-top: 2px;
  color: var(--color-text-faint);
  font-size: 12px;
  letter-spacing: 0.04em;
  transition: color var(--duration-base) ease;
}

.corner-links {
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
  transition: border-color var(--duration-base) ease;
}

.contact-link {
  display: inline-block;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
  font-size: 12.5px;
  transition: color var(--duration-fast) ease;
}

.contact-link:hover {
  color: var(--color-primary);
}

@media (prefers-reduced-motion: reduce) {
  .avatar-img {
    transition: none;
  }
}
</style>
