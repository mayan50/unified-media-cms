<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getTasks } from '../../api/modules'

const router = useRouter()
const viewMode = ref<'card' | 'table'>('card')
const tasks = ref<any[]>([])
const loading = ref(true)
const page = ref(1); const totalItems = ref(0); const pageSize = 20
const searchKeyword = ref('')
const filterStatus = ref<string[]>([])

const CFG: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待执行', color: '#8e92a8' }, RUNNING: { label: '执行中', color: '#5b8def' },
  SUCCESS: { label: '成功', color: '#3dd68c' }, FAILED: { label: '失败', color: '#f06580' },
}
const statusItems = Object.entries(CFG).map(([v, c]) => ({ title: c.label, value: v }))
function sc(s: string) { return CFG[s] || { label: s, color: '#8e92a8' } }
function fmt(d: string) { if (!d) return '-'; return new Date(d).toLocaleDateString('zh-CN') }

async function fetchTasks() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize, sort: 'createdAt,desc' }
    if (searchKeyword.value) params.search = searchKeyword.value
    if (filterStatus.value.length) params.status = filterStatus.value[0]
    const { data } = await getTasks(params)
    tasks.value = data.items || []
    totalItems.value = data.total || 0
  } finally { loading.value = false }
}
onMounted(fetchTasks)

function onSearch() { page.value = 1; fetchTasks() }
const totalPages = computed(() => Math.max(1, Math.ceil(totalItems.value / pageSize)))
function goPage(p: number) { page.value = p; fetchTasks() }
</script>

<template>
  <div class="page-view">
    <div class="page-toolbar">
      <div class="page-toolbar-left">
        <h3>任务记录</h3>
        <div class="view-toggle ml-3">
          <v-btn icon="mdi-view-grid" size="small" :variant="viewMode==='card'?'tonal':'plain'" :color="viewMode==='card'?'primary':undefined" @click="viewMode='card'" />
          <v-btn icon="mdi-view-list" size="small" :variant="viewMode==='table'?'tonal':'plain'" :color="viewMode==='table'?'primary':undefined" @click="viewMode='table'" />
        </div>
        <v-text-field v-model="searchKeyword" placeholder="搜索文件名" prepend-inner-icon="mdi-magnify" density="compact" hide-details clearable style="width:200px" @keyup.enter="onSearch" @click:clear="onSearch" />
        <v-select v-model="filterStatus" :items="statusItems" multiple chips placeholder="状态" density="compact" hide-details clearable style="width:140px" @update:model-value="onSearch" />
      </div>
      <span class="text-caption text-disabled">{{ totalItems }} 条</span>
    </div>

    <v-progress-linear v-if="loading" indeterminate color="primary" />

    <!-- Card View -->
    <div v-if="viewMode==='card'" class="page-cards">
      <div v-for="t in tasks" :key="t.id" class="page-card task-card" @click="router.push(`/workshop/tasks/${t.id}`)">
        <div class="page-card-top">
          <span class="page-card-name">{{ t.filePath?.split('/').pop() || '-' }}</span>
          <span class="stb" :style="{ background: sc(t.status).color }">{{ sc(t.status).label }}</span>
        </div>
        <div class="task-card-meta"><span class="text-caption text-disabled">{{ fmt(t.createdAt) }}</span></div>
      </div>
      <div v-if="!tasks.length && !loading" class="text-caption text-disabled text-center py-10" style="grid-column:1/-1">暂无记录</div>
    </div>

    <!-- Table View -->
    <div v-else class="page-body">
      <div class="page-table-wrap">
        <v-table density="compact" hover>
          <thead><tr><th>文件</th><th>状态</th><th>时间</th></tr></thead>
          <tbody><tr v-for="t in tasks" :key="t.id" @click="router.push(`/workshop/tasks/${t.id}`)" style="cursor:pointer">
            <td>{{ t.filePath?.split('/').pop() || '-' }}</td>
            <td><span class="stb" :style="{ background: sc(t.status).color }">{{ sc(t.status).label }}</span></td>
            <td class="text-caption">{{ fmt(t.createdAt) }}</td>
          </tr></tbody>
        </v-table>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="page-pager">
      <v-btn icon="mdi-chevron-left" size="small" variant="plain" :disabled="page<=1" @click="goPage(page-1)" />
      <span class="text-caption mx-2">{{ page }}/{{ totalPages }} ({{ totalItems }}项)</span>
      <v-btn icon="mdi-chevron-right" size="small" variant="plain" :disabled="page>=totalPages" @click="goPage(page+1)" />
    </div>
  </div>
</template>

<style scoped>
.task-card { cursor: pointer; transition: border-color .15s; display: flex; flex-direction: column; gap: 10px; }
.task-card:hover { border-color: rgb(var(--v-theme-primary)); }
.task-card-meta { font-size: 12px; color: rgb(var(--v-theme-secondary)); }
.stb { display: inline-block; font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; color: #fff; }
</style>
