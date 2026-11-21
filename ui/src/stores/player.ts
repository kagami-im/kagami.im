import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type PlayerForm = 'expanded' | 'mini'

const STORAGE_KEY = 'kagami-ui:player-form'

function loadPlayerForm(): PlayerForm {
  const saved = localStorage.getItem(STORAGE_KEY)
  return saved === 'mini' || saved === 'expanded' ? saved : 'expanded'
}

export const usePlayerStore = defineStore('player', () => {
  // The player component is always mounted; `form` only decides how it is
  // presented: full-screen record (expanded) or docked header orb (mini).
  const form = ref<PlayerForm>(loadPlayerForm())

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

  return { form, expandPlayer, collapsePlayer }
})
