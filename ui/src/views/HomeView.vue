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

const emit = defineEmits<{ back: [] }>()
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
  <div v-else class="home-page" @dblclick.self="emit('back')">
    <div class="home-shell" @dblclick.self="emit('back')">
      <main class="home-main" @dblclick.self="emit('back')">
        <HeroBanner @back="emit('back')" />
        <WelcomePanel />

        <div class="home-columns" @dblclick.self="emit('back')">
          <ArticleList />

          <aside class="home-sidebar" aria-label="侧栏" @dblclick.self="emit('back')">
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
