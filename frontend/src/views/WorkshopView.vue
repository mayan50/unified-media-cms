<script setup lang="ts">
import { ref } from 'vue'
import TaskCenter from '../components/workshop/TaskCenter.vue'
import TemplateBuilder from '../components/workshop/TemplateBuilder.vue'

const activeTab = ref<'tasks' | 'templates'>('tasks')
</script>

<template>
  <div class="workshop-view">
    <div class="workshop-tabs">
      <v-btn
        variant="plain"
        size="small"
        :color="activeTab === 'tasks' ? 'primary' : undefined"
        class="tab-btn"
        @click="activeTab = 'tasks'"
      >
        <v-icon icon="mdi-format-list-bulleted" start />
        任务看板
      </v-btn>
      <v-btn
        variant="plain"
        size="small"
        :color="activeTab === 'templates' ? 'primary' : undefined"
        class="tab-btn"
        @click="activeTab = 'templates'"
      >
        <v-icon icon="mdi-tools" start />
        模板编排
      </v-btn>
    </div>

    <div class="workshop-content">
      <TaskCenter v-if="activeTab === 'tasks'" />
      <TemplateBuilder v-else />
    </div>
  </div>
</template>

<style scoped>
.workshop-view {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.workshop-tabs {
  display: flex;
  gap: 2px;
  padding: 12px 20px 0;
  border-bottom: 1px solid rgb(var(--v-border-color));
  background: rgb(var(--v-theme-surface));
}

.tab-btn {
  text-transform: none !important;
  font-weight: 500;
  font-size: 13px;
  border-radius: 0 !important;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}

.workshop-content {
  flex: 1;
  overflow: hidden;
}
</style>
