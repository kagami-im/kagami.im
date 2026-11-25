import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

export interface Track {
  title: string
  artist?: string
  src: string
  cover: string
  lrc?: string
}

type LoopMode = 'all' | 'one'

const STATE_KEY = 'kagami-ui:playlist-state'

export const usePlaylistStore = defineStore('playlist', () => {
  const tracks = ref<Track[]>([])
  const index = ref(0)
  const loopMode = ref<LoopMode>('all')
  // Surface the fetch outcome so the playlist panel can render three states
  // instead of an empty list when /playlist.json fails to load.
  const status = ref<'loading' | 'ready' | 'failed'>('loading')

  const currentTrack = computed(() => tracks.value[index.value] ?? null)

  // Restore the last-used track and loop mode (clamped once the list arrives).
  try {
    const saved = JSON.parse(localStorage.getItem(STATE_KEY) ?? '{}')
    if (typeof saved.index === 'number') index.value = saved.index
    if (saved.loopMode === 'one' || saved.loopMode === 'all') loopMode.value = saved.loopMode
  } catch {
    // Corrupt state falls back to the defaults.
  }

  const loadPlaylist = async () => {
    status.value = 'loading'
    try {
      const resp = await fetch('/playlist.json')
      if (!resp.ok) {
        throw new Error(`playlist.json responded ${resp.status}`)
      }
      const data = (await resp.json()) as { tracks?: Track[] }
      tracks.value = data.tracks ?? []
      if (index.value >= tracks.value.length) index.value = 0
      status.value = 'ready'
    } catch (e) {
      console.error('Playlist load failed', e)
      status.value = 'failed'
    }
  }

  void loadPlaylist()

  const setIndex = (i: number) => {
    index.value = Math.min(Math.max(i, 0), Math.max(tracks.value.length - 1, 0))
  }
  const next = () => setIndex((index.value + 1) % Math.max(tracks.value.length, 1))
  const prev = () =>
    setIndex((index.value - 1 + Math.max(tracks.value.length, 1)) % Math.max(tracks.value.length, 1))
  const toggleLoop = () => {
    loopMode.value = loopMode.value === 'all' ? 'one' : 'all'
  }

  // Remember which track and loop mode were last used (not the position).
  watch([index, loopMode], ([i, mode]) => {
    localStorage.setItem(STATE_KEY, JSON.stringify({ index: i, loopMode: mode }))
  })

  return {
    tracks,
    index,
    loopMode,
    status,
    currentTrack,
    loadPlaylist,
    setIndex,
    next,
    prev,
    toggleLoop,
  }
})
