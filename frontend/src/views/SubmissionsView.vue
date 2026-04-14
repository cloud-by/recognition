<script setup>
import { onMounted, ref } from 'vue'
import { apiGet } from '@/api/http'

const userId = 1
const submissions = ref([])
const message = ref('')

onMounted(async () => {
  try {
    const resp = await apiGet(`/submissions?userId=${userId}`)
    submissions.value = resp.data || []
  } catch {
    message.value = '当前暂无提交记录，或后端未启动。'
  }
})
</script>

<template>
  <section>
    <h2>提交记录</h2>
    <p v-if="message">{{ message }}</p>
    <ul>
      <li v-for="item in submissions" :key="item.id">
        #{{ item.id }} - Problem {{ item.problemId }} - {{ item.language }} - {{ item.judgeStatus }}
      </li>
    </ul>
  </section>
</template>
