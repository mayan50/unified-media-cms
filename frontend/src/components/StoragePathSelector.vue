<script setup lang="ts">
import { ref, computed, watch, nextTick, type Ref } from 'vue'
import { getStorageNodes, browseStorageNode } from '../api/modules'

export interface StoragePathValue {
  storage_node_id: string
  path: string
}

const props = withDefaults(defineProps<{
  modelValue?: StoragePathValue
  selectableType?: 'file' | 'directory' | 'both'
  label?: string
  placeholder?: string
}>(), {
  selectableType: 'both',
  placeholder: '选择或输入路径…',
})

const emit = defineEmits<{
  'update:modelValue': [value: StoragePathValue]
}>()

const safeValue = computed<StoragePathValue>(() => props.modelValue || { storage_node_id: '', path: '' })
const displayPath = computed(() => safeValue.value.path || '')

// Vuetify doesn't have an inline popover like Element Plus. Use v-menu instead.
const menuOpen = ref(false)

// Storage nodes
interface StorageNodeItem { id: string; name: string; providerType: string }
const storageNodes = ref<StorageNodeItem[]>([])
const selectedNodeId = ref<string>('')
const selectedNodeName = ref<string>('')

let nodesLoaded = false
async function loadStorageNodes() {
  if (nodesLoaded) return
  try {
    const { data } = await getStorageNodes()
    storageNodes.value = data
    nodesLoaded = true
    if (safeValue.value.storage_node_id) {
      await selectStorageNode(safeValue.value.storage_node_id, false)
    } else if (data.length > 0) {
      await selectStorageNode(data[0].id, false)
    }
  } catch { /* silent */ }
}

async function selectStorageNode(id: string, clearPath = true) {
  const node = storageNodes.value.find(n => n.id === id)
  if (!node) return
  selectedNodeId.value = id
  selectedNodeName.value = node.name
  rootItems.value = []
  expandedPaths.value = new Set()
  await loadChildrenAsync('/', rootItems)
  if (clearPath) {
    emit('update:modelValue', { ...safeValue.value, storage_node_id: id, path: '' })
  }
}

async function loadChildrenAsync(path: string, target: TreeItem[] | Ref<TreeItem[]>) {
  if (!selectedNodeId.value) return
  const arr = Array.isArray(target) ? target : (target as Ref<TreeItem[]>).value
  try {
    const { data } = await browseStorageNode(selectedNodeId.value, path)
    const items: TreeItem[] = data.map((e: any) => ({
      name: e.name, is_dir: e.is_dir, path: e.path,
      children: [], loaded: false, loading: false,
    }))
    arr.splice(0, arr.length, ...items)
  } catch { /* silent */ }
}

// Tree data
export interface TreeItem {
  name: string
  is_dir: boolean
  path: string
  children: TreeItem[]
  loaded: boolean
  loading: boolean
}

const rootItems = ref<TreeItem[]>([])
const expandedPaths = ref<Set<string>>(new Set())

async function loadChildren(path: string, target: TreeItem[] | Ref<TreeItem[]>) {
  if (!selectedNodeId.value) return
  const arr = Array.isArray(target) ? target : (target as Ref<TreeItem[]>).value
  try {
    const { data } = await browseStorageNode(selectedNodeId.value, path)
    const items: TreeItem[] = data.map((e: any) => ({
      name: e.name, is_dir: e.is_dir, path: e.path,
      children: [], loaded: false, loading: false,
    }))
    arr.splice(0, arr.length, ...items)
  } catch { /* silent */ }
}

function toggleExpand(item: TreeItem) {
  if (!item.is_dir) return
  if (expandedPaths.value.has(item.path)) {
    expandedPaths.value.delete(item.path)
  } else {
    expandedPaths.value.add(item.path)
    if (!item.loaded) {
      item.loading = true
      loadChildrenAsync(item.path, item.children).finally(() => {
        item.loaded = true
        item.loading = false
      })
    }
  }
  // 强制触发响应式更新：替换为新的 Set 实例
  expandedPaths.value = new Set(expandedPaths.value)
}

function selectItem(item: TreeItem) {
  if (props.selectableType === 'file' && item.is_dir) return
  if (props.selectableType === 'directory' && !item.is_dir) return
  emit('update:modelValue', { storage_node_id: selectedNodeId.value, path: item.path })
  menuOpen.value = false
}

function canSelect(item: TreeItem): boolean {
  if (props.selectableType === 'file' && item.is_dir) return false
  if (props.selectableType === 'directory' && !item.is_dir) return false
  return true
}

function onManualInput(val: string) {
  emit('update:modelValue', { ...safeValue.value, path: val })
}

function isActive(item: TreeItem): boolean {
  return safeValue.value.path === item.path && safeValue.value.storage_node_id === selectedNodeId.value
}

async function onMenuOpen() {
  await loadStorageNodes()
  // 菜单打开时，如果已有路径，自动展开并滚动
  const p = safeValue.value.path
  if (p && p !== '/' && selectedNodeId.value) {
    await expandToPath(p)
  }
}

// Node select items for v-select
const nodeSelectItems = computed(() => storageNodes.value.map(n => ({ title: n.name, value: n.id })))

// Auto-expand: 手动输入路径或打开菜单时逐级展开目录树
async function expandToPath(targetPath: string) {
  if (!targetPath || targetPath === '/') return
  if (!selectedNodeId.value) return
  if (!rootItems.value.length) return

  const segments = targetPath.split('/').filter(Boolean)
  let parentPath = '/'
  for (let i = 0; i < segments.length; i++) {
    const segPath = '/' + segments.slice(0, i + 1).join('/')
    const parentArr = parentPath === '/' ? rootItems.value :
      findChildrenInTree(rootItems.value, parentPath)
    if (!parentArr || !parentArr.length) break

    let item = parentArr.find((it: TreeItem) => it.path === segPath)
    if (!item) {
      const items = await loadChildrenData(parentPath)
      parentArr.splice(0, parentArr.length, ...items)
      item = parentArr.find((it: TreeItem) => it.path === segPath)
    }
    if (!item || !item.is_dir) break
    if (!item.loaded) {
      item.loading = true
      const children = await loadChildrenData(item.path)
      item.children.splice(0, item.children.length, ...children)
      item.loaded = true
      item.loading = false
    }
    expandedPaths.value.add(item.path)
    expandedPaths.value = new Set(expandedPaths.value)
    parentPath = item.path
  }
  await nextTick()
  const el = document.querySelector(`.browser-panel [data-path="${targetPath}"]`)
  if (el) el.scrollIntoView({ block: 'nearest' })
}

watch(() => safeValue.value.path, (newPath) => {
  if (newPath) expandToPath(newPath)
})

function findChildrenInTree(items: TreeItem[], targetPath: string): TreeItem[] | null {
  for (const item of items) {
    if (item.path === targetPath) return item.children
    if (item.is_dir && item.children.length) {
      const found = findChildrenInTree(item.children, targetPath)
      if (found) return found
    }
  }
  return null
}

async function loadChildrenData(dirPath: string): Promise<TreeItem[]> {
  if (!selectedNodeId.value) return []
  try {
    const { data } = await browseStorageNode(selectedNodeId.value, dirPath)
    return data.map((e: any) => ({
      name: e.name, is_dir: e.is_dir, path: e.path,
      children: [], loaded: false, loading: false,
    }))
  } catch { return [] }
}
</script>

<template>
  <v-menu
    v-model="menuOpen"
    :close-on-content-click="false"
    offset="0 4"
    max-width="420"
    @update:model-value="onMenuOpen"
  >
    <template #activator="{ props: menuProps }">
      <v-text-field
        :model-value="displayPath"
        :label="label"
        :placeholder="placeholder"
        hide-details
        @update:model-value="onManualInput"
        v-bind="menuProps"
      >
        <template #append-inner>
          <v-icon
            icon="mdi-folder-open"
            size="small"
            class="folder-trigger"
            @click.stop="menuOpen = !menuOpen"
          />
        </template>
      </v-text-field>
    </template>

    <v-card class="browser-panel" rounded="lg">
      <div class="pa-3 border-b">
        <div class="text-caption text-disabled mb-1">存储节点</div>
        <v-select
          :model-value="selectedNodeId"
          :items="nodeSelectItems"
          density="compact"
          hide-details
          @update:model-value="selectStorageNode($event, true)"
        />
      </div>

      <div v-if="safeValue.path" class="pa-2 px-3 bg-surface border-b">
        <code class="text-caption text-primary">{{ selectedNodeName }}:{{ safeValue.path }}</code>
      </div>

      <div class="browser-tree">
        <template v-if="rootItems.length">
          <TreeItemRow
            v-for="item in rootItems"
            :key="item.path"
            :item="item"
            :depth="0"
            :expanded-paths="expandedPaths"
            :selected-path="safeValue.path"
            :selected-node-id="safeValue.storage_node_id"
            :selectable-type="selectableType"
            :handle-toggle="toggleExpand"
            :handle-select="selectItem"
          />
        </template>
        <div v-else class="pa-5 text-center">
          <span class="text-caption text-disabled">加载中…</span>
        </div>
      </div>
    </v-card>
  </v-menu>
</template>


<script lang="ts">
import { defineComponent, h, type PropType } from 'vue'

export const TreeItemRow = defineComponent({
  name: 'TreeItemRow',
  props: {
    item: { type: Object as PropType<TreeItem>, required: true },
    depth: { type: Number, default: 0 },
    expandedPaths: { type: Object as PropType<Set<string>>, required: true },
    selectedPath: String,
    selectedNodeId: String,
    selectableType: String as PropType<'file' | 'directory' | 'both'>,
    handleToggle: Function as PropType<(item: TreeItem) => void>,
    handleSelect: Function as PropType<(item: TreeItem) => void>,
  },
  setup(props) {
    const indent = props.depth * 16 + 12
    return () => {
      const item = props.item
      const children: any[] = []
      const isExpanded = props.expandedPaths.has(item.path)
      const isActive = props.selectedPath === item.path
      const canSelect = props.selectableType === 'both'
        || (props.selectableType === 'file' && !item.is_dir)
        || (props.selectableType === 'directory' && item.is_dir)

      // Render children if expanded
      if (item.is_dir && isExpanded && item.children.length > 0) {
        for (const child of item.children) {
          children.push(h(TreeItemRow, {
            key: child.path,
            item: child,
            depth: props.depth + 1,
            expandedPaths: props.expandedPaths,
            selectedPath: props.selectedPath,
            selectedNodeId: props.selectedNodeId,
            selectableType: props.selectableType,
            handleToggle: props.handleToggle,
            handleSelect: props.handleSelect,
          }))
        }
      }

      return h('div', {}, [
        h('div', {
          'data-path': item.path,
          class: ['tree-row', { active: isActive, selectable: canSelect }],
          style: { paddingLeft: indent + 'px' },
        }, [
          h('span', {
            class: 'tree-chevron',
            onClick: (e: Event) => { e.stopPropagation(); props.handleToggle?.(item) },
          }, [
            item.is_dir
              ? (item.loading ? '⏳' : isExpanded ? '▾' : '▸')
              : h('span', { class: 'chevron-blank' }),
          ]),
          h('span', { class: 'tree-icon' }, item.is_dir ? '📁' : '📄'),
          h('span', {
            class: 'tree-name',
            onClick: (e: Event) => { e.stopPropagation(); props.handleSelect?.(item) },
          }, item.name),
        ]),
        ...children,
      ])
    }
  },
})

</script>

<style>
/* Tree rows (unscoped since they're in v-menu teleported) */
.browser-panel .tree-row {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  cursor: default;
  font-size: 12px;
  color: rgb(var(--v-theme-on-surface));
  transition: background 0.1s;
}
.browser-panel .tree-row:hover {
  background: rgba(var(--v-theme-on-surface), 0.05);
}
.browser-panel .tree-row.active {
  background: rgba(var(--v-theme-primary), 0.12);
}
.browser-panel .tree-row.selectable .tree-name {
  cursor: pointer;
}
.browser-panel .tree-row.selectable .tree-name:hover {
  color: rgb(var(--v-theme-primary));
}

.browser-panel .tree-chevron {
  width: 22px;
  text-align: center;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
  font-size: 20px;
  line-height: 1;
  color: rgb(var(--v-theme-secondary));
}
.browser-panel .chevron-blank {
  display: inline-block;
  width: 11px;
}
.browser-panel .tree-icon {
  font-size: 13px;
  flex-shrink: 0;
}
.browser-panel .tree-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

<style scoped>
.folder-trigger {
  cursor: pointer;
  color: rgb(var(--v-theme-secondary));
  transition: color 0.15s;
}
.folder-trigger:hover {
  color: rgb(var(--v-theme-primary));
}

.browser-panel {
  max-height: 420px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.border-b {
  border-bottom: 1px solid rgb(var(--v-border-color));
}

.browser-tree {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}
</style>
