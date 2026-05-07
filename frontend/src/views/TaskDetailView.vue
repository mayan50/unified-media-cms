<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTaskLogs } from '../api/modules'
import { useTaskStore } from '../stores/task'

const route = useRoute()
const router = useRouter()
const taskStore = useTaskStore()

const taskId = route.params.id as string
const logs = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  await taskStore.fetchTasks()
  loading.value = true
  try {
    const { data } = await getTaskLogs(taskId)
    logs.value = data
  } finally { loading.value = false }
})

const task = ref<any>(null)
taskStore.fetchTasks().then(() => {
  task.value = taskStore.tasks.find((t: any) => t.id === taskId)
})

const STATUS_CFG: Record<string, { label: string; color: string; bg: string }> = {
  QUEUED: { label: '未执行', color: '#8e92a8', bg: 'rgba(142,146,168,0.12)' },
  RUNNING: { label: '执行中', color: '#5b8def', bg: 'rgba(91,141,239,0.12)' },
  COMPLETED: { label: '已完成', color: '#3dd68c', bg: 'rgba(61,214,140,0.12)' },
  PENDING_MANUAL: { label: '待仲裁', color: '#f06580', bg: 'rgba(240,101,128,0.12)' },
  FAILED: { label: '失败', color: '#f06580', bg: 'rgba(240,101,128,0.12)' },
}
function sc(status: string) { return STATUS_CFG[status] || { label: status, color: '#8e92a8', bg: 'rgba(142,146,168,0.12)' } }

const logStatusCfg: Record<string, { label: string; color: string }> = {
  SUCCESS: { label: '成功', color: '#3dd68c' },
  FAILED: { label: '失败', color: '#f06580' },
  SKIPPED: { label: '跳过', color: '#8e92a8' },
}
function lsc(status: string) { return logStatusCfg[status] || { label: status, color: '#8e92a8' } }

function formatTime(dateStr: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN')
}
function formatMs(ms: number | null) {
  if (!ms) return '-'
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}
</script>

<template>
  <div class="task-detail">
    <div class="td-header">
      <v-btn icon="mdi-arrow-left" size="small" variant="plain" @click="router.push('/workshop/tasks')" />
      <div v-if="task">
        <h2 class="td-title">{{ task.name || '未命名任务' }}</h2>
        <code class="td-id">ID: {{ taskId?.substring(0, 8) }}</code>
        <span class="status-badge ml-2" :style="{ color: sc(task.currentStatus).color, background: sc(task.currentStatus).bg }">{{ sc(task.currentStatus).label }}</span>
      </div>
    </div>

    <v-progress-linear v-if="loading" indeterminate color="primary" />

    <div class="td-logs">
      <h3 class="td-section-title">子任务执行记录 ({{ logs.length }})</h3>
      <div v-if="logs.length" class="log-list">
        <div v-for="log in logs" :key="log.id" class="log-item">
          <div class="log-left">
            <span class="log-dot" :style="{ background: lsc(log.status).color }"></span>
            <div>
              <div class="log-node-name">{{ log.nodeName }}</div>
              <div class="log-meta">{{ formatTime(log.createdAt) }} · {{ formatMs(log.executionTimeMs) }}</div>
            </div>
          </div>
          <div class="log-right">
            <span class="log-status" :style="{ color: lsc(log.status).color }">{{ lsc(log.status).label }}</span>
            <div v-if="log.errorMessage" class="log-error">{{ log.errorMessage }}</div>
          </div>
        </div>
      </div>
      <div v-else class="log-empty">暂无执行记录</div>
    </div>
  </div>
</template>

<style scoped>
.task-detail {
  height: 100%;
  overflow: auto;
  padding: 20px 24px;
}
.td-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.td-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.td-id {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  color: rgb(var(--v-theme-secondary));
}
.td-section-title {
  font-size: 14px; font-weight: 600;
  margin: 0 0 14px;
}

.log-list { display: flex; flex-direction: column; gap: 2px; }
.log-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 8px;
  background: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-border-color));
}
.log-left { display: flex; gap: 10px; align-items: flex-start; }
.log-dot {
  width: 10px; height: 10px; border-radius: 50%;
  margin-top: 4px; flex-shrink: 0;
}
.log-node-name { font-size: 13px; font-weight: 600; }
.log-meta { font-size: 11px; color: rgb(var(--v-theme-secondary)); margin-top: 2px; }
.log-right { text-align: right; }
.log-status { font-size: 12px; font-weight: 600; }
.log-error { font-size: 11px; color: rgb(var(--v-theme-error)); margin-top: 4px; max-width: 300px; word-break: break-all; }
.log-empty { text-align: center; padding: 40px; color: rgb(var(--v-theme-secondary)); font-size: 14px; }

.status-badge {
  display: inline-block; font-size: 11px; font-weight: 600;
  padding: 2px 8px; border-radius: 4px; white-space: nowrap; vertical-align: middle;
}
</style>
