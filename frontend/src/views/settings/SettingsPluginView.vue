<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../api/index'

const plugins = ref<any[]>([])
const loading = ref(false)
const viewMode = ref<'card' | 'table'>('card')

async function fetchPlugins() {
  loading.value = true
  try { const { data } = await api.get('/plugins'); plugins.value = data || [] }
  finally { loading.value = false }
}

async function handleReload() {
  try { await api.post('/plugins/reload'); await fetchPlugins() }
  catch { /* ignore */ }
}

onMounted(fetchPlugins)
</script>

<template>
  <div class="page-view">
    <div class="page-toolbar">
      <h3>节点插件管理</h3>
      <div class="page-toolbar-right">
        <div class="view-toggle mr-3">
          <v-btn icon="mdi-view-grid" size="small" :variant="viewMode === 'card' ? 'tonal' : 'plain'" :color="viewMode === 'card' ? 'primary' : undefined" @click="viewMode = 'card'" />
          <v-btn icon="mdi-view-list" size="small" :variant="viewMode === 'table' ? 'tonal' : 'plain'" :color="viewMode === 'table' ? 'primary' : undefined" @click="viewMode = 'table'" />
        </div>
        <v-btn color="primary" size="small" prepend-icon="mdi-refresh" :loading="loading" @click="handleReload">扫描并热重载</v-btn>
      </div>
    </div>

    <v-progress-linear v-if="loading" indeterminate color="primary" />

    <!-- Card View -->
    <div v-if="viewMode === 'card'" class="page-cards">
      <div v-for="p in plugins" :key="p.pluginId" class="page-card">
        <div class="page-card-top">
          <div>
            <div class="page-card-name">{{ p.name || p.pluginId }}</div>
            <div class="text-caption text-disabled">{{ p.description || '暂无描述' }}</div>
          </div>
          <v-chip size="x-small" color="success" variant="tonal">运行中</v-chip>
        </div>
        <div class="text-caption mt-2">{{ p.nodeCount }} 个节点：{{ (p.nodeNames || []).join(', ') }}</div>
      </div>
      <div v-if="!plugins.length && !loading" class="text-caption text-disabled text-center py-8" style="grid-column:1/-1">暂无外部插件</div>
    </div>

    <!-- Table View -->
    <div v-else class="page-body">
      <div class="page-table-wrap">
      <v-table density="compact" hover>
        <thead><tr><th>名称</th><th>描述</th><th>节点数</th><th>节点列表</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="p in plugins" :key="p.pluginId">
            <td>{{ p.name || p.pluginId }}</td>
            <td class="text-caption">{{ p.description || '-' }}</td>
            <td>{{ p.nodeCount }}</td>
            <td class="text-caption">{{ (p.nodeNames || []).join(', ') }}</td>
            <td><v-chip size="x-small" color="success" variant="tonal">运行中</v-chip></td>
          </tr>
        </tbody>
      </v-table>
      </div>
    </div>
  </div>
</template>
