import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const STORAGE_KEY = 'kagami-ui:dark'

function loadIsDark(): boolean {
  if (localStorage.getItem(STORAGE_KEY) !== null) {
    return localStorage.getItem(STORAGE_KEY) === 'true'
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(loadIsDark())

  // Apply to the root element so semantic tokens switch in one place.
  watch(
    isDark,
    (value) => {
      document.documentElement.classList.toggle('dark', value)
    },
    { immediate: true }
  )

  const toggleTheme = () => {
    isDark.value = !isDark.value
    localStorage.setItem(STORAGE_KEY, String(isDark.value))
  }

  return { isDark, toggleTheme }
})
