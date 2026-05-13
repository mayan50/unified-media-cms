<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { key: 'storage', label: '存储节点', icon: 'mdi-server', path: '/settings/storage' },
  { key: 'llm', label: 'AI 算力配置', icon: 'mdi-brain', path: '/settings/llm' },
  { key: 'categories-tags', label: '分类与标签', icon: 'mdi-tag-multiple', path: '/settings/categories-tags' },
]
</script>

<template>
  <div class="settings-layout">
    <aside class="sl-sidebar">
      <div class="sl-sidebar-top">
        <v-btn icon="mdi-arrow-left" size="small" variant="plain" @click="router.push('/')" />
        <div class="sl-sidebar-title">系统设置</div>
      </div>
      <nav class="sl-nav">
        <v-btn
          v-for="item in menuItems"
          :key="item.key"
          :variant="route.path === item.path ? 'tonal' : 'plain'"
          :color="route.path === item.path ? 'primary' : undefined"
          block
          class="sl-nav-item"
          @click="router.push(item.path)"
        >
          <v-icon :icon="item.icon" start />
          {{ item.label }}
        </v-btn>
      </nav>
    </aside>
    <div class="sl-main">
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.settings-layout { display: flex; height: 100%; background: rgb(var(--v-theme-background)); }
.sl-sidebar {
  width: 220px; flex-shrink: 0; display: flex; flex-direction: column;
  background: rgb(var(--v-theme-surface)); border-right: 1px solid rgb(var(--v-border-color));
}
.sl-sidebar-top {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 16px; border-bottom: 1px solid rgb(var(--v-border-color));
}
.sl-sidebar-title { font-size: 15px; font-weight: 700; letter-spacing: -0.2px; }
.sl-nav { padding: 12px 8px; display: flex; flex-direction: column; gap: 2px; }
.sl-nav-item {
  justify-content: flex-start !important; text-transform: none !important;
  font-weight: 500; font-size: 13px; border-radius: 8px;
}
.sl-main { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
</style>
