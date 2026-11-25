<script setup lang="ts">
import AppHeader from './components/AppHeader.vue'
import VinylPlayer from './components/VinylPlayer.vue'
import { usePlayerStore } from './stores/player'

const playerStore = usePlayerStore()

// Interactive controls, content cards, and selectable reading text opt out of
// the blank-space double-click gesture that expands the vinyl player.
const DBLCLICK_IGNORE_SELECTOR = [
  'a',
  'button',
  'input',
  'textarea',
  'select',
  '[role="button"]',
  '.selectable',
  '.article-list',
  '.profile-card',
  '.daily-note',
  '.welcome-panel',
  '.detail-card',
].join(', ')

const handleStageDblclick = (event: MouseEvent) => {
  if (playerStore.form !== 'mini') {
    return
  }
  const target = event.target as HTMLElement | null
  if (!target || target.closest(DBLCLICK_IGNORE_SELECTOR)) {
    return
  }
  playerStore.expandPlayer()
}
</script>

<template>
  <!--
    AppHeader and VinylPlayer stay mounted across route transitions so audio
    playback and header docking survive page navigation without interruption.
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
