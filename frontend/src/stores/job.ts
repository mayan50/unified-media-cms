import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getJobs } from '../api/modules'

export const useJobStore = defineStore('job', () => {
  const jobs = ref<any[]>([])
  const loading = ref(false)

  async function fetchJobs() {
    loading.value = true
    try {
      const { data } = await getJobs()
      jobs.value = data.items || data
    } finally {
      loading.value = false
    }
  }

  return { jobs, loading, fetchJobs }
})
