import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAssets, getAssetDetail, updateAsset } from '../api/modules'

export const useAssetStore = defineStore('asset', () => {
  const assets = ref<any[]>([])
  const currentAsset = ref<any>(null)
  const loading = ref(false)

  async function fetchAssets() {
    loading.value = true
    try {
      const { data } = await getAssets()
      assets.value = data
    } finally {
      loading.value = false
    }
  }

  async function fetchAssetDetail(id: string) {
    loading.value = true
    try {
      const { data } = await getAssetDetail(id)
      currentAsset.value = data
    } finally {
      loading.value = false
    }
  }

  async function updateAssetDetail(id: string, data: any) {
    const { data: updated } = await updateAsset(id, data)
    await fetchAssetDetail(id)
    return updated
  }

  return { assets, currentAsset, loading, fetchAssets, fetchAssetDetail, updateAssetDetail }
})
