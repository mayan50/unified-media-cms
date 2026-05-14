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
export const updateAssetFull = (id: string, data: any) => api.put(`/assets/${id}/full`, data)
export const deleteAsset = (id: string) => api.delete(`/assets/${id}`)

// === Jobs ===
export const getJobs = (params?: any) => api.get('/jobs', { params })
export const getJob = (id: string) => api.get(`/jobs/${id}`)
export const createJob = (data: any) => api.post('/jobs', data)
export const startJob = (id: string) => api.post(`/jobs/${id}/start`)
export const continueJob = (id: string) => api.post(`/jobs/${id}/continue`)
export const stopJob = (id: string) => api.post(`/jobs/${id}/stop`)
export const resetJob = (id: string) => api.post(`/jobs/${id}/reset`)
export const updateJob = (id: string, data: any) => api.put(`/jobs/${id}`, data)
export const deleteJob = (id: string, params?: any) => api.delete(`/jobs/${id}`, { params })

// === Tasks ===
export const getJobTasks = (jobId: string, page = 1, size = 50) => api.get(`/jobs/${jobId}/tasks`, { params: { page, size } })
export const getTasks = (params?: any) => api.get('/tasks', { params })
export const getTask = (id: string) => api.get(`/tasks/${id}`)
export const getTaskDetail = (id: string) => api.get(`/tasks/${id}/detail`)
export const getTaskLogs = (taskId: string) => api.get(`/tasks/${taskId}/logs`)
export const retryTask = (id: string) => api.post(`/tasks/${id}/retry`)
export const deleteTask = (taskId: string, deleteAsset = false, deleteSourceFiles = false) =>
  api.delete(`/tasks/${taskId}`, { params: { deleteAsset, deleteSourceFiles } })


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
export const createCategory = (data: any) => api.post('/categories', data)
export const updateCategory = (id: string, data: any) => api.put(`/categories/${id}`, data)
export const deleteCategory = (id: string) => api.delete(`/categories/${id}`)
export const createTag = (data: any) => api.post('/tags', data)
export const updateTag = (id: string, data: any) => api.put(`/tags/${id}`, data)
export const deleteTag = (id: string) => api.delete(`/tags/${id}`)

// === Languages & Creators ===
export const getLanguages = () => api.get('/languages')
export const getCreators = (params?: any) => api.get('/creators', { params })
