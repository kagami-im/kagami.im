import { defineStore, storeToRefs } from 'pinia'
import { computed, nextTick, ref, watch } from 'vue'
import { usePlaylistStore } from './playlist'

export function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return '0:00'
  }
  const minutes = Math.floor(seconds / 60)
  const padded = String(Math.floor(seconds % 60)).padStart(2, '0')
  return `${minutes}:${padded}`
}

export const useAudioStore = defineStore('audio', () => {
  const playlistStore = usePlaylistStore()
  const { tracks, index, currentTrack } = storeToRefs(playlistStore)

  const audioEl = ref<HTMLAudioElement | null>(null)
  const isPlaying = ref(false)
  const currentTime = ref(0)
  const duration = ref(0)
  const playbackError = ref(false)
  const pendingPlay = ref(false)

  const scrubbing = ref(false)
  const scrubPercent = ref(0)

  const progressPercent = computed(() =>
    duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0
  )

  const displayPercent = computed(() =>
    scrubbing.value ? scrubPercent.value : progressPercent.value
  )

  const bindAudioElement = (el: HTMLAudioElement | null) => {
    audioEl.value = el
  }

  const handleAudioError = () => {
    playbackError.value = true
    isPlaying.value = false
    console.error('Audio playback error', audioEl.value?.error)
  }

  const onAudioPlay = () => {
    isPlaying.value = true
    playbackError.value = false
  }

  const onAudioPause = () => {
    isPlaying.value = false
  }

  const togglePlay = async () => {
    const audio = audioEl.value
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

  const restartCurrent = () => {
    const audio = audioEl.value
    if (!audio) {
      return
    }
    audio.currentTime = 0
    void audio.play().catch((e) => console.error('Playback failed', e))
  }

  const playTrack = (i: number) => {
    if (i === index.value) {
      if (isPlaying.value) {
        audioEl.value?.pause()
      } else {
        void togglePlay()
      }
      return
    }
    pendingPlay.value = true
    playlistStore.setIndex(i)
  }

  const playPrev = () => {
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
    if (tracks.value.length <= 1) {
      restartCurrent()
      return
    }
    pendingPlay.value = true
    playlistStore.next()
  }

  const handleTimeUpdate = () => {
    if (audioEl.value) {
      currentTime.value = audioEl.value.currentTime
    }
  }

  const handleLoadedMetadata = () => {
    if (audioEl.value) {
      duration.value = audioEl.value.duration
    }
  }

  const ratioFromEvent = (event: PointerEvent, track: HTMLElement | null): number | null => {
    if (!track || !Number.isFinite(duration.value) || duration.value <= 0) {
      return null
    }
    const rect = track.getBoundingClientRect()
    return Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width))
  }

  const startScrub = (event: PointerEvent, track: HTMLElement | null) => {
    if (!Number.isFinite(duration.value) || duration.value <= 0) {
      return
    }
    const el = event.currentTarget as HTMLElement | null
    el?.setPointerCapture(event.pointerId)
    scrubbing.value = true
    const ratio = ratioFromEvent(event, track)
    if (ratio !== null) {
      scrubPercent.value = ratio * 100
    }
  }

  const moveScrub = (event: PointerEvent, track: HTMLElement | null) => {
    if (!scrubbing.value) {
      return
    }
    const ratio = ratioFromEvent(event, track)
    if (ratio !== null) {
      scrubPercent.value = ratio * 100
    }
  }

  const endScrub = (event: PointerEvent, track: HTMLElement | null) => {
    if (!scrubbing.value) {
      return
    }
    scrubbing.value = false
    const ratio = ratioFromEvent(event, track)
    const audio = audioEl.value
    if (ratio === null || !audio) {
      return
    }
    audio.currentTime = ratio * duration.value
    currentTime.value = audio.currentTime
  }

  const handleProgressKeydown = (event: KeyboardEvent) => {
    const audio = audioEl.value
    if (!audio || !Number.isFinite(duration.value) || duration.value <= 0) {
      return
    }
    if (event.key === 'ArrowRight') {
      audio.currentTime = Math.min(duration.value, audio.currentTime + 5)
      event.preventDefault()
    } else if (event.key === 'ArrowLeft') {
      audio.currentTime = Math.max(0, audio.currentTime - 5)
      event.preventDefault()
    } else if (event.key === 'Home') {
      audio.currentTime = 0
      event.preventDefault()
    } else if (event.key === 'End') {
      audio.currentTime = duration.value
      event.preventDefault()
    }
  }

  const updateMediaSession = (track: { title: string; artist?: string; cover: string }) => {
    if (!('mediaSession' in navigator)) {
      return
    }
    navigator.mediaSession.metadata = new MediaMetadata({
      title: track.title,
      artist: track.artist ?? 'Kagami',
      album: 'Kagami',
      artwork: [{ src: track.cover, sizes: '1024x1024', type: 'image/webp' }],
    })
  }

  const registerMediaSessionHandlers = () => {
    if (!('mediaSession' in navigator)) {
      return
    }
    navigator.mediaSession.setActionHandler('play', () => {
      void togglePlay()
    })
    navigator.mediaSession.setActionHandler('pause', () => {
      void togglePlay()
    })
    navigator.mediaSession.setActionHandler('previoustrack', () => playPrev())
    navigator.mediaSession.setActionHandler('nexttrack', () => playNext())
    navigator.mediaSession.setActionHandler('seekto', (details) => {
      const audio = audioEl.value
      if (audio && details.seekTime != null) {
        audio.currentTime = details.seekTime
      }
    })
  }

  const unregisterMediaSessionHandlers = () => {
    if (!('mediaSession' in navigator)) {
      return
    }
    navigator.mediaSession.setActionHandler('play', null)
    navigator.mediaSession.setActionHandler('pause', null)
    navigator.mediaSession.setActionHandler('previoustrack', null)
    navigator.mediaSession.setActionHandler('nexttrack', null)
    navigator.mediaSession.setActionHandler('seekto', null)
  }

  watch(currentTrack, async (track, prev) => {
    if (!track) {
      return
    }
    updateMediaSession(track)
    playbackError.value = false

    if (!prev) {
      return
    }
    const audio = audioEl.value
    if (!audio) {
      return
    }
    const shouldPlay = pendingPlay.value || isPlaying.value
    pendingPlay.value = false
    isPlaying.value = false
    currentTime.value = 0
    duration.value = 0
    await nextTick()
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

  return {
    isPlaying,
    currentTime,
    duration,
    scrubbing,
    displayPercent,
    playbackError,
    bindAudioElement,
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
    registerMediaSessionHandlers,
    unregisterMediaSessionHandlers,
  }
})
