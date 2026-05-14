<script setup lang="ts">
import { ref, onMounted, markRaw, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { VueFlow, useVueFlow, type Node, type Edge } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import { getTemplates, createTemplate, updateTemplate, getNodeRegistry } from '../../api/modules'
import { useToast } from '../../composables/useToast'
import FlowNode from '../../components/FlowNode.vue'
import StoragePathSelector from '../../components/StoragePathSelector.vue'

const { toast } = useToast()
const route = useRoute()
const router = useRouter()

// ---- Registry types ----
interface ConfigFieldDef { key: string; label: string; type: string; defaultValue: string; selectableType?: 'file' | 'directory' | 'both' }
interface NodeRegistryDef {
  name: string; label: string; icon: string; description: string
  hasConfig: boolean; configSchema: ConfigFieldDef[]; uiSummaryKeys: string[]; available: boolean
  nodeType: string
}
interface CategoryDef { key: string; label: string; icon: string; nodes: NodeRegistryDef[] }

// ---- Registry state ----
const categories = ref<CategoryDef[]>([])
const registryMap = computed(() => {
  const m = new Map<string, NodeRegistryDef>()
  for (const cat of categories.value) for (const n of cat.nodes) m.set(n.name, n)
  return m
})

function findNodeDef(name: string): NodeRegistryDef | undefined {
  return registryMap.value.get(name)
}

function buildSummaryLabelMap(def: NodeRegistryDef | undefined): Record<string, string> {
  if (!def) return {}
  const map: Record<string, string> = {}
  for (const key of def.uiSummaryKeys) {
    const field = def.configSchema.find(f => f.key === key)
    if (field) map[key] = field.label
  }
  return map
}

const REQUIRES_INPUT = new Set(['ArchiveNode','RenameNode','LlmAnalyzerNode','DoubanScraperNode','FormatConverterNode','TxtExtractorNode','EpubMetaParserNode','TmdbScraperNode','VideoFrameNode','ChapterParserNode'])
const TERMINATOR_NODES = new Set(['ArchiveNode'])

// ---- State ----
const templates = ref<any[]>([])
const loading = ref(false)
const editorVisible = ref(false)
const editId = ref<string | null>(null)
const editName = ref('')
const editDesc = ref('')

const openCategories = ref<Set<string>>(new Set(['input', 'processing', 'output', 'flow_control']))

function toggleCategory(key: string) {
  if (openCategories.value.has(key)) openCategories.value.delete(key)
  else openCategories.value.add(key)
}

// ---- Vue Flow ----
const { onConnect, addEdges, project, getNodes, getEdges, removeNodes, removeEdges } = useVueFlow()

const flowNodes = ref<Node[]>([])
const flowEdges = ref<Edge[]>([])
const selectedNodeId = ref<string | null>(null)

// Config dialog
const configDialogVisible = ref(false)
const configNode = ref<Node | null>(null)
const configForm = ref<Record<string, any>>({})

// Context menu
const ctxMenu = reactive({ visible: false, x: 0, y: 0, type: '' as 'node' | 'edge', id: '' })

// Validation state
const validationErrors = ref<{ type: string; message: string; nodeIds: string[] }[]>([])

// ---- Load data ----
onMounted(async () => {
  loading.value = true
  try {
    const [tplRes, regRes] = await Promise.all([getTemplates(), getNodeRegistry()])
    templates.value = tplRes.data
    categories.value = regRes.data
    // 刷新后恢复编辑器状态
    const editQuery = route.query.edit as string | undefined
    if (editQuery) {
      const tpl = templates.value.find((t: any) => t.id === editQuery)
      if (tpl) openEditTemplate(tpl)
    }
  } finally {
    loading.value = false
  }
})

// ---- Connect edges ----
onConnect((params) => {
  addEdges([{ ...params, animated: true, style: { stroke: '#8b6ffc', strokeWidth: 2 } }])
})

// ---- Node interactions ----
function onNodeClick({ node }: { node: Node }) {
  selectedNodeId.value = node.id
}

function onNodeDoubleClick({ node }: { node: Node }) {
  openConfigDialog(node)
}

function onPaneClick() {
  selectedNodeId.value = null
  hideCtxMenu()
}

function onEdgeContextMenu({ event, edge }: { event: MouseEvent; edge: Edge }) {
  event.preventDefault()
  ctxMenu.visible = true; ctxMenu.x = event.clientX; ctxMenu.y = event.clientY
  ctxMenu.type = 'edge'; ctxMenu.id = edge.id
}

function onNodeContextMenu({ event, node }: { event: MouseEvent; node: Node }) {
  event.preventDefault()
  ctxMenu.visible = true; ctxMenu.x = event.clientX; ctxMenu.y = event.clientY
  ctxMenu.type = 'node'; ctxMenu.id = node.id
  selectedNodeId.value = node.id
}

function hideCtxMenu() { ctxMenu.visible = false }

function ctxMenuConfigure() {
  const node = flowNodes.value.find(n => n.id === ctxMenu.id)
  if (node) openConfigDialog(node)
  hideCtxMenu()
}

function ctxMenuDeleteNode() {
  removeNodes([ctxMenu.id])
  if (configNode.value?.id === ctxMenu.id) { configDialogVisible.value = false; configNode.value = null }
  hideCtxMenu()
}

function ctxMenuDeleteEdge() {
  removeEdges([ctxMenu.id])
  hideCtxMenu()
}

// ---- Config dialog ----
function openConfigDialog(node: Node) {
  selectedNodeId.value = node.id
  configNode.value = node
  const rawConfig = { ...(node.data?.config || {}), condition: node.data?.condition || '' }
  const schema = registryMap.value.get(node.data?.name)?.configSchema || []
  for (const field of schema) {
    if (field.type === 'storage_path' && (!rawConfig[field.key] || typeof rawConfig[field.key] === 'string')) {
      rawConfig[field.key] = { storage_node_id: '', path: rawConfig[field.key] || '' }
    }
  }
  configForm.value = rawConfig
  configDialogVisible.value = true
}

function applyConfig() {
  if (!configNode.value) return
  const node = flowNodes.value.find(n => n.id === configNode.value!.id)
  if (node) {
    node.data = {
      ...node.data,
      config: { ...configForm.value },
      condition: configForm.value.condition,
      configured: true,
      summaryLabelMap: node.data.summaryLabelMap || {},
    }
  }
  configDialogVisible.value = false
}

// ---- Drag from palette ----
let dragNodeDef: NodeRegistryDef | null = null

function onPoolDragStart(nodeDef: NodeRegistryDef) {
  dragNodeDef = nodeDef
}

function onCanvasDrop(event: DragEvent) {
  if (!dragNodeDef) return
  const def = dragNodeDef
  dragNodeDef = null

  const bounds = (event.target as HTMLElement).getBoundingClientRect()
  const position = project({ x: event.clientX - bounds.left, y: event.clientY - bounds.top })

  const newNode: Node = {
    id: `${def.name}_${Date.now()}`,
    type: 'custom',
    position,
    data: {
      label: def.label, name: def.name, icon: def.icon,
      description: def.description, hasConfig: def.hasConfig,
      configured: false, config: {}, condition: '', error: '',
      uiSummaryKeys: def.uiSummaryKeys,
      summaryLabelMap: buildSummaryLabelMap(def),
    },
  }
  flowNodes.value = [...flowNodes.value, newNode]
}

// ---- Validation ----
function checkFlow() {
  const nodes = getNodes.value
  const edges = getEdges.value
  const errors: { type: string; message: string; nodeIds: string[] }[] = []

  for (const n of flowNodes.value) { if (n.data) n.data.error = '' }
  if (nodes.length === 0) { toast.warning('画布上没有节点'); return }

  const hasIncoming = new Set<string>()
  const hasOutgoing = new Set<string>()
  const adj = new Map<string, string[]>()
  for (const e of edges) {
    hasIncoming.add(e.target); hasOutgoing.add(e.source)
    if (!adj.has(e.source)) adj.set(e.source, [])
    adj.get(e.source)!.push(e.target)
  }

  const orphans = nodes.filter(n => !hasIncoming.has(n.id) && !hasOutgoing.has(n.id))
  if (orphans.length > 0)
    errors.push({ type: 'orphan', message: `${orphans.length} 个游离节点未连接任何连线`, nodeIds: orphans.map(n => n.id) })

  const missingInput = nodes.filter(n => REQUIRES_INPUT.has(n.data?.name) && !hasIncoming.has(n.id))
  if (missingInput.length > 0)
    errors.push({ type: 'missing_input', message: `${missingInput.length} 个节点缺少必要输入`, nodeIds: missingInput.map(n => n.id) })

  const WHITE = 0, GRAY = 1, BLACK = 2
  const color = new Map<string, number>()
  for (const n of nodes) color.set(n.id, WHITE)
  let cycleFound = false; const cycleNodes: string[] = []
  function dfs(u: string) {
    color.set(u, GRAY)
    for (const v of (adj.get(u) || [])) {
      if (color.get(v) === GRAY) { cycleFound = true; cycleNodes.push(u); return }
      if (color.get(v) === WHITE) dfs(v)
      if (cycleFound) return
    }
    color.set(u, BLACK)
  }
  for (const n of nodes) { if (color.get(n.id) === WHITE) { dfs(n.id); if (cycleFound) break } }
  if (cycleFound)
    errors.push({ type: 'cycle', message: '检测到循环路径，流程不能包含死循环', nodeIds: cycleNodes })

  if (!nodes.some(n => TERMINATOR_NODES.has(n.data?.name)))
    errors.push({ type: 'no_terminator', message: '流程缺少结束节点（归档写入），数据流无法落地', nodeIds: [] })

  // IO 检查：分析模板是否需要输入/输出路径
  const nodeTypes = new Set(nodes.map(n => registryMap.value.get(n.data?.name)?.nodeType))
  const needsInput = nodeTypes.has('INPUT')
  const needsOutput = nodeTypes.has('OUTPUT')
  const ioMessages: string[] = []
  if (needsInput) ioMessages.push('⚠ 此流水线需要输入路径（新建任务时必须提供）')
  else ioMessages.push('✓ 此流水线不需要输入路径')
  if (needsOutput) ioMessages.push('⚠ 此流水线需要输出路径（新建任务时必须提供）')
  else ioMessages.push('✓ 此流水线不需要输出路径')

  for (const err of errors) {
    for (const nid of err.nodeIds) {
      const node = flowNodes.value.find(n => n.id === nid)
      if (node?.data) node.data.error = err.type === 'orphan' ? '游离节点' : err.type === 'missing_input' ? '缺少输入' : '循环路径'
    }
  }
  validationErrors.value = errors

  if (errors.length === 0) {
    toast.success('流程检查通过！无逻辑错误\n' + ioMessages.join('\n'))
  } else {
    toast.confirm('流程检查发现问题', [...errors.map(e => `• ${e.message}`), '', ...ioMessages].join('\n'))
  }
}

// ---- Save ----
async function saveAndDeploy() {
  const nodes = getNodes.value
  const edges = getEdges.value

  // Cycle check
  const adj = new Map<string, string[]>()
  for (const e of edges) { if (!adj.has(e.source)) adj.set(e.source, []); adj.get(e.source)!.push(e.target) }
  const WHITE = 0, GRAY = 1, BLACK = 2
  const color = new Map<string, number>()
  for (const n of nodes) color.set(n.id, WHITE)
  const path: string[] = []
  let cycleFound = false; let cyclePath: string[] = []
  function dfs(u: string) {
    color.set(u, GRAY); path.push(u)
    for (const v of (adj.get(u) || [])) {
      if (color.get(v) === GRAY) {
        cycleFound = true
        const start = path.indexOf(v)
        cyclePath = path.slice(start).map(id => nodes.find(nn => nn.id === id)?.data?.label || id)
        return
      }
      if (color.get(v) === WHITE) dfs(v)
      if (cycleFound) return
    }
    path.pop(); color.set(u, BLACK)
  }
  for (const n of nodes) { if (color.get(n.id) === WHITE) { dfs(n.id); if (cycleFound) break } }
  if (cycleFound) {
    toast.error(`检测到死循环路径: ${cyclePath.join(' → ')}`)
    return
  }

  // Orphan warning
  const connected = new Set<string>()
  for (const e of edges) { connected.add(e.source); connected.add(e.target) }
  const orphans = nodes.filter(n => !connected.has(n.id))
  if (orphans.length > 0) {
    const ok = await toast.confirm('孤岛节点警告', `存在 ${orphans.length} 个游离节点，是否继续？`)
    if (!ok) return
  }

  const graphPayload = {
    nodes: nodes.map(n => ({
      id: n.id, name: n.data?.name, label: n.data?.label, icon: n.data?.icon,
      config: n.data?.config || {}, condition: n.data?.condition || '', position: n.position,
    })),
    edges: edges.map(e => ({ id: e.id, source: e.source, target: e.target, sourcePort: e.sourceHandle || 'default' })),
  }
  if (!editName.value.trim()) {
    toast.warning('请输入模板名称')
    return
  }
  try {
    if (editId.value) {
      await updateTemplate(editId.value, { name: editName.value.trim(), description: editDesc.value, graphPayload, isDefault: false })
    } else {
      await createTemplate({ name: editName.value.trim(), description: editDesc.value, graphPayload, isDefault: false })
    }
    toast.success('模板已保存')
    editorVisible.value = false
    router.replace({ query: {} })
    const { data } = await getTemplates()
    templates.value = data
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.response?.data?.error || '保存失败'
    toast.error(msg)
  }
}

// ---- Open editor ----
function openNewTemplate() {
  editId.value = null; editName.value = ''; editDesc.value = ''
  flowNodes.value = []; flowEdges.value = []
  selectedNodeId.value = null; configDialogVisible.value = false
  editorVisible.value = true
  router.replace({ query: { edit: 'new' } })
}

function openEditTemplate(tpl: any) {
  editId.value = tpl.id; editName.value = tpl.name; editDesc.value = tpl.description || ''

  if (tpl.graphPayload && tpl.graphPayload.nodes) {
    const gp = tpl.graphPayload
    flowNodes.value = gp.nodes.map((n: any, i: number) => {
      const reg = findNodeDef(n.name)
      return {
        id: n.id, type: 'custom', position: n.position || { x: 260, y: 80 + i * 100 },
        data: {
          label: n.label || n.name.replace('Node', ''),
          name: n.name, icon: n.icon || '⚙️',
          description: reg?.description || '',
          hasConfig: reg?.hasConfig ?? (!!n.config && Object.keys(n.config).length > 0),
          configured: !!n.config && Object.keys(n.config).length > 0,
          config: n.config || {},
          condition: n.condition || '', error: '',
          uiSummaryKeys: reg?.uiSummaryKeys || [],
          summaryLabelMap: buildSummaryLabelMap(reg),
        },
      }
    })
    flowEdges.value = (gp.edges || []).map((e: any) => ({
      id: e.id, source: e.source, target: e.target,
      animated: true, style: { stroke: '#8b6ffc', strokeWidth: 2 },
      sourceHandle: e.sourcePort === 'default' ? undefined : e.sourcePort,
    }))
  }
  editorVisible.value = true
  router.replace({ query: { edit: editId.value! } })
}
</script>

<template>
  <div class="page-view" @click="hideCtxMenu">
    <!-- Template List -->
    <template v-if="!editorVisible">
      <div class="page-toolbar">
        <h3>处理模板</h3>
        <v-btn color="primary" size="default" prepend-icon="mdi-plus" @click="openNewTemplate">新建模板</v-btn>
      </div>
      <div class="page-body">
        <div class="page-cards">
        <v-progress-circular v-if="loading" indeterminate color="primary" class="ma-auto" />
        <div v-for="tpl in templates" :key="tpl.id" class="page-card tpl-card" @click="openEditTemplate(tpl)">
          <div class="page-card-top tpl-card-header">
            <span class="tpl-name">{{ tpl.name }}</span>
            <v-chip v-if="tpl.isDefault" size="x-small" color="success" variant="tonal">默认</v-chip>
          </div>
          <div class="tpl-card-desc">{{ tpl.description || '无描述' }}</div>
          <div class="tpl-card-nodes">
            <v-chip
              v-for="(n, i) in (tpl.graphPayload?.nodes || [])"
              :key="i"
              size="x-small"
              color="primary"
              variant="tonal"
              class="tpl-node-chip"
            >
              {{ n.icon }} {{ n.label || n.name?.replace('Node', '') }}
            </v-chip>
          </div>
        </div>
        <div v-if="!templates.length && !loading" class="tpl-empty">
          <div class="text-caption text-disabled text-center py-10">暂无模板，点击新建开始</div>
        </div>
        </div>
      </div>
    </template>

    <!-- ====== FLOW EDITOR ====== -->
    <div v-else class="flow-editor">
      <div class="flow-topbar">
        <v-btn icon="mdi-arrow-left" size="small" variant="plain" @click="editorVisible = false; router.replace({ query: {} })" />
        <v-text-field v-model="editName" placeholder="模板名称" density="compact" class="tpl-name-input" />
        <v-text-field v-model="editDesc" placeholder="模板描述 (可选)" density="compact" class="tpl-desc-input" />
        <v-spacer />
        <v-btn size="default" prepend-icon="mdi-check-circle" @click="checkFlow">检查流程</v-btn>
        <v-btn color="primary" size="default" @click="saveAndDeploy">保存并部署</v-btn>
      </div>

      <div class="flow-body">
        <!-- LEFT: Node Palette -->
        <aside class="node-palette">
          <div class="palette-title">节点组件</div>
          <div v-for="cat in categories" :key="cat.key" class="cat-group">
            <div class="cat-header" @click="toggleCategory(cat.key)">
              <span class="cat-icon">{{ cat.icon }}</span>
              <span class="cat-label">{{ cat.label }}</span>
              <span class="cat-toggle">{{ openCategories.has(cat.key) ? '▾' : '▸' }}</span>
            </div>
            <div v-show="openCategories.has(cat.key)" class="cat-nodes">
              <div
                v-for="ndef in cat.nodes"
                :key="ndef.name"
                class="pool-item"
                :class="{ disabled: !ndef.available }"
                :draggable="ndef.available"
                @dragstart="onPoolDragStart(ndef)"
              >
                <span class="pool-icon">{{ ndef.icon }}</span>
                <div class="pool-info">
                  <div class="pool-name">{{ ndef.label }}</div>
                  <div class="pool-desc">{{ ndef.description }}</div>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <!-- CENTER: Canvas -->
        <div class="flow-canvas" @drop="onCanvasDrop" @dragover.prevent>
          <VueFlow
            v-model:nodes="flowNodes"
            v-model:edges="flowEdges"
            :default-viewport="{ zoom: 1, x: 0, y: 0 }"
            :min-zoom="0.3" :max-zoom="2"
            :snap-to-grid="true" :snap-grid="[20, 20]"
            fit-view-on-init
            @node-click="onNodeClick"
            @node-double-click="onNodeDoubleClick"
            @node-context-menu="onNodeContextMenu"
            @edge-context-menu="onEdgeContextMenu"
            @pane-click="onPaneClick"
            :node-types="{ custom: markRaw(FlowNode) }"
            :edge-style="{ stroke: '#8b6ffc', strokeWidth: 2 }"
            class="vue-flow-dark"
          >
            <Background :gap="20" :size="1" />
            <Controls />
          </VueFlow>
        </div>
      </div>
    </div>

    <!-- Config Dialog -->
    <v-dialog v-model="configDialogVisible" max-width="560">
      <v-card v-if="configNode" class="config-dialog-card">
        <v-card-title class="text-h6 pa-6 pb-2">{{ configNode.data?.icon }} {{ configNode.data?.label }} 配置</v-card-title>
        <v-card-text class="config-dialog-body">
          <v-text-field :model-value="configNode.data?.name" label="节点类型" disabled density="default" class="mb-4" />
          <p v-if="configNode.data?.description" class="config-desc mb-4">{{ configNode.data.description }}</p>

          <template v-for="field in (registryMap.get(configNode.data?.name)?.configSchema || [])" :key="field.key">
            <StoragePathSelector
              v-if="field.type === 'storage_path'"
              v-model="configForm[field.key]"
              :selectable-type="field.selectableType || 'both'"
              :label="field.label"
              :placeholder="field.defaultValue || undefined"
              class="mb-4"
            />
            <v-text-field v-else-if="field.type === 'text'" v-model="configForm[field.key]" :label="field.label" :placeholder="field.defaultValue || undefined" density="default" class="mb-4" />
            <v-text-field v-else-if="field.type === 'number'" v-model.number="configForm[field.key]" type="number" :label="field.label" density="default" class="mb-4" />
            <v-textarea v-else-if="field.type === 'textarea'" v-model="configForm[field.key]" :label="field.label" :rows="5" :placeholder="field.defaultValue || undefined" density="default" class="mb-4" />
          </template>

          <template v-if="configNode.data?.name === 'RouterNode'">
            <v-text-field v-model="configForm.condition" label="条件表达式" placeholder="detectedFormat == 'TXT'" density="default" class="mb-4" />
          </template>

          <v-divider class="my-4" />
          <v-textarea v-model="configForm.note" label="备注" :rows="3" placeholder="可选" density="default" />
        </v-card-text>
        <v-card-actions class="config-dialog-actions pa-4">
          <v-spacer />
          <v-btn size="large" variant="text" @click="configDialogVisible = false">取消</v-btn>
          <v-btn size="large" color="primary" @click="applyConfig">应用配置</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Context Menu -->
    <Teleport to="body">
      <div v-if="ctxMenu.visible" class="ctx-menu" :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }" @click.stop>
        <template v-if="ctxMenu.type === 'node'">
          <div class="ctx-item" @click="ctxMenuConfigure"><v-icon icon="mdi-cog" size="14" class="mr-2" />配置节点</div>
          <div class="ctx-sep"></div>
          <div class="ctx-item ctx-danger" @click="ctxMenuDeleteNode"><v-icon icon="mdi-delete" size="14" class="mr-2" />删除节点</div>
        </template>
        <template v-if="ctxMenu.type === 'edge'">
          <div class="ctx-item ctx-danger" @click="ctxMenuDeleteEdge"><v-icon icon="mdi-delete" size="14" class="mr-2" />删除连线</div>
        </template>
      </div>
    </Teleport>
  </div>
</template>

<style>
.vue-flow-dark { --vf-node-bg: rgb(var(--v-theme-surface)); --vf-node-text: rgb(var(--v-theme-on-surface)); --vf-handle: rgb(var(--v-theme-primary)); }
.vue-flow-dark .vue-flow__background { background: rgb(var(--v-theme-background)); }
.vue-flow-dark .vue-flow__edge-path { stroke: rgb(var(--v-border-color)); stroke-width: 2; }
.vue-flow-dark .vue-flow__controls { background: rgb(var(--v-theme-surface)); border: 1px solid rgb(var(--v-border-color)); border-radius: 8px; }
.vue-flow-dark .vue-flow__controls-button { background: rgb(var(--v-theme-surface-variant)); border-bottom: 1px solid rgb(var(--v-border-color)); color: rgb(var(--v-theme-on-surface-variant)); fill: rgb(var(--v-theme-on-surface-variant)); }
.vue-flow-dark .vue-flow__controls-button:hover { background: rgb(var(--v-theme-surface-bright)); color: rgb(var(--v-theme-on-surface)); fill: rgb(var(--v-theme-on-surface)); }
</style>

<style scoped>
/* Template card overrides */
.tpl-card {
  padding: 14px;
  cursor: pointer;
  transition: border-color 0.15s;
  background: rgb(var(--v-theme-surface-variant));
}
.tpl-card:hover { border-color: rgb(var(--v-theme-primary)); }
.tpl-card-header { margin-bottom: 6px; }
.tpl-name { flex: 1; }
.tpl-card-desc { font-size: 12px; color: rgb(var(--v-theme-secondary)); margin-bottom: 10px; }
.tpl-card-nodes { display: flex; flex-wrap: wrap; gap: 4px; }
.tpl-node-chip { cursor: inherit !important; }
.tpl-empty { grid-column: 1 / -1; padding: 40px 0; }

.flow-editor { display: flex; flex-direction: column; height: 100%; overflow: hidden; }
.flow-topbar {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 16px;
  border-bottom: 1px solid rgb(var(--v-border-color));
  background: rgb(var(--v-theme-surface));
}
.tpl-name-input { max-width: 200px; }
.tpl-desc-input { max-width: 240px; }
.flow-body { flex: 1; display: flex; overflow: hidden; }

.node-palette {
  width: 220px; flex-shrink: 0;
  height: 100%;
  border-right: 1px solid rgb(var(--v-border-color));
  background: rgb(var(--v-theme-surface));
  overflow-y: auto; padding: 8px;
  overscroll-behavior: contain;
}
.palette-title {
  font-size: 10px; font-weight: 700; text-transform: uppercase;
  letter-spacing: 1px; color: rgb(var(--v-theme-secondary));
  padding: 6px 8px;
}
.cat-group { margin-bottom: 4px; }
.cat-header {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 8px; border-radius: 6px; cursor: pointer;
  font-size: 12px; font-weight: 600; color: rgb(var(--v-theme-secondary));
  transition: all 0.12s;
}
.cat-header:hover { background: rgba(var(--v-theme-on-surface), 0.05); color: rgb(var(--v-theme-on-surface)); }
.cat-icon { font-size: 13px; }
.cat-label { flex: 1; }
.cat-toggle { font-size: 10px; color: rgb(var(--v-theme-secondary)); }
.cat-nodes { padding: 2px 0 4px 4px; }

.pool-item {
  display: flex; gap: 6px; align-items: flex-start;
  padding: 7px 8px; border-radius: 6px; margin-bottom: 3px;
  border: 1px solid rgb(var(--v-border-color));
  background: rgb(var(--v-theme-surface-variant));
  cursor: grab; transition: border-color 0.12s; font-size: 12px;
}
.pool-item:hover { border-color: rgb(var(--v-theme-primary)); }
.pool-item:active { cursor: grabbing; }
.pool-item.disabled { opacity: 0.4; cursor: not-allowed; }
.pool-icon { font-size: 14px; margin-top: 1px; }
.pool-info { flex: 1; min-width: 0; }
.pool-name { font-size: 11px; font-weight: 600; }
.pool-desc {
  font-size: 10px; color: rgb(var(--v-theme-secondary));
  line-height: 1.3; margin-top: 2px;
  overflow: hidden; text-overflow: ellipsis;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}

.flow-canvas { flex: 1; background: rgb(var(--v-theme-background)); }

/* Config dialog: body scrolls, actions fixed at bottom */
.config-dialog-card {
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}
.config-dialog-body {
  overflow-y: auto;
  flex: 1;
  overscroll-behavior: contain;
}
.config-dialog-actions {
  border-top: 1px solid rgba(var(--v-border-color), 0.5);
  flex-shrink: 0;
}

.config-desc { font-size: 12px; color: rgb(var(--v-theme-secondary)); line-height: 1.5; }

.ctx-menu {
  position: fixed; z-index: 9999; min-width: 150px;
  background: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-border-color));
  border-radius: 8px; box-shadow: 0 8px 30px rgba(0,0,0,0.45);
  padding: 4px;
}
.ctx-item {
  display: flex; align-items: center;
  padding: 8px 12px; border-radius: 6px; cursor: pointer;
  font-size: 13px; transition: background 0.12s;
}
.ctx-item:hover { background: rgba(var(--v-theme-on-surface), 0.06); }
.ctx-danger { color: rgb(var(--v-theme-error)); }
.ctx-danger:hover { background: rgba(var(--v-theme-error), 0.1); }
.ctx-sep { height: 1px; background: rgb(var(--v-border-color)); margin: 4px 8px; }
</style>
