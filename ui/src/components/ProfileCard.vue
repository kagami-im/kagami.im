<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useThemeStore } from '../stores/theme'
import avatarDark from '../assets/avatar-dark.png'
import avatarLight from '../assets/avatar-light.png'

const themeStore = useThemeStore()
const { isDark } = storeToRefs(themeStore)
</script>

<template>
  <section class="profile-card">
    <div class="profile-head">
      <span class="profile-avatar" aria-hidden="true">
        <img :src="isDark ? avatarDark : avatarLight" alt="" draggable="false" />
      </span>
      <div class="profile-identity">
        <h2 class="profile-name">Kagami</h2>
        <p class="profile-bio">......</p>
      </div>
    </div>

    <div class="profile-links">
      <span class="profile-link"><a href="mailto:admin@kagami.im">Email</a> </span>
    </div>
  </section>
</template>

<style scoped>
.profile-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.profile-head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.profile-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--color-avatar);
  overflow: hidden;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.profile-identity {
  min-width: 0;
}

.profile-name {
  font-size: 17px;
  font-weight: 600;
}

.profile-bio {
  margin-top: var(--space-1);
  color: var(--color-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.profile-links {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.profile-link {
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--color-nav-surface);
  color: var(--color-text-muted);
  font-size: 11px;
}
</style>
