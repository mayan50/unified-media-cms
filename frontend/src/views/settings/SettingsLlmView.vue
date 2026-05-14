<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLlmConfig, saveLlmConfig } from '../../api/modules'
import { useToast } from '../../composables/useToast'

const { toast } = useToast()
const config = ref({ provider: 'ollama', baseUrl: '', apiKey: '', modelName: '' })

onMounted(async () => {
  try { const { data } = await getLlmConfig(); if (data && Object.keys(data).length) config.value = { ...config.value, ...data } }
  catch { /* */ }
})

async function save() {
  try { await saveLlmConfig(config.value); toast.success('AI 配置已保存') }
  catch { toast.error('保存失败') }
}
</script>

<template>
  <div class="page-view">
    <div class="page-toolbar"><h3>AI 算力配置</h3></div>
    <div class="page-body">
      <div class="page-card">
        <div class="text-caption text-disabled mb-2">提供商</div>
        <v-radio-group v-model="config.provider" inline hide-details density="compact" class="mb-4">
          <v-radio label="Ollama" value="ollama" />
          <v-radio label="OpenAI" value="openai" />
        </v-radio-group>
        <v-text-field v-model="config.baseUrl" label="Base URL" :placeholder="config.provider === 'ollama' ? 'http://localhost:11434' : 'https://api.openai.com'" class="mb-4" />
        <v-text-field v-if="config.provider === 'openai'" v-model="config.apiKey" label="API Key" type="password" class="mb-4" />
        <v-text-field v-model="config.modelName" label="模型名称" :placeholder="config.provider === 'ollama' ? 'llama3' : 'gpt-4o-mini'" class="mb-4" />
        <v-btn color="primary" @click="save">保存配置</v-btn>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-card { max-width: 520px; }
</style>
