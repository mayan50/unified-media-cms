<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getJob, getJobTasks, startJob, stopJob, resetJob, deleteJob, retryTask } from '../api/modules'
import { Client } from '@stomp/stompjs'
const route = useRoute()
const router = useRouter()
const jobId = route.params.id as string

let stompClient: Client | null = null

const job = ref<any>(null)
const tasks = ref<any[]>([])
const loading = ref(true)
const actionLoading = ref('')
const totalTasks = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchQuery = ref('')
const statusFilter = ref('')

const statusList = [
  { title: '全部', value: '' },
  { title: '待执行', value: 'PENDING' },
  { title: '执行中', value: 'RUNNING' },
  { title: '成功', value: 'SUCCESS' },
  { title: '失败', value: 'FAILED' },
  { title: '跳过', value: 'SKIPPED' },
]

const CFG: Record<string, { label: string; color: string }> = {
  PENDING:  { label: '待执行', color: '#8e92a8' },
  RUNNING:  { label: '执行中', color: '#5b8def' },
  COMPLETED: { label: '已完成', color: '#3dd68c' },
  FAILED:   { label: '失败', color: '#f06580' },
  SUCCESS:  { label: '成功', color: '#3dd68c' },
  SKIPPED:  { label: '跳过', color: '#8e92a8' },
}
function sc(s: string) { return CFG[s] || { label: s, color: '#8e92a8' } }

const nodeLabels = ref<Record<string, string>>({})
function nodeLabel(name: string) { return nodeLabels.value[name] || name }
function fmt(d: string) { if (!d) return '-'; return new Date(d).toLocaleString('zh-CN') }
function fmtShort(d: string) {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN', { month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' })
}

const stats = computed(() => {
  const all = tasks.value
  return {
    scanned: all.length,
    running: all.filter(t => t.status === 'RUNNING').length,
    success: all.filter(t => t.status === 'SUCCESS').length,
    failed: all.filter(t => t.status === 'FAILED').length,
  }
})

const filteredTasks = computed(() => {
  let list = tasks.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(t => (t.filePath || '').toLowerCase().includes(q))
  }
  if (statusFilter.value) {
    list = list.filter(t => t.status === statusFilter.value)
  }
  return list
})

async function loadData() {
  loading.value = true
  try {
    const [jobRes, tasksRes] = await Promise.all([
      getJob(jobId),
      getJobTasks(jobId, currentPage.value, pageSize.value)
    ])
    job.value = jobRes.data
    tasks.value = tasksRes.data?.items || []
    totalTasks.value = tasksRes.data?.total || 0
    nodeLabels.value = tasksRes.data?.nodeLabels || {}
  } finally { loading.value = false }
}

async function doAction(action: string, fn: () => Promise<any>) {
  actionLoading.value = action
  try { await fn(); await loadData() }
  catch { /* toast later */ }
  finally { actionLoading.value = '' }
}

async function doRetryTask(taskId: string) {
  actionLoading.value = 'retry-' + taskId
  try { await retryTask(taskId); await loadData() }
  catch { /* ignore */ }
  finally { actionLoading.value = '' }
}

function connectWs() {
  if (stompClient) stompClient.deactivate()
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  stompClient = new Client({
    brokerURL: proto + '//' + location.host + '/ws',
    reconnectDelay: 3000,
    onConnect: () => {
      stompClient!.subscribe('/topic/job/' + jobId, (msg) => {
        const body = JSON.parse(msg.body)
        if (body.type === 'logsCleared' && body.taskId) {
          const t = tasks.value.find((x: any) => x.id === body.taskId)
          if (t) t.lastCompletedNode = '等待重试'
        } else if (body.type === 'nodeLog' && body.taskId) {
          const t = tasks.value.find((x: any) => x.id === body.taskId)
          if (t) t.lastCompletedNode = body.data.nodeLabel || body.data.nodeName
        } else if (body.type === 'taskStatusChanged') {
          const t = tasks.value.find((x: any) => x.id === body.taskId)
          if (t) t.status = body.status
        } else if (body.type === 'jobStatusChanged') {
          if (job.value) job.value.status = body.status
        }
      })
    },
  })
  stompClient.activate()
}

onMounted(async () => { await loadData(); connectWs() })
onUnmounted(() => { stompClient?.deactivate() })
</script>

<template>
  <div class="jd-page">
    <v-progress-linear v-if="loading" indeterminate color="primary" />

    <template v-if="job">
      <!-- Top bar -->
      <div class="jd-top">
        <v-btn icon="mdi-arrow-left" size="small" variant="plain" @click="router.push('/workshop/tasks')" />
        <span class="text-caption text-disabled ml-2">作业详情</span>
      </div>

      <div class="jd-body">
        <!-- ====== Left Sidebar (30%) ====== -->
        <div class="jd-left">
          <!-- Job Info -->
          <div class="jd-card">
            <h2 class="jd-job-title">{{ job.name || '未命名作业' }}</h2>
            <div class="jd-meta-row">
              <span class="jd-meta-label">ID</span>
              <code class="jd-meta-val">{{ jobId?.substring(0, 8) }}</code>
            </div>
            <div class="jd-meta-row">
              <span class="jd-meta-label">创建时间</span>
              <span class="jd-meta-val">{{ fmt(job.createdAt) }}</span>
            </div>

            <!-- Action buttons -->
            <div class="jd-actions">
              <v-btn icon="mdi-refresh" size="small" variant="tonal" color="primary"
                :loading="actionLoading === 'start'"
                @click="doAction('start', () => startJob(jobId))" />
              <v-btn icon="mdi-stop-circle" size="small" variant="tonal" color="warning"
                :loading="actionLoading === 'stop'"
                :disabled="job.status !== 'RUNNING'"
                @click="doAction('stop', () => stopJob(jobId))" />
              <v-btn icon="mdi-delete" size="small" variant="tonal" color="error"
                :loading="actionLoading === 'delete'"
                @click="doAction('delete', () => deleteJob(jobId, false, false))" />
              <span class="stb" :style="{ background: sc(job.status).color }">{{ sc(job.status).label }}</span>
            </div>
          </div>

          <!-- Stats Gauges -->
          <div class="jd-card">
            <div class="jd-card-title">任务统计</div>
            <div class="jd-gauges">
              <div class="jd-gauge">
                <v-progress-circular :model-value="stats.scanned" :max="Math.max(totalTasks, 1)" size="64" width="6" color="grey" bg-color="rgba(255,255,255,.06)">
                  {{ stats.scanned }}
                </v-progress-circular>
                <span class="jd-gauge-label">扫描文件</span>
              </div>
              <div class="jd-gauge">
                <v-progress-circular :model-value="stats.running" :max="Math.max(totalTasks, 1)" size="64" width="6" color="warning" bg-color="rgba(255,255,255,.06)">
                  {{ stats.running }}
                </v-progress-circular>
                <span class="jd-gauge-label">处理中</span>
              </div>
              <div class="jd-gauge">
                <v-progress-circular :model-value="stats.success" :max="Math.max(totalTasks, 1)" size="64" width="6" color="success" bg-color="rgba(255,255,255,.06)">
                  {{ stats.success }}
                </v-progress-circular>
                <span class="jd-gauge-label">成功</span>
              </div>
              <div class="jd-gauge">
                <v-progress-circular :model-value="stats.failed" :max="Math.max(totalTasks, 1)" size="64" width="6" color="error" bg-color="rgba(255,255,255,.06)">
                  {{ stats.failed }}
                </v-progress-circular>
                <span class="jd-gauge-label">失败</span>
              </div>
            </div>
          </div>

          <!-- Properties -->
          <div class="jd-card">
            <div class="jd-card-title">作业属性</div>
            <div class="jd-props">
              <div class="jd-prop-row"><span class="jd-prop-k">输入路径</span><span class="jd-prop-v mono">{{ job.inputPathText || '-' }}</span></div>
              <div class="jd-prop-row"><span class="jd-prop-k">输出路径</span><span class="jd-prop-v mono">{{ job.outputPathText || '-' }}</span></div>
              <div class="jd-prop-row"><span class="jd-prop-k">开始时间</span><span class="jd-prop-v">{{ fmt(job.createdAt) }}</span></div>
              <div class="jd-prop-row"><span class="jd-prop-k">更新时间</span><span class="jd-prop-v">{{ fmt(job.updatedAt) }}</span></div>
            </div>
          </div>
        </div>

        <!-- ====== Right Main (70%) ====== -->
        <div class="jd-right">
          <!-- Filter Bar -->
          <div class="jd-card">
            <div class="jd-filter-bar">
              <v-text-field v-model="searchQuery" label="搜索文件名" density="compact" hide-details clearable
                prepend-inner-icon="mdi-magnify" style="max-width: 260px" />
              <v-select v-model="statusFilter" :items="statusList" label="状态" density="compact" hide-details style="max-width: 140px" />
              <v-spacer />
              <span class="text-caption text-disabled">总计：{{ totalTasks }} 条任务</span>
            </div>
          </div>

          <!-- Task Table -->
          <div class="jd-card mt-3">
            <v-table density="compact" hover>
              <thead>
                <tr>
                  <th class="jd-th">状态</th>
                  <th class="jd-th">文件名</th>
                  <th class="jd-th">当前节点</th>
                  <th class="jd-th">更新时间</th>
                  <th class="jd-th" style="width:60px">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="t in filteredTasks" :key="t.id" class="jd-tr">
                  <td>
                    <span class="stb" :style="{ background: sc(t.status).color }">{{ sc(t.status).label }}</span>
                  </td>
                  <td>
                    <a class="jd-task-link" @click="router.push(`/workshop/records/${t.id}`)">
                      {{ t.filePath?.split('/').pop() || t.filePath || '-' }}
                    </a>
                  </td>
                  <td class="text-caption text-disabled">{{ nodeLabel(t.lastCompletedNode) || '未开始' }}</td>
                  <td class="text-caption text-disabled">{{ fmtShort(t.updatedAt) || fmtShort(t.createdAt) }}</td>
                  <td>
                    <v-btn v-if="t.status === 'FAILED'" icon="mdi-refresh" size="x-small" variant="text" color="warning"
                      :loading="actionLoading === 'retry-' + t.id"
                      @click="doRetryTask(t.id)" />
                    <v-btn v-else icon="mdi-dots-vertical" size="x-small" variant="text"
                      @click="router.push(`/workshop/records/${t.id}`)" />
                  </td>
                </tr>
                <tr v-if="!filteredTasks.length">
                  <td colspan="5" class="text-center py-6 text-disabled">暂无任务</td>
                </tr>
              </tbody>
            </v-table>

            <!-- Pagination -->
            <div v-if="totalTasks > pageSize" class="jd-pagination">
              <v-pagination v-model="currentPage" :length="Math.ceil(totalTasks / pageSize)" :total-visible="7"
                density="compact" @update:model-value="loadData()" />
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.jd-page { height: 100%; overflow: auto; padding: 16px 24px 24px; background: #1a1b1e; }
.jd-top { display: flex; align-items: center; margin-bottom: 16px; }

.jd-body { display: grid; grid-template-columns: 320px 1fr; gap: 16px; align-items: start; }
@media (max-width: 900px) { .jd-body { grid-template-columns: 1fr; } }

.jd-left { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
.jd-right { min-width: 0; }

/* Cards */
.jd-card {
  background: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-border-color));
  border-radius: 10px;
  padding: 16px;
}
.jd-card-title { font-size: 13px; font-weight: 600; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid rgb(var(--v-border-color)); }

/* Job info */
.jd-job-title { font-size: 20px; font-weight: 700; margin: 0 0 12px; line-height: 1.3; }
.jd-meta-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; font-size: 12px; }
.jd-meta-label { color: #909399; min-width: 56px; }
.jd-meta-val { color: rgb(var(--v-theme-secondary)); }
.jd-meta-val.mono { font-family: 'JetBrains Mono', monospace; font-size: 11px; }
.jd-actions { display: flex; align-items: center; gap: 6px; margin-top: 14px; }

/* Stats gauges */
.jd-gauges { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.jd-gauge { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.jd-gauge-label { font-size: 10px; color: #909399; }

/* Props */
.jd-props { display: flex; flex-direction: column; gap: 6px; }
.jd-prop-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; font-size: 12px; }
.jd-prop-k { color: #909399; white-space: nowrap; }
.jd-prop-v { color: rgb(var(--v-theme-secondary)); text-align: right; word-break: break-all; }
.jd-prop-v.mono { font-family: 'JetBrains Mono', monospace; font-size: 11px; }

/* Filter bar */
.jd-filter-bar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

/* Table */
.jd-th { font-size: 11px; font-weight: 600; color: #909399; text-transform: uppercase; padding: 8px 10px !important; }
.jd-tr:hover { background: rgb(var(--v-theme-surface-variant)); }
.jd-task-link { color: rgb(var(--v-theme-primary)); cursor: pointer; font-size: 13px; text-decoration: none; }
.jd-task-link:hover { text-decoration: underline; }

/* Pagination */
.jd-pagination { display: flex; justify-content: flex-end; margin-top: 12px; }

/* Status badge */
.stb { display: inline-block; font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 4px; color: #fff; white-space: nowrap; }
</style>
