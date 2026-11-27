<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { usePlayerStore } from '../stores/player'
import { usePlaylistStore } from '../stores/playlist'
import { useAudioStore } from '../stores/audio'

const playerStore = usePlayerStore()
const { form, stageDockRect } = storeToRefs(playerStore)
const playlistStore = usePlaylistStore()
const { loopMode, currentTrack } = storeToRefs(playlistStore)
const audioStore = useAudioStore()
const { isPlaying } = storeToRefs(audioStore)

const audioElRef = ref<HTMLAudioElement | null>(null)

watch(audioElRef, (el) => {
  audioStore.bindAudioElement(el)
})

const coverSrc = computed(() => currentTrack.value?.cover ?? '')
const isStageVisible = computed(() => form.value === 'mini' && stageDockRect.value !== null)
const isVinylVisible = computed(() => form.value === 'expanded' || isStageVisible.value)

const playerStyle = computed(() => {
  const rect = stageDockRect.value
  if (!rect) {
    return undefined
  }
  return {
    '--dock-top': `${rect.top}px`,
    '--dock-left': `${rect.left}px`,
    '--dock-size': `${rect.width}px`,
  }
})

// Enable CSS morph transitions only when switching between the home desk slot
// and the full-screen immersive stage, so scrolling tracks the slot with zero lag.
const isMorphing = ref(false)
let morphTimer: number | null = null

const stopMorph = () => {
  if (morphTimer !== null) {
    window.clearTimeout(morphTimer)
    morphTimer = null
  }
  isMorphing.value = false
}

const handleWindowScroll = () => {
  if (form.value === 'mini' && isMorphing.value) {
    stopMorph()
  }
}

watch(form, () => {
  stopMorph()
  isMorphing.value = true
  morphTimer = window.setTimeout(() => {
    morphTimer = null
    isMorphing.value = false
  }, 680)
})

const handleAlbumClick = () => {
  void audioStore.togglePlay()
}

const handleAlbumDblclick = () => {
  if (form.value === 'mini') {
    playerStore.expandPlayer()
  }
}

const handleAlbumKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    void audioStore.togglePlay()
  }
}

onMounted(() => {
  audioStore.registerMediaSessionHandlers()
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleWindowScroll)
  audioStore.unregisterMediaSessionHandlers()
  audioStore.bindAudioElement(null)
  stopMorph()
})
</script>

<template>
  <!--
    Always mounted so audio playback survives route transitions.
    Visually renders in two states only: docked on the home listening desk,
    or expanded into the full-screen immersive vinyl stage.
  -->
  <div
    class="vinyl-player"
    :class="{
      'is-expanded': form === 'expanded',
      'is-stage': isStageVisible,
      'is-hidden': !isVinylVisible,
      'is-morphing': isMorphing,
    }"
    :style="playerStyle"
    @dblclick="playerStore.collapsePlayer()"
  >
    <div class="backdrop" aria-hidden="true"></div>

    <div
      v-show="isVinylVisible"
      class="album-anchor"
      role="button"
      tabindex="0"
      :aria-label="isPlaying ? '暂停播放' : '播放音乐'"
      @keydown="handleAlbumKeydown"
    >
      <div
        class="album-scale"
        @click.stop="handleAlbumClick"
        @dblclick.stop="handleAlbumDblclick"
      >
        <div class="album" :class="{ playing: isPlaying }">
          <img v-if="coverSrc" :src="coverSrc" class="album-cover" draggable="false" alt="cover" />
          <div v-else class="album-placeholder" aria-hidden="true"></div>
          <span class="album-spindle" aria-hidden="true"></span>
        </div>
      </div>
    </div>

    <Transition name="caption-fade">
      <div v-if="form === 'expanded' && currentTrack" class="expanded-caption" @dblclick.stop>
        <Transition name="caption-fade" mode="out-in">
          <div :key="currentTrack.src" class="caption-inner">
            <p class="caption-title">{{ currentTrack.title }}</p>
            <p v-if="currentTrack.artist" class="caption-artist">{{ currentTrack.artist }}</p>
          </div>
        </Transition>

        <div class="caption-controls">
          <button type="button" class="stage-ctrl" aria-label="上一首" @click="audioStore.playPrev()">
            <svg viewBox="0 0 20 20" width="15" height="15" fill="currentColor">
              <path d="M5 4.5h1.6v11H5v-11Zm2.6 5.5 7.4-5v10l-7.4-5Z" />
            </svg>
          </button>
          <button
            type="button"
            class="stage-ctrl is-primary"
            :aria-label="isPlaying ? '暂停' : '播放'"
            @click="audioStore.togglePlay()"
          >
            <svg v-if="isPlaying" viewBox="0 0 20 20" width="15" height="15" fill="currentColor">
              <rect x="5.5" y="4.5" width="3.2" height="11" rx="1" />
              <rect x="11.3" y="4.5" width="3.2" height="11" rx="1" />
            </svg>
            <svg v-else viewBox="0 0 20 20" width="15" height="15" fill="currentColor">
              <path d="M6.4 4.3 15.6 10l-9.2 5.7V4.3Z" />
            </svg>
          </button>
          <button type="button" class="stage-ctrl" aria-label="下一首" @click="audioStore.playNext()">
            <svg viewBox="0 0 20 20" width="15" height="15" fill="currentColor">
              <path d="M13.4 4.5H15v11h-1.6v-11ZM5 5l7.4 5-7.4 5V5Z" />
            </svg>
          </button>
        </div>
      </div>
    </Transition>

    <audio
      v-if="currentTrack"
      ref="audioElRef"
      :src="currentTrack.src"
      :loop="loopMode === 'one'"
      preload="metadata"
      @play="audioStore.onAudioPlay()"
      @pause="audioStore.onAudioPause()"
      @timeupdate="audioStore.handleTimeUpdate()"
      @loadedmetadata="audioStore.handleLoadedMetadata()"
      @ended="audioStore.handleEnded()"
      @error="audioStore.handleAudioError()"
    ></audio>
  </div>
</template>

<style scoped>
.vinyl-player {
  --album-size: clamp(220px, 34vw, 360px);
  --dock-size: 232px;
  --dock-top: 140px;
  --dock-left: calc(50% - var(--dock-size) / 2);

  position: fixed;
  inset: 0;
  /* Sit beneath the fixed AppHeader (z-index: 20) while docked on the home desk
     so the record scrolls cleanly underneath the frosted header; rise above it
     when expanded into the full-screen immersive stage. */
  z-index: 15;
  pointer-events: none;
}

.vinyl-player.is-expanded {
  z-index: 40;
  pointer-events: auto;
}

.backdrop {
  position: absolute;
  inset: 0;
  background-color: var(--color-intro-bg);
  background-image: var(--bg-ambient);
  opacity: 0;
  transition: opacity var(--duration-slow) ease;
}

.is-expanded .backdrop {
  opacity: 1;
}

.album-anchor {
  position: absolute;
  top: var(--dock-top);
  left: var(--dock-left);
  width: var(--dock-size);
  height: var(--dock-size);
  pointer-events: auto;
}

.is-morphing .album-anchor {
  transition:
    top var(--duration-slow) var(--ease-out),
    left var(--duration-slow) var(--ease-out),
    width var(--duration-slow) var(--ease-out),
    height var(--duration-slow) var(--ease-out);
}

.is-expanded .album-anchor {
  top: calc(44% - var(--album-size) / 2);
  left: calc(50% - var(--album-size) / 2);
  width: var(--album-size);
  height: var(--album-size);
}

.album-anchor:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 4px;
  border-radius: 50%;
}

/* Scale the entire disc (cover, rim sheen, spindle, shadow) together on hover
   so the cover never overflows its circular border. */
.album-scale {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: var(--shadow-cover);
  transition:
    transform 0.35s var(--ease-out),
    box-shadow 0.35s ease;
}

.album-scale:hover {
  transform: scale(1.03);
  box-shadow: var(--shadow-cover-hover);
}

.album {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  animation: spin 24s linear infinite;
  animation-play-state: paused;
  will-change: transform;
}

.album.playing {
  animation-play-state: running;
}

/* Soft diffuse sheen & delicate inner rim so the cover blends organically into the platter */
.album::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: radial-gradient(
    circle at 32% 24%,
    var(--color-vinyl-sheen) 0%,
    transparent 52%
  );
  box-shadow:
    inset 0 0 0 1px var(--color-cover-edge),
    inset 0 0 14px var(--color-vinyl-rim);
  pointer-events: none;
  transition: box-shadow var(--duration-base) ease;
}

.album-cover,
.album-placeholder {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  background: var(--color-surface-muted);
  transition: background-color var(--duration-base) ease;
}

/* Subtle translucent glass-hub spindle instead of a stark opaque cutout */
.album-spindle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 7.5%;
  height: 7.5%;
  min-width: 14px;
  min-height: 14px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: var(--color-spindle-ring);
  backdrop-filter: blur(4px);
  border: 1px solid var(--color-cover-edge);
  box-shadow: 0 0 8px var(--color-vinyl-rim);
  pointer-events: none;
  transition:
    background-color var(--duration-base) ease,
    border-color var(--duration-base) ease;
}

.album-spindle::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 36%;
  height: 36%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: var(--color-spindle-core);
}

/* Expanded immersive stage caption & transport */
.expanded-caption {
  position: absolute;
  left: 50%;
  top: calc(44% + var(--album-size) / 2 + var(--space-8));
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  text-align: center;
  max-width: min(86vw, 480px);
  pointer-events: auto;
}

.caption-title {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: 0.02em;
}

.caption-artist {
  margin-top: var(--space-1);
  color: var(--color-text-muted);
  font-size: 13px;
}

.caption-controls {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3);
}

.stage-ctrl {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  background: var(--color-surface-glass);
  color: var(--color-text-muted);
  cursor: pointer;
  transition:
    background-color var(--duration-fast) ease,
    border-color var(--duration-fast) ease,
    color var(--duration-fast) ease,
    transform var(--duration-fast) var(--ease-out);
}

.stage-ctrl:hover {
  background: var(--color-surface-muted);
  border-color: var(--color-border-strong);
  color: var(--color-text);
}

.stage-ctrl.is-primary {
  width: 40px;
  height: 40px;
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-on-accent);
}

.stage-ctrl.is-primary:hover {
  opacity: 0.92;
  transform: scale(1.04);
}

.stage-ctrl:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.caption-fade-enter-active,
.caption-fade-leave-active {
  transition:
    opacity var(--duration-base) var(--ease-out),
    transform var(--duration-base) var(--ease-out);
}

.caption-fade-enter-from,
.caption-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@media (prefers-reduced-motion: reduce) {
  .backdrop,
  .is-morphing .album-anchor,
  .album-scale {
    transition-duration: 0.01ms;
  }

  .album {
    animation: none;
  }
}
</style>
