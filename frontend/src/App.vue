<template>
  <router-view />
  <ConfirmModal ref="confirmModal" />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ConfirmModal from './components/ConfirmModal.vue'
import { setConfirmModal } from './composables/useConfirmModal.js'

const confirmModal = ref(null)

onMounted(() => {
  if (confirmModal.value) {
    setConfirmModal(confirmModal.value)
  }
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background: linear-gradient(135deg, #fce4ec 0%, #e3f2fd 100%);
  min-height: 100vh;
}
</style>
