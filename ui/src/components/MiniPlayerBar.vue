<script setup lang="ts">
import { ref } from 'vue'
import { formatTime } from '../composables/useAudioPlayer'

defineProps<{
  isPlaying: boolean
  playbackError: boolean
  trackTitle: string
  currentTime: number
  duration: number
  displayPercent: number
}>()

const emit = defineEmits<{
  prev: []
  togglePlay: []
  next: []
  togglePlaylist: []
  scrubStart: [event: PointerEvent, track: HTMLElement | null]
  scrubMove: [event: PointerEvent, track: HTMLElement | null]
  scrubEnd: [event: PointerEvent, track: HTMLElement | null]
  progressKeydown: [event: KeyboardEvent]
}>()

const barElRef = ref<HTMLElement | null>(null)
const progressElRef = ref<HTMLElement | null>(null)

defineExpose({ el: barElRef })
</script>

<template>
  <div ref="barElRef" class="mini-bar" @dblclick.stop>
    <button type="button" class="bar-button bar-skip" aria-label="上一首" @click="emit('prev')">
      ❮❮
    </button>
    <button
      type="button"
      class="bar-button"
      :aria-label="isPlaying ? '暂停' : '播放'"
      @click="emit('togglePlay')"
    >
      {{ isPlaying ? '❚❚' : '▶' }}
    </button>
    <button type="button" class="bar-button bar-skip" aria-label="下一首" @click="emit('next')">
      ❯❯
    </button>
    <span
      class="bar-title"
      :class="{ error: playbackError }"
      :title="playbackError ? '音频加载或播放失败，请稍后重试' : trackTitle"
    >
      {{ playbackError ? '⚠ 播放出错' : trackTitle || '…' }}
    </span>
    <div
      ref="progressElRef"
      class="bar-progress"
      role="slider"
      tabindex="0"
      aria-label="播放进度"
      :aria-valuemin="0"
      :aria-valuemax="Math.max(Math.round(duration), 0)"
      :aria-valuenow="Math.round(currentTime)"
      :aria-valuetext="`${formatTime(currentTime)} / ${formatTime(duration)}`"
      @pointerdown="emit('scrubStart', $event, progressElRef)"
      @pointermove="emit('scrubMove', $event, progressElRef)"
      @pointerup="emit('scrubEnd', $event, progressElRef)"
      @keydown="emit('progressKeydown', $event)"
    >
      <div class="bar-track">
        <div class="bar-fill" :style="{ width: `${displayPercent}%` }"></div>
      </div>
    </div>
    <span class="bar-time">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</span>
    <button
      type="button"
      class="bar-button bar-list"
      aria-label="播放列表"
      @click="emit('togglePlaylist')"
    >
      ☰
    </button>
  </div>
</template>

<style scoped>
/* Hover/tap control bar, anchored to the left of the orb at the same height. */
.mini-bar {
  position: absolute;
  top: calc(var(--orb-top) + var(--orb-size) / 2 - 20px);
  right: calc(var(--orb-right) + var(--orb-size) + var(--space-2));
  max-width: calc(100vw - var(--space-6));
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
  flex-shrink: 0;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text);
  font-size: 12px;
  cursor: pointer;
}

.bar-button:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 1px;
}

.bar-button:hover {
  background: var(--color-surface-muted);
}

.bar-title {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text);
  font-size: 13px;
}

.bar-title.error {
  color: var(--color-text);
  font-weight: 600;
}

.bar-progress {
  display: flex;
  align-items: center;
  flex: 0 1 110px;
  min-width: 56px;
  width: 110px;
  height: 14px;
  cursor: pointer;
}

.bar-progress:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
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
  flex-shrink: 0;
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

@media (max-width: 767px) {
  .mini-bar {
    right: var(--space-3);
    left: var(--space-3);
    top: calc(var(--orb-top) + var(--orb-size) + var(--space-2));
    gap: var(--space-1);
    padding: 0 var(--space-2);
  }

  .bar-time {
    display: none;
  }
}
</style>
