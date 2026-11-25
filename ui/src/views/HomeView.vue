<script setup lang="ts">
import { onMounted } from 'vue'
import HeroBanner from '../components/HeroBanner.vue'
import WelcomePanel from '../components/WelcomePanel.vue'
import ArticleList from '../components/ArticleList.vue'
import ProfileCard from '../components/ProfileCard.vue'
import DailyNote from '../components/DailyNote.vue'
import { useArticlesStore } from '../stores/articles'
import { usePlayerStore } from '../stores/player'

const playerStore = usePlayerStore()
const articlesStore = useArticlesStore()

onMounted(() => {
  void articlesStore.loadArticles()
})
</script>

<template>
  <div class="home-page" :class="{ veiled: playerStore.form === 'expanded' }">
    <HeroBanner />

    <div class="home-shell">
      <WelcomePanel />

      <div class="home-columns">
        <main class="home-main">
          <ArticleList />
        </main>

        <aside class="home-sidebar" aria-label="侧栏">
          <ProfileCard />
          <DailyNote />
        </aside>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  position: relative;
  min-height: 100vh;
  padding-block: 0 var(--space-8);
  overflow-x: clip;
  /* While the player covers the page the content "sinks underwater": dimmed,
     slightly lowered and blurred. Lifting the veil raises and sharpens it in
     one motion, so the reveal reads as surfacing, not an unveiled curtain. */
  transition:
    opacity 0.8s ease 0.1s,
    transform 0.9s cubic-bezier(0.22, 0.61, 0.36, 1) 0.1s,
    filter 0.9s cubic-bezier(0.22, 0.61, 0.36, 1) 0.1s;
}

.home-page.veiled {
  opacity: 0;
  transform: translateY(16px);
  filter: blur(6px);
}

/* Content blocks surface with a slight stagger; the banner stays put as the
   water surface. */
.home-shell > * {
  transition:
    opacity 0.75s ease 0.25s,
    transform 0.8s cubic-bezier(0.22, 0.61, 0.36, 1) 0.25s,
    filter 0.8s ease 0.25s;
}

.home-shell > *:nth-child(2) {
  transition-delay: 0.35s;
}

.home-page.veiled .home-shell > * {
  opacity: 0;
  transform: translateY(18px);
  filter: blur(4px);
}

.home-shell {
  width: 100%;
  max-width: var(--layout-max-width);
  margin-inline: auto;
  margin-top: var(--space-3);
  padding-inline: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.home-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--layout-sidebar-width);
  gap: var(--space-4);
  align-items: start;
}

.home-main {
  min-width: 0;
}

.home-sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
}

@media (max-width: 1023px) {
  .home-columns {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .home-page {
    padding-block: 0 var(--space-6);
  }

  .home-shell {
    padding-inline: var(--space-3);
    gap: var(--space-3);
  }

  .home-sidebar {
    gap: var(--space-3);
  }
}

@media (prefers-reduced-motion: reduce) {
  .home-page,
  .home-shell > * {
    transition: opacity 0.2s ease;
  }

  .home-page.veiled,
  .home-page.veiled .home-shell > * {
    transform: none;
    filter: none;
  }
}
</style>
