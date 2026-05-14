<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAssetStore } from '../../stores/asset'

const router = useRouter()
const route = useRoute()
const assetStore = useAssetStore()

const searchText = ref('')
const viewMode = ref<'grid' | 'list'>('grid')
const filterVisible = ref(false)

// Filter state
const selectedCategories = ref<string[]>([])
const selectedTags = ref<string[]>([])

onMounted(async () => {
  await assetStore.fetchAssets()
  // Populate search from query
  if (route.query.q) {
    searchText.value = route.query.q as string
  }
})

const filteredAssets = computed(() => {
  let list = assetStore.assets
  if (searchText.value) {
    const q = searchText.value.toLowerCase()
    list = list.filter((a: any) => a.title?.toLowerCase().includes(q))
  }
  return list
})

function goToDetail(id: string) {
  router.push(`/asset/${id}`)
}

function getCoverUrl(asset: any) {
  if (asset.coverUrl) return asset.coverUrl
  // Generate a deterministic placeholder color based on title
  const colors = ['#7c5cfc', '#e94560', '#3dd68c', '#f0a840', '#5b8def', '#d946a8', '#14b8a6']
  const idx = asset.title ? asset.title.charCodeAt(0) % colors.length : 0
  const c = colors[idx]
  const text = asset.title?.substring(0, 2) || 'B'
  return `https://placehold.co/200x280/${c.replace('#','')}/ffffff?text=${encodeURIComponent(text)}&font=inter`
}

// Context menu
const contextMenuVisible = ref(false)
const contextMenuAsset = ref<any>(null)
const contextMenuPos = ref({ x: 0, y: 0 })

function showContextMenu(asset: any, event: MouseEvent) {
  event.preventDefault()
  event.stopPropagation()
  contextMenuAsset.value = asset
  contextMenuPos.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
}

function closeContextMenu() {
  contextMenuVisible.value = false
}
</script>

<template>
  <div class="library-view" @click="closeContextMenu">
    <!-- Title Bar -->
    <div class="library-titlebar">
      <div class="titlebar-left">
        <h2 class="titlebar-heading">All Books</h2>
        <span class="titlebar-count">{{ filteredAssets.length }} 项</span>
      </div>
      <div class="titlebar-right">
        <v-text-field
          v-model="searchText"
          placeholder="过滤..."
          prepend-inner-icon="mdi-magnify"
          hide-details
          density="compact"
          clearable
          class="filter-input"
        />
        <div class="view-toggle">
          <button :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'">
            <v-icon icon="mdi-view-grid" size="small" />
          </button>
          <button :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">
            <v-icon icon="mdi-view-list" size="small" />
          </button>
        </div>
      </div>
    </div>

    <!-- Grid View -->
    <div v-if="viewMode === 'grid'" class="poster-grid">
      <v-progress-linear v-if="assetStore.loading" indeterminate color="primary" class="grid-loader" />
      <div
        v-for="asset in filteredAssets"
        :key="asset.id"
        class="poster-card"
        @click="goToDetail(asset.id)"
      >
        <div class="card-cover">
          <img :src="getCoverUrl(asset)" :alt="asset.title" loading="lazy" />
          <div class="card-overlay">
            <button class="card-more" @click.stop="showContextMenu(asset, $event)">
              <v-icon icon="mdi-dots-horizontal" size="14" />
            </button>
          </div>
        </div>
        <div class="card-title">{{ asset.title }}</div>
      </div>

      <div v-if="!assetStore.loading && filteredAssets.length === 0" class="empty-grid">
        <v-icon icon="mdi-folder-open-outline" size="48" color="secondary" class="mb-2" />
        <div class="text-caption text-disabled">暂无资产</div>
      </div>
    </div>

    <!-- List View -->
    <div v-else class="asset-list">
      <v-progress-linear v-if="assetStore.loading" indeterminate color="primary" class="mb-2" />
      <div
        v-for="asset in filteredAssets"
        :key="asset.id"
        class="list-item"
        @click="goToDetail(asset.id)"
      >
        <div class="list-cover">
          <img :src="getCoverUrl(asset)" :alt="asset.title" loading="lazy" />
        </div>
        <div class="list-info">
          <div class="list-title">{{ asset.title }}</div>
          <div class="list-meta">
            <span>{{ asset.mediaType }}</span>
            <span v-if="asset.publishYear"> · {{ asset.publishYear }}</span>
          </div>
          <div class="list-summary" v-if="asset.summary">{{ asset.summary.substring(0, 120) }}{{ asset.summary.length > 120 ? '...' : '' }}</div>
        </div>
        <div class="list-actions">
          <v-btn icon="mdi-pencil" size="small" variant="plain" @click.stop="goToDetail(asset.id)" />
        </div>
      </div>

      <div v-if="!assetStore.loading && filteredAssets.length === 0" class="empty-grid">
        <v-icon icon="mdi-folder-open-outline" size="48" color="secondary" class="mb-2" />
        <div class="text-caption text-disabled">暂无资产</div>
      </div>
    </div>

    <!-- Context Menu Overlay -->
    <Teleport to="body">
      <div v-if="contextMenuVisible" class="context-menu-overlay" @click="closeContextMenu" @contextmenu.prevent="closeContextMenu">
        <div
          class="context-menu"
          :style="{ left: contextMenuPos.x + 'px', top: contextMenuPos.y + 'px' }"
          @click.stop
        >
          <div class="ctx-item" @click="contextMenuAsset && goToDetail(contextMenuAsset.id); closeContextMenu()">
            <v-icon icon="mdi-pencil" size="14" class="mr-2" /> 查看详情
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.library-view {
  padding: 20px 24px;
  height: 100%;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* ========== Title Bar ========== */
.library-titlebar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.titlebar-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.titlebar-heading {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.titlebar-count {
  font-size: 12px;
  color: var(--text-muted);
}

.titlebar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-input {
  width: 200px;
}

.view-toggle {
  display: flex;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  overflow: hidden;
}
.view-toggle button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.15s;
}
.view-toggle button:hover {
  color: var(--text-primary);
}
.view-toggle button.active {
  background: var(--accent);
  color: #fff;
}

/* ========== Grid View ========== */
.poster-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
  align-content: start;
  flex: 1;
}

.poster-card {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.poster-card:hover {
  transform: translateY(-3px) scale(1.02);
}

.card-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 2/3;
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-elevated);
  box-shadow: var(--shadow-card);
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.6) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  padding: 8px;
}

.poster-card:hover .card-overlay {
  opacity: 1;
}

.card-more {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(4px);
  color: #fff;
  cursor: pointer;
  transition: background 0.15s;
}
.card-more:hover {
  background: rgba(255,255,255,0.3);
}

.card-title {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 2px;
}

.empty-grid {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  min-height: 50vh;
}

/* ========== List View ========== */
.asset-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.list-item:hover {
  background: var(--bg-hover);
}

.list-cover {
  width: 40px;
  height: 56px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-elevated);
}
.list-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.list-info {
  flex: 1;
  min-width: 0;
}
.list-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.list-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}
.list-summary {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.list-actions {
  flex-shrink: 0;
}

/* ========== Context Menu ========== */
.context-menu-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
}
.context-menu {
  position: fixed;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 4px;
  min-width: 160px;
  box-shadow: var(--shadow-float);
  z-index: 10000;
}
.ctx-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 5px;
  font-size: 13px;
  color: var(--text-primary);
  cursor: pointer;
  transition: background 0.12s;
}
.ctx-item:hover {
  background: var(--bg-hover);
}
</style>
