<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import ArticleList from '../components/ArticleList.vue'
import ProfileCard from '../components/ProfileCard.vue'
import { useArticlesStore } from '../stores/articles'
import { usePlayerStore } from '../stores/player'
import { usePlaylistStore } from '../stores/playlist'
import { formatTime, useAudioStore } from '../stores/audio'

const articlesStore = useArticlesStore()
const playerStore = usePlayerStore()
const playlistStore = usePlaylistStore()
const audioStore = useAudioStore()

const { form } = storeToRefs(playerStore)
const { tracks, index, loopMode, status, currentTrack } = storeToRefs(playlistStore)
const { isPlaying, currentTime, duration, displayPercent, playbackError } = storeToRefs(audioStore)

const deskElRef = ref<HTMLElement | null>(null)
const stageSlotRef = ref<HTMLElement | null>(null)
const progressElRef = ref<HTMLElement | null>(null)

const trackNumberLabel = computed(() => {
  if (tracks.value.length === 0) {
    return '-- / --'
  }
  const cur = String(index.value + 1).padStart(2, '0')
  const total = String(tracks.value.length).padStart(2, '0')
  return `${cur} / ${total}`
})

let rafId: number | null = null
let resizeObserver: ResizeObserver | null = null

const measureStageSlot = () => {
  const slot = stageSlotRef.value
  if (!slot) {
    return
  }
  const rect = slot.getBoundingClientRect()
  if (rect.width <= 0 || rect.height <= 0) {
    return
  }
  playerStore.setStageDockRect({
    top: rect.top,
    left: rect.left,
    right: window.innerWidth - rect.right,
    width: rect.width,
    height: rect.height,
  })
}

const scheduleMeasure = () => {
  if (rafId !== null) {
    return
  }
  rafId = window.requestAnimationFrame(() => {
    rafId = null
    measureStageSlot()
  })
}

onMounted(() => {
  void articlesStore.loadArticles()
  void nextTick(measureStageSlot)
  window.addEventListener('scroll', scheduleMeasure, { passive: true })
  window.addEventListener('resize', scheduleMeasure, { passive: true })

  // Observe both the desk container and the platter slot: when async playlist
  // items expand the desk height, the vertically centered platter shifts down
  // even though its own 232x232 box size stays unchanged.
  if ('ResizeObserver' in window) {
    resizeObserver = new ResizeObserver(scheduleMeasure)
    if (deskElRef.value) resizeObserver.observe(deskElRef.value)
    if (stageSlotRef.value) resizeObserver.observe(stageSlotRef.value)
  }

  if (document.fonts?.ready) {
    void document.fonts.ready.then(scheduleMeasure)
  }
})

watch([status, () => tracks.value.length, form], () => {
  void nextTick(scheduleMeasure)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', scheduleMeasure)
  window.removeEventListener('resize', scheduleMeasure)
  if (rafId !== null) {
    window.cancelAnimationFrame(rafId)
    rafId = null
  }
  resizeObserver?.disconnect()
  resizeObserver = null
  playerStore.setStageDockRect(null)
})
</script>

<template>
  <div class="home-page" :class="{ veiled: form === 'expanded' }">
    <div class="home-shell">
      <!-- Afternoon listening desk: primary visual & auditory centerpiece -->
      <section ref="deskElRef" class="listening-desk" aria-label="唱片台" @dblclick.stop>
        <div class="desk-turntable">
          <div ref="stageSlotRef" class="vinyl-stage-slot" aria-hidden="true"></div>
        </div>

        <div class="desk-console">
          <div class="console-header">
            <span class="console-kicker">
              <span class="status-dot" :class="{ playing: isPlaying }"></span>
              ～(∠・ω< )⌒★
            </span>
            <div class="console-tools">
              <span class="track-counter tabular-nums">{{ trackNumberLabel }}</span>
              <button
                v-if="status === 'ready'"
                type="button"
                class="subtle-pill"
                :title="loopMode === 'all' ? '切换为单曲循环' : '切换为列表循环'"
                @click="playlistStore.toggleLoop()"
              >
                {{ loopMode === 'all' ? '列表循环' : '单曲循环' }}
              </button>
              <button
                type="button"
                class="subtle-pill"
                title="进入全屏沉浸视图"
                @click="playerStore.expandPlayer()"
              >
                沉浸
              </button>
            </div>
          </div>

          <div class="now-playing">
            <Transition name="track-fade" mode="out-in">
              <div :key="currentTrack?.src ?? 'empty'" class="track-meta">
                <h1 class="track-title selectable">
                  {{ playbackError ? '音频加载失败，请稍后重试' : currentTrack?.title ?? '等待歌单载入…' }}
                </h1>
                <p class="track-artist selectable">
                  {{ currentTrack?.artist || '—' }}
                </p>
              </div>
            </Transition>

            <div class="transport-row">
              <div class="transport-buttons">
                <button
                  type="button"
                  class="ctrl-button"
                  aria-label="上一首"
                  @click="audioStore.playPrev()"
                >
                  <svg viewBox="0 0 20 20" width="15" height="15" fill="currentColor">
                    <path d="M5 4.5h1.6v11H5v-11Zm2.6 5.5 7.4-5v10l-7.4-5Z" />
                  </svg>
                </button>
                <button
                  type="button"
                  class="ctrl-button is-primary"
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
                <button
                  type="button"
                  class="ctrl-button"
                  aria-label="下一首"
                  @click="audioStore.playNext()"
                >
                  <svg viewBox="0 0 20 20" width="15" height="15" fill="currentColor">
                    <path d="M13.4 4.5H15v11h-1.6v-11ZM5 5l7.4 5-7.4 5V5Z" />
                  </svg>
                </button>
              </div>

              <div class="scrubber-wrap">
                <div
                  ref="progressElRef"
                  class="scrubber-hit"
                  role="slider"
                  tabindex="0"
                  aria-label="播放进度"
                  :aria-valuemin="0"
                  :aria-valuemax="Math.max(Math.round(duration), 0)"
                  :aria-valuenow="Math.round(currentTime)"
                  :aria-valuetext="`${formatTime(currentTime)} / ${formatTime(duration)}`"
                  @pointerdown="audioStore.startScrub($event, progressElRef)"
                  @pointermove="audioStore.moveScrub($event, progressElRef)"
                  @pointerup="audioStore.endScrub($event, progressElRef)"
                  @keydown="audioStore.handleProgressKeydown($event)"
                >
                  <div class="scrubber-track">
                    <div class="scrubber-fill" :style="{ width: `${displayPercent}%` }"></div>
                  </div>
                </div>
                <span class="scrubber-time tabular-nums">
                  {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
                </span>
              </div>
            </div>
          </div>

          <div class="desk-playlist">
            <ol v-if="status === 'ready' && tracks.length" class="track-list">
              <li
                v-for="(track, i) in tracks"
                :key="track.src"
                class="track-row"
                :class="{ 'is-active': i === index }"
                tabindex="0"
                @click="audioStore.playTrack(i)"
                @keydown.enter.prevent="audioStore.playTrack(i)"
                @keydown.space.prevent="audioStore.playTrack(i)"
              >
                <span class="row-index tabular-nums">
                  {{ i === index ? (isPlaying ? '❚❚' : '▶') : String(i + 1).padStart(2, '0') }}
                </span>
                <span class="row-title" :title="track.title">{{ track.title }}</span>
                <span v-if="track.artist" class="row-artist">{{ track.artist }}</span>
              </li>
            </ol>

            <div v-else-if="status === 'loading'" class="playlist-empty">
              歌单加载中…
            </div>

            <div v-else-if="status === 'failed'" class="playlist-empty">
              <span>歌单暂时无法载入</span>
              <button type="button" class="subtle-pill" @click="playlistStore.loadPlaylist()">
                重试
              </button>
            </div>

            <div v-else class="playlist-empty">
              暂无曲目
            </div>
          </div>
        </div>
      </section>

      <!-- Restrained desk shelf: article index + quiet identity corner -->
      <div class="desk-columns">
        <main class="desk-main">
          <ArticleList />
        </main>

        <aside class="desk-aside" aria-label="关于与昼夜">
          <ProfileCard />
        </aside>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  position: relative;
  min-height: 100vh;
  padding: calc(58px + var(--space-8)) 0 var(--space-16);
  overflow-x: clip;
  transition:
    opacity var(--duration-slow) ease 0.06s,
    filter var(--duration-slow) var(--ease-out) 0.06s;
}

.home-page.veiled {
  opacity: 0;
  filter: blur(6px);
}

.home-shell {
  width: 100%;
  max-width: var(--layout-max-width);
  margin-inline: auto;
  padding-inline: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-12);
}

/* --- Listening Desk --- */
.listening-desk {
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  gap: var(--space-10);
  align-items: center;
  padding: var(--space-8);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface-glass);
  backdrop-filter: blur(16px);
  box-shadow: var(--shadow-soft);
  transition:
    background-color var(--duration-base) ease,
    border-color var(--duration-base) ease;
}

.desk-turntable {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
}

/* Invisible geometric slot measured at runtime so the vinyl sits cleanly
   on the desk without leaving a dark empty circle behind during expansion. */
.vinyl-stage-slot {
  width: 232px;
  height: 232px;
  border-radius: 50%;
}

.desk-hint {
  color: var(--color-text-faint);
  font-size: 11.5px;
  letter-spacing: 0.03em;
}

.desk-console {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.console-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.console-kicker {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-muted);
  font-size: 12px;
  letter-spacing: 0.06em;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-text-faint);
  transition: background-color var(--duration-base) ease;
}

.status-dot.playing {
  background: var(--color-primary);
}

.console-tools {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.track-counter {
  color: var(--color-text-faint);
  font-size: 12px;
  margin-right: var(--space-1);
}

.subtle-pill {
  padding: 3px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--color-text-muted);
  font-size: 11.5px;
  cursor: pointer;
  transition:
    background-color var(--duration-fast) ease,
    border-color var(--duration-fast) ease,
    color var(--duration-fast) ease;
}

.subtle-pill:hover {
  background: var(--color-surface-muted);
  border-color: var(--color-border-strong);
  color: var(--color-text);
}

.subtle-pill:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
}

.now-playing {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.track-meta {
  min-height: 58px;
}

.track-title {
  font-family: var(--font-serif);
  font-size: clamp(20px, 2.4vw, 26px);
  font-weight: 600;
  line-height: 1.35;
  color: var(--color-text);
  letter-spacing: 0.01em;
}

.track-artist {
  margin-top: var(--space-1);
  color: var(--color-text-muted);
  font-size: 13.5px;
}

.track-fade-enter-active,
.track-fade-leave-active {
  transition:
    opacity var(--duration-base) var(--ease-out),
    transform var(--duration-base) var(--ease-out);
}

.track-fade-enter-from {
  opacity: 0;
  transform: translateY(5px);
}

.track-fade-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}

.transport-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
}

.transport-buttons {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.ctrl-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  transition:
    background-color var(--duration-fast) ease,
    border-color var(--duration-fast) ease,
    color var(--duration-fast) ease,
    transform var(--duration-fast) var(--ease-out);
}

.ctrl-button:hover {
  background: var(--color-surface-muted);
  border-color: var(--color-border-strong);
  color: var(--color-text);
}

.ctrl-button.is-primary {
  width: 40px;
  height: 40px;
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-on-accent);
}

.ctrl-button.is-primary:hover {
  opacity: 0.92;
  transform: scale(1.03);
}

.ctrl-button:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
}

.scrubber-wrap {
  flex: 1 1 200px;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.scrubber-hit {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  height: 20px;
  cursor: pointer;
}

.scrubber-hit:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
  border-radius: var(--radius-xs);
}

.scrubber-track {
  width: 100%;
  height: 3px;
  border-radius: var(--radius-pill);
  background: var(--color-border-strong);
  overflow: hidden;
}

.scrubber-fill {
  height: 100%;
  border-radius: inherit;
  background: var(--color-accent-warm);
  transition: width 80ms linear;
}

.scrubber-time {
  flex-shrink: 0;
  color: var(--color-text-faint);
  font-size: 11.5px;
  white-space: nowrap;
}

/* --- Inline Track List --- */
.desk-playlist {
  border-top: 1px solid var(--color-border);
  padding-top: var(--space-3);
  /* Reserve the steady-state queue height so async playlist loading never
     causes a layout jump on page refresh. */
  min-height: 229px;
}

.track-list {
  display: flex;
  flex-direction: column;
  height: 216px;
  overflow-y: auto;
  padding-right: var(--space-1);
}

.track-row {
  position: relative;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-3);
  padding: 7px var(--space-3);
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-muted);
  font-size: 13px;
  cursor: pointer;
  transition:
    background-color var(--duration-base) ease,
    border-color var(--duration-base) ease,
    color var(--duration-base) ease;
}

.track-row:last-child {
  border-bottom: 0;
}

.track-row:hover {
  background: var(--color-surface-muted);
  color: var(--color-text);
}

.track-row:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: -2px;
  border-radius: var(--radius-xs);
}

.track-row.is-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.track-row.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 2px;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
}

.row-index {
  font-size: 11px;
  color: var(--color-text-faint);
  text-align: center;
}

.track-row.is-active .row-index {
  color: var(--color-primary);
}

.row-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--font-serif);
  font-size: 13.5px;
}

.row-artist {
  color: var(--color-text-faint);
  font-size: 12px;
  white-space: nowrap;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.playlist-empty {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-2);
  color: var(--color-text-faint);
  font-size: 13px;
}

/* --- Lower Desk Columns --- */
.desk-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--layout-sidebar-width);
  gap: var(--space-10);
  align-items: start;
}

.desk-main,
.desk-aside {
  min-width: 0;
}

@media (max-width: 960px) {
  .listening-desk {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-6);
    padding: var(--space-6);
  }

  .vinyl-stage-slot {
    width: 196px;
    height: 196px;
  }

  .desk-columns {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-8);
  }
}

@media (max-width: 767px) {
  .home-page {
    padding: calc(92px + var(--space-4)) 0 var(--space-12);
  }

  .home-shell {
    padding-inline: var(--space-4);
    gap: var(--space-8);
  }

  .listening-desk {
    padding: var(--space-5);
  }

  .row-artist {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .home-page,
  .track-fade-enter-active,
  .track-fade-leave-active {
    transition: opacity 0.2s ease;
  }

  .home-page.veiled,
  .track-fade-enter-from,
  .track-fade-leave-to {
    transform: none;
    filter: none;
  }
}
</style>
