import { createApp } from 'vue'
import { createPinia } from 'pinia'

import './styles/tokens.css'
import './style.css'
import App from './App.vue'
import { router } from './router'

// Apply the saved theme before mount to avoid a flash of the wrong colors.
const saved = localStorage.getItem('kagami-ui:dark')
const prefersDark = saved === null && window.matchMedia('(prefers-color-scheme: dark)').matches
if (saved === 'true' || prefersDark) {
  document.documentElement.classList.add('dark')
}

createApp(App).use(createPinia()).use(router).mount('#app')
