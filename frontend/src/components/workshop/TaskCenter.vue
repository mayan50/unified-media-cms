<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTaskStore } from '../../stores/task'
import { getStorageNodes, getJobs, getTemplates, createJob, startJob, continueJob, stopJob, resetJob, deleteJob, updateJob, getJobTasks } from '../../api/modules'
import StoragePathSelector from './StoragePathSelector.vue'
import { useToast } from '../../composables/useToast'

const { toast } = useToast()
const taskStore = useTaskStore()
const router = useRouter()

// State
const viewMode = ref<'card' | 'table'>('card')
const searchKeyword = ref('')
const filterStatus = ref<string[]>([])
const filterTemplate = ref('')
const templateOptions = ref<{ id: string; name: string }[]>([])
const loading = ref(false)

// Pagination
const page = ref(1)
const pageSize = 20

// Dialogs
const newTaskDialogVisible = ref(false)
const editDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const editTaskId = ref('')
const editTaskName = ref('')
const editInputPath = ref<StoragePathValue>({ storage_node_id: '', path: '' })
const editOutputPath = ref<StoragePathValue>({ storage_node_id: '', path: '' })
const deleteJobId = ref('')

interface StoragePathValue { storage_node_id: string; path: string }
const newTaskForm = ref({
  name: '', templateId: '' as string | undefined,
  inputPath: { storage_node_id: '', path: '' } as StoragePathValue,
  outputPath: { storage_node_id: '', path: '' } as StoragePathValue,
})

const STATUS_CFG: Record<string, { label: string; color: string; bg: string }> = {
  PENDING: { label: '待执行', color: '#8e92a8', bg: 'rgba(142,146,168,0.12)' },
  QUEUED: { label: '待执行', color: '#8e92a8', bg: 'rgba(142,146,168,0.12)' },
  RUNNING: { label: '执行中', color: '#5b8def', bg: 'rgba(91,141,239,0.12)' },
  COMPLETED: { label: '已完成', color: '#3dd68c', bg: 'rgba(61,214,140,0.12)' },
  FAILED: { label: '失败', color: '#f06580', bg: 'rgba(240,101,128,0.12)' },
}
const statusItems = Object.entries(STATUS_CFG).map(([v, c]) => ({ title: c.label, value: v }))

function sc(s: string) { return STATUS_CFG[s] || { label: s, color: '#8e92a8', bg: 'rgba(142,146,168,0.12)' } }
function formatTime(d: string) {
  if (!d) return '-'
  const diff = Date.now() - new Date(d).getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + 'm'
  if (diff < 86400000) return Math.floor(diff / 3600000) + 'h'
  return new Date(d).toLocaleDateString('zh-CN')
}

const totalItems = ref(0)

async function fetchJobs() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize, sort: 'createdAt,desc' }
    if (searchKeyword.value) params.search = searchKeyword.value
    if (filterStatus.value.length) params.status = filterStatus.value[0]
    const { data } = await getJobs(params)
    taskStore.jobs = data.items || []
    totalItems.value = data.total || 0
  } finally { loading.value = false }
}

onMounted(async () => {
  try {
    const [_, tplRes] = await Promise.all([fetchJobs(), getTemplates()])
    templateOptions.value = tplRes.data
  } catch { loading.value = false }
})

function onSearch() { page.value = 1; fetchJobs() }
const totalPages = computed(() => Math.max(1, Math.ceil(totalItems.value / pageSize)))
const pagedJobs = computed(() => taskStore.jobs || [])

// New job
const templateNeeds = computed(() => {
  const tpl = templateOptions.value.find(t => t.id === newTaskForm.value.templateId)
  if (!tpl || !(tpl as any).graphPayload) return { needsInput: false, needsOutput: false }
  const nodes = (tpl as any).graphPayload?.nodes || []
  return { needsInput: nodes.some((n: any) => n.name === 'FileSnifferNode'), needsOutput: nodes.some((n: any) => n.name === 'ArchiveNode') }
})

async function submitNewTask() {
  const needs = templateNeeds.value
  if (needs.needsInput && !newTaskForm.value.inputPath.path) { toast.warning('需要输入路径'); return }
  if (needs.needsOutput && !newTaskForm.value.outputPath.path) { toast.warning('需要输出路径'); return }
  try {
    const fp = newTaskForm.value
    await createJob({ name: fp.name || undefined, templateId: fp.templateId || undefined, inputPath: fp.inputPath.path ? fp.inputPath : undefined, outputPath: fp.outputPath.path ? fp.outputPath : undefined })
    newTaskDialogVisible.value = false; toast.success('作业已创建')
    newTaskForm.value = { name: '', templateId: undefined, inputPath: { storage_node_id: '', path: '' }, outputPath: { storage_node_id: '', path: '' } }
    fetchJobs()
  } catch { toast.error('创建失败') }
}

// Actions
async function handleStart(id: string) { try { await startJob(id); toast.success('已启动'); fetchJobs() } catch { toast.error('失败') } }
async function handleContinue(id: string) { try { await continueJob(id); toast.success('继续执行'); fetchJobs() } catch { toast.error('失败') } }
async function handleStop(id: string) { try { await stopJob(id); toast.success('已终止'); fetchJobs() } catch { toast.error('失败') } }
async function handleReset(id: string) { try { await resetJob(id); toast.success('已重置'); fetchJobs() } catch { toast.error('失败') } }
function openEditDialog(j: any) {
  editTaskId.value = j.id; editTaskName.value = j.name || ''
  editInputPath.value = j.inputPath || { storage_node_id: '', path: '' }
  editOutputPath.value = j.outputPath || { storage_node_id: '', path: '' }
  editDialogVisible.value = true
}
async function handleSaveEdit() {
  if (!editTaskName.value.trim()) { toast.warning('名称不能为空'); return }
  try {
    await updateJob(editTaskId.value, {
      name: editTaskName.value.trim(),
      inputPathText: editInputPath.value.path?.trim() || null,
      outputPathText: editOutputPath.value.path?.trim() || null,
    })
    editDialogVisible.value = false; toast.success('已更新'); fetchJobs()
  } catch { toast.error('更新失败，名称可能重复') }
}
function openDeleteDialog(id: string) { deleteJobId.value = id; deleteDialogVisible.value = true }
async function handleDelete() { try { await deleteJob(deleteJobId.value); deleteDialogVisible.value = false; toast.success('已删除'); fetchJobs() } catch { toast.error('删除失败') } }

const tableHeaders = [
  { title: '作业名称', key: 'name', minWidth: 160 },
  { title: '状态', key: 'status', width: 90 },
  { title: '时间', key: 'createdAt', width: 100 },
  { title: '操作', key: 'actions', width: 200, align: 'center' as const, sortable: false },
]
</script>

<template>
  <div class="task-center">
    <div class="tc-toolbar">
      <div class="tc-toolbar-left">
        <div class="view-toggle mr-3">
          <v-btn icon="mdi-view-grid" size="small" :variant="viewMode === 'card' ? 'tonal' : 'plain'" :color="viewMode === 'card' ? 'primary' : undefined" @click="viewMode = 'card'" />
          <v-btn icon="mdi-view-list" size="small" :variant="viewMode === 'table' ? 'tonal' : 'plain'" :color="viewMode === 'table' ? 'primary' : undefined" @click="viewMode = 'table'" />
        </div>
        <v-text-field v-model="searchKeyword" placeholder="搜索作业名称" prepend-inner-icon="mdi-magnify" density="compact" hide-details clearable style="width:200px" @keyup.enter="onSearch" @click:clear="onSearch" />
        <v-select v-model="filterStatus" :items="statusItems" multiple chips placeholder="状态" density="compact" hide-details clearable style="width:160px" @update:model-value="onSearch" />
      </div>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="newTaskDialogVisible = true">新建作业</v-btn>
    </div>

    <v-progress-linear v-if="loading" indeterminate color="primary" />

    <!-- Card View -->
    <div v-if="viewMode === 'card'" class="tc-cards">
      <div v-for="job in pagedJobs" :key="job.id" class="task-card" @click="router.push(`/workshop/tasks/${job.id}`)">
        <div class="task-card-top">
          <div>
            <div class="task-card-name">{{ job.name || '未命名作业' }}</div>
            <code class="task-card-subtitle">{{ job.id?.substring(0, 8) }}</code>
          </div>
          <span class="status-badge" :style="{ color: sc(job.status).color, background: sc(job.status).bg }">{{ sc(job.status).label }}</span>
        </div>
        <div class="task-card-meta"><span>{{ formatTime(job.createdAt) }}</span></div>
        <div class="task-card-actions" @click.stop>
          <v-btn v-if="job.status !== 'RUNNING'" icon="mdi-pencil" size="x-small" variant="tonal" color="primary" @click="openEditDialog(job)" title="修改" />
          <v-btn v-if="job.status === 'PENDING' || job.status === 'QUEUED'" icon="mdi-play" size="x-small" variant="tonal" color="success" @click="handleStart(job.id)" title="执行" />
          <v-btn v-if="job.status === 'RUNNING'" icon="mdi-play-circle" size="x-small" variant="tonal" color="info" @click="handleContinue(job.id)" title="继续" />
          <v-btn v-if="job.status === 'COMPLETED' || job.status === 'FAILED'" icon="mdi-refresh" size="x-small" variant="tonal" color="success" @click="handleStart(job.id)" title="重新执行" />
          <v-btn v-if="job.status !== 'RUNNING'" icon="mdi-restart" size="x-small" variant="tonal" color="warning" @click="handleReset(job.id)" title="重置" />
          <v-btn v-if="job.status === 'RUNNING'" icon="mdi-stop" size="x-small" variant="tonal" color="error" @click="handleStop(job.id)" title="终止" />
          <v-btn v-if="job.status !== 'RUNNING'" icon="mdi-delete" size="x-small" variant="tonal" color="error" @click="openDeleteDialog(job.id)" title="删除" />
        </div>
      </div>
      <div v-if="!pagedJobs.length" class="tc-empty">暂无作业</div>
    </div>

    <!-- Table View -->
    <div v-else class="tc-table-wrap">
      <v-data-table :items="pagedJobs" :headers="tableHeaders" hover density="compact" @click:row="(_, row: any) => router.push(`/workshop/tasks/${row.item.id}`)">
        <template #item.name="{ item }"><span class="fw-medium">{{ item.name || '未命名作业' }}</span></template>
        <template #item.status="{ item }"><span class="status-badge" :style="{ color: sc(item.status).color, background: sc(item.status).bg }">{{ sc(item.status).label }}</span></template>
        <template #item.createdAt="{ item }"><span class="text-caption">{{ formatTime(item.createdAt) }}</span></template>
        <template #item.actions="{ item }">
          <div class="d-flex ga-1" @click.stop>
            <v-btn v-if="item.status !== 'RUNNING'" icon="mdi-pencil" size="x-small" variant="plain" color="primary" @click="openEditDialog(item)" title="修改" />
            <v-btn v-if="item.status === 'PENDING' || item.status === 'QUEUED'" icon="mdi-play" size="x-small" variant="plain" color="success" @click="handleStart(item.id)" title="执行" />
            <v-btn v-if="item.status === 'RUNNING'" icon="mdi-stop" size="x-small" variant="plain" color="error" @click="handleStop(item.id)" title="终止" />
            <v-btn v-if="item.status === 'COMPLETED' || item.status === 'FAILED'" icon="mdi-refresh" size="x-small" variant="plain" color="success" @click="handleStart(item.id)" title="重跑" />
            <v-btn v-if="item.status !== 'RUNNING'" icon="mdi-delete" size="x-small" variant="plain" color="error" @click="openDeleteDialog(item.id)" title="删除" />
          </div>
        </template>
      </v-data-table>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="tc-pagination">
      <v-btn icon="mdi-chevron-left" size="small" variant="plain" :disabled="page <= 1" @click="page--; fetchJobs()" />
      <span class="text-caption mx-2">{{ page }} / {{ totalPages }} ({{ totalItems }}项)</span>
      <v-btn icon="mdi-chevron-right" size="small" variant="plain" :disabled="page >= totalPages" @click="page++; fetchJobs()" />
    </div>

    <!-- New Job Dialog -->
    <v-dialog v-model="newTaskDialogVisible" max-width="520">
      <v-card><v-card-title>新建作业</v-card-title>
        <v-card-text class="d-flex flex-column" style="gap: 16px">
          <v-text-field v-model="newTaskForm.name" label="作业名称" placeholder="如：处理科幻小说目录" />
          <v-select v-model="newTaskForm.templateId" :items="templateOptions.map(t => ({ title: t.name, value: t.id }))" label="流水线模板" placeholder="选择模板" clearable />
          <StoragePathSelector v-model="newTaskForm.inputPath" :label="templateNeeds.needsInput ? '输入路径 *' : '输入路径'" selectable-type="both" />
          <StoragePathSelector v-model="newTaskForm.outputPath" :label="templateNeeds.needsOutput ? '输出路径 *' : '输出路径'" selectable-type="directory" />
        </v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="newTaskDialogVisible = false">取消</v-btn><v-btn color="primary" @click="submitNewTask">创建作业</v-btn></v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Edit Dialog -->
    <v-dialog v-model="editDialogVisible" max-width="480">
      <v-card><v-card-title>修改作业</v-card-title>
        <v-card-text class="d-flex flex-column" style="gap: 16px">
          <v-text-field v-model="editTaskName" label="作业名称" density="compact" hide-details />
          <StoragePathSelector v-model="editInputPath" label="输入路径" selectable-type="both" />
          <StoragePathSelector v-model="editOutputPath" label="输出路径" selectable-type="directory" />
        </v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="editDialogVisible = false">取消</v-btn><v-btn color="primary" @click="handleSaveEdit">保存</v-btn></v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Delete Dialog -->
    <v-dialog v-model="deleteDialogVisible" max-width="400" persistent>
      <v-card><v-card-title>删除作业</v-card-title>
        <v-card-text>确定删除？关联任务记录将一并删除。</v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="deleteDialogVisible = false">取消</v-btn><v-btn color="error" @click="handleDelete">删除</v-btn></v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.task-center { height: 100%; display: flex; flex-direction: column; }
.tc-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 12px 20px; border-bottom: 1px solid rgb(var(--v-border-color)); background: rgb(var(--v-theme-surface)); }
.tc-toolbar-left { display: flex; gap: 8px; align-items: center; }
.tc-cards { flex: 1; overflow: auto; padding: 12px 20px 20px; display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; align-content: start; }
.task-card { background: rgb(var(--v-theme-surface)); border: 1px solid rgb(var(--v-border-color)); border-radius: 10px; padding: 14px; cursor: pointer; transition: border-color .15s; display: flex; flex-direction: column; gap: 10px; }
.task-card:hover { border-color: rgb(var(--v-theme-primary)); }
.task-card-top { display: flex; justify-content: space-between; align-items: flex-start; }
.task-card-name { font-size: 14px; font-weight: 600; max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-card-subtitle { font-size: 10px; color: rgb(var(--v-theme-secondary)); font-family: 'JetBrains Mono', monospace; }
.task-card-meta { font-size: 12px; color: rgb(var(--v-theme-secondary)); }
.task-card-actions { display: flex; gap: 4px; justify-content: flex-end; padding-top: 6px; border-top: 1px solid rgb(var(--v-border-color)); }
.tc-empty { grid-column: 1 / -1; text-align: center; padding: 60px 0; color: rgb(var(--v-theme-secondary)); font-size: 14px; }
.tc-table-wrap { flex: 1; overflow: auto; padding: 0 20px 20px; }
.tc-pagination { display: flex; justify-content: center; align-items: center; padding: 8px; border-top: 1px solid rgb(var(--v-border-color)); background: rgb(var(--v-theme-surface)); }
.status-badge { display: inline-block; font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; white-space: nowrap; }
.fw-medium { font-weight: 500; }
</style>
