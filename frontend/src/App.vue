<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from './components/AppShell.vue'
import { startAntiCheatTracking } from '@/api/antiCheat'

const route = useRoute()
const router = useRouter()
let stopTracking = null

const hideShell = computed(() => Boolean(route.meta?.hideShell))

onMounted(() => {
  stopTracking = startAntiCheatTracking(() => router.currentRoute.value.path)
})

onUnmounted(() => {
  stopTracking?.()
})
</script>

<template>
  <RouterView v-if="hideShell" />
  <AppShell v-else>
    <RouterView />
  </AppShell>
</template>
