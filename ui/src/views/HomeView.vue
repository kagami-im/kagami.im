<script setup lang="ts">
import { nextTick, watch } from 'vue'
import { storeToRefs } from 'pinia'
import AppHeader from '../components/AppHeader.vue'
import HeroBanner from '../components/HeroBanner.vue'
import WelcomePanel from '../components/WelcomePanel.vue'
import ArticleList from '../components/ArticleList.vue'
import ProfileCard from '../components/ProfileCard.vue'
import DailyNote from '../components/DailyNote.vue'
import ArticleDetailView from './ArticleDetailView.vue'
import { useArticlesStore } from '../stores/articles'
import { usePlayerStore } from '../stores/player'

const playerStore = usePlayerStore()
const articlesStore = useArticlesStore()
const { selectedArticle } = storeToRefs(articlesStore)

// The home DOM is torn down while the detail view is shown, which resets scroll.
// Remember the offset on the way in and restore it after the home DOM is back.
let savedScrollY = 0
watch(selectedArticle, (article, previous) => {
  if (article && !previous) {
    savedScrollY = window.scrollY
  } else if (!article && previous) {
    nextTick(() => window.scrollTo(0, savedScrollY))
  }
})
</script>

<template>
  <AppHeader />
  <ArticleDetailView v-if="selectedArticle" @back="articlesStore.closeArticle()" />
  <div
    v-else
    class="home-page"
    :class="{ veiled: playerStore.form === 'expanded' }"
    @dblclick.self="playerStore.expandPlayer()"
  >
    <div class="home-shell" @dblclick.self="playerStore.expandPlayer()">
      <main class="home-main" @dblclick.self="playerStore.expandPlayer()">
        <HeroBanner />
        <WelcomePanel />

        <div class="home-columns" @dblclick.self="playerStore.expandPlayer()">
          <ArticleList />

          <aside class="home-sidebar" aria-label="侧栏" @dblclick.self="playerStore.expandPlayer()">
            <ProfileCard />
            <DailyNote />
          </aside>
        </div>
      </main>
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
   water surface. Scoped to direct children of .home-main (child component
   roots carry this component's scope attribute) so inner hover effects are
   untouched. */
.home-main > *:nth-child(n + 2) {
  transition:
    opacity 0.75s ease 0.25s,
    transform 0.8s cubic-bezier(0.22, 0.61, 0.36, 1) 0.25s,
    filter 0.8s ease 0.25s;
}

.home-main > *:nth-child(3) {
  transition-delay: 0.35s;
}

.home-page.veiled .home-main > *:nth-child(n + 2) {
  opacity: 0;
  transform: translateY(18px);
  filter: blur(4px);
}

.home-shell {
  width: 100%;
  max-width: var(--layout-max-width);
  margin-inline: auto;
  padding-inline: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.home-main {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
}

.home-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--layout-sidebar-width);
  gap: var(--space-4);
  align-items: start;
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

  .home-main,
  .home-sidebar {
    gap: var(--space-3);
  }
}
</style>
