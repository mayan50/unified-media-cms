import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import './assets/global-page-layout.css'
import { aliases, mdi } from './icons'
import App from './App.vue'
import router from './router'

const vuetify = createVuetify({
  theme: {
    defaultTheme: 'dark',
    themes: {
      dark: {
        dark: true,
        colors: {
          background: '#0d1117',
          surface: '#161b22',
          'surface-variant': '#1c2129',
          'surface-bright': '#21262e',
          primary: '#8b6ffc',
          secondary: '#5c6572',
          error: '#f06580',
          warning: '#f0a840',
          success: '#3dd68c',
          info: '#5b8def',
          'on-background': '#e6edf3',
          'on-surface': '#e6edf3',
          'on-surface-variant': '#8b949e',
          'on-primary': '#ffffff',
          'border-color': '#303548',
        },
      },
      light: {
        dark: false,
        colors: {
          background: '#f4f5f8',
          surface: '#ffffff',
          'surface-variant': '#f4f5f8',
          'surface-bright': '#ffffff',
          primary: '#6f4ffc',
          secondary: '#8b8fa0',
          error: '#e0445a',
          warning: '#d9920e',
          success: '#22b573',
          info: '#5b8def',
          'on-background': '#1c1f2e',
          'on-surface': '#1c1f2e',
          'on-surface-variant': '#5c6070',
          'on-primary': '#ffffff',
          'border-color': '#d9dce5',
        },
      },
    },
  },
  defaults: {
    VBtn: { variant: 'flat' },
    VTextField: { variant: 'outlined', density: 'compact', color: 'primary', hideDetails: 'auto' },
    VSelect: { variant: 'outlined', density: 'compact', color: 'primary', hideDetails: 'auto' },
    VTextarea: { variant: 'outlined', density: 'compact', color: 'primary', hideDetails: 'auto' },
    VCombobox: { variant: 'outlined', density: 'compact', color: 'primary', hideDetails: 'auto' },
    VFileInput: { variant: 'outlined', density: 'compact', color: 'primary', hideDetails: 'auto' },
    VDialog: { maxWidth: 480 },
    VChip: { size: 'small' },
    VList: { density: 'compact' },
    VTable: { density: 'compact' },
    VDataTable: { density: 'compact' },
    VCard: { color: 'surface' },
    VAppBar: { color: 'surface' },
    VNavigationDrawer: { color: 'surface' },
  },
  icons: {
    defaultSet: 'mdi',
    aliases,
    sets: { mdi },
  },
})

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(vuetify)
app.mount('#app')
