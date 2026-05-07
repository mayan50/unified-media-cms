<script setup lang="ts">
import { useToast } from '../composables/useToast'

const { snackbars, removeSnackbar, confirmVisible, confirmTitle, confirmText, closeConfirm } = useToast()
</script>

<template>
  <div class="toast-container">
    <v-snackbar
      v-for="s in snackbars"
      :key="s.id"
      :model-value="true"
      :color="s.color"
      :timeout="s.timeout"
      location="top end"
      @update:model-value="removeSnackbar(s.id)"
    >
      {{ s.text }}
      <template #actions>
        <v-btn variant="text" icon="mdi-close" size="small" @click="removeSnackbar(s.id)" />
      </template>
    </v-snackbar>

    <v-dialog
      :model-value="confirmVisible"
      max-width="400"
      persistent
      @update:model-value="closeConfirm(false)"
    >
      <v-card>
        <v-card-title class="text-h6">{{ confirmTitle }}</v-card-title>
        <v-card-text>{{ confirmText }}</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="closeConfirm(false)">取消</v-btn>
          <v-btn color="primary" @click="closeConfirm(true)">确定</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9999;
  pointer-events: none;
}
</style>
