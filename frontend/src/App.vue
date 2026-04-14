<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from './components/AppShell.vue'
import { startAntiCheatTracking } from '@/api/antiCheat'

const router = useRouter()
let stopTracking = null

onMounted(() => {
  stopTracking = startAntiCheatTracking(() => router.currentRoute.value.path)
})

onUnmounted(() => {
  stopTracking?.()
})
</script>

<template>
  <AppShell>
    <RouterView />
  </AppShell>
</template>
