import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'kagami-ui:show-intro'

function loadShowIntro(): boolean {
  return localStorage.getItem(STORAGE_KEY) !== 'false'
}

export const useViewStore = defineStore('view', () => {
  // Fresh visits see the intro; returning visitors resume the view they were on.
  const showIntro = ref(loadShowIntro())

  const toggleView = () => {
    showIntro.value = !showIntro.value
    localStorage.setItem(STORAGE_KEY, String(showIntro.value))
  }

  return { showIntro, toggleView }
})
