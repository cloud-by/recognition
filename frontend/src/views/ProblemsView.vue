<script setup>
import { onMounted, ref } from 'vue'
import { apiGet } from '@/api/http'

const loading = ref(false)
const error = ref('')
const problems = ref([])

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const resp = await apiGet('/problems')
    problems.value = resp.data || []
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section>
    <h2>题目列表</h2>
    <p v-if="loading">加载中...</p>
    <p v-else-if="error">{{ error }}</p>
    <table v-else>
      <thead>
      <tr>
        <th>ID</th><th>标题</th><th>时间限制</th><th>内存限制</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="p in problems" :key="p.id">
        <td>{{ p.id }}</td>
        <td>{{ p.title }}</td>
        <td>{{ p.timeLimitMs }}ms</td>
        <td>{{ p.memoryLimitMb }}MB</td>
      </tr>
      </tbody>
    </table>
  </section>
</template>
