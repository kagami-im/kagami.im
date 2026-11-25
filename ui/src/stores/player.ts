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

function loadPlayerForm(): PlayerForm {
  const saved = localStorage.getItem(STORAGE_KEY)
  return saved === 'mini' || saved === 'expanded' ? saved : 'expanded'
}

export const usePlayerStore = defineStore('player', () => {
  // The player component is always mounted; `form` only decides how it is
  // presented: full-screen record (expanded) or docked header orb (mini).
  const form = ref<PlayerForm>(loadPlayerForm())

  // Measured geometry of the placeholder slot inside AppHeader. Keeping this
  // in the store lets VinylPlayer dock directly onto the slot without
  // hardcoding AppHeader's padding or button widths.
  const dockRect = ref<DockRect | null>(null)

  // While the record stage covers the viewport there is nothing behind it to
  // scroll, and a live scrollbar floating over the backdrop reads as a bug.
  // The class lands on <html> (rule in style.css): root-level overflow owns
  // the viewport directly, with no reliance on the body-to-viewport
  // propagation. scrollbar-gutter: stable (style.css) keeps the layout width
  // constant so docking back causes no reflow.
  watch(
    form,
    (value) => {
      document.documentElement.classList.toggle('player-expanded', value === 'expanded')
    },
    { immediate: true }
  )

  const expandPlayer = () => {
    form.value = 'expanded'
    localStorage.setItem(STORAGE_KEY, form.value)
  }

  const collapsePlayer = () => {
    form.value = 'mini'
    localStorage.setItem(STORAGE_KEY, form.value)
  }

  const setDockRect = (rect: DockRect | null) => {
    dockRect.value = rect
  }

  return { form, dockRect, expandPlayer, collapsePlayer, setDockRect }
})
