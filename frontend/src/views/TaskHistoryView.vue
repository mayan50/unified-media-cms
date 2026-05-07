<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTaskStore } from '../stores/task'
import { startTask, stopTask, deleteTask, updateTask } from '../api/modules'
import { useToast } from '../composables/useToast'

const { toast } = useToast()
const taskStore = useTaskStore()
const router = useRouter()

const viewMode = ref<'card' | 'table'>('card')

onMounted(() => taskStore.fetchTasks())

const filteredTasks = computed(() => {
  // 任务记录默认只显示已完成和失败的历史任务
  return taskStore.tasks.filter((t: any) =>
    t.currentStatus === 'COMPLETED' || t.currentStatus === 'FAILED'
  )
})

const STATUS_CFG: Record<string, { label: string; color: string; bg: string }> = {
  QUEUED: { label: '未执行', color: '#8e92a8', bg: 'rgba(142,146,168,0.12)' },
  RUNNING: { label: '执行中', color: '#5b8def', bg: 'rgba(91,141,239,0.12)' },
  COMPLETED: { label: '已完成', color: '#3dd68c', bg: 'rgba(61,214,140,0.12)' },
  PENDING_MANUAL: { label: '待仲裁', color: '#f06580', bg: 'rgba(240,101,128,0.12)' },
  FAILED: { label: '失败', color: '#f06580', bg: 'rgba(240,101,128,0.12)' },
}
function sc(status: string) { return STATUS_CFG[status] || { label: status, color: '#8e92a8', bg: 'rgba(142,146,168,0.12)' } }
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

// ---- Edit ----
const editDialogVisible = ref(false)
const editTaskId = ref('')
const editTaskName = ref('')
function openEditDialog(task: any) {
  editTaskId.value = task.id; editTaskName.value = task.name || ''; editDialogVisible.value = true
}
async function handleSaveEdit() {
  if (!editTaskName.value.trim()) { toast.warning('名称不能为空'); return }
  try { await updateTask(editTaskId.value, { name: editTaskName.value.trim() }); editDialogVisible.value = false; toast.success('已更新'); taskStore.fetchTasks() }
  catch { toast.error('更新失败') }
}
async function handleStart(taskId: string) {
  try { await startTask(taskId); toast.success('已重新执行'); taskStore.fetchTasks() } catch { toast.error('启动失败') }
}
async function handleDelete(taskId: string) {
  try { await deleteTask(taskId); toast.success('已删除'); taskStore.fetchTasks() } catch { toast.error('删除失败') }
}
</script>

<template>
  <div class="task-records">
    <div class="tr-toolbar">
      <div class="tr-toolbar-left">
        <div class="view-toggle">
          <v-btn icon="mdi-view-grid" size="small" :variant="viewMode === 'card' ? 'tonal' : 'plain'" :color="viewMode === 'card' ? 'primary' : undefined" @click="viewMode = 'card'" />
          <v-btn icon="mdi-view-list" size="small" :variant="viewMode === 'table' ? 'tonal' : 'plain'" :color="viewMode === 'table' ? 'primary' : undefined" @click="viewMode = 'table'" />
        </div>
        <span class="text-caption text-disabled">已完成 / 失败的任务记录</span>
      </div>
    </div>

    <!-- Card View -->
    <div v-if="viewMode === 'card'" class="tc-cards">
      <div v-for="task in filteredTasks" :key="task.id" class="task-card" @click="router.push(`/workshop/records/${task.id}`)">
        <div class="task-card-top">
          <div>
            <div class="task-card-name">{{ task.name || '未命名任务' }}</div>
            <code class="task-card-subtitle">{{ task.id?.substring(0, 8) }}</code>
          </div>
          <span class="status-badge" :style="{ color: sc(task.currentStatus).color, background: sc(task.currentStatus).bg }">{{ sc(task.currentStatus).label }}</span>
        </div>
        <div class="task-card-meta">
          <span>默认模板</span>
          <span class="text-caption text-disabled"> · {{ formatTime(task.createdAt) }}</span>
        </div>
        <div class="task-card-actions" @click.stop>
          <v-btn icon="mdi-pencil" size="x-small" variant="tonal" color="secondary" @click="openEditDialog(task)" title="修改" />
          <v-btn icon="mdi-refresh" size="x-small" variant="tonal" color="success" @click="handleStart(task.id)" title="重新执行" />
          <v-btn icon="mdi-delete" size="x-small" variant="tonal" color="error" @click="handleDelete(task.id)" title="删除" />
        </div>
      </div>
      <div v-if="!filteredTasks.length" class="tc-empty">暂无历史记录</div>
    </div>

    <!-- Edit Dialog -->
    <v-dialog v-model="editDialogVisible" max-width="400">
      <v-card>
        <v-card-title>修改任务</v-card-title>
        <v-card-text><v-text-field v-model="editTaskName" label="任务名称" density="default" /></v-card-text>
        <v-card-actions>
          <v-spacer /><v-btn variant="text" @click="editDialogVisible = false">取消</v-btn>
          <v-btn color="primary" @click="handleSaveEdit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.task-records {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.tr-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid rgb(var(--v-border-color));
  background: rgb(var(--v-theme-surface));
}
.tr-toolbar-left { display: flex; gap: 12px; align-items: center; }

/* Reuse card styles from TaskCenter */
.tc-cards {
  flex: 1; overflow: auto; padding: 12px 20px 20px;
  display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px; align-content: start;
}
.task-card {
  background: rgb(var(--v-theme-surface)); border: 1px solid rgb(var(--v-border-color));
  border-radius: 10px; padding: 14px; cursor: pointer; transition: border-color 0.15s;
  display: flex; flex-direction: column; gap: 10px;
}
.task-card:hover { border-color: rgb(var(--v-theme-primary)); }
.task-card-top { display: flex; justify-content: space-between; align-items: center; }
.task-card-name {
  font-size: 14px; font-weight: 600; overflow: hidden; text-overflow: ellipsis;
  white-space: nowrap; max-width: 180px;
}
.task-card-subtitle { font-size: 10px; color: rgb(var(--v-theme-secondary)); font-family: 'JetBrains Mono', monospace; }
.task-card-meta { font-size: 12px; color: rgb(var(--v-theme-secondary)); }
.task-card-actions { display: flex; gap: 4px; justify-content: flex-end; padding-top: 6px; border-top: 1px solid rgb(var(--v-border-color)); }
.tc-empty { grid-column: 1 / -1; text-align: center; padding: 60px 0; color: rgb(var(--v-theme-secondary)); font-size: 14px; }
.status-badge { display: inline-block; font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; white-space: nowrap; }
</style>
