<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTaskDetail, getJob, getJobTasks, retryTask, deleteTask, updateAssetFull, getLanguages, getCreators, getCategories, getTags, createTag, createCategory } from '../api/modules'
import NodeLogTimeline, { type NodeLogEntry } from '../components/workshop/NodeLogTimeline.vue'
import { Client } from '@stomp/stompjs'
const route = useRoute()
const router = useRouter()
const id = route.params.id as string
let stompClient: Client | null = null

const task = ref<any>(null)
const asset = ref<any>(null)
const bookDetail = ref<any>(null)
const creators = ref<any[]>([])
const externalIds = ref<any[]>([])
const currentTags = ref<string[]>([])
const currentCategories = ref<string[]>([])
const nodeLogs = ref<NodeLogEntry[]>([])
const job = ref<any>(null)
const relatedTasks = ref<any[]>([])
const loading = ref(true)
const retrying = ref(false)
const deleting = ref(false)
const err = ref('')
const toastMsg = ref('')
const summaryExpanded = ref(false)
const toastType = ref<'success' | 'error'>('success')

// Reference data
const allLanguages = ref<any[]>([])
const allCreators = ref<any[]>([])
const allCategories = ref<any[]>([])
const allTags = ref<any[]>([])

// Edit dialog
const editDialog = ref(false)
const editSaving = ref(false)
const editForm = ref({
  title: '', subtitle: '', summary: '', coverUrl: '',
  mediaType: 'BOOK',
  publisher: '', publishedDate: '', language: '',
  pages: null as number | null, wordCount: null as number | null, chapterCount: null as number | null,
  seriesName: '', seriesNumber: null as number | null, totalBooks: null as number | null,
  completionStatus: '', rating: null as number | null,
})
const editTags = ref<string[]>([])
const editCategories = ref<string[]>([])
const editAuthors = ref<string[]>([])
const editTranslators = ref<string[]>([])
const editExternalIds = ref<{source:string,identifier:string}[]>([])
const authorSearch = ref('')
const translatorSearch = ref('')
const editLocked = ref<Set<string>>(new Set())

const externalIdSources = ['douban', 'isbn10', 'isbn13', 'asin']
function addExternalId() { editExternalIds.value.push({ source: 'douban', identifier: '' }) }
function removeExternalId(idx: number) { editExternalIds.value.splice(idx, 1) }

function addAuthorFromSearch() {
  const n = authorSearch.value.trim()
  if (n && !editAuthors.value.includes(n)) { editAuthors.value.push(n) }
  authorSearch.value = ''
}
function addTranslatorFromSearch() {
  const n = translatorSearch.value.trim()
  if (n && !editTranslators.value.includes(n)) { editTranslators.value.push(n) }
  translatorSearch.value = ''
}

const creatorNameItems = computed(() => allCreators.value.map((x: any) => x.name))

function toggleLock(field: string) {
  if (editLocked.value.has(field)) editLocked.value.delete(field)
  else editLocked.value.add(field)
  editLocked.value = new Set(editLocked.value)
}

// Delete dialog
const deleteDialog = ref(false)
const deleteAssetOpt = ref(true)
const deleteSourceOpt = ref(false)

function showToast(msg: string, type: 'success' | 'error' = 'success') {
  toastMsg.value = msg; toastType.value = type
  setTimeout(() => { toastMsg.value = '' }, 3000)
}

async function loadRefData() {
  try {
    const [langs, creators, cats, tags] = await Promise.all([
      getLanguages(), getCreators(), getCategories(), getTags()
    ])
    allLanguages.value = langs.data || []
    allCreators.value = creators.data || []
    allCategories.value = cats.data || []
    allTags.value = tags.data || []
  } catch { /* optional */ }
}

function openEditDialog() {
  const a = asset.value, bd = bookDetail.value
  editForm.value = {
    title: a?.title || '',
    subtitle: bd?.subtitle || '',
    summary: a?.summary || '',
    coverUrl: a?.coverUrl || '',
    mediaType: a?.mediaType || 'BOOK',
    publisher: bd?.publisher || '',
    publishedDate: bd?.publishedDate ? bd.publishedDate.substring(0,7) : '',
    language: bd?.language || '',
    pages: bd?.pages ?? null,
    wordCount: bd?.wordCount ?? null,
    chapterCount: bd?.chapterCount ?? null,
    seriesName: bd?.seriesName || '', seriesNumber: bd?.seriesNumber ?? null,
    totalBooks: bd?.totalBooks ?? null,
    completionStatus: bd?.completionStatus || '',
    rating: bd?.rating ?? null,
  }
  editTags.value = [...currentTags.value]
  editCategories.value = [...currentCategories.value]
  editAuthors.value = creators.value.filter((c: any) => !c.role || c.role === '作者').map((c: any) => c.name)
  editTranslators.value = creators.value.filter((c: any) => c.role === '译者').map((c: any) => c.name)
  editExternalIds.value = (externalIds.value || []).map((x: any) => ({ source: x.source, identifier: x.identifier }))
  // Merge locked fields from both Asset and BookDetail
  const aLocked: string[] = a?.lockedFields || []
  const bLocked: string[] = bd?.lockedFields || []
  editLocked.value = new Set([...aLocked, ...bLocked])
  editDialog.value = true
}

async function saveEdit() {
  if (!task.value?.assetId) return
  editSaving.value = true
  try {
    // Resolve tag IDs (create new ones if needed)
    const tagIds: string[] = []
    for (const name of editTags.value) {
      const n = name.trim(); if (!n) continue
      let t = allTags.value.find((x: any) => x.name === n)
      if (!t) t = (await getTags()).data.find((x: any) => x.name === n)
      if (!t) { t = (await createTag({ name: n })).data; allTags.value.push(t) }
      tagIds.push(t.id)
    }
    const catIds: string[] = []
    for (const name of editCategories.value) {
      const n = name.trim(); if (!n) continue
      let c = allCategories.value.find((x: any) => x.name === n)
      if (!c) c = (await getCategories()).data.find((x: any) => x.name === n)
      if (!c) { c = (await createCategory({ name: n })).data; allCategories.value.push(c) }
      catIds.push(c.id)
    }

    // Deduplicate by name+role
    const seen = new Set<string>()
    const creators: {name:string,role:string}[] = []
    for (const name of editAuthors.value) {
      const n = name.trim(); if (!n) continue
      if (!seen.has(n + '|作者')) { seen.add(n + '|作者'); creators.push({ name: n, role: '作者' }) }
    }
    for (const name of editTranslators.value) {
      const n = name.trim(); if (!n) continue
      if (!seen.has(n + '|译者')) { seen.add(n + '|译者'); creators.push({ name: n, role: '译者' }) }
    }
    const extIds = editExternalIds.value.filter(x => x.source && x.identifier).map(x => ({ source: x.source, identifier: x.identifier }))
    const payload = {
      ...editForm.value,
      tagIds,
      categoryIds: catIds,
      creators,
      externalIds: extIds,
      lockedFields: JSON.stringify([...editLocked.value]),
    }
    await updateAssetFull(task.value.assetId, payload)
    editDialog.value = false
    // Reload to get fresh data
    const { data } = await getTaskDetail(id)
    task.value = data.task; asset.value = data.asset; bookDetail.value = data.bookDetail
    creators.value = data.creators || []; externalIds.value = data.externalIds || []
    nodeLogs.value = data.nodeLogs || []; showToast('保存成功')
  } catch { showToast('保存失败', 'error') }
  finally { editSaving.value = false }
}

// Load ref data on mount
loadRefData()

async function doRetry() {
  retrying.value = true
  try {
    await retryTask(id)
    showToast('已重新提交执行')
  } catch { showToast('重试失败', 'error') }
  finally { retrying.value = false }
}

async function doDelete() {
  deleting.value = true
  try {
    await deleteTask(id, deleteAssetOpt.value, deleteSourceOpt.value)
    showToast('删除成功')
    setTimeout(() => router.push('/workshop/records'), 800)
  } catch { showToast('删除失败', 'error'); deleting.value = false }
}

const taskStatusCfg: Record<string, { label: string; color: string }> = {
  PENDING:  { label: '待执行', color: '#8e92a8' },
  RUNNING:  { label: '执行中', color: '#5b8def' },
  SUCCESS:  { label: '成功', color: '#3dd68c' },
  FAILED:   { label: '失败', color: '#f06580' },
  SKIPPED:  { label: '跳过', color: '#8e92a8' },
}
function sts(s: string) { return taskStatusCfg[s] || { label: s, color: '#8e92a8' } }
function fmt(d: string) { if (!d) return '-'; return new Date(d).toLocaleString('zh-CN') }

function getCoverUrl() {
  if (asset.value?.coverUrl) return asset.value.coverUrl
  const title = asset.value?.title || task.value?.filePath?.split('/').pop() || '?'
  const colors = ['#7c5cfc', '#e94560', '#3dd68c', '#f0a840', '#5b8def']
  const idx = title.charCodeAt(0) % colors.length
  return `https://placehold.co/300x420/${colors[idx].replace('#','')}/ffffff?text=${encodeURIComponent(title.substring(0,2))}&font=inter`
}

function getTitle() {
  return asset.value?.title || task.value?.filePath?.split('/').pop() || '未命名'
}

const completionStatusLabel: Record<string, string> = {
  ongoing: '连载中', completed: '已完结',
}
const extSourceLabels: Record<string, string> = {
  douban: '豆瓣 ID', isbn10: 'ISBN-10', isbn13: 'ISBN-13', asin: 'ASIN',
}
function extSourceLabel(s: string) { return extSourceLabels[s] || s }

// Fields for left column data table
interface DetailField {
  key: string
  label: string
  value: any
}
const alwaysShowKeys = new Set(['title','author','publishedDate','publisher','language','completionStatus'])
const detailFields = computed<DetailField[]>(() => {
  const bd = bookDetail.value
  const a = asset.value
  const authorStr = creators.value
    .filter((c: any) => !c.role || c.role === '作者')
    .map((c: any) => c.name).join('、') || null
  const fields: DetailField[] = [
    { key: 'title',          label: '标题',      value: a?.title },
    { key: 'author',         label: '作者',      value: authorStr },
    { key: 'publishedDate',  label: '出版日期',  value: bd?.publishedDate?.substring(0,7) || '' },
    { key: 'publisher',      label: '出版社',    value: bd?.publisher },
    { key: 'language',       label: '语言',      value: bd?.language },
    { key: 'completionStatus', label: '连载状态', value: completionStatusLabel[bd?.completionStatus || ''] || bd?.completionStatus },
    { key: 'subtitle',       label: '副标题',    value: bd?.subtitle },
    ...externalIds.value.map((x: any) => ({
      key: `ext_${x.source}`, label: extSourceLabel(x.source), value: x.identifier
    })),
    { key: 'pages',          label: '页数',      value: bd?.pages },
    { key: 'wordCount',      label: '字数',      value: bd?.wordCount != null ? formatNumber(bd.wordCount) : null },
    { key: 'chapterCount',   label: '章节数',    value: bd?.chapterCount },
    { key: 'seriesName',     label: '系列',      value: bd?.seriesName },
    { key: 'seriesNumber',   label: '系列编号',  value: bd?.seriesNumber },
    { key: 'totalBooks',     label: '总册数',    value: bd?.totalBooks },
    { key: 'rating',         label: '评分',      value: bd?.rating },
  ]
  return fields.filter(f => alwaysShowKeys.has(f.key) || (f.value != null && f.value !== ''))
})

function formatNumber(n: number | null) {
  if (n == null) return '-'
  if (n > 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

onMounted(async () => {
  try {
    const { data } = await getTaskDetail(id)
    task.value = data.task
    asset.value = data.asset
    bookDetail.value = data.bookDetail
    creators.value = data.creators || []
    externalIds.value = data.externalIds || []
    currentTags.value = (data.tags || []).map((t: any) => t.name)
    currentCategories.value = (data.categories || []).map((c: any) => c.name)
    nodeLogs.value = data.nodeLogs || []

    if (data.task?.jobId) {
      try {
        const [jobRes, tasksRes] = await Promise.all([
          getJob(data.task.jobId),
          getJobTasks(data.task.jobId, 1, 100)
        ])
        job.value = jobRes.data
        relatedTasks.value = (tasksRes.data?.items || []).filter((t: any) => t.id !== id)
      } catch { /* optional */ }
    }
  } catch (e: any) {
    err.value = e?.response?.status === 404 ? '任务不存在' : e?.message || '加载失败'
  } finally { loading.value = false }
  connectWs()
})

function connectWs() {
  if (stompClient) stompClient.deactivate()
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  stompClient = new Client({
    brokerURL: proto + '//' + location.host + '/ws',
    reconnectDelay: 3000,
    onConnect: () => {
      stompClient!.subscribe('/topic/task/' + id, (msg) => {
        const body = JSON.parse(msg.body)
        if (body.type === 'logsCleared') {
          nodeLogs.value = []
        } else if (body.type === 'nodeLog' && body.data) {
          const log = body.data as NodeLogEntry
          if (!nodeLogs.value.some(l => l.id === log.id)) {
            nodeLogs.value.push(log)
          }
        } else if (body.type === 'statusChanged') {
          if (task.value) task.value.status = body.status
        }
      })
    },
  })
  stompClient.activate()
}

onUnmounted(() => { stompClient?.deactivate() })
</script>

<template>
  <div class="rd-page">
    <div class="rd-top">
      <v-btn icon="mdi-arrow-left" size="small" variant="plain" @click="router.push('/workshop/records')" />
      <span class="text-caption text-disabled ml-2">任务记录</span>
    </div>

    <v-progress-linear v-if="loading" indeterminate color="primary" />
    <div v-if="err" class="text-center py-8 text-disabled">{{ err }}</div>

    <template v-if="task">
      <!-- ====== Header ====== -->
      <div class="rd-hero">
        <div class="rd-cover">
          <img :src="getCoverUrl()" :alt="getTitle()" />
        </div>
        <div class="rd-meta">
          <h1 class="rd-title">{{ getTitle() }}</h1>

          <div v-if="creators.length" class="rd-creators mb-2">
            <span v-if="creators.find((c:any) => !c.role || c.role==='作者')" class="rd-creator-item">
              <v-icon icon="mdi-account-edit" size="14" color="secondary" />
              {{ creators.filter((c:any) => !c.role || c.role==='作者').map((c:any) => c.name).join('、') }}
            </span>
            <span v-if="creators.find((c:any) => c.role==='译者')" class="rd-creator-item">
              <v-icon icon="mdi-translate" size="14" color="secondary" />
              {{ creators.filter((c:any) => c.role==='译者').map((c:any) => c.name).join('、') }} 译
            </span>
          </div>

          <!-- Action buttons + status -->
          <div class="rd-actions">
            <v-btn icon="mdi-pencil" size="small" variant="tonal" color="primary"
              :disabled="!task.assetId" @click="openEditDialog" />
            <v-btn icon="mdi-refresh" size="small" variant="tonal" color="warning"
              :disabled="task.status === 'RUNNING'" :loading="retrying" @click="doRetry" />
            <v-btn icon="mdi-delete" size="small" variant="tonal" color="error"
              :loading="deleting" @click="deleteDialog = true" />
            <span class="stb" :style="{ background: sts(task.status).color }">{{ sts(task.status).label }}</span>
          </div>

          <!-- Metadata row -->
          <div class="rd-metas">
            <span v-if="bookDetail?.rating" class="rd-meta-item">
              <v-icon icon="mdi-star" size="14" color="#f0a840" /> {{ bookDetail.rating }}
            </span>
            <span v-if="bookDetail?.publishedDate" class="rd-meta-item">{{ bookDetail.publishedDate }}</span>
            <span v-if="bookDetail?.pages" class="rd-meta-item">{{ bookDetail.pages }}页</span>
            <span v-if="bookDetail?.wordCount" class="rd-meta-item">{{ formatNumber(bookDetail.wordCount) }}字</span>
            <span v-if="bookDetail?.publisher" class="rd-meta-item">{{ bookDetail.publisher }}</span>
          </div>

          <!-- Tags -->
          <div v-if="bookDetail?.completionStatus || bookDetail?.language" class="rd-tags">
            <span v-if="bookDetail?.language" class="rd-tag">{{ bookDetail.language }}</span>
            <span v-if="bookDetail?.completionStatus" class="rd-tag type">
              {{ completionStatusLabel[bookDetail.completionStatus] || bookDetail.completionStatus }}
            </span>
            <span v-if="asset?.mediaType" class="rd-tag type">{{ asset.mediaType }}</span>
          </div>

          <!-- Summary -->
          <div v-if="asset?.summary" class="rd-summary-wrap">
            <p class="rd-summary" :class="{ clamped: !summaryExpanded && asset.summary.length > 200 }">{{ asset.summary }}</p>
            <span v-if="asset.summary.length > 200" class="rd-summary-toggle" @click="summaryExpanded = !summaryExpanded">
              {{ summaryExpanded ? '收起' : '展开全部' }}
            </span>
          </div>
        </div>
      </div>

      <!-- ====== Bottom Two-Column ====== -->
      <div class="rd-body">
        <!-- Left: Detailed Data -->
        <div class="rd-left">
          <div class="rd-card">
            <div class="rd-card-title">详细数据</div>
            <div v-if="detailFields.length" class="rd-detail-list">
              <div v-for="f in detailFields" :key="f.key" class="rd-detail-row">
                <span class="rd-detail-label">{{ f.label }}</span>
                <span class="rd-detail-value">{{ f.value || '-' }}</span>
              </div>
            </div>
            <div v-else class="rd-empty">暂无详细数据</div>
          </div>

          <!-- Task info card -->
          <div class="rd-card mt-4">
            <div class="rd-card-title">任务信息</div>
            <div class="rd-detail-list">
              <div class="rd-detail-row">
                <span class="rd-detail-label">文件路径</span>
                <span class="rd-detail-value mono">{{ task.filePath || '-' }}</span>
                </div>
              <div class="rd-detail-row">
                <span class="rd-detail-label">创建时间</span>
                <span class="rd-detail-value">{{ fmt(task.createdAt) }}</span>
                </div>
              <div class="rd-detail-row">
                <span class="rd-detail-label">更新时间</span>
                <span class="rd-detail-value">{{ fmt(task.updatedAt) }}</span>
                </div>
              <div v-if="job" class="rd-detail-row">
                <span class="rd-detail-label">所属作业</span>
                <span class="rd-detail-value">{{ job.name }}</span>
                </div>
              <div v-if="task.errorMessage" class="rd-detail-row">
                <span class="rd-detail-label">错误信息</span>
                <span class="rd-detail-value err">{{ task.errorMessage }}</span>
                </div>
            </div>
          </div>
        </div>

        <!-- Right: Node Execution Log -->
        <div class="rd-right">
          <div class="rd-card">
            <div class="rd-card-title">节点执行日志</div>
            <NodeLogTimeline :node-logs="nodeLogs" />
          </div>

          <!-- Related tasks -->
          <div v-if="relatedTasks.length" class="rd-card mt-4">
            <div class="rd-card-title">同作业其他文件 ({{ relatedTasks.length }})</div>
            <div class="rd-related">
              <div v-for="rt in relatedTasks" :key="rt.id" class="rd-rel-item"
                @click="router.push(`/workshop/records/${rt.id}`)">
                <v-icon icon="mdi-file-document-outline" size="18" color="secondary" />
                <span class="rd-rel-name">{{ rt.filePath?.split('/').pop() || '-' }}</span>
                <span class="stb" :style="{ background: sts(rt.status).color }">{{ sts(rt.status).label }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Edit Dialog -->
    <v-dialog v-model="editDialog" max-width="800" scrollable>
      <v-card>
        <v-card-title>编辑资产信息</v-card-title>
        <v-card-text class="edit-body">
          <!-- Row 1: title + subtitle -->
          <v-row dense class="mt-2"><v-col cols="8"><v-text-field v-model="editForm.title" label="标题" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('title')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('title')" /></template></v-text-field></v-col><v-col cols="4"><v-text-field v-model="editForm.subtitle" label="副标题" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('subtitle')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('subtitle')" /></template></v-text-field></v-col></v-row>
          <!-- Row 2: authors + translators -->
          <v-row dense class="mt-4"><v-col cols="6"><v-combobox v-model="editAuthors" v-model:search="authorSearch" :items="creatorNameItems" label="作者" chips multiple closable-chips density="compact" hide-details @keydown.enter.prevent="addAuthorFromSearch"><template #append-inner><v-icon :icon="editLocked.has('authors')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('authors')" /></template></v-combobox></v-col><v-col cols="6"><v-combobox v-model="editTranslators" v-model:search="translatorSearch" :items="creatorNameItems" label="译者" chips multiple closable-chips density="compact" hide-details @keydown.enter.prevent="addTranslatorFromSearch"><template #append-inner><v-icon :icon="editLocked.has('translators')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('translators')" /></template></v-combobox></v-col></v-row>
          <!-- Row 3: publishedDate + language + publisher -->
          <v-row dense class="mt-4"><v-col cols="4"><v-text-field v-model="editForm.publishedDate" label="出版日期 (年月)" type="month" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('publishedDate')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('publishedDate')" /></template></v-text-field></v-col><v-col cols="4"><v-select v-model="editForm.language" :items="allLanguages" item-title="name" item-value="code" label="语言" density="compact" hide-details clearable><template #append-inner><v-icon :icon="editLocked.has('language')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('language')" /></template></v-select></v-col><v-col cols="4"><v-text-field v-model="editForm.publisher" label="出版社" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('publisher')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('publisher')" /></template></v-text-field></v-col></v-row>
          <!-- Row 3b: mediaType -->
          <v-row dense class="mt-4"><v-col cols="6"><v-select v-model="editForm.mediaType" :items="['BOOK','COMIC','VIDEO','UNKNOWN']" label="媒体类型" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('mediaType')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('mediaType')" /></template></v-select></v-col></v-row>
          <!-- Row 4: pages + wordCount + chapterCount -->
          <v-row dense class="mt-4"><v-col cols="4"><v-text-field v-model.number="editForm.pages" label="页数" type="number" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('pages')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('pages')" /></template></v-text-field></v-col><v-col cols="4"><v-text-field v-model.number="editForm.wordCount" label="字数" type="number" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('wordCount')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('wordCount')" /></template></v-text-field></v-col><v-col cols="4"><v-text-field v-model.number="editForm.chapterCount" label="章节数" type="number" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('chapterCount')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('chapterCount')" /></template></v-text-field></v-col></v-row>
          <!-- Row 5: completionStatus + rating -->
          <v-row dense class="mt-4"><v-col cols="6"><v-select v-model="editForm.completionStatus" :items="[{title:'连载中',value:'ongoing'},{title:'已完结',value:'completed'}]" label="连载状态" density="compact" hide-details clearable><template #append-inner><v-icon :icon="editLocked.has('completionStatus')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('completionStatus')" /></template></v-select></v-col><v-col cols="6"><v-text-field v-model.number="editForm.rating" label="评分" type="number" min="0" max="10" step="0.1" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('rating')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('rating')" /></template></v-text-field></v-col></v-row>
          <!-- Row 6: External IDs -->
          <div class="mt-4">
            <div class="text-caption text-disabled mb-1">外部标识</div>
            <div v-for="(ext, i) in editExternalIds" :key="i" class="d-flex gap-2 mb-2">
              <v-select v-model="ext.source" :items="externalIdSources" label="来源" density="compact" hide-details style="max-width:120px" />
              <v-text-field v-model="ext.identifier" label="ID" density="compact" hide-details />
              <v-btn icon="mdi-close" size="x-small" variant="text" color="error" @click="removeExternalId(i)" />
            </div>
            <v-btn size="x-small" variant="tonal" prepend-icon="mdi-plus" @click="addExternalId">添加</v-btn>
          </div>
          <!-- Row 8: seriesName + seriesNumber + totalBooks -->
          <v-row dense class="mt-4"><v-col cols="5"><v-text-field v-model="editForm.seriesName" label="系列名" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('seriesName')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('seriesName')" /></template></v-text-field></v-col><v-col cols="3"><v-text-field v-model.number="editForm.seriesNumber" label="册号" type="number" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('seriesNumber')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('seriesNumber')" /></template></v-text-field></v-col><v-col cols="4"><v-text-field v-model.number="editForm.totalBooks" label="总册数" type="number" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('totalBooks')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('totalBooks')" /></template></v-text-field></v-col></v-row>
          <!-- Row 9: coverUrl + summary -->
          <v-row dense class="mt-4"><v-col cols="12"><v-text-field v-model="editForm.coverUrl" label="封面 URL" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('coverUrl')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('coverUrl')" /></template></v-text-field></v-col></v-row>
          <v-row dense class="mt-4"><v-col cols="12"><v-textarea v-model="editForm.summary" label="简介" rows="3" density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('summary')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('summary')" /></template></v-textarea></v-col></v-row>
          <!-- Tags -->
          <div class="mt-4"><v-combobox v-model="editTags" :items="allTags.map((x:any)=>x.name)" label="标签" chips multiple closable-chips density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('tags')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('tags')" /></template></v-combobox></div>
          <!-- Categories -->
          <div class="mt-4"><v-combobox v-model="editCategories" :items="allCategories.map((x:any)=>x.name)" label="分类" chips multiple closable-chips density="compact" hide-details><template #append-inner><v-icon :icon="editLocked.has('categories')?'mdi-lock':'mdi-lock-open-variant'" size="16" class="lock-icon" @click.stop="toggleLock('categories')" /></template></v-combobox></div>
        </v-card-text>
        <v-card-actions class="mt-2">
          <v-spacer />
          <v-btn variant="text" @click="editDialog = false">取消</v-btn>
          <v-btn color="primary" @click="saveEdit" :loading="editSaving">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Delete Dialog -->
    <v-dialog v-model="deleteDialog" max-width="420">
      <v-card>
        <v-card-title>确认删除</v-card-title>
        <v-card-text>
          <p class="mb-4">此操作将删除该任务记录。是否同时删除关联数据？</p>
          <v-checkbox v-model="deleteAssetOpt" label="同时删除关联资产及文件" density="compact" hide-details class="mb-2" />
          <v-checkbox v-model="deleteSourceOpt" label="同时删除源文件" density="compact" hide-details />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteDialog = false">取消</v-btn>
          <v-btn color="error" @click="doDelete">确认删除</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Toast -->
    <div v-if="toastMsg" class="rd-toast" :class="toastType">{{ toastMsg }}</div>
  </div>
</template>

<style scoped>
.rd-page { height: 100%; overflow: auto; padding: 16px 24px 24px; }
.rd-top { display: flex; align-items: center; margin-bottom: 12px; }

/* ====== Header ====== */
.rd-hero {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  padding: 20px;
  background: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-border-color));
  border-radius: 12px;
  margin-bottom: 16px;
}
.rd-cover {
  flex-shrink: 0;
  width: 160px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,.15);
}
.rd-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  aspect-ratio: 2/3;
  display: block;
}
.rd-meta { flex: 1; min-width: 0; }
.rd-title { font-size: 22px; font-weight: 700; margin: 0 0 12px; line-height: 1.2; }

.rd-actions { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }

.rd-metas { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; flex-wrap: wrap; font-size: 13px; color: rgb(var(--v-theme-secondary)); }
.rd-meta-item { display: inline-flex; align-items: center; gap: 3px; }

.rd-tags { display: flex; gap: 6px; margin-bottom: 12px; flex-wrap: wrap; }
.rd-tag {
  font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 4px;
  background: rgb(var(--v-theme-surface-variant)); color: rgb(var(--v-theme-secondary));
  border: 1px solid rgb(var(--v-border-color));
}
.rd-tag.type { background: rgba(124,92,252,.12); color: rgb(var(--v-theme-primary)); border-color: rgba(124,92,252,.3); }

.rd-summary { color: rgb(var(--v-theme-secondary)); font-size: 13px; line-height: 1.7; margin: 0 0 4px; word-break: break-word; }
.rd-summary.clamped { display: -webkit-box; -webkit-line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; }
.rd-summary-toggle { font-size: 12px; color: rgb(var(--v-theme-primary)); cursor: pointer; user-select: none; }
.rd-summary-toggle:hover { text-decoration: underline; }

.rd-creators { display: flex; gap: 8px; flex-wrap: wrap; }
.rd-creator-item { display: inline-flex; align-items: center; gap: 4px; font-size: 13px; color: rgb(var(--v-theme-secondary)); }

/* ====== Two-Column ====== */
.rd-body { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 800px) { .rd-body { grid-template-columns: 1fr; } }
.rd-left, .rd-right { min-width: 0; }

/* ====== Cards ====== */
.rd-card { background: rgb(var(--v-theme-surface)); border: 1px solid rgb(var(--v-border-color)); border-radius: 10px; padding: 16px; }
.rd-card-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid rgb(var(--v-border-color)); }
.rd-empty { text-align: center; padding: 20px; color: rgb(var(--v-theme-secondary)); font-size: 13px; }

/* ====== Detail rows ====== */
.rd-detail-list { display: flex; flex-direction: column; gap: 4px; }
.rd-detail-row {
  display: flex; align-items: center; gap: 10px;
  padding: 7px 10px; border-radius: 6px; font-size: 13px;
  transition: background .12s;
}
.rd-detail-row:hover { background: rgb(var(--v-theme-surface-variant)); }
.rd-detail-label { color: rgb(var(--v-theme-secondary)); flex-shrink: 0; min-width: 70px; }
.rd-detail-value { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rd-detail-value.mono { font-family: 'JetBrains Mono', monospace; font-size: 11px; }
.rd-detail-value.err { color: rgb(var(--v-theme-error)); }
.lock-icon { cursor: pointer; opacity: .4; transition: opacity .15s; }
.lock-icon:hover { opacity: 1; }

/* ====== Related ====== */
.rd-related { display: flex; flex-direction: column; gap: 6px; }
.rd-rel-item { display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-radius: 6px; cursor: pointer; transition: background .15s; }
.rd-rel-item:hover { background: rgb(var(--v-theme-surface-variant)); }
.rd-rel-name { flex: 1; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Status badge */
.stb { display: inline-block; font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; color: #fff; }

/* Toast */
.rd-toast { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); padding: 10px 24px; border-radius: 8px; font-size: 13px; font-weight: 500; z-index: 9999; color: #fff; }
.rd-toast.success { background: #3dd68c; }
.rd-toast.error { background: #f06580; }

/* Edit dialog */
.edit-body { max-height: 60vh; overflow-y: auto; }
.chip-row { display: flex; flex-wrap: wrap; gap: 2px; }

</style>
