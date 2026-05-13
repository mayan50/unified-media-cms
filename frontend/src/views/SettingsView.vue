<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStorageNodes, createStorageNode, updateStorageNode, deleteStorageNode, getLlmConfig, saveLlmConfig } from '../api/modules'
import { useToast } from '../composables/useToast'

const { toast } = useToast()
const router = useRouter()

const storageNodes = ref<any[]>([])
const storageLoading = ref(false)

// New/Edit storage dialog
const storageDialogVisible = ref(false)
const editNodeId = ref<string | null>(null)
const storageForm = ref({ name: '', providerType: 'LOCAL', isReadonly: false })
const localBasePath = ref('/')
const minioConfig = ref({ endpoint: '', bucket: '', accessKey: '', secretKey: '' })

const confirmDialogVisible = ref(false)
const confirmDeleteId = ref('')

const llmConfig = ref({ provider: 'ollama', baseUrl: '', apiKey: '', modelName: '' })

async function fetchStorageNodes() {
  storageLoading.value = true
  try { const { data } = await getStorageNodes(); storageNodes.value = data }
  finally { storageLoading.value = false }
}

function openNewDialog() {
  editNodeId.value = null
  storageForm.value = { name: '', providerType: 'LOCAL', isReadonly: false }
  localBasePath.value = '/'
  minioConfig.value = { endpoint: '', bucket: '', accessKey: '', secretKey: '' }
  storageDialogVisible.value = true
}

function openEditDialog(node: any) {
  editNodeId.value = node.id
  storageForm.value = { name: node.name, providerType: node.providerType, isReadonly: node.isReadonly }
  const config = typeof node.connectionConfig === 'string' ? JSON.parse(node.connectionConfig) : node.connectionConfig
  if (node.providerType === 'LOCAL') {
    localBasePath.value = config?.basePath || '/'
  } else {
    minioConfig.value = { endpoint: config?.endpoint || '', bucket: config?.bucket || '', accessKey: config?.accessKey || '', secretKey: config?.secretKey || '' }
  }
  storageDialogVisible.value = true
}

async function saveStorageNode() {
  let config: any
  if (storageForm.value.providerType === 'LOCAL') {
    config = { basePath: localBasePath.value }
  } else {
    config = { ...minioConfig.value }
  }
  try {
    if (editNodeId.value) {
      await updateStorageNode(editNodeId.value, {
        name: storageForm.value.name, providerType: storageForm.value.providerType,
        connectionConfig: config, isReadonly: storageForm.value.isReadonly
      })
      toast.success('存储节点已更新')
    } else {
      await createStorageNode({
        name: storageForm.value.name, providerType: storageForm.value.providerType,
        connectionConfig: config, isReadonly: storageForm.value.isReadonly
      })
      toast.success('存储节点已创建')
    }
    storageDialogVisible.value = false
    fetchStorageNodes()
  } catch { toast.error('保存失败') }
}

function askDeleteNode(id: string) {
  confirmDeleteId.value = id; confirmDialogVisible.value = true
}

async function removeStorageNodeConfirmed() {
  try { await deleteStorageNode(confirmDeleteId.value); toast.success('已删除'); fetchStorageNodes() }
  catch { /* */ }
  confirmDialogVisible.value = false
}

async function fetchLlmConfig() {
  try {
    const { data } = await getLlmConfig()
    if (data && Object.keys(data).length) llmConfig.value = { ...llmConfig.value, ...data }
  } catch { /* */ }
}

async function saveLlmConfigAction() {
  try { await saveLlmConfig(llmConfig.value); toast.success('AI 配置已保存') }
  catch { toast.error('保存失败') }
}

onMounted(() => { fetchStorageNodes(); fetchLlmConfig() })
</script>

<template>
  <div class="settings-page">
    <div class="sp-header">
      <v-btn icon="mdi-arrow-left" size="small" variant="plain" @click="router.push('/')" />
      <h2>系统设置</h2>
    </div>

    <div class="sp-body">
      <!-- Storage Nodes -->
      <section class="sp-section">
        <div class="sp-section-header">
          <h3>存储节点</h3>
          <v-btn color="primary" size="small" prepend-icon="mdi-plus" @click="openNewDialog">新建</v-btn>
        </div>
        <v-progress-linear v-if="storageLoading" indeterminate color="primary" class="mb-3" />
        <div v-if="storageNodes.length" class="sp-card-grid">
          <div v-for="node in storageNodes" :key="node.id" class="sp-card">
            <div class="sp-card-top">
              <div>
                <div class="sp-card-name">{{ node.name }}</div>
                <v-chip size="x-small" :color="node.providerType === 'LOCAL' ? 'primary' : 'warning'" variant="tonal">{{ node.providerType }}</v-chip>
              </div>
              <div class="sp-card-actions">
                <v-btn icon="mdi-pencil" size="x-small" variant="tonal" color="primary" @click="openEditDialog(node)" />
                <v-btn icon="mdi-delete" size="x-small" variant="tonal" color="error" @click="askDeleteNode(node.id)" />
              </div>
            </div>
            <code class="sp-card-path">{{ node.connectionConfig?.basePath || node.connectionConfig?.endpoint || '-' }}</code>
          </div>
        </div>
        <div v-if="!storageNodes.length && !storageLoading" class="text-caption text-disabled text-center py-4">暂无存储节点</div>
      </section>

      <!-- LLM -->
      <section class="sp-section">
        <h3>AI 算力配置</h3>
        <div class="mb-4">
          <div class="text-caption text-disabled mb-1">提供商</div>
          <v-radio-group v-model="llmConfig.provider" inline hide-details density="compact">
            <v-radio label="Ollama" value="ollama" />
            <v-radio label="OpenAI" value="openai" />
          </v-radio-group>
        </div>
        <v-text-field v-model="llmConfig.baseUrl" label="Base URL" :placeholder="llmConfig.provider === 'ollama' ? 'http://localhost:11434' : 'https://api.openai.com'" class="mb-4" />
        <v-text-field v-if="llmConfig.provider === 'openai'" v-model="llmConfig.apiKey" label="API Key" type="password" class="mb-4" />
        <v-text-field v-model="llmConfig.modelName" label="模型名称" :placeholder="llmConfig.provider === 'ollama' ? 'llama3' : 'gpt-4o-mini'" class="mb-4" />
        <v-btn color="primary" @click="saveLlmConfigAction">保存配置</v-btn>
      </section>
    </div>

    <!-- Storage Node Dialog -->
    <v-dialog v-model="storageDialogVisible" max-width="460">
      <v-card>
        <v-card-title>{{ editNodeId ? '编辑存储节点' : '新建存储节点' }}</v-card-title>
        <v-card-text>
          <v-text-field v-model="storageForm.name" label="名称" placeholder="如：本地 NAS 盘" class="mb-4" />
          <v-select v-model="storageForm.providerType" label="类型" :items="['LOCAL', 'MINIO', 'S3']" class="mb-4" />
          <v-text-field v-if="storageForm.providerType === 'LOCAL'" v-model="localBasePath" label="根路径" placeholder="/" class="mb-4" />
          <template v-if="storageForm.providerType !== 'LOCAL'">
            <v-text-field v-model="minioConfig.endpoint" label="Endpoint" placeholder="http://localhost:9000" class="mb-4" />
            <v-text-field v-model="minioConfig.bucket" label="Bucket" class="mb-4" />
            <v-text-field v-model="minioConfig.accessKey" label="Access Key" class="mb-4" />
            <v-text-field v-model="minioConfig.secretKey" label="Secret Key" type="password" class="mb-4" />
          </template>
          <v-switch v-model="storageForm.isReadonly" label="只读" color="primary" />
        </v-card-text>
        <v-card-actions>
          <v-spacer /><v-btn variant="text" @click="storageDialogVisible = false">取消</v-btn>
          <v-btn color="primary" @click="saveStorageNode">{{ editNodeId ? '更新' : '创建' }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Confirm Delete -->
    <v-dialog v-model="confirmDialogVisible" max-width="400" persistent>
      <v-card>
        <v-card-title>确认删除</v-card-title>
        <v-card-text>确定删除此存储节点？</v-card-text>
        <v-card-actions>
          <v-spacer /><v-btn variant="text" @click="confirmDialogVisible = false">取消</v-btn>
          <v-btn color="error" @click="removeStorageNodeConfirmed">删除</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.settings-page {
  height: 100%; overflow: auto; padding: 20px 24px;
}
.sp-header { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.sp-header h2 { margin: 0; font-size: 20px; font-weight: 700; }
.sp-body { max-width: 720px; display: flex; flex-direction: column; gap: 24px; }
.sp-section {
  background: rgb(var(--v-theme-surface)); border: 1px solid rgb(var(--v-border-color));
  border-radius: 10px; padding: 20px;
}
.sp-section h3 { margin: 0 0 14px; font-size: 15px; font-weight: 600; }
.sp-section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.sp-section-header h3 { margin: 0; }
.sp-card-grid { display: flex; flex-direction: column; gap: 8px; }
.sp-card {
  background: rgb(var(--v-theme-surface-variant)); border: 1px solid rgb(var(--v-border-color));
  border-radius: 8px; padding: 14px;
}
.sp-card-top { display: flex; justify-content: space-between; align-items: flex-start; }
.sp-card-name { font-size: 14px; font-weight: 600; margin-bottom: 6px; }
.sp-card-path { font-size: 12px; color: rgb(var(--v-theme-secondary)); font-family: 'JetBrains Mono', monospace; }
.sp-card-actions { display: flex; gap: 4px; }
</style>
