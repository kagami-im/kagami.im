<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import VinylPlayer from './components/VinylPlayer.vue'
import { usePlayerStore } from './stores/player'

const route = useRoute()
const playerStore = usePlayerStore()

// Interactive controls, content surfaces, and selectable reading text opt out
// of the blank-space double-click gesture that opens the immersive vinyl stage.
const DBLCLICK_IGNORE_SELECTOR = [
  'a',
  'button',
  'input',
  'textarea',
  'select',
  '[role="button"]',
  '.selectable',
  '.listening-desk',
  '.article-index',
  '.desk-corner',
  '.detail-article',
].join(', ')

// Only allow double-clicking into the full-screen vinyl stage on the home page.
const handleStageDblclick = (event: MouseEvent) => {
  if (route.name !== 'home' || playerStore.form !== 'mini') {
    return
  }
  const target = event.target as HTMLElement | null
  if (!target || target.closest(DBLCLICK_IGNORE_SELECTOR)) {
    return
  }
  playerStore.expandPlayer()
}

// Collapse the immersive stage if navigation leaves the home page without
// overwriting the user's saved home preference.
watch(
  () => route.name,
  (name) => {
    if (name !== 'home' && playerStore.form === 'expanded') {
      playerStore.collapsePlayer(false)
    }
  }
)
</script>

<template>
  <!--
    AppHeader and VinylPlayer stay mounted across route transitions so audio
    playback and slot docking survive navigation without interruption.
  -->
  <AppHeader />
  <div class="app-stage" @dblclick="handleStageDblclick">
    <RouterView />
  </div>
  <VinylPlayer />
</template>

<style scoped>
.app-stage {
  min-height: 100vh;
}
</style>
