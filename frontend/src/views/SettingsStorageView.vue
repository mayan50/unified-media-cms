<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStorageNodes, createStorageNode, updateStorageNode, deleteStorageNode } from '../api/modules'
import { useToast } from '../composables/useToast'

const { toast } = useToast()

const nodes = ref<any[]>([])
const loading = ref(false)
const viewMode = ref<'card' | 'table'>('card')
const dialogVisible = ref(false)
const confirmVisible = ref(false)
const confirmId = ref('')
const editId = ref<string | null>(null)
const form = ref({ name: '', providerType: 'LOCAL', isReadonly: false })
const localPath = ref('/')
const minioConf = ref({ endpoint: '', bucket: '', accessKey: '', secretKey: '' })

onMounted(() => fetchNodes())

async function fetchNodes() {
  loading.value = true
  try { const { data } = await getStorageNodes(); nodes.value = data } finally { loading.value = false }
}

function openNew() {
  editId.value = null; form.value = { name: '', providerType: 'LOCAL', isReadonly: false }; localPath.value = '/'
  minioConf.value = { endpoint: '', bucket: '', accessKey: '', secretKey: '' }; dialogVisible.value = true
}
function openEdit(n: any) {
  editId.value = n.id; form.value = { name: n.name, providerType: n.providerType, isReadonly: n.isReadonly }
  const c = typeof n.connectionConfig === 'string' ? JSON.parse(n.connectionConfig) : n.connectionConfig
  if (n.providerType === 'LOCAL') localPath.value = c?.basePath || '/'
  else minioConf.value = { endpoint: c?.endpoint || '', bucket: c?.bucket || '', accessKey: c?.accessKey || '', secretKey: c?.secretKey || '' }
  dialogVisible.value = true
}
async function save() {
  const cfg = form.value.providerType === 'LOCAL' ? { basePath: localPath.value } : { ...minioConf.value }
  try {
    if (editId.value) {
      await updateStorageNode(editId.value, { name: form.value.name, providerType: form.value.providerType, connectionConfig: cfg, isReadonly: form.value.isReadonly })
      toast.success('已更新')
    } else {
      await createStorageNode({ name: form.value.name, providerType: form.value.providerType, connectionConfig: cfg, isReadonly: form.value.isReadonly })
      toast.success('已创建')
    }
    dialogVisible.value = false; fetchNodes()
  } catch { toast.error('保存失败') }
}
function askDelete(id: string) { confirmId.value = id; confirmVisible.value = true }
async function doDelete() {
  try { await deleteStorageNode(confirmId.value); toast.success('已删除'); fetchNodes() } catch { /* */ }
  confirmVisible.value = false
}
</script>

<template>
  <div class="page-view">
    <div class="page-toolbar">
      <h3>存储节点</h3>
      <div class="page-toolbar-right">
        <div class="view-toggle mr-3">
          <v-btn icon="mdi-view-grid" size="small" :variant="viewMode === 'card' ? 'tonal' : 'plain'" :color="viewMode === 'card' ? 'primary' : undefined" @click="viewMode = 'card'" />
          <v-btn icon="mdi-view-list" size="small" :variant="viewMode === 'table' ? 'tonal' : 'plain'" :color="viewMode === 'table' ? 'primary' : undefined" @click="viewMode = 'table'" />
        </div>
        <v-btn color="primary" size="small" prepend-icon="mdi-plus" @click="openNew">新建</v-btn>
      </div>
    </div>

    <v-progress-linear v-if="loading" indeterminate color="primary" />

    <!-- Card View -->
    <div v-if="viewMode === 'card'" class="page-cards">
      <div v-for="n in nodes" :key="n.id" class="page-card">
        <div class="page-card-top">
          <div>
            <div class="page-card-name">{{ n.name }}</div>
            <v-chip size="x-small" :color="n.providerType === 'LOCAL' ? 'primary' : 'warning'" variant="tonal">{{ n.providerType }}</v-chip>
          </div>
          <div class="page-card-actions">
            <v-btn icon="mdi-pencil" size="x-small" variant="tonal" color="primary" @click="openEdit(n)" title="修改" />
            <v-btn icon="mdi-delete" size="x-small" variant="tonal" color="error" @click="askDelete(n.id)" title="删除" />
          </div>
        </div>
        <code class="page-card-path">{{ n.connectionConfig?.basePath || n.connectionConfig?.endpoint || '-' }}</code>
      </div>
      <div v-if="!nodes.length && !loading" class="text-caption text-disabled text-center py-8" style="grid-column:1/-1">暂无存储节点</div>
    </div>

    <!-- Table View -->
    <div v-else class="page-body">
      <div class="page-table-wrap">
      <v-table density="compact" hover>
        <thead><tr><th>名称</th><th>类型</th><th>路径</th><th>只读</th><th style="width:100px">操作</th></tr></thead>
        <tbody>
          <tr v-for="n in nodes" :key="n.id">
            <td>{{ n.name }}</td>
            <td><v-chip size="x-small" :color="n.providerType === 'LOCAL' ? 'primary' : 'warning'" variant="tonal">{{ n.providerType }}</v-chip></td>
            <td><code style="font-size:12px">{{ n.connectionConfig?.basePath || n.connectionConfig?.endpoint || '-' }}</code></td>
            <td>{{ n.isReadonly ? '是' : '否' }}</td>
            <td>
              <v-btn icon="mdi-pencil" size="x-small" variant="plain" color="primary" @click="openEdit(n)" title="修改" />
              <v-btn icon="mdi-delete" size="x-small" variant="plain" color="error" @click="askDelete(n.id)" title="删除" />
            </td>
          </tr>
        </tbody>
      </v-table>
      <div v-if="!nodes.length && !loading" class="text-caption text-disabled text-center py-8">暂无存储节点</div>
      </div>
    </div>

    <!-- Dialog -->
    <v-dialog v-model="dialogVisible" max-width="460">
      <v-card>
        <v-card-title>{{ editId ? '编辑存储节点' : '新建存储节点' }}</v-card-title>
        <v-card-text>
          <v-text-field v-model="form.name" label="名称" placeholder="如：本地 NAS 盘" class="mb-4" />
          <v-select v-model="form.providerType" label="类型" :items="['LOCAL', 'MINIO', 'S3']" class="mb-4" />
          <v-text-field v-if="form.providerType === 'LOCAL'" v-model="localPath" label="根路径" placeholder="/" class="mb-4" />
          <template v-if="form.providerType !== 'LOCAL'">
            <v-text-field v-model="minioConf.endpoint" label="Endpoint" placeholder="http://localhost:9000" class="mb-4" />
            <v-text-field v-model="minioConf.bucket" label="Bucket" class="mb-4" />
            <v-text-field v-model="minioConf.accessKey" label="Access Key" class="mb-4" />
            <v-text-field v-model="minioConf.secretKey" label="Secret Key" type="password" class="mb-4" />
          </template>
          <v-switch v-model="form.isReadonly" label="只读" color="primary" />
        </v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="dialogVisible = false">取消</v-btn><v-btn color="primary" @click="save">{{ editId ? '更新' : '创建' }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Confirm Delete -->
    <v-dialog v-model="confirmVisible" max-width="400" persistent>
      <v-card>
        <v-card-title>确认删除</v-card-title>
        <v-card-text>确定删除此存储节点？</v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="confirmVisible = false">取消</v-btn><v-btn color="error" @click="doDelete">删除</v-btn></v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
