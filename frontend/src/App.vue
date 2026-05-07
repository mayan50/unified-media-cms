<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTaskStore } from './stores/task'
import { useThemeStore } from './stores/theme'
import { getStorageNodes } from './api/modules'
import SettingsPanel from './components/SettingsPanel.vue'
import ToastProvider from './components/ToastProvider.vue'

const router = useRouter()
const route = useRoute()
const taskStore = useTaskStore()
const themeStore = useThemeStore()

const sidebarCollapsed = ref(false)
const isWorkshop = computed(() => route.path.startsWith('/workshop'))
const activeNav = ref('all')
const storageNodes = ref<any[]>([])
const pendingCount = ref(0)
const globalSearch = ref('')
const settingsVisible = ref(false)

let pollTimer: ReturnType<typeof setInterval> | null = null

async function loadSidebarData() {
  try { const { data } = await getStorageNodes(); storageNodes.value = data } catch { /* */ }
}

async function loadPendingCount() {
  try {
    await taskStore.fetchTasks()
    pendingCount.value = taskStore.tasks.filter(
      (t: any) => t.currentStatus === 'PENDING_MANUAL' || t.currentStatus === 'FAILED'
    ).length
  } catch { /* */ }
}

function handleNavClick(key: string) {
  activeNav.value = key
  router.push('/')
}

function handleSearch() {
  router.push({ path: '/', query: { q: globalSearch.value } })
}

onMounted(() => { loadSidebarData(); loadPendingCount(); pollTimer = setInterval(loadPendingCount, 15000) })
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<template>
  <v-app :theme="themeStore.mode">
    <ToastProvider />

    <!-- HEADER -->
    <v-app-bar flat height="56" class="app-bar">
      <div class="header-left">
        <v-btn
          v-if="!isWorkshop"
          :icon="sidebarCollapsed ? 'mdi-menu-open' : 'mdi-menu'"
          size="small"
          variant="plain"
          @click="sidebarCollapsed = !sidebarCollapsed"
        />
        <h1 class="app-logo" @click="router.push('/')">
          <span class="logo-icon">M</span>
          <span class="logo-text" v-show="!sidebarCollapsed">Unified Media</span>
        </h1>
      </div>

      <div class="header-center">
        <v-text-field
          v-model="globalSearch"
          placeholder="搜索资产、任务…"
          prepend-inner-icon="mdi-magnify"
          hide-details
          density="compact"
          flat
          solo
          class="global-search"
          clearable
          @keyup.enter="handleSearch"
        />
      </div>

      <div class="header-right">
        <v-badge :model-value="pendingCount" :content="pendingCount" color="error" overlap>
          <v-btn
            :icon="'mdi-tools'"
            size="small"
            :variant="route.path.startsWith('/workshop') ? 'tonal' : 'plain'"
            :color="route.path.startsWith('/workshop') ? 'primary' : undefined"
            @click="router.push(isWorkshop ? '/' : '/workshop')"
          />
        </v-badge>
        <v-btn icon="mdi-cog" size="small" variant="plain" @click="settingsVisible = !settingsVisible" />
        <v-btn
          :icon="themeStore.mode === 'dark' ? 'mdi-white-balance-sunny' : 'mdi-moon-waning-crescent'"
          size="small"
          variant="plain"
          class="theme-toggle-btn"
          @click="themeStore.toggle()"
        />
      </div>
    </v-app-bar>

    <!-- SIDEBAR -->
    <v-navigation-drawer
      v-if="!isWorkshop"
      :rail="sidebarCollapsed"
      permanent
      width="220"
      rail-width="60"
      class="app-sidebar"
    >
      <v-list density="compact" nav class="py-2">
        <v-list-item
          prepend-icon="mdi-home"
          title="首页"
          :active="route.path === '/'"
          @click="router.push('/')"
        />
      </v-list>

      <v-divider class="mx-3" />

      <div class="nav-section-label">LIBRARY</div>
      <v-list density="compact" nav>
        <v-list-item
          v-for="node in storageNodes"
          :key="node.id"
          prepend-icon="mdi-folder"
          :title="node.name"
          @click="handleNavClick(node.id)"
        />
        <v-list-item
          v-if="!storageNodes.length"
          disabled
          title="暂无库"
        />
      </v-list>

      <v-divider class="mx-3" />

      <div class="nav-section-label">TAGS</div>
      <v-list density="compact" nav>
        <v-list-item disabled prepend-icon="mdi-tag" title="即将推出" />
      </v-list>

      <template #append>
        <div class="pa-3 text-center">
          <span class="text-caption text-disabled">v1.0.0</span>
        </div>
      </template>
    </v-navigation-drawer>

    <!-- MAIN -->
    <v-main class="app-main">
      <router-view />
    </v-main>

    <!-- SETTINGS DRAWER -->
    <v-navigation-drawer
      v-model="settingsVisible"
      location="right"
      temporary
      width="520"
    >
      <v-toolbar flat density="compact" color="surface">
        <v-toolbar-title>系统设置</v-toolbar-title>
        <v-btn icon="mdi-close" variant="plain" size="small" @click="settingsVisible = false" />
      </v-toolbar>
      <div class="pa-5">
        <SettingsPanel />
      </div>
    </v-navigation-drawer>
  </v-app>
</template>

<style>
/* ============================================================
   THEME CSS VARIABLES (complement Vuetify theme)
   ============================================================ */
:root {
  --bg-base: #0d1117;
  --bg-surface: #161b22;
  --bg-elevated: #1c2129;
  --bg-hover: #21262e;
  --bg-active: #292e38;
  --border-color: #303548;
  --text-primary: #e6edf3;
  --text-secondary: #8b949e;
  --text-muted: #5c6572;
  --accent: #8b6ffc;
  --accent-soft: rgba(139,111,252,0.14);
  --accent-hover: #a48dfd;
  --danger: #f06580;
  --success: #3dd68c;
  --warning: #f0a840;
  --shadow-card: 0 2px 10px rgba(0,0,0,0.35);
  --shadow-float: 0 8px 30px rgba(0,0,0,0.45);
  --cover-shadow: 0 4px 20px rgba(0,0,0,0.45);
}

[data-theme="light"] {
  --bg-base: #f4f5f8;
  --bg-surface: #ffffff;
  --bg-elevated: #ffffff;
  --bg-hover: #eef0f5;
  --bg-active: #e2e5ed;
  --border-color: #d9dce5;
  --text-primary: #1c1f2e;
  --text-secondary: #5c6070;
  --text-muted: #8b8fa0;
  --accent: #6f4ffc;
  --accent-soft: rgba(111,79,252,0.08);
  --accent-hover: #5a3be0;
  --danger: #e0445a;
  --success: #22b573;
  --warning: #d9920e;
  --shadow-card: 0 1px 4px rgba(0,0,0,0.08);
  --shadow-float: 0 8px 30px rgba(0,0,0,0.12);
  --cover-shadow: 0 4px 16px rgba(0,0,0,0.14);
}

*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
html, body, #app { height: 100%; }
body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* Fix Vuetify border opacity - make borders more visible */
.v-theme--dark {
  --v-border-opacity: 0.14;
}
.v-theme--light {
  --v-border-opacity: 0.10;
}

/* Ensure Vuetify text-disabled class uses a visible shade */
.v-theme--dark .text-disabled {
  color: #5c6572 !important;
}
.v-theme--light .text-disabled {
  color: #8b8fa0 !important;
}

/* Make v-card variant=flat actually flat */
.v-card--variant-flat {
  box-shadow: none !important;
}

/* Monospace font utility */
code, pre, .mono {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}
</style>

<style scoped>
.app-bar {
  border-bottom: 1px solid rgb(var(--v-border-color));
}
.header-left { display: flex; align-items: center; gap: 8px; }
.header-center { flex: 1; max-width: 560px; margin: 0 auto; }
.header-right { display: flex; align-items: center; gap: 4px; }

.global-search :deep(.v-field__outline) { display: none; }
.global-search :deep(.v-field) {
  border-radius: 10px;
  background: rgb(var(--v-theme-surface-variant));
}

.app-logo { display: flex; align-items: center; gap: 10px; cursor: pointer; margin: 0; }
.logo-icon {
  display: flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; border-radius: 8px;
  background: linear-gradient(135deg, rgb(var(--v-theme-primary)), #a855f7);
  color: #fff; font-weight: 800; font-size: 16px;
}
.logo-text { font-size: 15px; font-weight: 600; color: rgb(var(--v-theme-on-surface)); letter-spacing: -0.3px; }

.nav-section-label {
  padding: 8px 16px 4px;
  font-size: 10px; font-weight: 700;
  letter-spacing: 1.2px;
  color: rgb(var(--v-theme-secondary));
  text-transform: uppercase;
}

.app-main {
  background: rgb(var(--v-theme-background));
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.app-sidebar {
  border-right: 1px solid rgb(var(--v-border-color));
}

.theme-toggle-btn {
  color: rgb(var(--v-theme-warning));
}
</style>
