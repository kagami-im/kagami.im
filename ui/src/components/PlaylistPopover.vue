<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { usePlaylistStore } from '../stores/playlist'

defineProps<{
  isPlaying: boolean
}>()

const emit = defineEmits<{
  select: [index: number]
}>()

const playlistStore = usePlaylistStore()
const { tracks, index, loopMode, status } = storeToRefs(playlistStore)
</script>

<template>
  <div class="playlist-panel" @dblclick.stop>
    <div class="panel-head">
      <span class="panel-title">
        播放列表<template v-if="status === 'ready'"> · {{ tracks.length }}</template>
      </span>
      <button
        v-if="status === 'ready'"
        type="button"
        class="panel-loop"
        :title="loopMode === 'all' ? '切换为单曲循环' : '切换为列表循环'"
        @click="playlistStore.toggleLoop()"
      >
        {{ loopMode === 'all' ? '列表循环' : '单曲循环' }}
      </button>
    </div>

    <ol v-if="status === 'ready'" class="panel-list">
      <li
        v-for="(track, i) in tracks"
        :key="track.src"
        :class="{ active: i === index }"
        tabindex="0"
        @click="emit('select', i)"
        @keydown.enter.prevent="emit('select', i)"
        @keydown.space.prevent="emit('select', i)"
      >
        <span class="track-mark">{{ i === index ? (isPlaying ? '❚❚' : '▶') : i + 1 }}</span>
        <span class="track-name" :title="track.title">{{ track.title }}</span>
        <span v-if="track.artist" class="track-artist">{{ track.artist }}</span>
      </li>
    </ol>

    <div v-else class="panel-status">
      <span>{{ status === 'loading' ? '歌单加载中…' : '歌单加载失败' }}</span>
      <button
        v-if="status === 'failed'"
        type="button"
        class="panel-retry"
        @click="playlistStore.loadPlaylist()"
      >
        重试
      </button>
    </div>
  </div>
</template>

<style scoped>
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

.panel-loop,
.panel-retry {
  padding: 2px 8px;
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-nav-surface);
  color: var(--color-text-muted);
  font-size: 11px;
  cursor: pointer;
}

.panel-loop:hover,
.panel-retry:hover {
  color: var(--color-text);
}

.panel-loop:focus-visible,
.panel-retry:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 1px;
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

.panel-list li:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: -2px;
}

.panel-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-3);
  color: var(--color-text-muted);
  font-size: 12px;
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

@media (max-width: 767px) {
  .playlist-panel {
    right: var(--space-3);
    left: var(--space-3);
    top: calc(var(--orb-top) + var(--orb-size) + 56px);
    width: auto;
  }
}
</style>
