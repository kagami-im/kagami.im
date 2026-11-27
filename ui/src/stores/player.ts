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

export const usePlayerStore = defineStore('player', () => {
  // Two visual states:
  // - 'mini': docked onto the home listening-desk platter slot (hidden on inner routes while audio keeps playing)
  // - 'expanded': full-screen immersive vinyl stage
  const form = ref<PlayerForm>('mini')

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
  }

  const collapsePlayer = () => {
    form.value = 'mini'
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
