<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAssetStore } from '../stores/asset'
import { useToast } from '../composables/useToast'

const { toast } = useToast()

const route = useRoute()
const router = useRouter()
const assetStore = useAssetStore()
const editDialogVisible = ref(false)

const editForm = ref({
  title: '',
  summary: '',
  publishYear: null as number | null,
  coverUrl: '',
})

onMounted(() => {
  assetStore.fetchAssetDetail(route.params.id as string)
})

const asset = computed(() => assetStore.currentAsset?.asset)
const files = computed(() => assetStore.currentAsset?.files || [])

function openEditDialog() {
  if (asset.value) {
    editForm.value = {
      title: asset.value.title || '',
      summary: asset.value.summary || '',
      publishYear: asset.value.publishYear,
      coverUrl: asset.value.coverUrl || '',
    }
  }
  editDialogVisible.value = true
}

async function saveEdit() {
  try {
    await assetStore.updateAssetDetail(route.params.id as string, editForm.value)
    editDialogVisible.value = false
    toast.success('更新成功')
  } catch (e) {
    toast.error('更新失败')
  }
}

function getCoverUrl() {
  if (asset.value?.coverUrl) return asset.value.coverUrl
  const colors = ['#7c5cfc', '#e94560', '#3dd68c', '#f0a840', '#5b8def']
  const idx = asset.value?.title ? asset.value.title.charCodeAt(0) % colors.length : 0
  const c = colors[idx]
  const text = asset.value?.title?.substring(0, 2) || 'B'
  return `https://placehold.co/300x420/${c.replace('#','')}/ffffff?text=${encodeURIComponent(text)}&font=inter`
}

function formatSize(bytes: number | null) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}
</script>

<template>
  <div class="detail-view" v-if="asset">
    <v-progress-linear v-if="assetStore.loading" indeterminate color="primary" class="mb-4" />
    <!-- Back -->
    <button class="back-btn" @click="router.push('/')">
      <v-icon icon="mdi-arrow-left" size="18" />
      <span>返回</span>
    </button>

    <!-- Hero -->
    <div class="detail-hero">
      <div class="hero-cover">
        <img :src="getCoverUrl()" :alt="asset.title" />
      </div>
      <div class="hero-meta">
        <h1 class="hero-title">{{ asset.title }}</h1>
        <div class="hero-tags">
          <span class="meta-tag" v-if="asset.publishYear">{{ asset.publishYear }}</span>
          <span class="meta-tag type">{{ asset.mediaType }}</span>
        </div>
        <p class="hero-summary">{{ asset.summary || '暂无简介' }}</p>
        <div class="hero-actions">
          <v-btn color="primary" prepend-icon="mdi-pencil" @click="openEditDialog">编辑</v-btn>
        </div>
      </div>
    </div>

    <!-- Files Section -->
    <div class="detail-files">
      <h3>物理文件</h3>
      <div class="file-list">
        <div v-for="file in files" :key="file.id" class="file-item">
          <div class="file-icon">
            <span class="file-ext">{{ file.fileFormat }}</span>
          </div>
          <div class="file-info">
            <div class="file-name">{{ file.relativePath }}</div>
            <div class="file-meta">
              <span>{{ file.storageNode?.name || '-' }}</span>
              <span> · {{ formatSize(file.fileSize) }}</span>
              <span v-if="file.isPrimary"> · 主文件</span>
            </div>
          </div>
          <v-btn icon="mdi-download" size="small" variant="plain" />
        </div>
        <div v-if="!files.length" class="file-empty">暂无关联物理文件</div>
      </div>
    </div>

    <!-- Edit Dialog -->
    <v-dialog v-model="editDialogVisible" max-width="480">
      <v-card>
        <v-card-title>编辑元数据</v-card-title>
        <v-card-text>
          <v-text-field v-model="editForm.title" label="标题" class="mb-4" />
          <v-text-field v-model.number="editForm.publishYear" label="年份" type="number" :min="0" :max="2100" class="mb-4" />
          <v-text-field v-model="editForm.coverUrl" label="封面" class="mb-4" />
          <v-textarea v-model="editForm.summary" label="简介" :rows="4" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="editDialogVisible = false">取消</v-btn>
          <v-btn color="primary" @click="saveEdit">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.detail-view {
  padding: 20px 24px;
  max-width: 900px;
  margin: 0 auto;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 6px;
  margin-bottom: 16px;
  transition: all 0.15s;
}
.back-btn:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

/* ========== Hero ========== */
.detail-hero {
  display: flex;
  gap: 28px;
  margin-bottom: 28px;
}

.hero-cover {
  flex-shrink: 0;
  width: 200px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: var(--cover-shadow);
}
.hero-cover img {
  width: 100%;
  display: block;
}

.hero-meta {
  flex: 1;
  min-width: 0;
}

.hero-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 12px;
  line-height: 1.2;
}

.hero-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.meta-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 4px;
  background: var(--bg-elevated);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}
.meta-tag.type {
  background: rgba(124,92,252,0.12);
  color: var(--accent);
  border-color: rgba(124,92,252,0.3);
}

.hero-summary {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 18px;
}

.hero-actions {
  display: flex;
  gap: 10px;
}

/* ========== Files ========== */
.detail-files {
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 16px;
}

.detail-files h3 {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  transition: background 0.12s;
}
.file-item:hover {
  background: var(--bg-hover);
}

.file-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: rgba(124,92,252,0.12);
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-ext {
  font-size: 10px;
  font-weight: 700;
  color: var(--accent);
  text-transform: uppercase;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-meta {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.file-empty {
  text-align: center;
  padding: 24px;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
