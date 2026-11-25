<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { usePlayerStore } from '../stores/player'
import { usePlaylistStore } from '../stores/playlist'
import { useAudioPlayer } from '../composables/useAudioPlayer'
import MiniPlayerBar from './MiniPlayerBar.vue'
import PlaylistPopover from './PlaylistPopover.vue'

const playerStore = usePlayerStore()
const { form, dockRect } = storeToRefs(playerStore)
const playlistStore = usePlaylistStore()
const { loopMode, currentTrack } = storeToRefs(playlistStore)

const audioElRef = ref<HTMLAudioElement | null>(null)
const anchorElRef = ref<HTMLElement | null>(null)
const miniBarRef = ref<InstanceType<typeof MiniPlayerBar> | null>(null)

const {
  isPlaying,
  currentTime,
  duration,
  scrubbing,
  displayPercent,
  playbackError,
  togglePlay,
  playTrack,
  playPrev,
  playNext,
  handleEnded,
  handleAudioError,
  onAudioPlay,
  onAudioPause,
  handleTimeUpdate,
  handleLoadedMetadata,
  startScrub,
  moveScrub,
  endScrub,
  handleProgressKeydown,
} = useAudioPlayer(audioElRef)

// The record face is the current track's cover, streamed from the R2 bucket.
const coverSrc = computed(() => currentTrack.value?.cover ?? '')

// Bind measured coordinates from AppHeader's dock slot when available,
// falling back to the CSS defaults before mount.
const dockStyle = computed(() => {
  const rect = dockRect.value
  if (!rect) {
    return undefined
  }
  return {
    '--orb-size': `${rect.width}px`,
    '--orb-top': `${rect.top}px`,
    '--orb-right': `${rect.right}px`,
  }
})

// Track whether the last pointer interaction was touch so mobile users can tap
// the mini orb to reveal/hide the control bar without needing hover.
let lastPointerType = 'mouse'
const onAlbumPointerDown = (event: PointerEvent) => {
  lastPointerType = event.pointerType
}

// Single click and double click must coexist on the docked orb: a click starts
// a short timer, and a dblclick cancels it before the action fires.
const SINGLE_CLICK_DELAY = 250
let clickTimer: number | null = null

const isHovered = ref(false)
let hoverTimer: number | null = null

// While the record morphs, Chrome fires enter/leave as its bounds sweep across
// a stationary pointer, which would flash the bar mid-flight. Swallow hover
// until the morph settles, then re-check with :hover.
const isMorphing = ref(false)
let morphTimer: number | null = null

const morphSettleMs = (): number => {
  const anchor = anchorElRef.value
  if (!anchor) {
    return 800
  }
  const durations = getComputedStyle(anchor).transitionDuration.split(',')
  const seconds = Math.max(...durations.map((d) => parseFloat(d)))
  return (Number.isFinite(seconds) ? seconds : 0.7) * 1000 + 100
}

const showMiniBar = () => {
  if (form.value !== 'mini' || isMorphing.value) {
    return
  }
  if (hoverTimer !== null) {
    window.clearTimeout(hoverTimer)
    hoverTimer = null
  }
  isHovered.value = true
}

const scheduleHideMiniBar = () => {
  if (form.value !== 'mini') {
    return
  }
  if (hoverTimer !== null) {
    window.clearTimeout(hoverTimer)
  }
  hoverTimer = window.setTimeout(() => {
    hoverTimer = null
    const anchor = anchorElRef.value
    const bar = miniBarRef.value?.el ?? null
    const keep =
      scrubbing.value ||
      anchor?.matches(':hover') === true ||
      bar?.matches(':hover') === true ||
      anchor?.contains(document.activeElement) === true ||
      bar?.contains(document.activeElement) === true
    if (!keep) {
      isHovered.value = false
    }
  }, 200)
}

const showMiniBarView = computed(() => form.value === 'mini' && isHovered.value)

const handleAlbumClick = () => {
  if (form.value === 'expanded') {
    void togglePlay()
    return
  }
  if (lastPointerType === 'touch') {
    isHovered.value = !isHovered.value
    if (!isHovered.value) {
      showPlaylist.value = false
    }
    return
  }
  if (clickTimer !== null) {
    return
  }
  clickTimer = window.setTimeout(() => {
    clickTimer = null
    void togglePlay()
  }, SINGLE_CLICK_DELAY)
}

const handleAlbumDblclick = () => {
  if (form.value !== 'mini') {
    return
  }
  if (clickTimer !== null) {
    window.clearTimeout(clickTimer)
    clickTimer = null
  }
  playerStore.expandPlayer()
}

const handleAlbumKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    void togglePlay()
  }
}

// Playlist panel & touch-opened mini bar: closed by any click outside.
const showPlaylist = ref(false)
const togglePlaylist = () => {
  showPlaylist.value = !showPlaylist.value
}

const onDocClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement | null
  if (!target) {
    return
  }
  if (showPlaylist.value && !target.closest('.playlist-panel') && !target.closest('.bar-list')) {
    showPlaylist.value = false
  }
  if (
    lastPointerType === 'touch' &&
    isHovered.value &&
    !target.closest('.album-anchor') &&
    !target.closest('.mini-bar') &&
    !target.closest('.playlist-panel')
  ) {
    isHovered.value = false
    showPlaylist.value = false
  }
}

watch(
  () => showPlaylist.value || (lastPointerType === 'touch' && isHovered.value),
  (listen) => {
    if (listen) {
      document.addEventListener('click', onDocClick)
    } else {
      document.removeEventListener('click', onDocClick)
    }
  }
)

watch(form, (value) => {
  if (morphTimer !== null) {
    window.clearTimeout(morphTimer)
    morphTimer = null
  }
  if (clickTimer !== null) {
    window.clearTimeout(clickTimer)
    clickTimer = null
  }
  if (hoverTimer !== null) {
    window.clearTimeout(hoverTimer)
    hoverTimer = null
  }
  isHovered.value = false
  showPlaylist.value = false
  if (value === 'mini') {
    isMorphing.value = true
    morphTimer = window.setTimeout(() => {
      morphTimer = null
      isMorphing.value = false
      if (anchorElRef.value?.matches(':hover')) {
        isHovered.value = true
      }
    }, morphSettleMs())
  } else {
    isMorphing.value = false
  }
})

onBeforeUnmount(() => {
  if (clickTimer !== null) {
    window.clearTimeout(clickTimer)
  }
  if (hoverTimer !== null) {
    window.clearTimeout(hoverTimer)
  }
  if (morphTimer !== null) {
    window.clearTimeout(morphTimer)
  }
  document.removeEventListener('click', onDocClick)
})
</script>

<template>
  <!--
    The player never unmounts; it only morphs. In expanded form it covers the
    viewport as an intro stage, in mini form the root is click-through and only
    the docked orb (plus control bar / playlist) is interactive.
  -->
  <div
    class="vinyl-player"
    :class="form === 'expanded' ? 'is-expanded' : 'is-mini'"
    :style="dockStyle"
    @dblclick="playerStore.collapsePlayer()"
  >
    <div class="backdrop" aria-hidden="true"></div>

    <div
      ref="anchorElRef"
      class="album-anchor"
      role="button"
      tabindex="0"
      :aria-label="isPlaying ? '暂停播放' : '播放音乐'"
      @keydown="handleAlbumKeydown"
      @focusin="showMiniBar"
      @focusout="scheduleHideMiniBar"
      @mouseenter="showMiniBar"
      @mouseleave="scheduleHideMiniBar"
    >
      <div
        class="album"
        :class="{ playing: isPlaying }"
        @pointerdown="onAlbumPointerDown"
        @click.stop="handleAlbumClick"
        @dblclick.stop="handleAlbumDblclick"
      >
        <img :src="coverSrc" class="album-cover" draggable="false" alt="cover" />
      </div>
    </div>

    <Transition name="bar">
      <MiniPlayerBar
        v-if="showMiniBarView"
        ref="miniBarRef"
        :is-playing="isPlaying"
        :playback-error="playbackError"
        :track-title="currentTrack?.title ?? ''"
        :current-time="currentTime"
        :duration="duration"
        :display-percent="displayPercent"
        @mouseenter="showMiniBar"
        @mouseleave="scheduleHideMiniBar"
        @focusout="scheduleHideMiniBar"
        @prev="playPrev"
        @toggle-play="togglePlay"
        @next="playNext"
        @toggle-playlist="togglePlaylist"
        @scrub-start="startScrub"
        @scrub-move="moveScrub"
        @scrub-end="endScrub"
        @progress-keydown="handleProgressKeydown"
      />
    </Transition>

    <Transition name="bar">
      <PlaylistPopover
        v-if="showPlaylist && form === 'mini'"
        :is-playing="isPlaying"
        @select="playTrack"
      />
    </Transition>

    <audio
      v-if="currentTrack"
      ref="audioElRef"
      :src="currentTrack.src"
      :loop="loopMode === 'one'"
      preload="metadata"
      @play="onAudioPlay"
      @pause="onAudioPause"
      @timeupdate="handleTimeUpdate"
      @loadedmetadata="handleLoadedMetadata"
      @ended="handleEnded"
      @error="handleAudioError"
    ></audio>
  </div>
</template>

<style scoped>
/* Fallback docking geometry before AppHeader reports its measured dock slot. */
.vinyl-player {
  --album-size: clamp(220px, 39vw, 420px);
  --orb-size: 32px;
  --orb-top: 12px;
  --orb-right: 96px;
  --morph-ease: cubic-bezier(0.22, 0.61, 0.36, 1);

  position: fixed;
  inset: 0;
  z-index: 40;
  /* In mini form the whole root is click-through; only the orb and popovers opt back in. */
  pointer-events: none;
}

.vinyl-player.is-expanded {
  pointer-events: auto;
}

.backdrop {
  position: absolute;
  inset: 0;
  background: var(--color-intro-bg);
  opacity: 1;
  transition: opacity 0.6s ease;
}

.is-mini .backdrop {
  opacity: 0;
}

/*
  The record morphs between two endpoints written with the same longhands
  (top / left / width / height) so every property interpolates continuously.
  left is a calc() in both states — an `auto` endpoint would jump instead of
  animate. Centering uses top/left math, not translate(), so the spin keyframes
  keep sole ownership of the album's transform.
*/
.album-anchor {
  position: absolute;
  top: calc(50% - var(--album-size) / 2);
  left: calc(50% - var(--album-size) / 2);
  width: var(--album-size);
  height: var(--album-size);
  pointer-events: auto;
  transition:
    top 0.7s var(--morph-ease),
    left 0.7s var(--morph-ease),
    width 0.7s var(--morph-ease),
    height 0.7s var(--morph-ease);
}

.album-anchor:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 4px;
}

.is-mini .album-anchor {
  top: var(--orb-top);
  left: calc(100% - var(--orb-right) - var(--orb-size));
  width: var(--orb-size);
  height: var(--orb-size);
}

.album {
  width: 100%;
  height: 100%;
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

.is-mini .album-cover {
  box-shadow: none;
}

.is-mini .album:hover .album-cover {
  transform: scale(1.2);
  box-shadow: none;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.bar-enter-active,
.bar-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.bar-enter-from,
.bar-leave-to {
  opacity: 0;
  transform: translateX(6px);
}

@media (max-width: 767px) {
  .vinyl-player {
    --orb-top: 8px;
    --orb-right: 92px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .backdrop,
  .album-anchor,
  .album-cover {
    transition-duration: 0.01ms;
  }

  .album {
    animation: none;
  }
}
</style>
