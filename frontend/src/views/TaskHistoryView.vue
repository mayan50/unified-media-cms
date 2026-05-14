<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getAllTasks } from '../api/modules'

const router = useRouter()
const viewMode = ref<'card' | 'table'>('card')
const allTasks = ref<any[]>([])
const loading = ref(true)
const page = ref(1); const totalItems = ref(0); const pageSize = 20

async function fetchTasks() {
  loading.value = true
  try {
    const { data } = await getAllTasks({ page: 1, size: 500, sort: 'createdAt,desc' })
    allTasks.value = data.items || []
    totalItems.value = data.total || 0
  } finally { loading.value = false }
}
onMounted(fetchTasks)

const paged = computed(() => allTasks.value.slice((page.value - 1) * pageSize, page.value * pageSize))
const totalPages = computed(() => Math.max(1, Math.ceil(totalItems.value / pageSize)))

const CFG: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待执行', color: '#8e92a8' }, RUNNING: { label: '执行中', color: '#5b8def' },
  SUCCESS: { label: '成功', color: '#3dd68c' }, FAILED: { label: '失败', color: '#f06580' },
}
function sc(s: string) { return CFG[s] || { label: s, color: '#8e92a8' } }
function fmt(d: string) { if (!d) return '-'; return new Date(d).toLocaleDateString('zh-CN') }
</script>

<template>
  <div class="tr-page">
    <div class="tr-bar">
      <div class="tr-left">
        <v-btn icon="mdi-view-grid" size="small" :variant="viewMode==='card'?'tonal':'plain'" :color="viewMode==='card'?'primary':undefined" @click="viewMode='card'" />
        <v-btn icon="mdi-view-list" size="small" :variant="viewMode==='table'?'tonal':'plain'" :color="viewMode==='table'?'primary':undefined" @click="viewMode='table'" />
      </div>
      <span class="text-caption text-disabled">{{ totalItems }} 条</span>
    </div>
    <v-progress-linear v-if="loading" indeterminate color="primary" />

    <div v-if="viewMode==='card'" class="tr-cards">
      <div v-for="t in paged" :key="t.id" class="tr-card" @click="router.push(`/workshop/records/${t.id}`)">
        <div class="tr-card-top"><span class="tr-name">{{ t.filePath?.split('/').pop() || '-' }}</span><span class="stb" :style="{ background: sc(t.status).color }">{{ sc(t.status).label }}</span></div>
        <div class="tr-meta"><span class="text-caption text-disabled">{{ fmt(t.createdAt) }}</span></div>
      </div>
      <div v-if="!paged.length && !loading" class="tr-empty">暂无记录</div>
    </div>

    <div v-else class="tr-table">
      <v-table density="compact" hover><thead><tr><th>文件</th><th>状态</th><th>时间</th></tr></thead>
        <tbody><tr v-for="t in paged" :key="t.id" @click="router.push(`/workshop/records/${t.id}`)" style="cursor:pointer">
          <td>{{ t.filePath?.split('/').pop() || '-' }}</td>
          <td><span class="stb" :style="{ background: sc(t.status).color }">{{ sc(t.status).label }}</span></td>
          <td class="text-caption">{{ fmt(t.createdAt) }}</td>
        </tr></tbody>
      </v-table>
    </div>

    <div v-if="totalPages > 1" class="tr-pager">
      <v-btn icon="mdi-chevron-left" size="small" variant="plain" :disabled="page<=1" @click="page--" />
      <span class="text-caption mx-2">{{ page }}/{{ totalPages }}</span>
      <v-btn icon="mdi-chevron-right" size="small" variant="plain" :disabled="page>=totalPages" @click="page++" />
    </div>
  </div>
</template>

<style scoped>
.tr-page { height: 100%; display: flex; flex-direction: column; }
.tr-bar { display: flex; justify-content: space-between; align-items: center; padding: 12px 20px; border-bottom: 1px solid rgb(var(--v-border-color)); background: rgb(var(--v-theme-surface)); }
.tr-left { display: flex; gap: 4px; }
.tr-cards { flex: 1; overflow: auto; padding: 12px 20px 20px; display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; align-content: start; }
.tr-card { background: rgb(var(--v-theme-surface)); border: 1px solid rgb(var(--v-border-color)); border-radius: 10px; padding: 14px; cursor: pointer; transition: border-color .15s; display: flex; flex-direction: column; gap: 10px; }
.tr-card:hover { border-color: rgb(var(--v-theme-primary)); }
.tr-card-top { display: flex; justify-content: space-between; }
.tr-name { font-size: 14px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 160px; }
.tr-meta { font-size: 12px; color: rgb(var(--v-theme-secondary)); }
.tr-empty { grid-column: 1/-1; text-align: center; padding: 60px 0; color: rgb(var(--v-theme-secondary)); }
.tr-table { flex: 1; overflow: auto; padding: 0 20px 20px; }
.tr-pager { display: flex; justify-content: center; align-items: center; padding: 8px; border-top: 1px solid rgb(var(--v-border-color)); background: rgb(var(--v-theme-surface)); }
.stb { display: inline-block; font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; color: #fff; }
</style>
