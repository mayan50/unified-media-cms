import { ref } from 'vue'

interface SnackbarItem {
  id: number
  text: string
  color?: string
  timeout?: number
}

const snackbars = ref<SnackbarItem[]>([])
const confirmResolve = ref<((v: boolean) => void) | null>(null)
const confirmVisible = ref(false)
const confirmTitle = ref('')
const confirmText = ref('')

let nextId = 0

function showSnackbar(text: string, color = 'success', timeout = 3000) {
  const id = nextId++
  snackbars.value.push({ id, text, color, timeout })
}

function removeSnackbar(id: number) {
  snackbars.value = snackbars.value.filter(s => s.id !== id)
}

function confirm(title: string, text: string): Promise<boolean> {
  confirmTitle.value = title
  confirmText.value = text
  confirmVisible.value = true
  return new Promise(resolve => {
    confirmResolve.value = resolve
  })
}

function closeConfirm(result: boolean) {
  confirmVisible.value = false
  if (confirmResolve.value) {
    confirmResolve.value(result)
    confirmResolve.value = null
  }
}

// Convenience helpers matching old ElMessage API
const toast = {
  success: (text: string) => showSnackbar(text, 'success'),
  error: (text: string) => showSnackbar(text, 'error', 5000),
  warning: (text: string) => showSnackbar(text, 'warning', 5000),
  info: (text: string) => showSnackbar(text, 'info'),
  confirm,
}

export function useToast() {
  return {
    snackbars,
    removeSnackbar,
    confirmVisible,
    confirmTitle,
    confirmText,
    closeConfirm,
    toast,
  }
}
