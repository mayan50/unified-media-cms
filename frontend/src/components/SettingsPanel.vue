<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStorageNodes, createStorageNode, deleteStorageNode, getLlmConfig, saveLlmConfig } from '../api/modules'
import { useToast } from '../composables/useToast'

const { toast } = useToast()

const storageNodes = ref<any[]>([])
const storageLoading = ref(false)
const storageDialogVisible = ref(false)
const storageForm = ref({
  name: '',
  providerType: 'LOCAL',
  isReadonly: false,
})
const localBasePath = ref('')
const minioConfig = ref({ endpoint: '', bucket: '', accessKey: '', secretKey: '' })

// Manual confirm dialog
const confirmDialogVisible = ref(false)
const confirmDeleteId = ref('')

const llmConfig = ref({ provider: 'ollama', baseUrl: '', apiKey: '', modelName: '' })

async function fetchStorageNodes() {
  storageLoading.value = true
  try {
    const { data } = await getStorageNodes()
    storageNodes.value = data
  } finally {
    storageLoading.value = false
  }
}

async function saveStorageNode() {
  let config: any
  if (storageForm.value.providerType === 'LOCAL') {
    config = { basePath: localBasePath.value }
  } else {
    config = { ...minioConfig.value }
  }
  try {
    await createStorageNode({
      name: storageForm.value.name,
      providerType: storageForm.value.providerType,
      connectionConfig: config,
      isReadonly: storageForm.value.isReadonly,
    })
    storageDialogVisible.value = false
    toast.success('存储节点已创建')
    fetchStorageNodes()
  } catch {
    toast.error('创建失败')
  }
}

function askDeleteNode(id: string) {
  confirmDeleteId.value = id
  confirmDialogVisible.value = true
}

async function removeStorageNodeConfirmed() {
  try {
    await deleteStorageNode(confirmDeleteId.value)
    toast.success('已删除')
    fetchStorageNodes()
  } catch { /* cancelled */ }
  confirmDialogVisible.value = false
}

async function fetchLlmConfig() {
  try {
    const { data } = await getLlmConfig()
    if (data && Object.keys(data).length) {
      llmConfig.value = { ...llmConfig.value, ...data }
    }
  } catch { /* no config yet */ }
}

async function saveLlmConfigAction() {
  try {
    await saveLlmConfig(llmConfig.value)
    toast.success('AI 配置已保存')
  } catch {
    toast.error('保存失败')
  }
}

onMounted(() => {
  fetchStorageNodes()
  fetchLlmConfig()
})
</script>

<template>
  <div class="settings-panel">
    <!-- Storage Nodes Section -->
    <div class="settings-section">
      <div class="section-header">
        <h3>存储节点</h3>
        <v-btn color="primary" size="small" prepend-icon="mdi-plus" @click="storageDialogVisible = true">新建</v-btn>
      </div>

      <v-progress-linear v-if="storageLoading" indeterminate color="primary" class="mb-4" />

      <v-list v-if="storageNodes.length" density="compact" bg-color="surface" rounded class="mb-2">
        <v-list-item v-for="node in storageNodes" :key="node.id">
          <template #prepend>
            <v-chip size="x-small" :color="node.providerType === 'LOCAL' ? 'primary' : 'warning'" variant="tonal">
              {{ node.providerType }}
            </v-chip>
          </template>
          <v-list-item-title>{{ node.name }}</v-list-item-title>
          <v-list-item-subtitle>
            <template v-if="node.providerType === 'LOCAL'">{{ node.connectionConfig?.basePath || '-' }}</template>
            <template v-else>{{ node.connectionConfig?.endpoint }}/{{ node.connectionConfig?.bucket }}</template>
          </v-list-item-subtitle>
          <template #append>
            <v-btn icon="mdi-delete" size="x-small" variant="plain" color="error" @click="askDeleteNode(node.id)" />
          </template>
        </v-list-item>
      </v-list>
      <div v-if="!storageNodes.length && !storageLoading" class="text-caption text-disabled text-center py-4">
        暂无存储节点
      </div>
    </div>

    <!-- LLM Config Section -->
    <div class="settings-section">
      <h3>AI 算力配置</h3>
      <div class="mb-4">
        <div class="text-caption text-disabled mb-1">提供商</div>
        <v-radio-group v-model="llmConfig.provider" inline density="compact">
          <v-radio label="Ollama" value="ollama" />
          <v-radio label="OpenAI" value="openai" />
        </v-radio-group>
      </div>
      <v-text-field v-model="llmConfig.baseUrl" label="Base URL" :placeholder="llmConfig.provider === 'ollama' ? 'http://localhost:11434' : 'https://api.openai.com'" class="mb-4" />
      <v-text-field v-if="llmConfig.provider === 'openai'" v-model="llmConfig.apiKey" label="API Key" type="password" class="mb-4" />
      <v-text-field v-model="llmConfig.modelName" label="模型名称" :placeholder="llmConfig.provider === 'ollama' ? 'llama3' : 'gpt-4o-mini'" class="mb-4" />
      <v-btn color="primary" @click="saveLlmConfigAction">保存配置</v-btn>
    </div>

    <!-- New Storage Node Dialog -->
    <v-dialog v-model="storageDialogVisible" max-width="460">
      <v-card>
        <v-card-title>新建存储节点</v-card-title>
        <v-card-text>
          <v-text-field v-model="storageForm.name" label="名称" placeholder="如：本地 NAS 盘" class="mb-4" />
          <v-select v-model="storageForm.providerType" label="类型" :items="['LOCAL', 'MINIO', 'S3']" class="mb-4" />
          <v-text-field v-if="storageForm.providerType === 'LOCAL'" v-model="localBasePath" label="根路径" placeholder="/path/to/storage" class="mb-4" />
          <template v-if="storageForm.providerType !== 'LOCAL'">
            <v-text-field v-model="minioConfig.endpoint" label="Endpoint" placeholder="http://localhost:9000" class="mb-4" />
            <v-text-field v-model="minioConfig.bucket" label="Bucket" class="mb-4" />
            <v-text-field v-model="minioConfig.accessKey" label="Access Key" class="mb-4" />
            <v-text-field v-model="minioConfig.secretKey" label="Secret Key" type="password" class="mb-4" />
          </template>
          <v-switch v-model="storageForm.isReadonly" label="只读" color="primary" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="storageDialogVisible = false">取消</v-btn>
          <v-btn color="primary" @click="saveStorageNode">创建</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Confirm Delete Dialog -->
    <v-dialog v-model="confirmDialogVisible" max-width="400" persistent>
      <v-card>
        <v-card-title>确认</v-card-title>
        <v-card-text>确定删除此存储节点？</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="confirmDialogVisible = false">取消</v-btn>
          <v-btn color="error" @click="removeStorageNodeConfirmed">删除</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.settings-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.settings-section {
  background: rgb(var(--v-theme-surface-variant));
  border: 1px solid rgb(var(--v-border-color));
  border-radius: 10px;
  padding: 16px;
}

.settings-section h3 {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 600;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.section-header h3 { margin: 0; }
</style>
