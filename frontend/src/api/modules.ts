import api from './index'

// === Storage Nodes ===
export const getStorageNodes = () => api.get('/storage-nodes')
export const getStorageNode = (id: string) => api.get(`/storage-nodes/${id}`)
export const createStorageNode = (data: any) => api.post('/storage-nodes', data)
export const updateStorageNode = (id: string, data: any) => api.put(`/storage-nodes/${id}`, data)
export const deleteStorageNode = (id: string) => api.delete(`/storage-nodes/${id}`)
export const browseStorageNode = (id: string, path: string = '/') => api.get(`/storage-nodes/${id}/browse`, { params: { path } })

// === Settings / LLM ===
export const getLlmConfig = () => api.get('/settings/llm')
export const saveLlmConfig = (data: any) => api.post('/settings/llm', data)

// === Assets ===
export const getAssets = (params?: any) => api.get('/assets', { params })
export const getAssetDetail = (id: string) => api.get(`/assets/${id}`)
export const updateAsset = (id: string, data: any) => api.put(`/assets/${id}`, data)

// === Tasks ===
export const getTasks = (params?: any) => api.get('/tasks', { params })
export const submitTask = (data: any) => api.post('/tasks/submit', data)
export const startTask = (taskId: string) => api.post(`/tasks/${taskId}/start`)
export const stopTask = (taskId: string) => api.post(`/tasks/${taskId}/stop`)
export const deleteTask = (taskId: string) => api.delete(`/tasks/${taskId}`)
export const updateTask = (taskId: string, data: any) => api.put(`/tasks/${taskId}`, data)
export const resumeTask = (taskId: string, data: any) => api.post(`/tasks/${taskId}/resume`, data)
export const getTaskLogs = (taskId: string) => api.get(`/tasks/${taskId}/logs`)

// === Nodes ===
export const getAvailableNodes = () => api.get('/nodes')
export const getNodeRegistry = () => api.get('/nodes/registry')

// === Templates ===
export const getTemplates = () => api.get('/templates')
export const createTemplate = (data: any) => api.post('/templates', data)
export const updateTemplate = (id: string, data: any) => api.put(`/templates/${id}`, data)
export const deleteTemplate = (id: string) => api.delete(`/templates/${id}`)

// === Categories & Tags ===
export const getCategories = () => api.get('/categories')
export const getTags = () => api.get('/tags')
