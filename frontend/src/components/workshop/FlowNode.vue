<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'

const props = defineProps<{
  id: string
  data: {
    label: string
    name: string
    icon: string
    description?: string
    hasConfig: boolean
    configured: boolean
    condition?: string
    error?: string
    config?: Record<string, any>
    uiSummaryKeys?: string[]
    summaryLabelMap?: Record<string, string>
  }
  selected?: boolean
}>()

const statusDot = computed(() => {
  if (props.data.error) return 'red'
  if (props.data.configured) return 'green'
  if (props.data.hasConfig) return 'yellow'
  return 'gray'
})

const summaryItems = computed(() => {
  const keys = props.data.uiSummaryKeys || []
  const config = props.data.config || {}
  const labelMap = props.data.summaryLabelMap || {}
  return keys.map(key => {
    const raw = config[key]
    const displayValue = raw && typeof raw === 'object' && 'path' in raw ? (raw as { path: string }).path : raw
    return { key, label: labelMap[key] || key, value: displayValue, configured: displayValue !== undefined && displayValue !== null && displayValue !== '' }
  })
})
</script>

<template>
  <div class="flow-node" :class="{ selected, router: data.name === 'RouterNode', error: !!data.error }">
    <Handle type="target" :position="Position.Left" class="handle" />
    <div class="node-body">
      <div class="node-header">
        <span class="node-icon">{{ data.icon }}</span>
        <span class="node-label">{{ data.label }}</span>
        <span class="node-status" :class="statusDot"></span>
      </div>
      <div v-if="data.description" class="node-description" :title="data.description">{{ data.description }}</div>
      <div v-if="data.condition" class="node-condition">IF {{ data.condition }}</div>
      <div v-if="data.error" class="node-error-hint">{{ data.error }}</div>
      <div v-if="summaryItems.length" class="node-summary">
        <div v-for="item in summaryItems" :key="item.key" class="summary-badge">
          <span class="summary-label">{{ item.label }}:</span>
          <span v-if="item.configured" class="summary-value">{{ item.value }}</span>
          <span v-else class="summary-placeholder">[未配置]</span>
        </div>
      </div>
    </div>
    <Handle type="source" :position="Position.Right" class="handle" />
  </div>
</template>

<style scoped>
.flow-node {
  background: rgb(var(--v-theme-surface));
  border: 2px solid rgb(var(--v-border-color));
  border-radius: 10px;
  min-width: 200px;
  max-width: 260px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  transition: border-color 0.15s, box-shadow 0.15s;
  cursor: default;
}
.flow-node:hover {
  border-color: rgb(var(--v-theme-primary));
}
.flow-node.selected {
  border-color: rgb(var(--v-theme-primary));
  box-shadow: 0 0 16px rgba(var(--v-theme-primary), 0.3);
}
.flow-node.router {
  border-style: dashed;
  border-color: rgb(var(--v-border-color));
}
.flow-node.router.selected {
  border-color: rgb(var(--v-theme-primary));
}
.flow-node.error {
  border-color: rgb(var(--v-theme-error));
  box-shadow: 0 0 12px rgba(var(--v-theme-error), 0.35);
}

.node-body { padding: 10px 14px; }

.node-header {
  display: flex; align-items: center; gap: 8px;
}
.node-icon { font-size: 16px; }
.node-label {
  font-size: 13px; font-weight: 600; color: rgb(var(--v-theme-on-surface)); flex: 1;
}
.node-status {
  width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
  border: 2px solid rgb(var(--v-theme-surface));
}
.node-status.green { background: rgb(var(--v-theme-success)); }
.node-status.yellow { background: rgb(var(--v-theme-warning)); }
.node-status.gray { background: rgb(var(--v-theme-secondary)); }
.node-status.red { background: rgb(var(--v-theme-error)); }

.node-description {
  margin-top: 5px; font-size: 10px;
  color: rgb(var(--v-theme-on-surface-variant));
  line-height: 1.3;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: default;
}

.node-condition {
  margin-top: 6px; padding: 4px 8px; border-radius: 4px;
  background: rgba(var(--v-theme-warning), 0.12); color: rgb(var(--v-theme-warning));
  font-size: 11px; font-family: 'JetBrains Mono', monospace;
}

.node-error-hint {
  margin-top: 4px; padding: 3px 8px; border-radius: 4px;
  background: rgba(var(--v-theme-error), 0.12); color: rgb(var(--v-theme-error));
  font-size: 10px; font-weight: 500;
}

.node-summary {
  margin-top: 8px;
  display: flex; flex-direction: column; gap: 4px;
}

.summary-badge {
  background: rgb(var(--v-theme-background));
  border-radius: 4px; padding: 3px 8px; font-size: 10px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.summary-label { color: rgb(var(--v-theme-secondary)); margin-right: 4px; }
.summary-value { color: #7ba4f7; }
.summary-placeholder { color: rgb(var(--v-theme-secondary)); font-style: italic; opacity: 0.5; }

.handle {
  width: 10px; height: 10px;
  background: rgb(var(--v-theme-primary));
  border: 2px solid rgb(var(--v-theme-surface));
}
</style>
