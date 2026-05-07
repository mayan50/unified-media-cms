import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'dark' | 'light'

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>((localStorage.getItem('theme') as ThemeMode) || 'dark')

  function toggle() {
    mode.value = mode.value === 'dark' ? 'light' : 'dark'
  }

  function apply() {
    document.documentElement.setAttribute('data-theme', mode.value)
    localStorage.setItem('theme', mode.value)
  }

  // Apply immediately & watch for changes
  apply()
  watch(mode, apply)

  return { mode, toggle }
})
