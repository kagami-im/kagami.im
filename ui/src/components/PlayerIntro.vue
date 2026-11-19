<script setup lang="ts">
import { onMounted, ref } from 'vue'
import coverSrc from '../assets/cover.jpg'
import musicSrc from '../assets/music.mp3'

const emit = defineEmits<{ enter: [] }>()

const audioElRef = ref<HTMLAudioElement | null>(null)
const isPlaying = ref(false)
const isEntering = ref(false)

onMounted(async () => {
  const audio = audioElRef.value
  if (!audio) {
    return
  }
  try {
    await audio.play()
  } catch {
    // Autoplay may be blocked by the browser; the disc stays still until the album is clicked.
  }
})

const togglePlay = async () => {
  const audio = audioElRef.value
  if (!audio) {
    return
  }
  if (audio.paused) {
    try {
      await audio.play()
    } catch (e) {
      console.error('Playback failed', e)
    }
  } else {
    audio.pause()
  }
}

const enter = () => {
  if (isEntering.value) {
    return
  }
  isEntering.value = true
  emit('enter')
}
</script>

<template>
  <div class="player-intro" @dblclick="enter">
    <div class="stage">
      <div
        class="album"
        :class="{ playing: isPlaying }"
        @click.stop="togglePlay"
        @dblclick.stop
      >
        <img :src="coverSrc" class="album-cover" draggable="false" alt="cover" />
      </div>
    </div>
    <audio
      ref="audioElRef"
      :src="musicSrc"
      loop
      preload="auto"
      @play="isPlaying = true"
      @pause="isPlaying = false"
    ></audio>
  </div>
</template>

<style scoped>
.player-intro {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--color-intro-bg);
}

.stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-8);
}

.album {
  width: clamp(220px, 39vw, 420px);
  height: clamp(220px, 39vw, 420px);
  border-radius: 50%;
  cursor: pointer;
  animation: spin 20s linear infinite;
  animation-play-state: paused;
  will-change: transform;
}

.album.playing {
  animation-play-state: running;
}

.album-cover {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--color-cover-edge);
  box-shadow: var(--shadow-cover);
  transition:
    transform 0.35s cubic-bezier(0.22, 0.61, 0.36, 1),
    box-shadow 0.35s ease;
}

.album:hover .album-cover {
  transform: scale(1.06);
  box-shadow: var(--shadow-cover-hover);
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
