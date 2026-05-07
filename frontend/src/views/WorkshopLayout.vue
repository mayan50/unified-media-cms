<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 编辑模板时隐藏侧边栏，给编辑器全宽空间
const isEditing = computed(() => {
  const q = route.query.edit
  return q !== undefined && q !== null
})

const menuItems = [
  { key: 'templates', label: '模板编排', icon: 'mdi-tools', path: '/workshop/templates' },
  { key: 'tasks', label: '任务看板', icon: 'mdi-format-list-bulleted', path: '/workshop/tasks' },
  { key: 'records', label: '任务记录', icon: 'mdi-history', path: '/workshop/records' },
]
</script>

<template>
  <div class="workshop-layout">
    <aside v-if="!isEditing" class="ws-sidebar">
      <div class="ws-sidebar-top">
        <v-btn icon="mdi-arrow-left" size="small" variant="plain" @click="router.push('/')" />
        <div class="ws-sidebar-title">生产车间</div>
      </div>

      <nav class="ws-nav">
        <v-btn
          v-for="item in menuItems"
          :key="item.key"
          :variant="route.path === item.path ? 'tonal' : 'plain'"
          :color="route.path === item.path ? 'primary' : undefined"
          block
          class="ws-nav-item"
          @click="router.push(item.path)"
        >
          <v-icon :icon="item.icon" start />
          {{ item.label }}
        </v-btn>
      </nav>
    </aside>

    <div class="ws-main">
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.workshop-layout {
  display: flex;
  height: 100%;
  background: rgb(var(--v-theme-background));
}

.ws-sidebar {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: rgb(var(--v-theme-surface));
  border-right: 1px solid rgb(var(--v-border-color));
}

.ws-sidebar-top {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid rgb(var(--v-border-color));
}

.ws-sidebar-title {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.2px;
}

.ws-nav {
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ws-nav-item {
  justify-content: flex-start !important;
  text-transform: none !important;
  font-weight: 500;
  font-size: 13px;
  border-radius: 8px;
}

.ws-main {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
