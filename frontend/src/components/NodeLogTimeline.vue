<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'

export interface NodeLogEntry {
  id: string
  nodeName: string
  nodeLabel: string
  status: string  // PENDING / RUNNING / SUCCESS / FAILED / SKIPPED
  startTime: string | null
  endTime: string | null
  durationMs: number | null
  logOutput: string | null
  errorMessage: string | null
}

const props = defineProps<{
  nodeLogs: NodeLogEntry[]
}>()

const expandedIds = ref<Set<string>>(new Set())
const timelineEnd = ref<HTMLElement | null>(null)

watch(() => props.nodeLogs, (logs) => {
  expandedIds.value = new Set(logs.filter(l => l.logOutput || l.errorMessage).map(l => l.id))
}, { immediate: true, deep: true })

function toggleExpand(id: string) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
  expandedIds.value = new Set(expandedIds.value)
}

const statusCfg: Record<string, { label: string; color: string }> = {
  PENDING:  { label: '等待', color: '#8e92a8' },
  RUNNING:  { label: '执行中', color: '#5b8def' },
  SUCCESS:  { label: '成功', color: '#3dd68c' },
  FAILED:   { label: '失败', color: '#f06580' },
  SKIPPED:  { label: '跳过', color: '#8e92a8' },
}
function st(s: string) { return statusCfg[s] || { label: s, color: '#8e92a8' } }

function fmt(d: string | null) {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN')
}
function fmtDuration(ms: number | null) {
  if (ms == null) return '-'
  if (ms < 1000) return ms + 'ms'
  if (ms < 60000) return (ms / 1000).toFixed(1) + 's'
  return (ms / 60000).toFixed(1) + 'min'
}

interface LogLine {
  type: 'title' | 'ok' | 'warn' | 'err' | 'plain'
  text: string
}
function parseLogOutput(raw: string): LogLine[] {
  return raw.split('\n').map(line => {
    const t = line.trim()
    if (/^\d+\s{3}/.test(t)) return { type: 'title', text: t }
    if (t.startsWith('[OK]')) return { type: 'ok', text: t.substring(4).trim() }
    if (t.startsWith('[WARN]')) return { type: 'warn', text: t.substring(6).trim() }
    if (t.startsWith('[ERR]')) return { type: 'err', text: t.substring(5).trim() }
    return { type: 'plain', text: t }
  }).filter(l => l.text)
}

watch(() => props.nodeLogs?.length, async () => {
  await nextTick()
  timelineEnd.value?.scrollIntoView({ behavior: 'smooth' })
})
</script>

<template>
  <div class="nlt-timeline">
    <div v-if="!nodeLogs.length" class="nlt-empty">暂无执行日志</div>
    <div v-for="(log, i) in nodeLogs" :key="log.id" class="nlt-item">
      <div class="nlt-rail">
        <div class="nlt-dot" :style="{ background: st(log.status).color }"></div>
        <div v-if="i < nodeLogs.length - 1" class="nlt-line"></div>
      </div>
      <div class="nlt-body" @click="toggleExpand(log.id)">
        <div class="nlt-head">
          <span class="nlt-label">{{ log.nodeLabel || log.nodeName }}</span>
          <span class="nlt-status" :style="{ color: st(log.status).color }">{{ st(log.status).label }}</span>
          <span class="nlt-duration">{{ fmtDuration(log.durationMs) }}</span>
        </div>
        <div class="nlt-time">{{ fmt(log.startTime) }}</div>
        <div v-if="log.errorMessage" class="nlt-error">{{ log.errorMessage }}</div>

        <div v-if="log.logOutput" class="nlt-log" :class="{ collapsed: !expandedIds.has(log.id) }">
          <div v-for="(l, li) in parseLogOutput(log.logOutput)" :key="li" class="nlt-log-line" :class="l.type">
            <v-icon v-if="l.type==='ok'" icon="mdi-check-circle" size="14" class="nlt-log-icon" />
            <v-icon v-else-if="l.type==='warn'" icon="mdi-alert" size="14" class="nlt-log-icon" />
            <v-icon v-else-if="l.type==='err'" icon="mdi-close-circle" size="14" class="nlt-log-icon" />
            <span v-else-if="l.type==='plain'" class="nlt-log-icon" />
            <span class="nlt-log-text">{{ l.text }}</span>
          </div>
        </div>
        <div v-if="log.logOutput" class="nlt-hint" @click.stop="toggleExpand(log.id)">
          {{ expandedIds.has(log.id) ? '收起' : '展开' }}
        </div>
      </div>
    </div>
    <div ref="timelineEnd" />
  </div>
</template>

<style scoped>
.nlt-timeline {
  display: flex;
  flex-direction: column;
  max-height: 480px;
  overflow-y: auto;
}
.nlt-empty {
  text-align: center;
  padding: 24px;
  color: var(--text-muted);
  font-size: 13px;
}

.nlt-item {
  display: flex;
  gap: 12px;
}
.nlt-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 16px;
  flex-shrink: 0;
}
.nlt-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}
.nlt-line {
  width: 2px;
  flex: 1;
  background: var(--border-color, rgba(255,255,255,.08));
  margin: 4px 0;
  min-height: 20px;
}
.nlt-body {
  flex: 1;
  padding-bottom: 14px;
  min-width: 0;
  cursor: default;
}
.nlt-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.nlt-label {
  font-size: 13px;
  font-weight: 600;
}
.nlt-status {
  font-size: 11px;
  font-weight: 600;
}
.nlt-duration {
  font-size: 11px;
  color: var(--text-muted);
  font-family: 'JetBrains Mono', monospace;
}
.nlt-time {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}
.nlt-error {
  font-size: 11px;
  color: #f06580;
  margin-top: 2px;
  word-break: break-all;
}
.nlt-log {
  margin-top: 6px;
  background: rgba(0,0,0,.2);
  border-radius: 6px;
  padding: 8px 10px;
}
.nlt-log.collapsed { display: none; }
.nlt-log {
  margin-top: 4px;
  background: rgba(0,0,0,.15);
  border-radius: 6px;
  padding: 6px 8px;
  max-height: 300px;
  overflow-y: auto;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
}
.nlt-log-line {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  padding: 1px 0;
  line-height: 1.5;
}
.nlt-log-line.title { font-weight: 700; color: var(--text-primary); padding-top: 4px; }
.nlt-log-line.title:not(:first-child) { margin-top: 6px; }
.nlt-log-icon { flex-shrink: 0; margin-top: 1px; }
.nlt-log-text { color: var(--text-secondary); word-break: break-all; white-space: pre-wrap; }
.nlt-log-line.ok .nlt-log-text { color: #3dd68c; }
.nlt-log-line.warn .nlt-log-text { color: #f0a840; }
.nlt-log-line.err .nlt-log-text { color: #f06580; }
.nlt-log-line.ok :deep(.nlt-log-icon) { color: #3dd68c !important; }
.nlt-log-line.warn :deep(.nlt-log-icon) { color: #f0a840 !important; }
.nlt-log-line.err :deep(.nlt-log-icon) { color: #f06580 !important; }
.nlt-hint {
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 2px;
  user-select: none;
  cursor: pointer;
}
</style>
