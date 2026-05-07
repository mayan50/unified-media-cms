<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTaskStore } from '../../stores/task'
import { getStorageNodes, getTemplates, submitTask, startTask, stopTask, deleteTask, updateTask, resumeTask, getTaskLogs } from '../../api/modules'
import StoragePathSelector from './StoragePathSelector.vue'
import { useToast } from '../../composables/useToast'

const { toast } = useToast()
const taskStore = useTaskStore()
const router = useRouter()

// ---- Filters ----
const filterStatus = ref<string[]>([])
const filterTemplate = ref('')
const viewMode = ref<'card' | 'table'>('card')

const templateOptions = ref<{ id: string; name: string }[]>([])
const storageNodes = ref<any[]>([])

// ---- New Task Dialog ----
const newTaskDialogVisible = ref(false)
interface StoragePathValue { storage_node_id: string; path: string }
const newTaskForm = ref({
  name: '',
  templateId: '' as string | undefined,
  inputPath: { storage_node_id: '', path: '' } as StoragePathValue,
  outputPath: { storage_node_id: '', path: '' } as StoragePathValue,
})

// ---- Detail Drawer ----
const editDialogVisible = ref(false)
const editTaskId = ref('')
const editTaskName = ref('')
const editTaskTemplateId = ref<string | undefined>()
const editTaskInputPath = ref<StoragePathValue>({ storage_node_id: '', path: '' })
const editTaskOutputPath = ref<StoragePathValue>({ storage_node_id: '', path: '' })


const drawerVisible = ref(false)
const drawerTask = ref<any>(null)
const drawerLogs = ref<any[]>([])
const drawerLoading = ref(false)
const editForm = ref<any>({ title: '', author: '', summary: '', coverUrl: '', publishYear: null, tags: [] })

onMounted(async () => {
  await taskStore.fetchTasks()
  try {
    const [nodesRes, tplRes] = await Promise.all([getStorageNodes(), getTemplates()])
    storageNodes.value = nodesRes.data
    templateOptions.value = tplRes.data
  } catch { /* */ }
})

const filteredTasks = computed(() => {
  let list = taskStore.tasks
  if (filterStatus.value.length) {
    list = list.filter((t: any) => filterStatus.value.includes(t.currentStatus))
  }
  return list
})

const STATUS_CFG: Record<string, { label: string; color: string; bg: string }> = {
  QUEUED: { label: '未执行', color: '#8e92a8', bg: 'rgba(142,146,168,0.12)' },
  RUNNING: { label: '执行中', color: '#5b8def', bg: 'rgba(91,141,239,0.12)' },
  COMPLETED: { label: '已完成', color: '#3dd68c', bg: 'rgba(61,214,140,0.12)' },
  PENDING_MANUAL: { label: '待仲裁', color: '#f06580', bg: 'rgba(240,101,128,0.12)' },
  FAILED: { label: '失败', color: '#f06580', bg: 'rgba(240,101,128,0.12)' },
}

const statusItems = Object.entries(STATUS_CFG).map(([value, cfg]) => ({ title: cfg.label, value }))

function statusCfg(status: string) { return STATUS_CFG[status] || { label: status, color: '#8e92a8', bg: 'rgba(142,146,168,0.12)' } }

function formatTime(dateStr: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const now = Date.now()
  const diff = now - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + 'm'
  if (diff < 86400000) return Math.floor(diff / 3600000) + 'h'
  return d.toLocaleDateString('zh-CN')
}

// Vuetify data-table headers
const tableHeaders = [
  { title: 'ID', key: 'id', width: 100 },
  { title: '模板', key: 'template', width: 120 },
  { title: '进度', key: 'progress', width: 80, align: 'center' as const },
  { title: '状态', key: 'status', width: 90 },
  { title: '时间', key: 'createdAt', width: 80, align: 'end' as const },
  { title: '操作', key: 'actions', width: 160, align: 'center' as const, sortable: false },
]

// ---- New Task ----
const templateNeeds = computed(() => {
  const tpl = templateOptions.value.find(t => t.id === newTaskForm.value.templateId)
  if (!tpl || !(tpl as any).graphPayload) return { needsInput: false, needsOutput: false }
  const nodes = (tpl as any).graphPayload?.nodes || []
  const hasInput = nodes.some((n: any) => n.name === 'FileSnifferNode')
  const hasOutput = nodes.some((n: any) => n.name === 'ArchiveNode')
  return { needsInput: hasInput, needsOutput: hasOutput }
})

async function submitNewTask() {
  const needs = templateNeeds.value
  if (needs.needsInput && !newTaskForm.value.inputPath.path) {
    toast.warning('该模板需要输入路径'); return
  }
  if (needs.needsOutput && !newTaskForm.value.outputPath.path) {
    toast.warning('该模板需要输出路径'); return
  }
  try {
    const fp = newTaskForm.value
    await submitTask({
      name: fp.name || undefined,
      templateId: fp.templateId || undefined,
      inputPath: fp.inputPath.path ? fp.inputPath : undefined,
      outputPath: fp.outputPath.path ? fp.outputPath : undefined,
    })
    newTaskDialogVisible.value = false
    toast.success('任务已创建')
    newTaskForm.value = {
      name: '',
      templateId: undefined,
      inputPath: { storage_node_id: '', path: '' },
      outputPath: { storage_node_id: '', path: '' },
    }
    taskStore.fetchTasks()
  } catch { toast.error('创建失败') }
}

// ---- Task Actions ----
async function handleStart(taskId: string) {
  try { await startTask(taskId); toast.success('任务已启动'); taskStore.fetchTasks() }
  catch { toast.error('启动失败') }
}
async function handleStop(taskId: string) {
  try { await stopTask(taskId); toast.success('任务已终止'); taskStore.fetchTasks() }
  catch { toast.error('终止失败') }
}
function openEditDialog(task: any) {
  editTaskId.value = task.id
  editTaskName.value = task.name || ''
  editTaskTemplateId.value = task.templateId || undefined
  editTaskInputPath.value = task.inputPath || { storage_node_id: '', path: '' }
  editTaskOutputPath.value = task.outputPath || { storage_node_id: '', path: '' }
  editDialogVisible.value = true
}
async function handleSaveEdit() {
  if (!editTaskName.value.trim()) { toast.warning('名称不能为空'); return }
  try {
    await updateTask(editTaskId.value, { name: editTaskName.value.trim() })
    editDialogVisible.value = false
    toast.success('已更新')
    taskStore.fetchTasks()
  } catch { toast.error('更新失败，名称可能重复') }
}
async function handleDelete(taskId: string) {
  try { await deleteTask(taskId); toast.success('任务已删除'); taskStore.fetchTasks() }
  catch { toast.error('删除失败') }
}

// ---- Drawer ----
async function openDrawer(_: any, row: any) {
  const task = row.item
  drawerTask.value = task
  drawerVisible.value = true
  drawerLoading.value = true
  try {
    const { data } = await getTaskLogs(task.id)
    drawerLogs.value = data

    const failedLog = data.find((l: any) => l.status === 'FAILED')
    if (failedLog?.outputPayload) {
      const payload = typeof failedLog.outputPayload === 'string'
        ? JSON.parse(failedLog.outputPayload) : failedLog.outputPayload
      if (payload.aiSuggestion) {
        editForm.value.title = payload.aiSuggestion.title || ''
        editForm.value.author = payload.aiSuggestion.author || ''
      }
      if (payload.candidates) {
        editForm.value._candidates = payload.candidates
      }
    }
  } finally {
    drawerLoading.value = false
  }
}

async function handleResume() {
  if (!drawerTask.value) return
  try {
    await resumeTask(drawerTask.value.id, editForm.value)
    toast.success('任务已恢复执行')
    drawerVisible.value = false
    taskStore.fetchTasks()
  } catch { toast.error('恢复失败') }
}

async function handleRetry() {
  await handleResume()
}

function handleKill() {
  toast.info('强制终止功能待后端实现')
}

function selectCandidate(c: any) {
  editForm.value.title = c.title || editForm.value.title
  editForm.value.coverUrl = c.coverUrl || editForm.value.coverUrl
  editForm.value._selectedCandidate = c
}
</script>

<template>
  <div class="task-center">
    <!-- Toolbar -->
    <div class="tc-toolbar">
      <div class="tc-toolbar-left">
        <div class="view-toggle">
          <v-btn icon="mdi-view-grid" size="small" :variant="viewMode === 'card' ? 'tonal' : 'plain'" :color="viewMode === 'card' ? 'primary' : undefined" @click="viewMode = 'card'" />
          <v-btn icon="mdi-view-list" size="small" :variant="viewMode === 'table' ? 'tonal' : 'plain'" :color="viewMode === 'table' ? 'primary' : undefined" @click="viewMode = 'table'" />
        </div>
        <v-select
          v-model="filterStatus"
          :items="statusItems"
          multiple
          chips
          placeholder="状态筛选"
          density="compact"
          clearable
          hide-details
          style="width: 200px"
        >
          <template #chip="{ item, props }">
            <v-chip v-bind="props" size="x-small" :color="statusCfg(item.value).color" variant="tonal">
              <span class="filter-dot" :style="{ background: statusCfg(item.value).color }"></span>
              {{ item.title }}
            </v-chip>
          </template>
          <template #item="{ item, props }">
            <v-list-item v-bind="props" :title="item.title">
              <template #prepend>
                <span class="filter-dot" :style="{ background: statusCfg(item.value).color }"></span>
              </template>
            </v-list-item>
          </template>
        </v-select>
        <v-select
          v-model="filterTemplate"
          :items="templateOptions.map(t => ({ title: t.name, value: t.name }))"
          placeholder="模板"
          density="compact"
          clearable
          hide-details
          style="width: 160px"
        />
      </div>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="newTaskDialogVisible = true">新建任务</v-btn>
    </div>

    <v-progress-linear v-if="taskStore.loading" indeterminate color="primary" />

    <!-- Card View -->
    <div v-if="viewMode === 'card'" class="tc-cards">
      <div v-for="task in filteredTasks" :key="task.id" class="task-card" @click="router.push(`/workshop/tasks/${task.id}`)">
        <div class="task-card-top">
          <div>
            <div class="task-card-name">{{ task.name || '未命名任务' }}</div>
            <code class="task-card-subtitle">{{ task.id?.substring(0, 8) }}</code>
          </div>
          <span class="status-badge" :style="{ color: statusCfg(task.currentStatus).color, background: statusCfg(task.currentStatus).bg }">
            {{ statusCfg(task.currentStatus).label }}
          </span>
        </div>
        <div class="task-card-meta">
          <span>默认模板</span>
          <span class="text-caption text-disabled"> · {{ formatTime(task.createdAt) }}</span>
        </div>
        <div class="task-card-actions" @click.stop>
          <v-btn v-if="task.currentStatus !== 'RUNNING'" icon="mdi-pencil" size="x-small" variant="tonal" color="primary" @click="openEditDialog(task)" title="修改" />
          <v-btn v-if="task.currentStatus === 'QUEUED'" icon="mdi-play" size="x-small" variant="tonal" color="success" @click="handleStart(task.id)" title="执行" />
          <v-btn v-if="task.currentStatus === 'COMPLETED' || task.currentStatus === 'FAILED'" icon="mdi-refresh" size="x-small" variant="tonal" color="success" @click="handleStart(task.id)" title="再次执行" />
          <v-btn v-if="task.currentStatus === 'RUNNING'" icon="mdi-stop" size="x-small" variant="tonal" color="error" @click="handleStop(task.id)" title="终止" />
          <v-btn v-if="task.currentStatus !== 'RUNNING'" icon="mdi-delete" size="x-small" variant="tonal" color="error" @click="handleDelete(task.id)" title="删除" />
        </div>
      </div>
      <div v-if="!filteredTasks.length" class="tc-empty">暂无任务</div>
    </div>

    <!-- Table View -->
    <div v-else class="tc-table-wrap">
      <v-progress-linear v-if="taskStore.loading" indeterminate color="primary" class="mb-2" />
      <v-data-table
        :items="filteredTasks"
        :headers="tableHeaders"
        hover
        density="compact"
        @click:row="(_, row: any) => router.push(`/workshop/tasks/${row.item.id}`)"
      >
        <template #item.id="{ item }">
          <code class="cell-id">{{ item.id?.substring(0, 8) }}</code>
        </template>
        <template #item.template>
          <span class="cell-muted">默认模板</span>
        </template>
        <template #item.progress="{ item }">
          <span class="cell-progress">{{ item._progress || '-' }}</span>
        </template>
        <template #item.status="{ item }">
          <span class="status-badge" :style="{ color: statusCfg(item.currentStatus).color, background: statusCfg(item.currentStatus).bg }">
            {{ statusCfg(item.currentStatus).label }}
          </span>
        </template>
        <template #item.createdAt="{ item }">
          <span class="cell-muted">{{ formatTime(item.createdAt) }}</span>
        </template>
        <template #item.actions="{ item }">
          <div class="d-flex ga-1 justify-center">
            <v-btn v-if="item.currentStatus !== 'RUNNING'" icon="mdi-pencil" size="x-small" variant="tonal" color="secondary" @click.stop="openEditDialog(item)" title="修改" />
            <v-btn v-if="item.currentStatus !== 'RUNNING'" icon="mdi-play" size="x-small" variant="tonal" color="success" @click.stop="handleStart(item.id)" title="运行" />
            <v-btn v-if="item.currentStatus === 'RUNNING'" icon="mdi-stop" size="x-small" variant="tonal" color="error" @click.stop="handleStop(item.id)" title="终止" />
            <v-btn v-if="item.currentStatus !== 'RUNNING'" icon="mdi-delete" size="x-small" variant="tonal" color="error" @click.stop="handleDelete(item.id)" title="删除" />
          </div>
        </template>
      </v-data-table>
    </div>

    <!-- ====== NEW TASK DIALOG ====== -->
    <v-dialog v-model="newTaskDialogVisible" max-width="520">
      <v-card>
        <v-card-title>新建任务</v-card-title>
        <v-card-text class="d-flex flex-column" style="gap: 16px;">
          <v-text-field v-model="newTaskForm.name" label="任务名称" placeholder="如：处理科幻小说目录" />
          <v-select
            v-model="newTaskForm.templateId"
            :items="templateOptions.map(t => ({ title: t.name, value: t.id }))"
            label="流水线模板"
            placeholder="选择模板"
            clearable
          />
          <StoragePathSelector
            v-model="newTaskForm.inputPath"
            :label="templateNeeds.needsInput ? '输入路径 *' : '输入路径'"
            selectable-type="both"
            placeholder="选择或输入源文件/目录路径"
          />
          <StoragePathSelector
            v-model="newTaskForm.outputPath"
            :label="templateNeeds.needsOutput ? '输出路径 *' : '输出路径'"
            selectable-type="directory"
            placeholder="选择或输入目标目录路径"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="newTaskDialogVisible = false">取消</v-btn>
          <v-btn color="primary" @click="submitNewTask">创建任务</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- ====== EDIT TASK DIALOG ====== -->
    <v-dialog v-model="editDialogVisible" max-width="520">
      <v-card>
        <v-card-title>修改任务</v-card-title>
        <v-card-text class="d-flex flex-column" style="gap: 16px;">
          <v-text-field v-model="editTaskName" label="任务名称" density="default" placeholder="输入唯一任务名称" />
          <v-select
            v-model="editTaskTemplateId"
            :items="templateOptions.map(t => ({ title: t.name, value: t.id }))"
            label="流水线模板"
            placeholder="不修改"
            clearable
          />
          <StoragePathSelector v-model="editTaskInputPath" label="输入路径" selectable-type="both" />
          <StoragePathSelector v-model="editTaskOutputPath" label="输出路径" selectable-type="directory" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="editDialogVisible = false">取消</v-btn>
          <v-btn color="primary" @click="handleSaveEdit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- ====== TASK DETAIL DRAWER ====== -->
    <v-navigation-drawer v-model="drawerVisible" location="right" temporary width="800">
      <v-toolbar flat density="compact" color="surface">
        <div class="drawer-header">
          <code class="drawer-task-id">任务 {{ drawerTask?.id?.substring(0, 8) }}</code>
          <span v-if="drawerTask" class="status-badge" :style="{ color: statusCfg(drawerTask.currentStatus).color, background: statusCfg(drawerTask.currentStatus).bg }">
            {{ statusCfg(drawerTask.currentStatus).label }}
          </span>
        </div>
        <v-btn icon="mdi-close" variant="plain" size="small" @click="drawerVisible = false" />
      </v-toolbar>

      <v-progress-linear v-if="drawerLoading" indeterminate color="primary" />

      <div class="drawer-body">
        <div class="drawer-grid">
          <!-- LEFT: Trace Tree -->
          <div class="drawer-left">
            <h4 class="section-label">执行溯源</h4>
            <div class="trace-timeline" v-if="drawerLogs.length">
              <div v-for="(log, idx) in drawerLogs" :key="log.id" class="trace-node">
                <div class="trace-rail">
                  <span class="trace-dot" :class="log.status?.toLowerCase()"></span>
                  <span v-if="idx < drawerLogs.length - 1" class="trace-line"></span>
                </div>
                <div class="trace-content">
                  <div class="trace-head">
                    <span class="trace-name">{{ log.nodeName }}</span>
                    <span class="trace-status" :class="log.status?.toLowerCase()">{{ log.status }}</span>
                  </div>
                  <div v-if="log.executionTimeMs" class="trace-meta">{{ log.executionTimeMs }}ms</div>
                  <div v-if="log.errorMessage" class="trace-error">{{ log.errorMessage }}</div>
                  <v-expansion-panels v-if="log.outputPayload" variant="accordion">
                    <v-expansion-panel>
                      <v-expansion-panel-title>Payload</v-expansion-panel-title>
                      <v-expansion-panel-text>
                        <pre class="trace-json">{{ JSON.stringify(log.outputPayload, null, 2) }}</pre>
                      </v-expansion-panel-text>
                    </v-expansion-panel>
                  </v-expansion-panels>
                </div>
              </div>
            </div>
            <div v-else class="trace-empty">暂无执行日志</div>
          </div>

          <!-- RIGHT: Context & Arbitration -->
          <div class="drawer-right">
            <h4 class="section-label">上下文 & 仲裁</h4>

            <!-- Candidates -->
            <div v-if="editForm._candidates?.length" class="arb-section">
              <div class="arb-subtitle">候选结果</div>
              <div class="candidate-list">
                <div
                  v-for="(c, idx) in editForm._candidates"
                  :key="idx"
                  class="candidate-card"
                  :class="{ selected: editForm._selectedCandidate === c }"
                  @click="selectCandidate(c)"
                >
                  <img v-if="c.coverUrl" :src="c.coverUrl" class="cand-cover" />
                  <div class="cand-info">
                    <div class="cand-title">{{ c.title || '-' }}</div>
                    <div class="cand-meta">{{ c.meta || c.doubanUrl || '-' }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Edit Form -->
            <div class="arb-section">
              <div class="arb-subtitle">修正元数据</div>
              <v-text-field v-model="editForm.title" label="标题" density="compact" class="mb-4" />
              <v-text-field v-model="editForm.author" label="作者" density="compact" class="mb-4" />
              <v-textarea v-model="editForm.summary" label="简介" density="compact" :rows="3" class="mb-4" />
              <v-text-field v-model="editForm.coverUrl" label="封面" density="compact" class="mb-4" />
              <v-text-field v-model.number="editForm.publishYear" label="年份" density="compact" type="number" :min="0" :max="2100" hide-details style="width:140px" />
            </div>

            <!-- Actions -->
            <div class="arb-actions">
              <v-btn @click="handleRetry">重试当前节点</v-btn>
              <v-btn color="primary" @click="handleResume">修改后继续</v-btn>
              <v-btn color="error" variant="tonal" @click="handleKill">强制终止</v-btn>
            </div>
          </div>
        </div>
      </div>
    </v-navigation-drawer>
  </div>
</template>

<style scoped>
.task-center {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tc-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid rgb(var(--v-border-color));
  background: rgb(var(--v-theme-surface));
}
.tc-toolbar-left { display: flex; gap: 8px; align-items: center; }

.filter-dot {
  display: inline-block; width: 7px; height: 7px;
  border-radius: 50%; margin-right: 6px; vertical-align: middle;
}

.tc-table-wrap {
  flex: 1;
  overflow: auto;
  padding: 0 20px 20px;
}

/* Card View */
.tc-cards {
  flex: 1;
  overflow: auto;
  padding: 12px 20px 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  align-content: start;
}
.task-card {
  background: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-border-color));
  border-radius: 10px;
  padding: 14px;
  cursor: pointer;
  transition: border-color 0.15s;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.task-card:hover { border-color: rgb(var(--v-theme-primary)); }
.task-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.task-card-name {
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}
.task-card-subtitle {
  font-size: 10px;
  color: rgb(var(--v-theme-secondary));
  font-family: 'JetBrains Mono', monospace;
}
.task-card-meta {
  font-size: 12px;
  color: rgb(var(--v-theme-secondary));
}
.task-card-actions {
  display: flex;
  gap: 4px;
  justify-content: flex-end;
  padding-top: 6px;
  border-top: 1px solid rgb(var(--v-border-color));
}
.tc-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 0;
  color: rgb(var(--v-theme-secondary));
  font-size: 14px;
}

.cell-id { font-family: 'JetBrains Mono', monospace; font-size: 12px; }
.cell-muted { color: rgb(var(--v-theme-secondary)); font-size: 12px; }
.cell-progress { font-family: 'JetBrains Mono', monospace; font-size: 12px; color: rgb(var(--v-theme-secondary)); }

.status-badge {
  display: inline-block; font-size: 11px; font-weight: 600;
  padding: 2px 8px; border-radius: 4px; white-space: nowrap;
}

.drawer-header {
  display: flex; align-items: center; gap: 10px;
}
.drawer-task-id {
  font-family: 'JetBrains Mono', monospace;
  font-size: 14px; font-weight: 600;
}

.drawer-body { height: 100%; overflow: hidden; }

.drawer-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  height: calc(100vh - 64px);
}

.drawer-left, .drawer-right {
  padding: 16px 18px;
  overflow-y: auto;
}
.drawer-left { border-right: 1px solid rgb(var(--v-border-color)); }

.section-label {
  font-size: 10px; font-weight: 700; text-transform: uppercase;
  letter-spacing: 1px; color: rgb(var(--v-theme-secondary)); margin: 0 0 14px;
}

.trace-timeline { display: flex; flex-direction: column; }

.trace-node {
  display: flex; gap: 12px; min-height: 48px;
}

.trace-rail {
  display: flex; flex-direction: column; align-items: center;
  width: 16px; flex-shrink: 0;
}
.trace-dot {
  width: 10px; height: 10px; border-radius: 50%;
  flex-shrink: 0; border: 2px solid rgb(var(--v-theme-surface-variant));
}
.trace-dot.success { background: rgb(var(--v-theme-success)); }
.trace-dot.failed { background: rgb(var(--v-theme-error)); }
.trace-dot.skipped { background: rgb(var(--v-theme-secondary)); }

.trace-line {
  width: 2px; flex: 1; background: rgb(var(--v-border-color)); margin: 2px 0;
}

.trace-content { flex: 1; padding-bottom: 14px; }
.trace-head {
  display: flex; align-items: center; gap: 8px; margin-bottom: 4px;
}
.trace-name { font-size: 13px; font-weight: 600; }
.trace-status {
  font-size: 10px; font-weight: 700; padding: 1px 6px; border-radius: 3px;
  text-transform: uppercase;
}
.trace-status.success { background: rgba(var(--v-theme-success), 0.12); color: rgb(var(--v-theme-success)); }
.trace-status.failed { background: rgba(var(--v-theme-error), 0.12); color: rgb(var(--v-theme-error)); }
.trace-status.skipped { background: rgba(var(--v-theme-secondary), 0.12); color: rgb(var(--v-theme-secondary)); }

.trace-meta { font-size: 11px; color: rgb(var(--v-theme-secondary)); margin-bottom: 4px; }
.trace-error { font-size: 12px; color: rgb(var(--v-theme-error)); margin-bottom: 6px; }
.trace-json {
  background: rgb(var(--v-theme-background));
  padding: 8px 10px; border-radius: 6px; font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
  max-height: 200px; overflow: auto; white-space: pre-wrap;
}
.trace-empty { color: rgb(var(--v-theme-secondary)); font-size: 13px; }

.arb-section { margin-bottom: 18px; }
.arb-subtitle {
  font-size: 11px; font-weight: 600; color: rgb(var(--v-theme-secondary));
  text-transform: uppercase; letter-spacing: 0.6px; margin-bottom: 8px;
}

.candidate-list { display: flex; flex-direction: column; gap: 6px; }
.candidate-card {
  display: flex; gap: 10px; padding: 8px 10px;
  border-radius: 8px; border: 1px solid rgb(var(--v-border-color));
  background: rgb(var(--v-theme-surface-variant)); cursor: pointer; transition: all 0.15s;
}
.candidate-card:hover { border-color: rgb(var(--v-theme-primary)); }
.candidate-card.selected { border-color: rgb(var(--v-theme-primary)); background: rgba(var(--v-theme-primary), 0.1); }

.cand-cover { width: 36px; height: 50px; border-radius: 4px; object-fit: cover; flex-shrink: 0; }
.cand-info { flex: 1; min-width: 0; }
.cand-title { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cand-meta { font-size: 11px; color: rgb(var(--v-theme-secondary)); margin-top: 2px; }

.arb-actions {
  display: flex; gap: 8px; padding-top: 12px;
  border-top: 1px solid rgb(var(--v-border-color));
}
</style>
