<script setup lang="ts">
import { storeToRefs } from 'pinia'
import PlayerIntro from './components/PlayerIntro.vue'
import HomeView from './views/HomeView.vue'
import { useViewStore } from './stores/view'

const viewStore = useViewStore()
const { showIntro } = storeToRefs(viewStore)
</script>

<template>
  <Transition name="stage" mode="out-in">
    <PlayerIntro v-if="showIntro" key="intro" @enter="viewStore.toggleView()" />
    <HomeView v-else key="home" @back="viewStore.toggleView()" />
  </Transition>
</template>

<style>
.stage-enter-active,
.stage-leave-active {
  transition:
    opacity 0.6s ease,
    transform 0.6s ease;
}

.stage-enter-from {
  opacity: 0;
  transform: scale(0.98);
}

.stage-leave-to {
  opacity: 0;
  transform: scale(1.04);
}
</style>
