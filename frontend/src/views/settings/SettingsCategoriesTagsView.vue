<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCategories, getTags, createCategory, updateCategory, deleteCategory, createTag, updateTag, deleteTag } from '../../api/modules'

const categories = ref<any[]>([])
const tags = ref<any[]>([])
const loading = ref(true)
const toast = ref('')

// Category form
const catDialog = ref(false)
const catEditId = ref<string | null>(null)
const catForm = ref({ name: '', mediaType: 'BOOK' })

// Tag form
const tagDialog = ref(false)
const tagEditId = ref<string | null>(null)
const tagForm = ref({ name: '' })

async function load() {
  try {
    const [c, t] = await Promise.all([getCategories(), getTags()])
    categories.value = c.data || []
    tags.value = t.data || []
  } finally { loading.value = false }
}
onMounted(load)

function show(msg: string) { toast.value = msg; setTimeout(() => { toast.value = '' }, 2000) }

// Category CRUD
function openCatCreate() { catEditId.value = null; catForm.value = { name: '', mediaType: 'BOOK' }; catDialog.value = true }
function openCatEdit(c: any) { catEditId.value = c.id; catForm.value = { name: c.name, mediaType: c.mediaType || 'BOOK' }; catDialog.value = true }
async function saveCat() {
  try {
    if (catEditId.value) { await updateCategory(catEditId.value, catForm.value); show('已更新') }
    else { await createCategory(catForm.value); show('已创建') }
    catDialog.value = false; load()
  } catch { show('保存失败') }
}
async function delCat(id: string) {
  try { await deleteCategory(id); show('已删除'); load() }
  catch { show('删除失败：可能存在引用') }
}

// Tag CRUD
function openTagCreate() { tagEditId.value = null; tagForm.value = { name: '' }; tagDialog.value = true }
function openTagEdit(t: any) { tagEditId.value = t.id; tagForm.value = { name: t.name }; tagDialog.value = true }
async function saveTag() {
  try {
    if (tagEditId.value) { await updateTag(tagEditId.value, tagForm.value); show('已更新') }
    else { await createTag(tagForm.value); show('已创建') }
    tagDialog.value = false; load()
  } catch { show('保存失败') }
}
async function delTag(id: string) {
  try { await deleteTag(id); show('已删除'); load() }
  catch { show('删除失败：可能存在引用') }
}
</script>

<template>
  <div class="page-view">
    <div class="page-toolbar"><h3>分类与标签</h3></div>

    <v-progress-linear v-if="loading" indeterminate color="primary" />

    <div v-if="!loading" class="page-body">
      <v-row>
      <!-- Categories -->
      <v-col cols="6">
        <div class="page-card">
          <div class="page-card-top">
            <span class="page-card-name">分类</span>
            <v-btn size="small" prepend-icon="mdi-plus" variant="tonal" color="primary" @click="openCatCreate">新增</v-btn>
          </div>
          <v-table density="compact">
            <thead><tr><th>名称</th><th>媒体类型</th><th class="text-right">操作</th></tr></thead>
            <tbody>
              <tr v-for="c in categories" :key="c.id">
                <td>{{ c.name }}</td>
                <td><span class="sct-badge">{{ c.mediaType || '-' }}</span></td>
                <td class="text-right">
                  <v-btn icon="mdi-pencil" size="x-small" variant="plain" @click="openCatEdit(c)" />
                  <v-btn icon="mdi-delete" size="x-small" variant="plain" color="error" @click="delCat(c.id)" />
                </td>
              </tr>
              <tr v-if="!categories.length"><td colspan="3" class="text-center text-disabled">暂无分类</td></tr>
            </tbody>
          </v-table>
        </div>
      </v-col>

      <!-- Tags -->
      <v-col cols="6">
        <div class="page-card">
          <div class="page-card-top">
            <span class="page-card-name">标签</span>
            <v-btn size="small" prepend-icon="mdi-plus" variant="tonal" color="primary" @click="openTagCreate">新增</v-btn>
          </div>
          <v-table density="compact">
            <thead><tr><th>名称</th><th class="text-right">操作</th></tr></thead>
            <tbody>
              <tr v-for="t in tags" :key="t.id">
                <td>{{ t.name }}</td>
                <td class="text-right">
                  <v-btn icon="mdi-pencil" size="x-small" variant="plain" @click="openTagEdit(t)" />
                  <v-btn icon="mdi-delete" size="x-small" variant="plain" color="error" @click="delTag(t.id)" />
                </td>
              </tr>
              <tr v-if="!tags.length"><td colspan="2" class="text-center text-disabled">暂无标签</td></tr>
            </tbody>
          </v-table>
        </div>
      </v-col>
    </v-row>
    </div>

    <!-- Category Dialog -->
    <v-dialog v-model="catDialog" max-width="360">
      <v-card>
        <v-card-title>{{ catEditId ? '编辑分类' : '新增分类' }}</v-card-title>
        <v-card-text>
          <v-text-field v-model="catForm.name" label="名称" density="compact" hide-details class="mb-3" />
          <v-select v-model="catForm.mediaType" :items="['BOOK','COMIC','VIDEO','UNKNOWN']" label="媒体类型" density="compact" hide-details />
        </v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="catDialog=false">取消</v-btn><v-btn color="primary" @click="saveCat">保存</v-btn></v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Tag Dialog -->
    <v-dialog v-model="tagDialog" max-width="360">
      <v-card>
        <v-card-title>{{ tagEditId ? '编辑标签' : '新增标签' }}</v-card-title>
        <v-card-text>
          <v-text-field v-model="tagForm.name" label="名称" density="compact" hide-details />
        </v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="tagDialog=false">取消</v-btn><v-btn color="primary" @click="saveTag">保存</v-btn></v-card-actions>
      </v-card>
    </v-dialog>

    <div v-if="toast" class="rd-toast success">{{ toast }}</div>
  </div>
</template>

<style scoped>
.sct-badge { font-size: 10px; font-weight: 600; padding: 1px 6px; border-radius: 3px; background: rgba(124,92,252,.12); color: rgb(var(--v-theme-primary)); }
.rd-toast { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); padding: 10px 24px; border-radius: 8px; font-size: 13px; font-weight: 500; z-index: 9999; color: #fff; background: #3dd68c; }
</style>
