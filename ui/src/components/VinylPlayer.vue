<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import coverSrc from '../assets/cover.webp'
import { usePlayerStore } from '../stores/player'
import { usePlaylistStore } from '../stores/playlist'

const playerStore = usePlayerStore()
const { form } = storeToRefs(playerStore)
const playlistStore = usePlaylistStore()
const { tracks, index, loopMode, currentTrack } = storeToRefs(playlistStore)

const audioElRef = ref<HTMLAudioElement | null>(null)
const anchorElRef = ref<HTMLElement | null>(null)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)

const progressPercent = computed(() =>
  duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0
)

const formatTime = (seconds: number): string => {
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return '0:00'
  }
  const minutes = Math.floor(seconds / 60)
  const padded = String(Math.floor(seconds % 60)).padStart(2, '0')
  return `${minutes}:${padded}`
}

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

// Single click and double click must coexist on the docked orb: a click starts
// a short timer, and a dblclick cancels it before the action fires.
const SINGLE_CLICK_DELAY = 250
let clickTimer: number | null = null

const handleAlbumClick = () => {
  if (form.value === 'expanded') {
    void togglePlay()
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

// The hover bar must survive moving the pointer from the orb onto itself, so
// hiding is delayed and cancelled on re-enter. Hover tracking only exists in
// mini form: the expanded record is the same element, and letting it arm
// isHovered would pop the bar the instant the player docks.
const isHovered = ref(false)
let hoverTimer: number | null = null

// While the record morphs, Chrome fires enter/leave as its bounds sweep across
// a stationary pointer, which would flash the bar mid-flight. Swallow hover
// until the morph settles, then re-check with :hover — a pointer parked on the
// dock gets the bar even though no further enter event will fire for it.
const MORPH_SETTLE_MS = 800
const isMorphing = ref(false)
let morphTimer: number | null = null

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
    isHovered.value = false
  }, 200)
}

const showMiniBarView = computed(() => form.value === 'mini' && isHovered.value)

// Set when a track should start playing as soon as it is loaded (prev/next,
// ended auto-advance, playlist click). Consumed by the track-change watcher.
const pendingPlay = ref(false)

const restartCurrent = () => {
  const audio = audioElRef.value
  if (!audio) {
    return
  }
  audio.currentTime = 0
  void audio.play().catch((e) => console.error('Playback failed', e))
}

const playTrack = (i: number) => {
  if (i === index.value) {
    restartCurrent()
    return
  }
  pendingPlay.value = true
  playlistStore.setIndex(i)
}

const playPrev = () => {
  // A single-track playlist wraps onto itself: prev/next just restart it.
  if (tracks.value.length <= 1) {
    restartCurrent()
    return
  }
  pendingPlay.value = true
  playlistStore.prev()
}

const playNext = () => {
  if (tracks.value.length <= 1) {
    restartCurrent()
    return
  }
  pendingPlay.value = true
  playlistStore.next()
}

const handleEnded = () => {
  // loop="one" repeats natively, so ended only fires in list mode: advance
  // (a single-track playlist wraps onto itself and restarts).
  if (tracks.value.length <= 1) {
    restartCurrent()
    return
  }
  pendingPlay.value = true
  playlistStore.next()
}

const updateMediaSession = (track: { title: string; artist?: string }) => {
  if (!('mediaSession' in navigator)) {
    return
  }
  navigator.mediaSession.metadata = new MediaMetadata({
    title: track.title,
    artist: track.artist ?? 'Kagami',
    album: 'Kagami',
    artwork: [{ src: coverSrc, sizes: '1024x1024', type: 'image/webp' }],
  })
}

// Metadata must be set for the boot track too (prev is undefined then), while
// the src reload below only makes sense when the track actually changed.
watch(currentTrack, (track) => {
  if (track) {
    updateMediaSession(track)
  }
})

watch(currentTrack, async (track, prev) => {
  if (!track || !prev) {
    return
  }
  const audio = audioElRef.value
  if (!audio) {
    return
  }
  const shouldPlay = pendingPlay.value || isPlaying.value
  pendingPlay.value = false
  isPlaying.value = false
  currentTime.value = 0
  duration.value = 0
  await nextTick() // let the src attribute land before touching the element
  if (shouldPlay) {
    try {
      await audio.play()
    } catch (e) {
      console.error('Playback failed', e)
    }
  }
})

watch(
  isPlaying,
  (playing) => {
    if ('mediaSession' in navigator) {
      navigator.mediaSession.playbackState = playing ? 'playing' : 'paused'
    }
  },
  { immediate: true }
)

// Playlist panel: click-toggled, closed by any click outside it or the toggle.
const showPlaylist = ref(false)
const togglePlaylist = () => {
  showPlaylist.value = !showPlaylist.value
}
const onDocClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement | null
  if (!target?.closest('.playlist-panel') && !target?.closest('.bar-list')) {
    showPlaylist.value = false
  }
}
watch(showPlaylist, (visible) => {
  if (visible) {
    document.addEventListener('click', onDocClick)
  } else {
    document.removeEventListener('click', onDocClick)
  }
})

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
    }, MORPH_SETTLE_MS)
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

const handleTimeUpdate = () => {
  if (audioElRef.value) {
    currentTime.value = audioElRef.value.currentTime
  }
}

const handleLoadedMetadata = () => {
  if (audioElRef.value) {
    duration.value = audioElRef.value.duration
  }
}

const progressElRef = ref<HTMLElement | null>(null)

const seek = (event: MouseEvent) => {
  const audio = audioElRef.value
  const track = progressElRef.value
  if (!audio || !track || !Number.isFinite(duration.value) || duration.value <= 0) {
    return
  }
  const rect = track.getBoundingClientRect()
  const ratio = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width))
  audio.currentTime = ratio * duration.value
  currentTime.value = audio.currentTime
}

onMounted(() => {
  // Expose transport controls to the OS (lock screen, media keys, headphones).
  if ('mediaSession' in navigator) {
    navigator.mediaSession.setActionHandler('play', () => {
      void togglePlay()
    })
    navigator.mediaSession.setActionHandler('pause', () => {
      void togglePlay()
    })
    navigator.mediaSession.setActionHandler('previoustrack', () => playPrev())
    navigator.mediaSession.setActionHandler('nexttrack', () => playNext())
  }
})
</script>

<template>
  <!--
    The player never unmounts; it only morphs. In expanded form it covers the
    viewport as an intro stage, in mini form the root is click-through and only
    the docked orb (plus hover bar) is interactive.
  -->
  <div
    class="vinyl-player"
    :class="form === 'expanded' ? 'is-expanded' : 'is-mini'"
    @dblclick="playerStore.collapsePlayer()"
  >
    <div class="backdrop" aria-hidden="true"></div>

    <div
      ref="anchorElRef"
      class="album-anchor"
      role="button"
      aria-label="音乐播放器"
      @mouseenter="showMiniBar"
      @mouseleave="scheduleHideMiniBar"
    >
      <div
        class="album"
        :class="{ playing: isPlaying }"
        @click.stop="handleAlbumClick"
        @dblclick.stop="handleAlbumDblclick"
      >
        <img :src="coverSrc" class="album-cover" draggable="false" alt="cover" />
      </div>
    </div>

    <Transition name="bar">
      <div
        v-if="showMiniBarView"
        class="mini-bar"
        @mouseenter="showMiniBar"
        @mouseleave="scheduleHideMiniBar"
        @dblclick.stop
      >
        <button type="button" class="bar-button bar-skip" aria-label="上一首" @click="playPrev">
          ❮❮
        </button>
        <button
          type="button"
          class="bar-button"
          :aria-label="isPlaying ? '暂停' : '播放'"
          @click="togglePlay"
        >
          {{ isPlaying ? '❚❚' : '▶' }}
        </button>
        <button type="button" class="bar-button bar-skip" aria-label="下一首" @click="playNext">
          ❯❯
        </button>
        <span class="bar-title" :title="currentTrack?.title">{{ currentTrack?.title ?? '…' }}</span>
        <div class="bar-progress" @click="seek">
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: `${progressPercent}%` }"></div>
          </div>
        </div>
        <span class="bar-time">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</span>
        <button type="button" class="bar-button bar-list" aria-label="播放列表" @click="togglePlaylist">
          ☰
        </button>
      </div>
    </Transition>

    <Transition name="bar">
      <div v-if="showPlaylist && form === 'mini'" class="playlist-panel" @dblclick.stop>
        <div class="panel-head">
          <span class="panel-title">播放列表 · {{ tracks.length }}</span>
          <button
            type="button"
            class="panel-loop"
            :title="loopMode === 'all' ? '切换为单曲循环' : '切换为列表循环'"
            @click="playlistStore.toggleLoop()"
          >
            {{ loopMode === 'all' ? '列表循环' : '单曲循环' }}
          </button>
        </div>
        <ol class="panel-list">
          <li
            v-for="(track, i) in tracks"
            :key="track.src"
            :class="{ active: i === index }"
            @click="playTrack(i)"
          >
            <span class="track-mark">{{ i === index ? (isPlaying ? '❚❚' : '▶') : i + 1 }}</span>
            <span class="track-name" :title="track.title">{{ track.title }}</span>
            <span v-if="track.artist" class="track-artist">{{ track.artist }}</span>
          </li>
        </ol>
      </div>
    </Transition>

    <audio
      v-if="currentTrack"
      ref="audioElRef"
      :src="currentTrack.src"
      :loop="loopMode === 'one'"
      preload="metadata"
      @play="isPlaying = true"
      @pause="isPlaying = false"
      @timeupdate="handleTimeUpdate"
      @loadedmetadata="handleLoadedMetadata"
      @ended="handleEnded"
    ></audio>
  </div>
</template>

<style scoped>
/* Docking geometry must mirror AppHeader: padding-inline 16px, 32px icon
   buttons, 8px gaps. From the right edge: theme [16..48], search [56..88],
   orb [96..128]; buttons are centered in the 56px header row. */
.vinyl-player {
  --album-size: clamp(220px, 39vw, 420px);
  --orb-size: 32px;
  --orb-top: 12px;
  --orb-right: 96px;
  --morph-ease: cubic-bezier(0.22, 0.61, 0.36, 1);

  position: fixed;
  inset: 0;
  z-index: 40;
  /* In mini form the whole root is click-through; only the orb and bar opt back in. */
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

/* The docked orb is a control, not a scene object: it sits flat in the header
   row beside the search/theme buttons, so the wall halo is dropped entirely
   (it fades out during the flight as box-shadow interpolates to none). */
.is-mini .album-cover {
  box-shadow: none;
}

/* The docked orb is too small for 1.06 to read; give hover a visible pop. */
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

/* Hover control bar, anchored to the left of the orb at the same height. */
.mini-bar {
  position: absolute;
  top: calc(var(--orb-top) + var(--orb-size) / 2 - 20px);
  right: calc(var(--orb-right) + var(--orb-size) + var(--space-2));
  display: flex;
  align-items: center;
  gap: var(--space-2);
  height: 40px;
  padding: 0 var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  box-shadow: var(--shadow-soft);
  pointer-events: auto;
}

.bar-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text);
  font-size: 12px;
  cursor: pointer;
}

.bar-button:hover {
  background: var(--color-surface-muted);
}

.bar-title {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text);
  font-size: 13px;
}

.bar-progress {
  display: flex;
  align-items: center;
  width: 110px;
  height: 14px;
  cursor: pointer;
}

.bar-track {
  width: 100%;
  height: 4px;
  border-radius: var(--radius-pill);
  background: var(--color-surface-muted);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary);
}

.bar-time {
  color: var(--color-text-muted);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.bar-skip {
  font-size: 8px;
  letter-spacing: -1px;
  color: var(--color-text-muted);
}

.bar-list {
  font-size: 12px;
}

/* Playlist panel, below the hover bar; click-toggled, click-outside closes. */
.playlist-panel {
  position: absolute;
  top: calc(var(--orb-top) + var(--orb-size) + 10px);
  right: calc(var(--orb-right) + var(--orb-size) + var(--space-2));
  width: 280px;
  max-height: 280px;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  pointer-events: auto;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.panel-title {
  color: var(--color-text);
  font-size: 12px;
  font-weight: 600;
}

.panel-loop {
  padding: 2px 8px;
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-nav-surface);
  color: var(--color-text-muted);
  font-size: 11px;
  cursor: pointer;
}

.panel-loop:hover {
  color: var(--color-text);
}

.panel-list {
  margin: 0;
  padding: var(--space-1);
  list-style: none;
  overflow-y: auto;
}

.panel-list li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  font-size: 12px;
  cursor: pointer;
}

.panel-list li:hover {
  background: var(--color-surface-muted);
}

.panel-list li.active {
  background: var(--color-surface-muted);
  color: var(--color-primary);
  font-weight: 600;
}

.track-mark {
  width: 18px;
  flex-shrink: 0;
  font-size: 10px;
  text-align: center;
}

.track-name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.track-artist {
  flex-shrink: 0;
  color: var(--color-text-faint);
  font-size: 11px;
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
  /* The header wraps to two rows here: the actions sit higher (8px padding
     instead of the 56px-row centering) with 12px inline padding, and the bar
     can't fit beside the orb. */
  .vinyl-player {
    --orb-top: 8px;
    --orb-right: 92px;
  }

  .mini-bar {
    right: auto;
    left: var(--space-3);
    top: calc(var(--orb-top) + var(--orb-size) + var(--space-2));
  }

  .playlist-panel {
    right: auto;
    left: var(--space-3);
    width: calc(100vw - var(--space-6));
  }
}
</style>
