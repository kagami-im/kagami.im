import { defineStore } from 'pinia'
import { ref } from 'vue'

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
