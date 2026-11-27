import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type PlayerForm = 'expanded' | 'mini'

export interface DockRect {
  top: number
  left: number
  right: number
  width: number
  height: number
}

const STORAGE_KEY = 'kagami-ui:player-form'

function loadInitialForm(): PlayerForm {
  // Never cover inner routes (e.g. direct article links or refreshes) with the
  // home-only immersive stage.
  if (window.location.pathname !== '/') {
    return 'mini'
  }
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'mini' || saved === 'expanded') {
    return saved
  }
  // First visit to the root path starts in the immersive vinyl stage.
  return 'expanded'
}

export const usePlayerStore = defineStore('player', () => {
  const form = ref<PlayerForm>(loadInitialForm())

  // Measured geometry of the platter slot inside HomeView's listening desk.
  const stageDockRect = ref<DockRect | null>(null)

  watch(
    form,
    (value) => {
      document.documentElement.classList.toggle('player-expanded', value === 'expanded')
    },
    { immediate: true }
  )

  const expandPlayer = () => {
    form.value = 'expanded'
    localStorage.setItem(STORAGE_KEY, 'expanded')
  }

  const collapsePlayer = (persist = true) => {
    form.value = 'mini'
    if (persist) {
      localStorage.setItem(STORAGE_KEY, 'mini')
    }
  }

  const setStageDockRect = (rect: DockRect | null) => {
    stageDockRect.value = rect
  }

  return {
    form,
    stageDockRect,
    expandPlayer,
    collapsePlayer,
    setStageDockRect,
  }
})
