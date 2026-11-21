<script setup lang="ts">
import HomeView from './views/HomeView.vue'
import VinylPlayer from './components/VinylPlayer.vue'
</script>

<template>
  <!--
    Both stages are always mounted. The player covers the page in expanded
    form and docks into the header in mini form; HomeView reveals itself as
    the backdrop fades. No Transition wrapper: the morph is CSS-driven inside
    VinylPlayer so the record never unmounts mid-flight.
  -->
  <HomeView />
  <VinylPlayer />
</template>
