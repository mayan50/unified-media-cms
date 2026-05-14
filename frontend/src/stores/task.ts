import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTasks, getTaskLogs, submitTask, resumeTask } from '../api/modules'

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<any[]>([])
  const currentTaskLogs = ref<any[]>([])
  const loading = ref(false)

  async function fetchTasks(status?: string) {
    loading.value = true
    try {
      const params: any = {}
      if (status) params.status = status
      const { data } = await getTasks(params)
      tasks.value = data.items || data
    } finally {
      loading.value = false
    }
  }

  async function fetchTaskLogs(taskId: string) {
    const { data } = await getTaskLogs(taskId)
    currentTaskLogs.value = data
  }

  async function submitNewTask(storageNodeId: string, relativePath: string, templateName?: string) {
    const { data } = await submitTask({ storageNodeId, relativePath, templateName })
    await fetchTasks()
    return data
  }

  async function resumeTaskAction(taskId: string, mergedPayload: any) {
    const { data } = await resumeTask(taskId, { mergedPayload })
    await fetchTasks()
    return data
  }

  return { tasks, currentTaskLogs, loading, fetchTasks, fetchTaskLogs, submitNewTask, resumeTaskAction }
})
