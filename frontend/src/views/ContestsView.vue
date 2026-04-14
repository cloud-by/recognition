<script setup>
import { onMounted, ref } from 'vue'
import { apiGet } from '@/api/http'

const contests = ref([])

onMounted(async () => {
  try {
    const resp = await apiGet('/contests')
    contests.value = resp.data || []
  } catch {
    contests.value = []
  }
})
</script>

<template>
  <section>
    <h2>比赛列表</h2>
    <div v-if="!contests.length">暂无比赛数据</div>
    <article v-for="contest in contests" :key="contest.id" class="card">
      <h3>{{ contest.title }}</h3>
      <p>{{ contest.contestType }} | {{ contest.startTime }} - {{ contest.endTime }}</p>
    </article>
  </section>
</template>

<style scoped>
.card { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
</style>
