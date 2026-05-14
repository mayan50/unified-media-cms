import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTasks } from '../api/modules'

export const useJobStore = defineStore('job', () => {
  const jobs = ref<any[]>([])
  const loading = ref(false)

  async function fetchJobs(status?: string) {
    loading.value = true
    try {
      const params: any = {}
      if (status) params.status = status
      const { data } = await getTasks(params)
      jobs.value = data.items || data
    } finally {
      loading.value = false
    }
  }

  return { jobs, loading, fetchJobs }
})
